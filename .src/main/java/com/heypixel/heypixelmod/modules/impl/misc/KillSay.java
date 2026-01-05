package com.heypixel.heypixelmod.modules.impl.misc;

import com.heypixel.heypixelmod.events.api.EventTarget;
import com.heypixel.heypixelmod.events.api.types.EventType;
import com.heypixel.heypixelmod.events.impl.EventMotion;
import com.heypixel.heypixelmod.events.impl.EventPacket;
import com.heypixel.heypixelmod.events.impl.EventRespawn;
import com.heypixel.heypixelmod.modules.Category;
import com.heypixel.heypixelmod.modules.Module;
import com.heypixel.heypixelmod.modules.ModuleInfo;
import com.heypixel.heypixelmod.utils.TimeHelper;
import com.heypixel.heypixelmod.values.Value;
import com.heypixel.heypixelmod.values.ValueBuilder;
import com.heypixel.heypixelmod.values.impl.BooleanValue;
import com.heypixel.heypixelmod.values.impl.FloatValue;
import com.heypixel.heypixelmod.values.impl.ModeValue;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArraySet;

@ModuleInfo(
   name = "KillSay",
   description = "Automatic send message when you killed someone!",
   category = Category.MISC
)
public class KillSay extends Module {
   Queue<String> messageQueue = new ConcurrentLinkedQueue<>();
   FloatValue delay = ValueBuilder.create(this, "Delay")
      .setDefaultFloatValue(6000.0F)
      .setFloatStep(100.0F)
      .setMinFloatValue(0.0F)
      .setMaxFloatValue(15000.0F)
      .build()
      .getFloatValue();
   ModeValue prefix = ValueBuilder.create(this, "Prefix").setDefaultModeIndex(0).setModes("None", "@", "!", "/shout ").build().getModeValue();
   private final List<BooleanValue> values = new ArrayList<>();
   public static Set<String> attackedPlayers = new CopyOnWriteArraySet<>();
   private final TimeHelper timer = new TimeHelper();
   Random random = new Random();

   @EventTarget
   public void onRespawn(EventRespawn e) {
      attackedPlayers.clear();
   }

   @EventTarget
   public void onPacket(EventPacket e) {
      if (e.getPacket() instanceof ClientboundPlayerInfoRemovePacket && e.getType() == EventType.RECEIVE && mc.getConnection() != null) {
         ClientboundPlayerInfoRemovePacket packet = (ClientboundPlayerInfoRemovePacket)e.getPacket();

         for (UUID entry : packet.profileIds()) {
            PlayerInfo playerInfo = mc.getConnection().getPlayerInfo(entry);
            if (playerInfo != null) {
               String playerName = playerInfo.getProfile().getName();
               if (attackedPlayers.contains(playerName)) {
                  String prefix = this.prefix.isCurrentMode("None") ? "" : this.prefix.getCurrentMode();
                  List<String> styles = this.values.stream().filter(BooleanValue::getCurrentValue).map(Value::getName).toList();
                  if (!styles.isEmpty()) {
                     String style = styles.get(this.random.nextInt(styles.size()));
                     String message = prefix + String.format(style, playerName);
                     this.messageQueue.offer(message);
                     attackedPlayers.remove(playerName);
                  }
               }
            }
         }
      }
   }

   @EventTarget
   public void onMotion(EventMotion e) {
      if (e.getType() == EventType.PRE) {
         if (mc.player != null && mc.player.tickCount < 10) {
            this.messageQueue.clear();
            attackedPlayers.clear();
            return;
         }

         if (this.timer.delay((double)this.delay.getCurrentValue()) && !this.messageQueue.isEmpty()) {
            String message = this.messageQueue.poll();
            mc.player.connection.sendChat(message);
            this.timer.reset();
         }
      }
   }

   public List<BooleanValue> getValues() {
      return this.values;
   }
}
