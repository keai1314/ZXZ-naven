package com.heypixel.heypixelmod.files.impl;

import com.heypixel.heypixelmod.Naven;
import com.heypixel.heypixelmod.files.ClientFile;
import com.heypixel.heypixelmod.modules.impl.misc.KillSay;
import com.heypixel.heypixelmod.values.ValueBuilder;
import com.heypixel.heypixelmod.values.impl.BooleanValue;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;

public class KillSaysFile extends ClientFile {
   private static final String[] styles = new String[]{
      "%s 你以为能战胜我吗？naven-xd.top",
      "%s 权威认证naven-xd.top",
      "%s 我喜欢你naven-xd.top",
      "%s 我是Naven-XD使用者，你不可能战胜我！",
      "%s 可以和我交往吗？naven-xd.top",
      "%s 你好可爱naven-xd.top",
      "%s naven-xd.topYYDS",
      "%s 你已被BJD害死！快使用naven-xd.top！",
      "%s 你已被我本人害死，快使用naven-xd.top！"
   };

   public KillSaysFile() {
      super("killsays.cfg");
   }

   @Override
   public void read(BufferedReader reader) throws IOException {
      KillSay module = (KillSay)Naven.getInstance().getModuleManager().getModule(KillSay.class);
      List<BooleanValue> values = module.getValues();

      String line;
      while ((line = reader.readLine()) != null) {
         values.add(ValueBuilder.create(module, line).setDefaultBooleanValue(false).build().getBooleanValue());
      }

      if (values.isEmpty()) {
         for (String style : styles) {
            values.add(ValueBuilder.create(module, style).setDefaultBooleanValue(false).build().getBooleanValue());
         }
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
