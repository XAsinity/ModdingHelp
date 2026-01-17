package com.combatplugin;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public class ParryDebugCommand extends AbstractPlayerCommand {
    private final OptionalArg<Boolean> enabledArg = this.withOptionalArg("enabled", "Toggle parry debug true/false", ArgTypes.BOOLEAN);

    public ParryDebugCommand() {
        super("parrydebug", "Toggle parry debug tracing", false);
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx,
                           @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref,
                           @Nonnull PlayerRef playerRef,
                           @Nonnull World world) {
        boolean newState;
        if (enabledArg.provided(ctx)) {
            newState = enabledArg.get(ctx);
        } else {
            newState = !ParryDebugSystems.ENABLED;
        }
        ParryDebugSystems.ENABLED = newState;
        Message msg = Message.raw("[CombatPlugin] Parry debug " + (newState ? "ENABLED" : "DISABLED"));
        ctx.sendMessage(msg);
        world.sendMessage(Message.raw("[CombatPlugin] Parry debug toggled by " + playerRef.getUsername() + " -> " + newState));
        CombatPlugin.appendEvent(ParryPluginListener.ts() + " ParryDebug set to " + newState);
    }
}