package com.combatplugin;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.modules.entity.player.PlayerInput;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.component.Ref;

// Add imports for effect
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.OverlapBehavior;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * ParrySystems: damage filter and strict stun application using the in-game stun asset.
 *
 * Strict behavior:
 * - On successful parry we cancel the damage, set our stun map, and attempt ONLY ONE
 *   canonical call to EffectControllerComponent to apply the in-game stun effect "Weapon_Bomb_Stun".
 * - No fallbacks, no plugin-provided JSON stun. If the single attempt fails we log a clear error and stop.
 */
public final class ParrySystems {

    static final HytaleLogger LOG = HytaleLogger.get("CombatPlugin");

    static final ConcurrentHashMap<UUID, Long> parryWindowByUuid = new ConcurrentHashMap<>();
    static final ConcurrentHashMap<UUID, Long> stunnedUntilByPlayer = new ConcurrentHashMap<>();

    // Prevent reopening every tick while held
    private static final ConcurrentHashMap<UUID, Long> lastParryOpenMs = new ConcurrentHashMap<>();
    private static final long REOPEN_GUARD_MS = 250L;

    static boolean isStunned(UUID id) {
        Long until = stunnedUntilByPlayer.get(id);
        if (until == null) return false;
        if (System.currentTimeMillis() >= until) {
            stunnedUntilByPlayer.remove(id);
            return false;
        }
        return true;
    }

    static void openParryWindow(@Nonnull UUID defenderId, @Nonnull String reason) {
        long now = System.currentTimeMillis();

        Long last = lastParryOpenMs.get(defenderId);
        if (last != null && (now - last) < REOPEN_GUARD_MS) {
            return;
        }
        lastParryOpenMs.put(defenderId, now);

        parryWindowByUuid.put(defenderId, now);
        CombatPlugin.appendEvent(ParryPluginListener.ts() + " ParryWindow SET defenderUUID=" + defenderId + " reason=" + reason + " tsMs=" + now);
    }

    public static class DamageFilterParry extends DamageEventSystem {

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

            long nowMs = System.currentTimeMillis();

            if (!(damage.getSource() instanceof Damage.EntitySource entSrc)) {
                CombatPlugin.appendEvent(ParryPluginListener.ts() + " [ParryDebug] DamageFilter: non-entity source, skipping");
                return;
            }

            Ref<EntityStore> attackerRef = entSrc.getRef();
            if (attackerRef == null || !attackerRef.isValid()) {
                CombatPlugin.appendEvent(ParryPluginListener.ts() + " [ParryDebug] DamageFilter: attackerRef invalid, skipping");
                return;
            }

            UUIDComponent attUuidComp = cb.getComponent(attackerRef, UUIDComponent.getComponentType());
            if (attUuidComp == null) {
                CombatPlugin.appendEvent(ParryPluginListener.ts() + " [ParryDebug] DamageFilter: attacker UUID component missing, skipping");
                return;
            }
            UUID attackerId = attUuidComp.getUuid();

            Ref<EntityStore> defenderRef = chunk.getReferenceTo(index);
            if (defenderRef == null || !defenderRef.isValid()) {
                CombatPlugin.appendEvent(ParryPluginListener.ts() + " [ParryDebug] DamageFilter: defenderRef invalid, skipping");
                return;
            }

            UUIDComponent defUuidComp = cb.getComponent(defenderRef, UUIDComponent.getComponentType());
            if (defUuidComp == null) {
                CombatPlugin.appendEvent(ParryPluginListener.ts() + " [ParryDebug] DamageFilter: defender UUID component missing, skipping");
                return;
            }
            UUID defenderId = defUuidComp.getUuid();

            String entry = ParryPluginListener.ts() + " [ParryDebug] DamageFilter ENTRY: nowMs=" + nowMs +
                    " attackerId=" + attackerId + " defenderId=" + defenderId +
                    " damageAmt=" + damage.getAmount() + " cancelled=" + damage.isCancelled();
            CombatPlugin.appendEvent(entry);

            Long blockTime = parryWindowByUuid.get(defenderId);

            if (blockTime == null) {
                CombatPlugin.appendEvent(ParryPluginListener.ts() + " [ParryDebug] No parryWindow entry for defenderId=" + defenderId);
                return;
            }

            long delta = nowMs - blockTime;

            if (delta > ParryPluginListener.PARRY_WINDOW_MS) {
                parryWindowByUuid.remove(defenderId);
                CombatPlugin.appendEvent(ParryPluginListener.ts() + " [ParryDebug] ParryWindow EXPIRED for defenderUUID=" + defenderId +
                        " blockTime=" + blockTime + " now=" + nowMs + " deltaMs=" + delta);
                return;
            }

            // Successful parry: cancel damage, mark stun, remove window
            damage.setCancelled(true);
            parryWindowByUuid.remove(defenderId);

            long until = nowMs + ParryPluginListener.STUN_DURATION_MS;
            stunnedUntilByPlayer.put(attackerId, until);

            // STRICT: Use only the canonical in-game stun asset. No fallbacks.
            String stunAssetId = "Weapon_Bomb_Stun";
            boolean applied = applyWeaponBombStunStrict(attackerRef, store, stunAssetId);

