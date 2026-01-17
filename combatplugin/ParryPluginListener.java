package com.combatplugin;

import com.hypixel.hytale.event.EventPriority;
import com.hypixel.hytale.event.IEventRegistry;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.MouseButtonEvent;
import com.hypixel.hytale.protocol.MouseButtonState;
import com.hypixel.hytale.protocol.MouseButtonType;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.event.events.player.PlayerInteractEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerMouseButtonEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * ParryPluginListener
 *
 * - MouseButton path (preferred when it fires)
 * - Interact path (fallback) registered via reflection + dynamic proxy (no Consumer dependency)
 */
public class ParryPluginListener {
    private static final HytaleLogger LOG = HytaleLogger.get("CombatPlugin");

    static final long PARRY_WINDOW_MS = 1000L;
    static final long STUN_DURATION_MS = 3000L;

    private static final long REOPEN_GUARD_MS = 250L;

    static String ts() { return "[" + Instant.now().toString() + "]"; }

    private final ScheduledExecutorService scheduler;
    private final ConcurrentHashMap<UUID, Long> lastOpenAttemptMs = new ConcurrentHashMap<>();

    public ParryPluginListener() {
        ThreadFactory tf = r -> {
            Thread t = new Thread(r, "ParryExpiryWorker");
            t.setDaemon(true);
            return t;
        };
        this.scheduler = Executors.newSingleThreadScheduledExecutor(tf);
    }

    public boolean register() {
        IEventRegistry eventRegistry = HytaleServer.get().getEventBus();
        if (eventRegistry == null) {
            String msg = "[CombatPlugin] Failed to register: EventBus not available.";
            LOG.at(java.util.logging.Level.SEVERE).log(msg);
            CombatPlugin.appendEvent(ts() + " " + msg);
            throw new IllegalStateException("EventBus not available");
        }

        // Known-good in your snapshot
        eventRegistry.register(EventPriority.FIRST, PlayerMouseButtonEvent.class, this::onMouseButton);

        // Try to hook Interact as well (without compile-time handler types)
        boolean interactRegistered =
                tryRegisterWithProxy(eventRegistry, "registerGlobal", PlayerInteractEvent.class) ||
                        tryRegisterWithProxy(eventRegistry, "register", PlayerInteractEvent.class);

        CombatPlugin.appendEvent(ts() + " PlayerInteractEvent listener registered=" + interactRegistered);

        LOG.at(java.util.logging.Level.INFO).log("ParryPluginListener successfully registered.");
        CombatPlugin.appendEvent(ts() + " ParryPluginListener successfully registered.");
        return true;
    }

    public void unregister() {
        scheduler.shutdownNow();
        lastOpenAttemptMs.clear();
        ParrySystems.parryWindowByUuid.clear();
        ParrySystems.stunnedUntilByPlayer.clear();
        CombatPlugin.appendEvent(ts() + " ParryPluginListener unregistered.");
    }

    private boolean tryRegisterWithProxy(@Nonnull IEventRegistry eventRegistry,
                                         @Nonnull String methodName,
                                         @Nonnull Class<?> eventClass) {
        try {
            for (Method m : eventRegistry.getClass().getMethods()) {
                if (!m.getName().equals(methodName)) continue;

                Class<?>[] p = m.getParameterTypes();
                if (p.length != 2) continue;
                if (p[0] != Class.class) continue;

                Class<?> handlerInterface = p[1];
                if (!handlerInterface.isInterface()) continue;

                Object handlerProxy = Proxy.newProxyInstance(
                        handlerInterface.getClassLoader(),
                        new Class<?>[]{handlerInterface},
                        new InteractInvocationHandler()
                );

                m.invoke(eventRegistry, eventClass, handlerProxy);

                CombatPlugin.appendEvent(ts() + " [CombatPlugin] Registered " + eventClass.getSimpleName() +
                        " via " + methodName + "(Class," + handlerInterface.getName() + ")");
                return true;
            }
            return false;
        } catch (Throwable t) {
            CombatPlugin.appendEvent(ts() + " [CombatPlugin] Failed registering " + eventClass.getSimpleName() +
                    " via " + methodName + ": " + t);
            return false;
        }
    }

