package com.heypixel.heypixelmod.commands.impl;

import com.heypixel.heypixelmod.commands.Command;
import com.heypixel.heypixelmod.commands.CommandInfo;
import com.heypixel.heypixelmod.utils.ChatUtils;

@CommandInfo(
        name = "help",
        description = "Get help",
        aliases = {"hp"}
)
public class CommandHelp extends Command {
    @Override
    public void onCommand(String[] args) {
        ChatUtils.addChatMessage("========Naven-XD========");
        ChatUtils.addChatMessage(".help .hp Current help list");
        ChatUtils.addChatMessage(".binds .bds List of key binds");
        ChatUtils.addChatMessage(".hide .h Hide or show a specific feature");
        ChatUtils.addChatMessage(".config .cfg Config file system");
        ChatUtils.addChatMessage(".bind .b Bind a key");
        ChatUtils.addChatMessage(".language .lang Language settings");
        ChatUtils.addChatMessage(".proxy .prox Proxy settings");
        ChatUtils.addChatMessage(".toggle .t Toggle a feature");
        ChatUtils.addChatMessage(".sethiddenname .shn Set custom hidden name for NameProtect");

    }

    @Override
    public String[] onTab(String[] args) {
        return new String[0];
    }
}