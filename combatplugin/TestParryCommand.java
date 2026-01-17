package com.combatplugin;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public class TestParryCommand extends AbstractPlayerCommand {
    public TestParryCommand() {
        super("testparry", "Open a parry window for yourself (for testing)", false);
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx,
                           @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref,
                           @Nonnull PlayerRef playerRef,
                           @Nonnull World world) {
        ParrySystems.parryWindowByUuid.put(playerRef.getUuid(), System.currentTimeMillis());
        ctx.sendMessage(Message.raw("[CombatPlugin] Test parry window opened for " + playerRef.getUsername()));
        CombatPlugin.appendEvent(ParryPluginListener.ts() + " TestParry: window set for " + playerRef.getUuid());
    }
}