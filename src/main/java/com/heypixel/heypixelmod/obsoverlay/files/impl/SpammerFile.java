package com.heypixel.heypixelmod.obsoverlay.files.impl;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.files.ClientFile;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.misc.Spammer;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;

public class SpammerFile extends ClientFile {
   private static final String[] styles = new String[]{
           "你活着有什么用？",
           "没用的烂区",
           "你就这点本事吗？",
           "你为啥不赶紧去4？",
           "打字慢成这样你活着还有啥用？",
           "你咋不回去种田？",
           "打字这么慢你是老年机吗",
           "跟你这种社会LJ对骂我都觉得无聊",
           "活着只会浪费社会资源的LJ东西",
           "你现在是不是在屏幕面前无能狂怒啊？",
           "现实中不如意在网上找存在感的LJ",
           "你为什么不能面对我啊？",
           "你是不是窝囊F啊？",
           "这就是你的能力吗，我觉得你不适合玩游戏",
           "是不是急了啊？",
           "LJ就是LJ啊，你承认不就是了？"
   };

   public SpammerFile() {
      super("spammers.cfg");
   }

   @Override
   public void read(BufferedReader reader) throws IOException {
      Spammer module = (Spammer)Naven.getInstance().getModuleManager().getModule(Spammer.class);
      List<BooleanValue> values = module.getValues();

      // 忽略配置文件中的内容，只使用代码中定义的默认消息
      for (String style : styles) {
         values.add(ValueBuilder.create(module, style).setDefaultBooleanValue(false).build().getBooleanValue());
      }
   }

   @Override
   public void save(BufferedWriter writer) throws IOException {
      Spammer module = (Spammer)Naven.getInstance().getModuleManager().getModule(Spammer.class);

      for (BooleanValue value : module.getValues()) {
         writer.write(value.getName() + "\n");
      }
   }
}