package com.heypixel.heypixelmod.obsoverlay.files.impl;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.files.ClientFile;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.misc.KillSay;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;

public class KillSaysFile extends ClientFile {
   private static final String[] styles = new String[]{
       "%s 1L",
       "%s 你就这点本事吗？",
       "%s LJ",
       "%s 没用的东西",
       "%s 你活着有啥用？",
       "%s 你咋不赶紧去4啊？",
       "%s 你咋不回去种田啊？",
       "%s 别急",
       "%s 你的技术就是这样的吗？",
       "%s 只能浪费社会资源的LJ"
   };

   public KillSaysFile() {
      super("killsays.cfg");
   }

   @Override
   public void read(BufferedReader reader) throws IOException {
      KillSay module = (KillSay)Naven.getInstance().getModuleManager().getModule(KillSay.class);
      List<BooleanValue> values = module.getValues();

      // 忽略配置文件中的内容，只使用代码中定义的默认消息
      for (String style : styles) {
         values.add(ValueBuilder.create(module, style).setDefaultBooleanValue(false).build().getBooleanValue());
      }
   }

   @Override
   public void save(BufferedWriter writer) throws IOException {
      KillSay module = (KillSay)Naven.getInstance().getModuleManager().getModule(KillSay.class);

      for (BooleanValue value : module.getValues()) {
         writer.write(value.getName() + "\n");
      }
   }
}