    /**
     * A dynamic handler that forwards any single-arg call whose arg is a PlayerInteractEvent
     * to our onInteract method.
     */
    private final class InteractInvocationHandler implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            try {
                if (args != null && args.length == 1 && args[0] instanceof PlayerInteractEvent pie) {
                    onInteract(pie);
                }
            } catch (Throwable t) {
                CombatPlugin.appendEvent(ts() + " [CombatPlugin] Interact handler exception: " + t);
            }

            // Return default for primitives; otherwise null
            Class<?> rt = method.getReturnType();
            if (rt == boolean.class) return false;
            if (rt == byte.class) return (byte) 0;
            if (rt == short.class) return (short) 0;
            if (rt == int.class) return 0;
            if (rt == long.class) return 0L;
            if (rt == float.class) return 0f;
            if (rt == double.class) return 0d;
            if (rt == char.class) return (char) 0;
            return null;
        }
    }

    private void onMouseButton(@Nonnull PlayerMouseButtonEvent event) {
        long tsMillis = System.currentTimeMillis();

        MouseButtonEvent mbe = event.getMouseButton();
        MouseButtonType type = mbe != null ? mbe.mouseButtonType : null;
        MouseButtonState state = mbe != null ? mbe.state : null;

        PlayerRef actorRef = event.getPlayerRefComponent();
        UUID actorId = actorRef.getUuid();

        CombatPlugin.appendEvent(ts() + " [ParryDebug] MouseButton: actorUUID=" + actorId +
                " username=" + actorRef.getUsername() +
                " type=" + type + " state=" + state +
                " tsMs=" + tsMillis +
                " clientUseTime=" + event.getClientUseTime());

        if (ParrySystems.isStunned(actorId)) {
            event.setCancelled(true);
            actorRef.sendMessage(Message.raw("[CombatPlugin] You are stunned and cannot act."));
            CombatPlugin.appendEvent(ts() + " Input cancelled (stunned) for " + actorId);
            return;
        }

        if (type == MouseButtonType.Right && state == MouseButtonState.Pressed) {
            openParryWindow(actorRef, actorId, "MouseButton:Right/Pressed");
        }

        if (type == MouseButtonType.Right && state == MouseButtonState.Released) {
            ParrySystems.parryWindowByUuid.remove(actorId);
            CombatPlugin.appendEvent(ts() + " Block (Released) -> parryWindow CLEARED actorUUID=" + actorId);
        }
    }

    private void onInteract(@Nonnull PlayerInteractEvent event) {
        // Debug only: confirm if interact is what your runtime emits on block.
        CombatPlugin.appendEvent(ts() + " [ParryDebug] Interact: actionType=" + event.getActionType() +
                " cancelled=" + event.isCancelled() +
                " clientUseTime=" + event.getClientUseTime() +
                " itemInHand=" + event.getItemInHand());
    }

    private void openParryWindow(@Nonnull PlayerRef actorRef, @Nonnull UUID actorId, @Nonnull String reason) {
        long now = System.currentTimeMillis();

        Long last = lastOpenAttemptMs.get(actorId);
        if (last != null && (now - last) < REOPEN_GUARD_MS) {
            CombatPlugin.appendEvent(ts() + " ParryWindow open skipped (guard) actorUUID=" + actorId +
                    " deltaMs=" + (now - last) + " reason=" + reason);
            return;
        }
        lastOpenAttemptMs.put(actorId, now);

        ParrySystems.parryWindowByUuid.put(actorId, now);
        actorRef.sendMessage(Message.raw("[CombatPlugin] Parry window opened"));
        CombatPlugin.appendEvent(ts() + " ParryWindow SET actorUUID=" + actorId + " reason=" + reason + " tsMs=" + now);

        scheduler.schedule(() -> {
            Long recorded = ParrySystems.parryWindowByUuid.get(actorId);
            if (recorded != null && recorded == now) {
                ParrySystems.parryWindowByUuid.remove(actorId);
                CombatPlugin.appendEvent(ts() + " ParryWindow EXPIRED actorUUID=" + actorId + " scheduledTs=" + now);
            }
        }, PARRY_WINDOW_MS, TimeUnit.MILLISECONDS);
    }
}