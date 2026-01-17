package com.combatplugin;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ComponentRegistry;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.entity.LivingEntity;

import javax.annotation.Nonnull;

/**
 * Replacement for deprecated AllLegacyLivingEntityTypesQuery.
 */
public final class LivingEntityQuery implements Query<EntityStore> {
    @Nonnull
    public static final LivingEntityQuery INSTANCE = new LivingEntityQuery();

    private LivingEntityQuery() {}

    @Override
    public boolean test(@Nonnull Archetype<EntityStore> archetype) {
        for (int i = archetype.getMinIndex(); i < archetype.length(); ++i) {
            ComponentType<EntityStore, ?> componentType = archetype.get(i);
            if (componentType == null) continue;
            Class<?> typeClass = componentType.getTypeClass();
            if (typeClass != null && LivingEntity.class.isAssignableFrom(typeClass)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean requiresComponentType(ComponentType<EntityStore, ?> componentType) {
        return false;
    }

    @Override
    public void validateRegistry(ComponentRegistry<EntityStore> registry) {
        // no-op
    }

    @Override
    public void validate() {
        // no-op
    }
}