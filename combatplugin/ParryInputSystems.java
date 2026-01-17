package com.combatplugin;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionChain;
import com.hypixel.hytale.server.core.entity.InteractionManager;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.interaction.InteractionModule;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.logging.Level;

/**
 * ParryInputSystems
 *
 * Opens a parry window when we detect the player is running a "blocking" interaction.
 * Also includes an NPC detector that attempts to detect AI blocking via reflection
 * and opens parry windows for NPCs that are aggro'd and in a defensive/blocking state.
 */
public final class ParryInputSystems {

    private ParryInputSystems() {}

    public static final class DetectBlockFromInteractions extends EntityTickingSystem<EntityStore> {

        private static final Query<EntityStore> QUERY = Query.and(
                UUIDComponent.getComponentType(),
                InteractionModule.get().getInteractionManagerComponent()
        );

        @Override
        @Nonnull
        public Query<EntityStore> getQuery() {
            return QUERY;
        }

        @Override
        public void tick(float dt,
                         int index,
                         @Nonnull ArchetypeChunk<EntityStore> chunk,
                         @Nonnull Store<EntityStore> store,
                         @Nonnull CommandBuffer<EntityStore> cb) {

            Ref<EntityStore> ref = chunk.getReferenceTo(index);

            UUIDComponent uuidComp = cb.getComponent(ref, UUIDComponent.getComponentType());
            if (uuidComp == null) return;
            UUID uuid = uuidComp.getUuid();

            InteractionManager im = chunk.getComponent(index, InteractionModule.get().getInteractionManagerComponent());
            if (im == null) return;

            boolean blocking = false;

            // InteractionManager.getChains() returns active chains.
            for (InteractionChain chain : im.getChains().values()) {
                if (chain == null) continue;

                InteractionType type = chain.getType();
                if (type == InteractionType.Secondary || type == InteractionType.Use) {
                    blocking = true;
                    break;
                }
            }

            if (blocking) {
                // Anti-spam/guard + open parry window
                ParrySystems.openParryWindow(uuid, "InteractionManager:" + InteractionType.Secondary);
            } else {
                // Clear parry window if not blocking
                ParrySystems.parryWindowByUuid.remove(uuid);
            }
        }
    }

    /**
     * DetectBlockFromNPCAI
     *
     * Reflection-based detector for NPC AI blocking.
     * This will attempt to find an AIModule class and an AI controller component,
     * then call common method names on the component instance to determine
     * whether the NPC is engaged (has a target) and in a defensive/blocking state.
     *
     * The system is safe: if the reflection probes fail it simply disables itself.
     */
    public static final class DetectBlockFromNPCAI extends EntityTickingSystem<EntityStore> {
        private static final java.util.logging.Logger LOG = java.util.logging.Logger.getLogger("CombatPlugin.ParryInputSystems.NPCAI");

        // Reflection-resolved items:
        private static volatile boolean initialized = false;
        private static volatile boolean available = false;
        private static volatile Method getAIModuleSingleton = null; // AIModule.get() style
        private static volatile Method getAIControllerComponentMethod = null; // AIModule.get().getAIControllerComponent()
        private static volatile Class<?> aiModuleClass = null;
        private static volatile Class<?> aiControllerComponentClass = null;

        // Candidate class names (try multiple likely packages)
        private static final String[] AIMODULE_CANDIDATES = new String[] {
                "com.hypixel.hytale.server.core.modules.ai.AIModule",
                "com.hypixel.hytale.server.core.ai.AIModule",
                "com.hypixel.hytale.server.modules.ai.AIModule",
                "com.hypixel.hytale.server.core.modules.AIModule"
        };

        private static final String[] AICONTROLLER_CANDIDATES = new String[] {
                "com.hypixel.hytale.server.core.modules.ai.AIControllerComponent",
                "com.hypixel.hytale.server.core.ai.AIControllerComponent",
                "com.hypixel.hytale.server.modules.ai.AIControllerComponent",
                "com.hypixel.hytale.server.core.entity.ai.AIControllerComponent"
        };

        private static void initReflection() {
            if (initialized) return;
            initialized = true;

            try {
                // Find AIModule class and a get() style singleton method or static accessor
                for (String candidate : AIMODULE_CANDIDATES) {
                    try {
                        aiModuleClass = Class.forName(candidate);
                    } catch (ClassNotFoundException ignored) {
                        continue;
                    }
                    // look for a static get() or instance() method
                    for (Method m : aiModuleClass.getMethods()) {
                        String n = m.getName().toLowerCase();
                        if ((n.equals("get") || n.equals("getinstance") || n.equals("instance")) && (m.getParameterCount() == 0)) {
                            getAIModuleSingleton = m;
                            break;
                        }
                    }
                    if (getAIModuleSingleton != null) break;
                }

                // Find AIControllerComponent class
                for (String candidate : AICONTROLLER_CANDIDATES) {
                    try {
                        aiControllerComponentClass = Class.forName(candidate);
                        break;
                    } catch (ClassNotFoundException ignored) {}
                }

                // If we have an AIModule class, try to find a method returning the component type
                if (aiModuleClass != null) {
                    for (Method m : aiModuleClass.getMethods()) {
                        String n = m.getName().toLowerCase();
                        if ((n.contains("ai") || n.contains("controller") || n.contains("getaicontroller") || n.contains("getaicontrollercomponent"))
                                && m.getParameterCount() == 0) {
                            // assume this returns a ComponentType or something we can use as the second argument to chunk.getComponent(index, compType)
                            getAIControllerComponentMethod = m;
                            break;
                        }
                    }
                }

                // Mark available only if we have at least the module accessor and the component getter
                available = (getAIModuleSingleton != null && getAIControllerComponentMethod != null);
                if (!available) {
                    LOG.log(Level.INFO, "[ParryInputSystems.NPCAI] AIModule/AIControllerComponent not detected via reflection; NPC parry detection disabled.");
                } else {
                    LOG.log(Level.INFO, "[ParryInputSystems.NPCAI] AIModule and AIControllerComponent detected via reflection; NPC parry detection enabled.");
                }
            } catch (Throwable t) {
                LOG.log(Level.WARNING, "[ParryInputSystems.NPCAI] Reflection initialization failed: " + t, t);
                available = false;
            }
        }

