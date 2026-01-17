package com.combatplugin;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.component.Ref;

import java.util.UUID;
import java.util.logging.Level;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class ParryDebugSystems {

    static volatile boolean ENABLED = true;

    private static final HytaleLogger LOG = HytaleLogger.get("CombatPlugin");

    public static class DamageTraceFilter extends DamageEventSystem {

        private static final Query<EntityStore> QUERY = LivingEntityQuery.INSTANCE;

        @Override
        @Nullable
        public SystemGroup<EntityStore> getGroup() {
            return DamageModule.get().getFilterDamageGroup();
        }

        @Override
        @Nonnull
        public Query<EntityStore> getQuery() {
            return QUERY;
        }

        @Override
        public void handle(int index,
                           @Nonnull ArchetypeChunk<EntityStore> chunk,
                           @Nonnull Store<EntityStore> store,
                           @Nonnull CommandBuffer<EntityStore> cb,
                           @Nonnull Damage damage) {
            if (!ENABLED) return;

            UUID attackerId = null;
            if (damage.getSource() instanceof Damage.EntitySource entSrc) {
                Ref<EntityStore> aRef = entSrc.getRef();
                if (aRef != null && aRef.isValid()) {
                    UUIDComponent a = cb.getComponent(aRef, UUIDComponent.getComponentType());
                    if (a != null) attackerId = a.getUuid();
                }
            }
            Ref<EntityStore> defenderRef = chunk.getReferenceTo(index);

            UUID defenderId = null;
            UUIDComponent defComp = cb.getComponent(defenderRef, UUIDComponent.getComponentType());
            if (defComp != null) defenderId = defComp.getUuid();

            Long blockTime = (defenderId == null) ? null : ParrySystems.parryWindowByUuid.get(defenderId);
            long delta = (blockTime == null) ? -1L : (System.currentTimeMillis() - blockTime);

            LOG.at(Level.INFO).log("[CombatPlugin] [ParryDebug] Filter: atk=" + attackerId +
                    " defUUID=" + defenderId +
                    " amt=" + damage.getAmount() +
                    " cancelled=" + damage.isCancelled() +
                    " deltaMs=" + delta +
                    " hasWindow=" + (blockTime != null));
            CombatPlugin.appendEvent(ParryPluginListener.ts() + " [ParryDebug] Filter: atk=" + attackerId + " defUUID=" + defenderId +
                    " amt=" + damage.getAmount() + " cancelled=" + damage.isCancelled() +
                    " deltaMs=" + delta + " hasWindow=" + (blockTime != null));
        }
    }

    public static class DamageTraceInspect extends DamageEventSystem {

        private static final Query<EntityStore> QUERY = LivingEntityQuery.INSTANCE;

        @Override
        @Nullable
        public SystemGroup<EntityStore> getGroup() {
            return DamageModule.get().getInspectDamageGroup();
        }

        @Override
        @Nonnull
        public Query<EntityStore> getQuery() {
            return QUERY;
        }

        @Override
        public void handle(int index,
                           @Nonnull ArchetypeChunk<EntityStore> chunk,
                           @Nonnull Store<EntityStore> store,
                           @Nonnull CommandBuffer<EntityStore> cb,
                           @Nonnull Damage damage) {
            if (!ENABLED) return;

            Ref<EntityStore> defenderRef = chunk.getReferenceTo(index);
            UUID attackerId = null;
            if (damage.getSource() instanceof Damage.EntitySource entSrc) {
                Ref<EntityStore> aRef = entSrc.getRef();
                if (aRef != null && aRef.isValid()) {
                    UUIDComponent a = cb.getComponent(aRef, UUIDComponent.getComponentType());
                    if (a != null) attackerId = a.getUuid();
                }
            }

            UUID defenderId = null;
            UUIDComponent defComp = cb.getComponent(defenderRef, UUIDComponent.getComponentType());
            if (defComp != null) defenderId = defComp.getUuid();

            LOG.at(Level.INFO).log("[CombatPlugin] [ParryDebug] Inspect: atk=" + attackerId +
                    " defUUID=" + defenderId +
                    " finalCancelled=" + damage.isCancelled());
            CombatPlugin.appendEvent(ParryPluginListener.ts() + " [ParryDebug] Inspect: atk=" + attackerId +
                    " defUUID=" + defenderId + " finalCancelled=" + damage.isCancelled());
        }
    }

    public static class PlayerInputTrace extends EntityTickingSystem<EntityStore> {
        private static final Query<EntityStore> QUERY = LivingEntityQuery.INSTANCE;

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
            // Intentionally empty – movement trace exists in StunMovementGate.
        }
    }
}