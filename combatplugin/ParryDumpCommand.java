package com.combatplugin;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.Message;

import javax.annotation.Nonnull;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Simple command to dump current parry and stunned maps to chat and the events log.
 */
public class ParryDumpCommand extends AbstractPlayerCommand {
    public ParryDumpCommand() {
        super("parrydump", "Dump parry windows and stun map", false);
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx,
                           @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref,
                           @Nonnull PlayerRef playerRef,
                           @Nonnull World world) {
        String parryKeys = ParrySystems.parryWindowByUuid.keySet().stream()
                .map(UUID::toString)
                .collect(Collectors.joining(", "));
        String stunKeys = ParrySystems.stunnedUntilByPlayer.keySet().stream()
                .map(UUID::toString)
                .collect(Collectors.joining(", "));
        String msg = "[CombatPlugin] Parry windows(" + ParrySystems.parryWindowByUuid.size() + "): " + (parryKeys.isEmpty() ? "<none>" : parryKeys)
                + " | Stunned(" + ParrySystems.stunnedUntilByPlayer.size() + "): " + (stunKeys.isEmpty() ? "<none>" : stunKeys);
        ctx.sendMessage(Message.raw(msg));
        CombatPlugin.appendEvent(ParryPluginListener.ts() + " ParryDump: " + msg);
    }
}