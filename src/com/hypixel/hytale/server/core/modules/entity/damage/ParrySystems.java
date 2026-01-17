/*
 * Decompiled with CFR 0.152.
 */
package com.hypixel.hytale.server.core.modules.entity.damage;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.OverlapBehavior;
import com.hypixel.hytale.server.core.entity.damage.DamageDataComponent;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.modules.entity.AllLegacyLivingEntityTypesQuery;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ParrySystems {

    /**
     * System that applies a "Stunned" effect to attackers when their attack is successfully parried.
     * A parry is detected when damage is marked as BLOCKED in the damage metadata.
     */
    public static class DamageFilterParry
    extends DamageEventSystem {
        @Nonnull
        private static final Query<EntityStore> QUERY = Query.and(AllLegacyLivingEntityTypesQuery.INSTANCE, DamageDataComponent.getComponentType(), EffectControllerComponent.getComponentType());
        private static final String STUNNED_EFFECT_ID = "Stunned";
        private static final float STUN_DURATION = 2.0f;

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

        /**
         * Handles damage events and applies stunned effect to attackers upon successful parry.
         * 
         * @param index The index in the archetype chunk
         * @param archetypeChunk The chunk containing entity data
         * @param store The entity store
         * @param commandBuffer The command buffer for entity modifications
         * @param damage The damage event being processed
         */
        @Override
        public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull Damage damage) {
            // Check if the damage was blocked (parried)
            Boolean isBlocked = damage.getMetaStore().getIfPresentMetaObject(Damage.BLOCKED);
            if (isBlocked == null || !isBlocked.booleanValue()) {
                return;
            }

            // Identify the attacker from the damage source
            Damage.Source source = damage.getSource();
            if (!(source instanceof Damage.EntitySource)) {
                return;
            }

            Damage.EntitySource entitySource = (Damage.EntitySource)source;
            Ref<EntityStore> attackerRef = entitySource.getRef();
            if (!attackerRef.isValid()) {
                return;
            }

            // Apply stunned effect to the attacker
            this.applyStunnedEffect(attackerRef, commandBuffer);
        }

        /**
         * Applies the "Stunned" effect to the attacker entity.
         * The stunned effect disables movement and interactions for a specified duration.
         * 
         * @param attackerRef Reference to the attacking entity
         * @param commandBuffer The command buffer for entity modifications
         */
        private void applyStunnedEffect(@Nonnull Ref<EntityStore> attackerRef, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
            // Get the effect controller component from the attacker
            EffectControllerComponent effectController = commandBuffer.getComponent(attackerRef, EffectControllerComponent.getComponentType());
            if (effectController == null) {
                return;
            }

            // Load the "Stunned" effect from the asset map
            EntityEffect stunnedEffect = (EntityEffect)EntityEffect.getAssetMap().getAsset(STUNNED_EFFECT_ID);
            if (stunnedEffect == null) {
                // Effect doesn't exist in assets - fail gracefully
                return;
            }

            // Apply the stunned effect with EXTEND overlap behavior
            // This ensures multiple parries extend the stun duration rather than replace it
            effectController.addEffect(attackerRef, stunnedEffect, STUN_DURATION, OverlapBehavior.EXTEND, commandBuffer);
        }
    }
}
