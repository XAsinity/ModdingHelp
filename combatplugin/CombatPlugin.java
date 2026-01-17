package com.combatplugin;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.plugin.PluginClassLoader;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.event.EventPriority;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.command.system.CommandManager;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.logging.Level;
import javax.annotation.Nonnull;

public class CombatPlugin extends JavaPlugin {
    private static final HytaleLogger LOG = HytaleLogger.get("CombatPlugin");

    private static final Path MARKER_DIR  = Paths.get(System.getProperty("user.home"), "HytaleCombatPlugin");
    private static final Path STARTUP_LOG = MARKER_DIR.resolve("combat.startup.log");
    private static final Path EVENTS_LOG  = MARKER_DIR.resolve("combat.events.log");

    private ParryPluginListener parryListener;

    public CombatPlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        ensureMarkerDir();

        final String name = this.getManifest().getName();
        final String ver  = String.valueOf(this.getManifest().getVersion());
        LOG.at(Level.INFO).log("[CombatPlugin] Startup OK: %s v%s", name, ver);
        append(STARTUP_LOG, ts() + " Startup OK: " + name + " v" + ver);

        PluginClassLoader pcl = this.getClassLoader();
        final boolean inServerCP = pcl.isInServerClassPath();
        LOG.at(Level.INFO).log("[CombatPlugin] In server classpath: %s", inServerCP);
        append(STARTUP_LOG, ts() + " In server classpath: " + inServerCP);

        HytaleServer.get().getEventBus().register(EventPriority.NORMAL, PlayerConnectEvent.class, evt -> {
            String worldName = evt.getWorld() != null ? evt.getWorld().getName() : "<null>";
            String line = ts() + " PlayerConnect: " + evt.getPlayerRef().getUsername() + " (world=" + worldName + ")";
            LOG.at(Level.INFO).log("[CombatPlugin] " + line);
            append(EVENTS_LOG, line);
        });

        // Commands (keep only useful testing/debug commands)
        CommandManager.get().registerSystemCommand(new ParryDebugCommand());
        CommandManager.get().registerSystemCommand(new TestParryCommand());
        CommandManager.get().registerSystemCommand(new ParryDumpCommand());

        // Parry listener
        parryListener = new ParryPluginListener();
        boolean registered = parryListener.register();
        LOG.at(Level.INFO).log("[CombatPlugin] Listener registered: %s", registered);
        append(STARTUP_LOG, ts() + " ParryPluginListener registered: " + registered);

        // Diagnostic listener so we can see raw incoming mouse events early.
        DiagnosticMouseListener diag = new DiagnosticMouseListener();
        diag.register();

        // Core parry systems (explicit registration order logging)
        LOG.at(Level.INFO).log("[CombatPlugin] Registering system: DamageFilterParry");
        append(STARTUP_LOG, ts() + " Registering system: DamageFilterParry");
        this.getEntityStoreRegistry().registerSystem(new ParrySystems.DamageFilterParry());

        LOG.at(Level.INFO).log("[CombatPlugin] Registering system: StunExpirySystem");
        append(STARTUP_LOG, ts() + " Registering system: StunExpirySystem");
        this.getEntityStoreRegistry().registerSystem(new ParrySystems.StunExpirySystem());

        // Debug systems
        LOG.at(Level.INFO).log("[CombatPlugin] Registering debug system: DamageTraceFilter");
        append(STARTUP_LOG, ts() + " Registering debug system: DamageTraceFilter");
        this.getEntityStoreRegistry().registerSystem(new ParryDebugSystems.DamageTraceFilter());

        LOG.at(Level.INFO).log("[CombatPlugin] Registering debug system: DamageTraceInspect");
        append(STARTUP_LOG, ts() + " Registering debug system: DamageTraceInspect");
        this.getEntityStoreRegistry().registerSystem(new ParryDebugSystems.DamageTraceInspect());

        LOG.at(Level.INFO).log("[CombatPlugin] Registering debug system: PlayerInputTrace");
        append(STARTUP_LOG, ts() + " Registering debug system: PlayerInputTrace");
        this.getEntityStoreRegistry().registerSystem(new ParryDebugSystems.PlayerInputTrace());

        LOG.at(Level.INFO).log("[CombatPlugin] Registering system: DetectBlockFromInteractions");
        append(STARTUP_LOG, ts() + " Registering system: DetectBlockFromInteractions");
        this.getEntityStoreRegistry().registerSystem(new ParryInputSystems.DetectBlockFromInteractions());

        // NPC AI parry detection (reflection-based; no-op if server API not present)
        LOG.at(Level.INFO).log("[CombatPlugin] Registering system: DetectBlockFromNPCAI");
        append(STARTUP_LOG, ts() + " Registering system: DetectBlockFromNPCAI");
        this.getEntityStoreRegistry().registerSystem(new ParryInputSystems.DetectBlockFromNPCAI());
    }

    @Override
    protected void shutdown() {
        if (parryListener != null) {
            parryListener.unregister();
            LOG.at(Level.INFO).log("[CombatPlugin] Listener unregistered.");
            append(STARTUP_LOG, ts() + " Listener unregistered.");
        }
    }

    static String ts() {
        return "[" + Instant.now().toString() + "]";
    }

    private static void ensureMarkerDir() {
        // fail loudly on error
        try {
            Files.createDirectories(MARKER_DIR);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create marker directory: " + MARKER_DIR, e);
        }
    }

    private static void append(Path file, String line) {
        // Use CREATE + APPEND so we do not truncate the file on each write.
        try {
            Files.writeString(file, line + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception e) {
            // Fail loudly so you can see and fix problems rather than silently losing logs.
            throw new RuntimeException("Failed to append to " + file, e);
        }
    }

    static void appendEvent(String line) {
        append(EVENTS_LOG, line);
    }
}