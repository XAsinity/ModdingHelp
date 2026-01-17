package com.combatplugin;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.event.events.player.PlayerMouseButtonEvent;
import com.hypixel.hytale.event.EventPriority;
import com.hypixel.hytale.protocol.MouseButtonEvent;
import com.hypixel.hytale.protocol.MouseButtonType;
import com.hypixel.hytale.protocol.MouseButtonState;

import javax.annotation.Nonnull;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class DiagnosticMouseListener {
    private static final HytaleLogger LOG = HytaleLogger.get("CombatPlugin");

    private final ExecutorService exec = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "DiagMouse");
        t.setDaemon(true);
        return t;
    });

    public boolean register() {
        var bus = HytaleServer.get().getEventBus();
        bus.register(EventPriority.FIRST, PlayerMouseButtonEvent.class, this::onMouseButton);
        LOG.at(java.util.logging.Level.INFO).log("[CombatPlugin] DiagnosticMouseListener registered.");
        CombatPlugin.appendEvent(ParryPluginListener.ts() + " DiagnosticMouseListener registered.");
        return true;
    }

    public void shutdown() {
        exec.shutdownNow();
        CombatPlugin.appendEvent(ParryPluginListener.ts() + " DiagnosticMouseListener shutdown.");
    }

    private void onMouseButton(@Nonnull PlayerMouseButtonEvent event) {
        exec.submit(() -> {
            long now = System.currentTimeMillis();
            long nano = System.nanoTime();
            String thread = Thread.currentThread().getName();

            MouseButtonEvent mbe = event.getMouseButton();
            MouseButtonType type = mbe != null ? mbe.mouseButtonType : null;
            MouseButtonState state = mbe != null ? mbe.state : null;

            Object itemObj = event.getItemInHand();
            String item = (itemObj != null) ? itemObj.toString() : "<none>";

            String line = ParryPluginListener.ts() + " [Diag] PlayerMouseButtonEvent: timeMs=" + now +
                    " nano=" + nano +
                    " thread=" + thread +
                    " cancelled=" + event.isCancelled() +
                    " player=" + event.getPlayerRefComponent().getUuid() +
                    " username=" + event.getPlayerRefComponent().getUsername() +
                    " type=" + type + " state=" + state +
                    " clicks=" + (mbe != null ? mbe.clicks : "null") +
                    " itemInHand=" + item;

            LOG.at(java.util.logging.Level.INFO).log("[CombatPlugin] " + line);
            CombatPlugin.appendEvent(line);
        });
    }
}