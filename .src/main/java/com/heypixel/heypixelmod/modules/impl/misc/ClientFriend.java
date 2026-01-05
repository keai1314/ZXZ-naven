package com.heypixel.heypixelmod.modules.impl.misc;

import com.heypixel.heypixelmod.Naven;
import com.heypixel.heypixelmod.modules.Category;
import com.heypixel.heypixelmod.modules.Module;
import com.heypixel.heypixelmod.modules.ModuleInfo;
import com.heypixel.heypixelmod.ui.notification.Notification;
import com.heypixel.heypixelmod.utils.TimeHelper;

@ModuleInfo(
   name = "ClientFriend",
   description = "Treat other users as friend!",
   category = Category.MISC
)
public class ClientFriend extends Module {
   public static TimeHelper attackTimer = new TimeHelper();

   @Override
   public void onDisable() {

      attackTimer.reset();
      Notification notification = Notification.create("You can attack other players after 15 seconds.", true);
      Naven.getInstance().getNotificationManager().addNotification(notification);
   }
}