package com.onaple.harvester.command;

import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;

public class addWorldCommand implements CommandExecutor{
    @Override
    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
        if (src instanceof Player){
            src.sendMessage(Text.of(((Player) src).getWorld().getName()));
        }
        return CommandResult.success();
    }
}
