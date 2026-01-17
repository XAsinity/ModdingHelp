package com.modding.combatplugin;

import com.hypixel.hytale.common.plugin.PluginManifest;
import com.hypixel.hytale.component.ComponentRegistryProxy;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;

/**
 * Combat Plugin that extends Hytale's combat system with custom parry mechanics.
 * 
 * This plugin adds a stun effect to attackers when their attacks are successfully parried.
 */
public class CombatPlugin extends JavaPlugin {

    public static final PluginManifest MANIFEST = PluginManifest.corePlugin(CombatPlugin.class)
            .depends(EntityModule.class)
            .depends(DamageModule.class)
            .build();

    private static CombatPlugin instance;

    public static CombatPlugin get() {
        return instance;
    }

    public CombatPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
    }

    @Override
    protected void setup() {
        ComponentRegistryProxy<EntityStore> entityStoreRegistry = this.getEntityStoreRegistry();
        
        // Register the ParrySystems.DamageFilterParry system
        entityStoreRegistry.registerSystem(new ParrySystems.DamageFilterParry());
    }
}
