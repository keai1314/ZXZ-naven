package com.heypixel.heypixelmod.obsoverlay.modules.impl.misc;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.ui.notification.Notification;
import com.heypixel.heypixelmod.obsoverlay.ui.notification.NotificationLevel;
import com.heypixel.heypixelmod.obsoverlay.utils.TimeHelper;
import com.heypixel.heypixelmod.obsoverlay.ui.LanguageManager;

@ModuleInfo(
   name = "客户端朋友",
   description = "把其他使用者视为朋友!",
   category = Category.MISC
)
public class ClientFriend extends Module {
   public static TimeHelper attackTimer = new TimeHelper();

   public String getLocalizedName() {
      return LanguageManager.getInstance().getLocalizedString("客户端朋友", "Client Friend");
   }

   public String getLocalizedDescription() {
      return LanguageManager.getInstance().getLocalizedString("把其他使用者视为朋友!", "Treat other users as friends!");
   }

   @Override
   public void onDisable() {
      attackTimer.reset();
      Notification notification = new Notification(NotificationLevel.INFO, "你可以在15秒后攻击其他玩家.", 15000L);
      Naven.getInstance().getNotificationManager().addNotification(notification);
   }
}
