package com.heypixel.heypixelmod.commands.impl;

import com.heypixel.heypixelmod.Naven;
import com.heypixel.heypixelmod.commands.Command;
import com.heypixel.heypixelmod.commands.CommandInfo;
import com.heypixel.heypixelmod.modules.impl.render.NameProtect;
import com.heypixel.heypixelmod.utils.ChatUtils;

@CommandInfo(
        name = "sethiddenname",
        description = "Set custom hidden name for NameProtect",
        aliases = {"shn"}
)
public class CommandSetHideName extends Command {
    @Override
    public void onCommand(String[] args) {
        if (args.length == 0) {
            ChatUtils.addChatMessage("Usage: .sethiddenname <name>");
            return;
        }
        String hiddenName = String.join(" ", args);
        NameProtect.setCustomHiddenName(hiddenName);
        Naven.getInstance().getFileManager().save();
        ChatUtils.addChatMessage("Hidden name set to: " + hiddenName + " §a(Saved!)");
    }

    @Override
    public String[] onTab(String[] args) {
        return new String[0];
    }
}