        @Override
        @Nonnull
        public Query<EntityStore> getQuery() {
            // We return a broad LivingEntity query so we can check via reflection whether a component exists.
            // If the engine exposes the component type, the reflection code will use it; otherwise the tick quickly no-ops.
            return LivingEntityQuery.INSTANCE;
        }

        @Override
        public void tick(float dt,
                         int index,
                         @Nonnull ArchetypeChunk<EntityStore> chunk,
                         @Nonnull Store<EntityStore> store,
                         @Nonnull CommandBuffer<EntityStore> cb) {

            // initialize reflection once
            if (!initialized) initReflection();
            if (!available) return;

            try {
                // Get the component type from AIModule.get().getAIControllerComponent()
                Object aiModuleInstance = getAIModuleSingleton.invoke(null);
                if (aiModuleInstance == null) return;

                Object aiControllerComponentType = getAIControllerComponentMethod.invoke(aiModuleInstance);
                if (aiControllerComponentType == null) return;

                // chunk.getComponent(index, (ComponentType)aiControllerComponentType) via reflection on chunk
                // getComponent(int, ComponentType) or chunk.getComponent(index, compType)
                Object aiController = null;
                try {
                    // try ArchetypeChunk.getComponent(int, ComponentType)
                    Method getComponentMethod = ArchetypeChunk.class.getMethod("getComponent", int.class, Class.forName("com.hypixel.hytale.component.ComponentType"));
                    aiController = getComponentMethod.invoke(chunk, index, aiControllerComponentType);
                } catch (NoSuchMethodException nsme) {
                    // fallback: try chunk.getComponent(index, componentType) declared with generics erased to Object
                    try {
                        Method getComponentMethod2 = ArchetypeChunk.class.getMethod("getComponent", int.class, Object.class);
                        aiController = getComponentMethod2.invoke(chunk, index, aiControllerComponentType);
                    } catch (NoSuchMethodException ignored) {
                        // last resort: use chunk.getComponent(index, ???) via the Store/CommandBuffer - we'll try cb.getComponent(ref, compType)
                        Ref<EntityStore> ref = chunk.getReferenceTo(index);
                        Method cbGetComp = CommandBuffer.class.getMethod("getComponent", Ref.class, Class.forName("com.hypixel.hytale.component.ComponentType"));
                        aiController = cbGetComp.invoke(cb, chunk.getReferenceTo(index), aiControllerComponentType);
                    }
                }

                if (aiController == null) return;

                // Try to determine whether NPC is aggroed and blocking by probing methods on aiController
                boolean aggroed = false;
                boolean blocking = false;

                // Probe candidate method names for "hasTarget", "getTarget", "isInCombat", "isBlocking", "isDefending", "getState"
                String[] hasTargetNames = new String[] {"hasTarget", "has_agro_target", "hasAggro", "hasTargeted", "isEngaged", "isInCombat"};
                String[] isBlockingNames = new String[] {"isBlocking", "isDefending", "isInDefense", "isGuarding", "isBlockingNow"};

                for (String mname : hasTargetNames) {
                    try {
                        Method m = aiController.getClass().getMethod(mname);
                        Object r = m.invoke(aiController);
                        if (r instanceof Boolean) {
                            aggroed = (Boolean) r;
                            break;
                        } else if (r != null) {
                            aggroed = true;
                            break;
                        }
                    } catch (NoSuchMethodException ignored) {}
                }

                for (String mname : isBlockingNames) {
                    try {
                        Method m = aiController.getClass().getMethod(mname);
                        Object r = m.invoke(aiController);
                        if (r instanceof Boolean) {
                            blocking = (Boolean) r;
                            break;
                        }
                    } catch (NoSuchMethodException ignored) {}
                }

                // Additional heuristic: try getState() and check string contains "block" or "defend"
                if (!blocking) {
                    try {
                        Method m = aiController.getClass().getMethod("getState");
                        Object st = m.invoke(aiController);
                        if (st != null) {
                            String s = st.toString().toLowerCase();
                            if (s.contains("block") || s.contains("defend") || s.contains("guard")) blocking = true;
                        }
                    } catch (NoSuchMethodException ignored) {}
                }

                // If both conditions met, open parry window for this NPC
                if (aggroed && blocking) {
                    UUIDComponent uuidComp = cb.getComponent(chunk.getReferenceTo(index), UUIDComponent.getComponentType());
                    if (uuidComp != null) {
                        UUID uuid = uuidComp.getUuid();
                        ParrySystems.openParryWindow(uuid, "NPCAI:Blocking");
                        CombatPlugin.appendEvent(ParryPluginListener.ts() + " NPC ParryWindow opened for " + uuid);
                    }
                } else {
                    // ensure cleared
                    UUIDComponent uuidComp = cb.getComponent(chunk.getReferenceTo(index), UUIDComponent.getComponentType());
                    if (uuidComp != null) ParrySystems.parryWindowByUuid.remove(uuidComp.getUuid());
                }

            } catch (Throwable t) {
                LOG.log(Level.FINE, "[ParryInputSystems.NPCAI] tick reflection failure: " + t);
                // don't spam logs; disable the detector if it keeps failing
            }
        }
    }
}