package com.heypixel.heypixelmod.modules.impl.render;

import com.heypixel.heypixelmod.events.api.EventTarget;
import com.heypixel.heypixelmod.events.api.types.EventType;
import com.heypixel.heypixelmod.events.impl.EventMotion;
import com.heypixel.heypixelmod.events.impl.EventPacket;
import com.heypixel.heypixelmod.events.impl.EventRender;
import com.heypixel.heypixelmod.events.impl.EventRespawn;
import com.heypixel.heypixelmod.modules.Category;
import com.heypixel.heypixelmod.modules.Module;
import com.heypixel.heypixelmod.modules.ModuleInfo;
import com.heypixel.heypixelmod.utils.BlockUtils;
import com.heypixel.heypixelmod.utils.ChunkUtils;
import com.heypixel.heypixelmod.utils.RenderUtils;
import com.heypixel.heypixelmod.values.ValueBuilder;
import com.heypixel.heypixelmod.values.impl.BooleanValue;
import com.heypixel.heypixelmod.values.impl.FloatValue;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockEventPacket;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@ModuleInfo(
   name = "ChestESP",
   description = "Highlights chests",
   category = Category.RENDER
)
public class ChestESP extends Module {
   private static final float[] chestColor = new float[]{0.0F, 1.0F, 0.0F};
   private static final float[] openedChestColor = new float[]{1.0F, 0.0F, 0.0F};
   private final List<BlockPos> openedChests = new CopyOnWriteArrayList<>();
   private final List<AABB> renderBoundingBoxes = new CopyOnWriteArrayList<>();

   public BooleanValue useCustomChestColor = ValueBuilder.create(this, "Use Custom Chest Color")
           .setDefaultBooleanValue(false)
           .build()
           .getBooleanValue();

   public FloatValue chestRed = ValueBuilder.create(this, "Chest Red")
           .setDefaultFloatValue(0F)
           .setFloatStep(5F)
           .setMinFloatValue(0F)
           .setMaxFloatValue(255F)
           .setVisibility(() -> this.useCustomChestColor.getCurrentValue())
           .build()
           .getFloatValue();

   public FloatValue chestGreen = ValueBuilder.create(this, "Chest Green")
           .setDefaultFloatValue(255F)
           .setFloatStep(5F)
           .setMinFloatValue(0F)
           .setMaxFloatValue(255F)
           .setVisibility(() -> this.useCustomChestColor.getCurrentValue())
           .build()
           .getFloatValue();

   public FloatValue chestBlue = ValueBuilder.create(this, "Chest Blue")
           .setDefaultFloatValue(0F)
           .setFloatStep(5F)
           .setMinFloatValue(0F)
           .setMaxFloatValue(255F)
           .setVisibility(() -> this.useCustomChestColor.getCurrentValue())
           .build()
           .getFloatValue();

   public BooleanValue useCustomOpenedChestColor = ValueBuilder.create(this, "Use Custom Opened Chest Color")
           .setDefaultBooleanValue(false)
           .build()
           .getBooleanValue();

   public FloatValue openedChestRed = ValueBuilder.create(this, "Opened Chest Red")
           .setDefaultFloatValue(255F)
           .setFloatStep(5F)
           .setMinFloatValue(0F)
           .setMaxFloatValue(255F)
           .setVisibility(() -> this.useCustomOpenedChestColor.getCurrentValue())
           .build()
           .getFloatValue();

   public FloatValue openedChestGreen = ValueBuilder.create(this, "Opened Chest Green")
           .setDefaultFloatValue(0F)
           .setFloatStep(5F)
           .setMinFloatValue(0F)
           .setMaxFloatValue(255F)
           .setVisibility(() -> this.useCustomOpenedChestColor.getCurrentValue())
           .build()
           .getFloatValue();

   public FloatValue openedChestBlue = ValueBuilder.create(this, "Opened Chest Blue")
           .setDefaultFloatValue(0F)
           .setFloatStep(5F)
           .setMinFloatValue(0F)
           .setMaxFloatValue(255F)
           .setVisibility(() -> this.useCustomOpenedChestColor.getCurrentValue())
           .build()
           .getFloatValue();