            if (!applied) {
                String err = ParryPluginListener.ts() + " [ParryError] Failed to apply stun asset '" + stunAssetId + "' to attacker " + attackerId +
                        ". No fallbacks will be attempted. Check server asset availability and EffectControllerComponent API.";
                LOG.at(Level.WARNING).log("[CombatPlugin] " + err);
                CombatPlugin.appendEvent(err);
            } else {
                String ok = ParryPluginListener.ts() + " [Parry] Stun asset '" + stunAssetId + "' applied to attacker " + attackerId +
                        " for " + ParryPluginListener.STUN_DURATION_MS + "ms";
                LOG.at(Level.INFO).log("[CombatPlugin] " + ok);
                CombatPlugin.appendEvent(ok);
            }

            String log1 = ParryPluginListener.ts() + " Parry success for defenderUUID=" + defenderId + " deltaMs=" + delta;
            LOG.at(Level.INFO).log("[CombatPlugin] " + log1);
            CombatPlugin.appendEvent(log1);
        }

        /**
         * Strict single-attempt application of the in-game stun asset "Weapon_Bomb_Stun".
         * This uses the common engine signature:
         *   EffectControllerComponent.addEffect(Ref, EntityEffect, float durationSeconds, OverlapBehavior, Store)
         *
         * If anything fails we log and return false (no fallbacks).
         */
        private boolean applyWeaponBombStunStrict(Ref<EntityStore> targetRef, Store<EntityStore> store, String stunAssetId) {
            try {
                int stunIndex = EntityEffect.getAssetMap().getIndex(stunAssetId);
                if (stunIndex == Integer.MIN_VALUE) {
                    String msg = ParryPluginListener.ts() + " [ParryError] Stun asset '" + stunAssetId + "' not present in EntityEffect asset map.";
                    LOG.at(Level.WARNING).log("[CombatPlugin] " + msg);
                    CombatPlugin.appendEvent(msg);
                    return false;
                }

                EntityEffect stunEffect = EntityEffect.getAssetMap().getAsset(stunIndex);
                if (stunEffect == null) {
                    String msg = ParryPluginListener.ts() + " [ParryError] Stun asset index found for '" + stunAssetId + "' but asset is null.";
                    LOG.at(Level.WARNING).log("[CombatPlugin] " + msg);
                    CombatPlugin.appendEvent(msg);
                    return false;
                }

                EffectControllerComponent effectController = store.getComponent(targetRef, EffectControllerComponent.getComponentType());
                if (effectController == null) {
                    String msg = ParryPluginListener.ts() + " [ParryError] EffectControllerComponent not available on target; cannot apply '" + stunAssetId + "'.";
                    LOG.at(Level.WARNING).log("[CombatPlugin] " + msg);
                    CombatPlugin.appendEvent(msg);
                    return false;
                }

                // Use the documented/common method that the engine commands use:
                // addEffect(Ref, EntityEffect, float durationSeconds, OverlapBehavior, Store)
                try {
                    float durationSeconds = ParryPluginListener.STUN_DURATION_MS / 1000.0f;
                    Method m = effectController.getClass().getMethod("addEffect", Ref.class, EntityEffect.class, float.class, OverlapBehavior.class, Store.class);
                    m.invoke(effectController, targetRef, stunEffect, durationSeconds, OverlapBehavior.OVERWRITE, store);
                    CombatPlugin.appendEvent(ParryPluginListener.ts() + " [Parry] Applied '" + stunAssetId + "' via addEffect(ref,EntityEffect,float,OverlapBehavior,store)");
                    return true;
                } catch (NoSuchMethodException nsme) {
                    String msg = ParryPluginListener.ts() + " [ParryError] EffectControllerComponent missing expected method addEffect(ref,EntityEffect,float,OverlapBehavior,store) for '" + stunAssetId + "'.";
                    LOG.at(Level.WARNING).log("[CombatPlugin] " + msg);
                    CombatPlugin.appendEvent(msg);
                    return false;
                } catch (Throwable t) {
                    String msg = ParryPluginListener.ts() + " [ParryError] Exception invoking addEffect for '" + stunAssetId + "': " + t;
                    LOG.at(Level.WARNING).log("[CombatPlugin] " + msg, t);
                    CombatPlugin.appendEvent(msg);
                    return false;
                }

            } catch (Throwable t) {
                String msg = ParryPluginListener.ts() + " [ParryError] Exception while preparing to apply stun asset '" + stunAssetId + "': " + t;
                LOG.at(Level.WARNING).log("[CombatPlugin] " + msg, t);
                CombatPlugin.appendEvent(msg);
                return false;
            }
        }
    }

    // StunExpirySystem: removes map entries when stun expires. Runs on entities with a UUIDComponent.
    public static class StunExpirySystem extends EntityTickingSystem<EntityStore> {
        private static final Query<EntityStore> QUERY = UUIDComponent.getComponentType();

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
            UUID id = uuidComp != null ? uuidComp.getUuid() : null;

            if (id == null) return;

            Long until = stunnedUntilByPlayer.get(id);

            if (until == null) return;

            if (System.currentTimeMillis() >= until) {
                stunnedUntilByPlayer.remove(id);
                String msg = ParryPluginListener.ts() + " [Parry] Stun expired for " + id;
                CombatPlugin.appendEvent(msg);
                LOG.at(Level.INFO).log("[CombatPlugin] " + msg);
            }
        }
    }
}