   @Override
   public void onDisable() {
   }

   @EventTarget
   public void onRespawn(EventRespawn e) {
      this.openedChests.clear();
   }

   @EventTarget
   public void onPacket(EventPacket e) {
      if (e.getType() == EventType.RECEIVE && e.getPacket() instanceof ClientboundBlockEventPacket) {
         ClientboundBlockEventPacket packet = (ClientboundBlockEventPacket)e.getPacket();
         if ((packet.getBlock() == Blocks.CHEST || packet.getBlock() == Blocks.TRAPPED_CHEST) && packet.getB0() == 1 && packet.getB1() == 1) {
            this.openedChests.add(packet.getPos());
         }
      }
   }

   @EventTarget
   public void onTick(EventMotion e) {
      if (e.getType() == EventType.PRE) {
         ArrayList<BlockEntity> blockEntities = ChunkUtils.getLoadedBlockEntities().collect(Collectors.toCollection(ArrayList::new));
         this.renderBoundingBoxes.clear();

         for (BlockEntity blockEntity : blockEntities) {
            if (blockEntity instanceof ChestBlockEntity) {
               ChestBlockEntity chestBE = (ChestBlockEntity)blockEntity;
               AABB box = this.getChestBox(chestBE);
               if (box != null) {
                  this.renderBoundingBoxes.add(box);
               }
            }
         }
      }
   }

   private AABB getChestBox(ChestBlockEntity chestBE) {
      BlockState state = chestBE.getBlockState();
      if (!state.hasProperty(ChestBlock.TYPE)) {
         return null;
      } else {
         ChestType chestType = (ChestType)state.getValue(ChestBlock.TYPE);
         if (chestType == ChestType.LEFT) {
            return null;
         } else {
            BlockPos pos = chestBE.getBlockPos();
            AABB box = BlockUtils.getBoundingBox(pos);
            if (chestType != ChestType.SINGLE) {
               BlockPos pos2 = pos.relative(ChestBlock.getConnectedDirection(state));
               if (BlockUtils.canBeClicked(pos2)) {
                  AABB box2 = BlockUtils.getBoundingBox(pos2);
                  box = box.minmax(box2);
               }
            }

            return box;
         }
      }
   }

   @EventTarget
   public void onRender(EventRender e) {
      PoseStack stack = e.getPMatrixStack();
      stack.pushPose();
      RenderSystem.disableDepthTest();
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.setShader(GameRenderer::getPositionShader);
      Tesselator tessellator = RenderSystem.renderThreadTesselator();
      BufferBuilder bufferBuilder = tessellator.getBuilder();

      for (AABB box : this.renderBoundingBoxes) {
         BlockPos pos = BlockPos.containing(box.minX, box.minY, box.minZ);
         boolean isOpened = this.openedChests.contains(pos);

         float red, green, blue;
         if (isOpened && useCustomOpenedChestColor.getCurrentValue()) {
             red = openedChestRed.getCurrentValue() / 255.0f;
             green = openedChestGreen.getCurrentValue() / 255.0f;
             blue = openedChestBlue.getCurrentValue() / 255.0f;
         } else if (!isOpened && useCustomChestColor.getCurrentValue()) {
             red = chestRed.getCurrentValue() / 255.0f;
             green = chestGreen.getCurrentValue() / 255.0f;
             blue = chestBlue.getCurrentValue() / 255.0f;
         } else {
             float[] defaultColor = isOpened ? openedChestColor : chestColor;
             red = defaultColor[0];
             green = defaultColor[1];
             blue = defaultColor[2];
         }

         RenderSystem.setShaderColor(red, green, blue, 0.25F);
         RenderUtils.drawSolidBox(bufferBuilder, stack.last().pose(), box);
      }

      RenderSystem.disableBlend();
      RenderSystem.enableDepthTest();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      stack.popPose();
   }
}