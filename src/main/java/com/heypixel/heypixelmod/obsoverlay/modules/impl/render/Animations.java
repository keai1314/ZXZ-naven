package com.heypixel.heypixelmod.obsoverlay.modules.impl.render;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMotion;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventPacket;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRender;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.combat.Aura;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.move.Scaffold;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.ModeValue;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.UseAnim;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import com.heypixel.heypixelmod.obsoverlay.ui.LanguageManager;

@ModuleInfo(name = "动作", description = "允许你修改游戏中的许多动作", category = Category.RENDER)
public class Animations extends Module {
    public BooleanValue onlyKillAura = ValueBuilder.create(this, "仅杀戮光环").setDefaultBooleanValue(false).build().getBooleanValue();
    public final ModeValue BlockMods = ValueBuilder.create(this, "防砍模式")
            .setModes("无", "1.7", "推")
            .setDefaultModeIndex(1)
            .build()
            .getModeValue();

    public final BooleanValue BlockOnlySword = ValueBuilder.create(this, "仅剑防砍")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();

    public final BooleanValue KillauraAutoBlock = ValueBuilder.create(this, "杀戮光环自动防砍")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();

    public final BooleanValue OverrideVanilla = ValueBuilder.create(this, "覆盖默认设置")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();

    public final BooleanValue dualWieldBlock = ValueBuilder.create(this, "双剑格挡")
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();

    public final BooleanValue allItemsAnimation = ValueBuilder.create(this, "全物品动画")
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();

    public final BooleanValue ShowHUDItem = ValueBuilder.create(this, "显示HUD项目")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();

    public final BooleanValue RenderOffhandShield = ValueBuilder.create(this, "渲染副手盾牌")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();

    public final FloatValue BlockingX = ValueBuilder.create(this, "防砍X坐标")
            .setDefaultFloatValue(0.56F)
            .setMinFloatValue(-2.0F)
            .setMaxFloatValue(2.0F)
            .setFloatStep(0.01F)
            .build()
            .getFloatValue();

    public final FloatValue BlockingY = ValueBuilder.create(this, "防砍Y坐标")
            .setDefaultFloatValue(-0.52F)
            .setMinFloatValue(-2.0F)
            .setMaxFloatValue(2.0F)
            .setFloatStep(0.01F)
            .build()
            .getFloatValue();

    private boolean flip;
    public static boolean isBlocking = false;
    private final Minecraft mc = Minecraft.getInstance();
    private float mainHandHeight = 0.0F;
    private float offHandHeight = 0.0F;
    private float oMainHandHeight = 0.0F;
    private float oOffHandHeight = 0.0F;
    private ItemStack mainHandItem = ItemStack.EMPTY;
    private ItemStack offHandItem = ItemStack.EMPTY;
    private boolean isScaffoldEnabled() {
        Scaffold scaffold = (Scaffold) Naven.getInstance().getModuleManager().getModule(Scaffold.class);
        return scaffold != null && scaffold.isEnabled();
    }

    @Override
    public void onEnable() {
        super.onEnable();
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        MinecraftForge.EVENT_BUS.unregister(this);
    }

    @SubscribeEvent
    public void onRenderHand(RenderHandEvent event) {
        if (isScaffoldEnabled()) {
            return;
        }

        if (!this.isEnabled() || !OverrideVanilla.getCurrentValue() || BlockMods.getCurrentMode().equals("None"))
            return;

        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        
        // 如果启用了全物品动画，或者物品是剑，都允许渲染自定义动画
        if (!(event.getItemStack().getItem() instanceof SwordItem) && !allItemsAnimation.getCurrentValue() && BlockOnlySword.getCurrentValue()) {
            return;
        }

        boolean isOffhandUsing = false;
        if (mc.player.isUsingItem() && mc.player.getUsedItemHand() == InteractionHand.OFF_HAND) {
            ItemStack offhandItem = mc.player.getOffhandItem();
            UseAnim useAnim = offhandItem.getUseAnimation();
            if (useAnim != UseAnim.BLOCK) {
                isOffhandUsing = true;
            }
        }

        boolean isKillauraBlocking = KillauraAutoBlock.getCurrentValue() && getAuraTarget() != null;

        if (onlyKillAura.getCurrentValue() && !isKillauraBlocking) {
            return;
        }

        if (isOffhandUsing && !isKillauraBlocking)
            return;

        if (!mc.options.keyUse.isDown() && !isKillauraBlocking)
            return;

        event.setCanceled(true);

        renderArmWithItem(
                mc.player,
                event.getPartialTick(),
                event.getEquipProgress(),
                event.getHand(),
                event.getSwingProgress(),
                event.getItemStack(),
                event.getEquipProgress(),
                event.getPoseStack(),
                event.getMultiBufferSource(),
                event.getPackedLight());
    }

    @EventTarget
    public void onPacket(EventPacket event) {
        if (event.getType() == EventType.SEND && event.getPacket() instanceof ServerboundSwingPacket) {
            flip = !flip;
        }
    }

    @EventTarget
    public void onMotion(EventMotion event) {
        if (event.getType() != EventType.PRE || mc.player == null)
            return;

        updateHandStates();
    }

    private void updateHandStates() {
        oMainHandHeight = mainHandHeight;
        oOffHandHeight = offHandHeight;

        LocalPlayer localplayer = mc.player;
        ItemStack itemstack = localplayer.getMainHandItem();
        ItemStack itemstack1 = localplayer.getOffhandItem();
        boolean isBlocking = isBlocking();

        if (isBlocking) {
            mainHandHeight = 1.0F;
            if (ItemStack.matches(mainHandItem, itemstack)) {
                mainHandItem = itemstack;
            }
            if (ItemStack.matches(offHandItem, itemstack1)) {
                offHandItem = itemstack1;
            }
            return;
        }

        if (localplayer.isHandsBusy()) {
            mainHandHeight = Mth.clamp(mainHandHeight - 0.4F, 0.0F, 1.0F);
            offHandHeight = Mth.clamp(offHandHeight - 0.4F, 0.0F, 1.0F);
        } else {
            float f = localplayer.getAttackStrengthScale(1.0F);
            boolean flag = ForgeHooksClient.shouldCauseReequipAnimation(mainHandItem, itemstack,
                    localplayer.getInventory().selected);
            boolean flag1 = ForgeHooksClient.shouldCauseReequipAnimation(offHandItem, itemstack1, -1);

            if (!flag && mainHandItem != itemstack) {
                mainHandItem = itemstack;
            }

            if (!flag1 && offHandItem != itemstack1) {
                offHandItem = itemstack1;
            }
            float targetMainHeight = !flag ? f * f * f : 0.0F;
            float targetOffHeight = !flag1 ? 1.0F : 0.0F;

            mainHandHeight += Mth.clamp(targetMainHeight - mainHandHeight, -0.2F, 0.2F);
            offHandHeight += Mth.clamp(targetOffHeight - offHandHeight, -0.2F, 0.2F);
        }

        if (mainHandHeight < 0.1F) {
            mainHandItem = itemstack;
        }

        if (offHandHeight < 0.1F) {
            offHandItem = itemstack1;
        }
    }
    private boolean isBlocking() {
        if (isScaffoldEnabled()) {
            return false;
        }

        if (!this.isEnabled() || BlockMods.getCurrentMode().equals("无"))
            return false;

        LocalPlayer player = mc.player;
        if (player == null)
            return false;

        ItemStack mainHandItem = player.getMainHandItem();
        // 如果启用了全物品动画，或者BlockOnlySword为false，或者物品是剑，都允许格挡动画
        if (BlockOnlySword.getCurrentValue() && !(mainHandItem.getItem() instanceof SwordItem) && !allItemsAnimation.getCurrentValue())
            return false;

        boolean isOffhandUsing = false;
        if (player.isUsingItem() && player.getUsedItemHand() == InteractionHand.OFF_HAND) {
            ItemStack offhandItem = player.getOffhandItem();
            UseAnim useAnim = offhandItem.getUseAnimation();
            if (useAnim != UseAnim.BLOCK) {
                isOffhandUsing = true;
            }
        }

        // 检查Aura模块是否正在格挡
        boolean isAuraBlocking = isAuraBlocking();
        boolean isKillauraBlocking = (KillauraAutoBlock.getCurrentValue() || isAuraBlocking) && getAuraTarget() != null;

        if (onlyKillAura.getCurrentValue()) {
            return isKillauraBlocking;
        }

        if (isKillauraBlocking) {
            return true;
        }

        if (isOffhandUsing) {
            return false;
        }

        return mc.options.keyUse.isDown();
    }

    // 检查Aura模块是否正在格挡
    private boolean isAuraBlocking() {
        Aura aura = (Aura) Naven.getInstance().getModuleManager().getModule(Aura.class);
        if (aura != null && aura.isEnabled()) {
            try {
                java.lang.reflect.Field isBlockingField = Aura.class.getDeclaredField("isBlocking");
                isBlockingField.setAccessible(true);
                return isBlockingField.getBoolean(aura);
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    @EventTarget
    public void onRender(EventRender event) {
        if (isScaffoldEnabled()) {
            return;
        }

        if (mc.player == null || mc.level == null)
            return;

        if (ShowHUDItem.getCurrentValue()) {
            renderHUDItem(event);
        }
    }

    private void renderHUDItem(EventRender event) {
        ItemStack mainHandItem = mc.player.getMainHandItem();
        if (mainHandItem.isEmpty())
            return;

        PoseStack poseStack = new PoseStack();
        MultiBufferSource bufferSource = mc.renderBuffers().bufferSource();
        float partialTicks = mc.getFrameTime();
        int packedLight = 15728880;

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        float itemX = screenWidth - 100;
        float itemY = screenHeight - 100;

        poseStack.translate(itemX, itemY, 0);

        float swingProgress = mc.player.getAttackAnim(partialTicks);
        if (swingProgress > 0) {
            float swingAngle = Mth.sin(swingProgress * swingProgress * (float) Math.PI) * 10.0F;
            poseStack.mulPose(Axis.ZP.rotationDegrees(swingAngle));
        }

        float scale = 1.5F;
        poseStack.scale(scale, scale, scale);

        renderItem(mc.player, mainHandItem, ItemDisplayContext.GUI, false, poseStack, bufferSource, packedLight);
    }

    private void renderArmWithItem(
            AbstractClientPlayer player,
            float partialTicks,
            float equipProgress,
            InteractionHand interactionHand,
            float swingProgress,
            ItemStack itemStack,
            float equippedProg,
            PoseStack poseStack,
            MultiBufferSource multiBufferSource,
            int light) {
        if (!player.isScoping()) {
            boolean flag = interactionHand == InteractionHand.MAIN_HAND;
            HumanoidArm humanoidarm = flag ? player.getMainArm() : player.getMainArm().getOpposite();
            Animations animations = this;
            poseStack.pushPose();
            boolean skipOffhandShield = !flag &&
                    player.getOffhandItem().getItem() instanceof ShieldItem &&
                    !RenderOffhandShield.getCurrentValue();

            if (!skipOffhandShield) {
                if (itemStack.isEmpty()) {
                    if (flag && !player.isInvisible()) {
                        renderPlayerArm(poseStack, multiBufferSource, light, equippedProg, swingProgress, humanoidarm);
                    }
                } else if (itemStack.is(Items.FILLED_MAP)) {
                    if (flag && offHandItem.isEmpty()) {
                        renderTwoHandedMap(poseStack, multiBufferSource, light, equipProgress, equippedProg,
                                swingProgress);
                    } else {
                        renderOneHandedMap(poseStack, multiBufferSource, light, equippedProg, humanoidarm,
                                swingProgress, itemStack);
                    }
                } else {
                    boolean flag1 = itemStack.is(Items.CROSSBOW) && CrossbowItem.isCharged(itemStack);
                    int i = humanoidarm == HumanoidArm.RIGHT ? 1 : -1;
                    if (itemStack.is(Items.CROSSBOW)) {
                        if (player.isUsingItem() && player.getUseItemRemainingTicks() > 0
                                && player.getUsedItemHand() == interactionHand) {
                            applyItemArmTransform(poseStack, humanoidarm, equippedProg);
                            poseStack.translate((double) ((float) i * -0.4785682F), -0.094387F, 0.0573153F);
                            poseStack.mulPose(Axis.XP.rotation(-11.935F * (float) Math.PI / 180.0F));
                            poseStack.mulPose(Axis.YP.rotation((float) i * 65.3F * (float) Math.PI / 180.0F));
                            poseStack.mulPose(Axis.ZP.rotation((float) i * -9.785F * (float) Math.PI / 180.0F));
                            float f6 = (float) itemStack.getUseDuration()
                                    - ((float) player.getUseItemRemainingTicks() - partialTicks + 1.0F);
                            float f10 = f6 / (float) CrossbowItem.getChargeDuration(itemStack);
                            f10 = Math.min(f10, 1.0F);
                            if (f10 > 0.1F) {
                                float f14 = Mth.sin((f6 - 0.1F) * 1.3F);
                                float f20 = f10 - 0.1F;
                                float f25 = f14 * f20;
                                poseStack.translate((double) (f25 * 0.0F), (double) (f25 * 0.004F),
                                        (double) (f25 * 0.0F));
                            }

                            poseStack.translate((double) (f10 * 0.0F), (double) (f10 * 0.0F), (double) (f10 * 0.04F));
                            poseStack.scale(1.0F, 1.0F, 1.0F + f10 * 0.2F);
                            poseStack.mulPose(Axis.YP.rotation((float) i * -45.0F * (float) Math.PI / 180.0F));
                        } else {
                            float f5 = -0.4F * Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);
                            float f9 = 0.2F * Mth.sin(Mth.sqrt(swingProgress) * (float) (Math.PI * 2));
                            float f13 = -0.2F * Mth.sin(swingProgress * (float) Math.PI);
                            poseStack.translate((double) ((float) i * f5), (double) f9, (double) f13);
                            applyItemArmTransform(poseStack, humanoidarm, equippedProg);
                            applyItemArmAttackTransform(poseStack, humanoidarm, swingProgress);
                            if (flag1 && swingProgress < 0.001F && flag) {
                                poseStack.translate((double) ((float) i * -0.641864F), 0.0, 0.0);
                                poseStack.mulPose(Axis.YP.rotation((float) i * 10.0F * (float) Math.PI / 180.0F));
                            }
                        }

                        renderItem(
                                player,
                                itemStack,
                                i == 1 ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                                        : ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
                                i != 1,
                                poseStack,
                                multiBufferSource,
                                light);
                    } else {
                        boolean flag2 = humanoidarm == HumanoidArm.RIGHT;
                        if (player.isUsingItem() && player.getUseItemRemainingTicks() > 0
                                && player.getUsedItemHand() == interactionHand) {
                            switch (itemStack.getUseAnimation()) {
                                case NONE:
                                case BLOCK:
                                    applyItemArmTransform(poseStack, humanoidarm, equippedProg);
                                    break;
                                case EAT:
                                case DRINK:
                                    applyEatTransform(poseStack, partialTicks, humanoidarm, itemStack);
                                    applyItemArmTransform(poseStack, humanoidarm, equippedProg);
                                    break;
                                case BOW:
                                    applyItemArmTransform(poseStack, humanoidarm, equippedProg);
                                    poseStack.translate((double) ((float) i * -0.2785682F), 0.183444F, 0.1573153F);
                                    poseStack.mulPose(Axis.XP.rotation(-13.935F * (float) Math.PI / 180.0F));
                                    poseStack.mulPose(Axis.YP.rotation((float) i * 35.3F * (float) Math.PI / 180.0F));
                                    poseStack.mulPose(Axis.ZP.rotation((float) i * -9.785F * (float) Math.PI / 180.0F));
                                    float f8 = (float) itemStack.getUseDuration()
                                            - ((float) player.getUseItemRemainingTicks() - partialTicks + 1.0F);
                                    float f12 = f8 / 20.0F;
                                    f12 = (f12 * f12 + f12 * 2.0F) / 3.0F;
                                    f12 = Math.min(f12, 1.0F);
                                    if (f12 > 0.1F) {
                                        float f19 = Mth.sin((f8 - 0.1F) * 1.3F);
                                        float f24 = f12 - 0.1F;
                                        float f26 = f19 * f24;
                                        poseStack.translate((double) (f26 * 0.0F), (double) (f26 * 0.004F),
                                                (double) (f26 * 0.0F));
                                    }

                                    poseStack.translate((double) (f12 * 0.0F), (double) (f12 * 0.0F),
                                            (double) (f12 * 0.04F));
                                    poseStack.scale(1.0F, 1.0F, 1.0F + f12 * 0.2F);
                                    poseStack.mulPose(Axis.YP.rotation((float) i * -45.0F * (float) Math.PI / 180.0F));
                                    break;
                                case SPEAR:
                                    applyItemArmTransform(poseStack, humanoidarm, equippedProg);
                                    poseStack.translate((double) ((float) i * -0.5F), 0.7F, 0.1F);
                                    poseStack.mulPose(Axis.XP.rotation(-55.0F * (float) Math.PI / 180.0F));
                                    poseStack.mulPose(Axis.YP.rotation((float) i * 35.3F * (float) Math.PI / 180.0F));
                                    poseStack.mulPose(Axis.ZP.rotation((float) i * -9.785F * (float) Math.PI / 180.0F));
                                    float f7 = (float) itemStack.getUseDuration()
                                            - ((float) player.getUseItemRemainingTicks() - partialTicks + 1.0F);
                                    float f11 = f7 / 10.0F;
                                    f11 = Math.min(f11, 1.0F);
                                    if (f11 > 0.1F) {
                                        float f18 = Mth.sin((f7 - 0.1F) * 1.3F);
                                        float f23 = f11 - 0.1F;
                                        float f4 = f18 * f23;
                                        poseStack.translate((double) (f4 * 0.0F), (double) (f4 * 0.004F),
                                                (double) (f4 * 0.0F));
                                    }

                                    poseStack.translate(0.0, 0.0, (double) (f11 * 0.2F));
                                    poseStack.scale(1.0F, 1.0F, 1.0F + f11 * 0.2F);
                                    poseStack.mulPose(Axis.YP.rotation((float) i * -45.0F * (float) Math.PI / 180.0F));
                            }
                        } else if ((player.isUsingItem()
                                || Minecraft.getInstance().options.keyUse.isDown()
                                || animations.KillauraAutoBlock.getCurrentValue() && getAuraTarget() != null)
                                && (!animations.BlockOnlySword.getCurrentValue() || player.getMainHandItem().getItem() instanceof SwordItem || animations.allItemsAnimation.getCurrentValue())
                                && !animations.BlockMods.getCurrentMode().equals("无")) {
                            String s = animations.BlockMods.getCurrentMode().toLowerCase();
                            switch (s) {
                                case "1.7":
                                    poseStack.translate((double) ((float) i * BlockingX.getCurrentValue()),
                                            (double) (BlockingY.getCurrentValue()), -0.72F);
                                    float f17 = Mth.sin(swingProgress * swingProgress * (float) Math.PI);
                                    float f22 = Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);
                                    poseStack.mulPose(Axis.YP
                                            .rotation((float) i * (45.0F + f17 * -20.0F) * (float) Math.PI / 180.0F));
                                    poseStack.mulPose(
                                            Axis.ZP.rotation((float) i * f22 * -20.0F * (float) Math.PI / 180.0F));
                                    poseStack.mulPose(Axis.XP.rotation(f22 * -80.0F * (float) Math.PI / 180.0F));
                                    poseStack.mulPose(Axis.YP.rotation((float) i * -45.0F * (float) Math.PI / 180.0F));
                                    poseStack.scale(0.9F, 0.9F, 0.9F);
                                    poseStack.translate(-0.2F, 0.126F, 0.2F);
                                    poseStack.mulPose(Axis.XP.rotation(-102.25F * (float) Math.PI / 180.0F));
                                    poseStack.mulPose(Axis.YP.rotation((float) i * 15.0F * (float) Math.PI / 180.0F));
                                    poseStack.mulPose(Axis.ZP.rotation((float) i * 80.0F * (float) Math.PI / 180.0F));
                                    break;
                                case "推":
                                    poseStack.translate((double) ((float) i * BlockingX.getCurrentValue()),
                                            (double) (BlockingY.getCurrentValue()), -0.72F);
                                    poseStack.translate((double) ((float) i * -0.1414214F), 0.08F, 0.1414214F);
                                    poseStack.mulPose(Axis.XP.rotation(-102.25F * (float) Math.PI / 180.0F));
                                    poseStack.mulPose(Axis.YP.rotation((float) i * 13.365F * (float) Math.PI / 180.0F));
                                    poseStack.mulPose(Axis.ZP.rotation((float) i * 78.05F * (float) Math.PI / 180.0F));
                                    float f15 = Mth.sin(swingProgress * swingProgress * (float) Math.PI);
                                    float f3 = Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);
                                    poseStack.mulPose(Axis.XP.rotation(f15 * -10.0F * (float) Math.PI / 180.0F));
                                    poseStack.mulPose(Axis.YP.rotation(f15 * -10.0F * (float) Math.PI / 180.0F));
                                    poseStack.mulPose(Axis.ZP.rotation(f15 * -10.0F * (float) Math.PI / 180.0F));
                                    poseStack.mulPose(Axis.XP.rotation(f3 * -10.0F * (float) Math.PI / 180.0F));
                                    poseStack.mulPose(Axis.YP.rotation(f3 * -10.0F * (float) Math.PI / 180.0F));
                                    poseStack.mulPose(Axis.ZP.rotation(f3 * -10.0F * (float) Math.PI / 180.0F));
                            }
                        } else if (player.isAutoSpinAttack()) {
                            applyItemArmTransform(poseStack, humanoidarm, equippedProg);
                            poseStack.translate((double) ((float) i * -0.4F), 0.8F, 0.3F);
                            poseStack.mulPose(Axis.YP.rotation((float) i * 65.0F * (float) Math.PI / 180.0F));
                            poseStack.mulPose(Axis.ZP.rotation((float) i * -85.0F * (float) Math.PI / 180.0F));
                        } else {
                            applyItemArmTransform(poseStack, humanoidarm, equippedProg);
                            if ((itemStack.getItem() instanceof SwordItem || animations.allItemsAnimation.getCurrentValue()) &&
                                    (mc.options.keyUse.isDown() || (animations.KillauraAutoBlock.getCurrentValue()
                                            && getAuraTarget() != null && getAuraTarget() instanceof LivingEntity
                                            && getTargetHudEnabled()))) {
                                String s = animations.BlockMods.getCurrentMode().toLowerCase();
                                switch (s) {
                                    case "1.7":
                                        poseStack.translate((double) ((float) i * 0.56F),
                                                (double) (-0.52F), -0.72F);
                                        float f17 = Mth.sin(swingProgress * swingProgress * (float) Math.PI);
                                        float f22 = Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);
                                        poseStack.mulPose(Axis.YP.rotation(
                                                (float) i * (45.0F + f17 * -20.0F) * (float) Math.PI / 180.0F));
                                        poseStack.mulPose(
                                                Axis.ZP.rotation((float) i * f22 * -20.0F * (float) Math.PI / 180.0F));
                                        poseStack.mulPose(Axis.XP.rotation(f22 * -80.0F * (float) Math.PI / 180.0F));
                                        poseStack.mulPose(
                                                Axis.YP.rotation((float) i * -45.0F * (float) Math.PI / 180.0F));
                                        poseStack.scale(0.9F, 0.9F, 0.9F);
                                        poseStack.translate(-0.2F, 0.126F, 0.2F);
                                        poseStack.mulPose(Axis.XP.rotation(-102.25F * (float) Math.PI / 180.0F));
                                        poseStack.mulPose(
                                                Axis.YP.rotation((float) i * 15.0F * (float) Math.PI / 180.0F));
                                        poseStack.mulPose(
                                                Axis.ZP.rotation((float) i * 80.0F * (float) Math.PI / 180.0F));
                                        break;
                                    case "推":
                                        poseStack.translate((double) ((float) i * 0.56F),
                                                (double) (-0.52F), -0.72F);
                                        poseStack.translate((double) ((float) i * -0.1414214F), 0.08F, 0.1414214F);
                                        poseStack.mulPose(Axis.XP.rotation(-102.25F * (float) Math.PI / 180.0F));
                                        poseStack.mulPose(
                                                Axis.YP.rotation((float) i * 13.365F * (float) Math.PI / 180.0F));
                                        poseStack.mulPose(
                                                Axis.ZP.rotation((float) i * 78.05F * (float) Math.PI / 180.0F));
                                        float f15 = Mth.sin(swingProgress * swingProgress * (float) Math.PI);
                                        float f3 = Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);
                                        poseStack.mulPose(Axis.XP.rotation(f15 * -10.0F * (float) Math.PI / 180.0F));
                                        poseStack.mulPose(Axis.YP.rotation(f15 * -10.0F * (float) Math.PI / 180.0F));
                                        poseStack.mulPose(Axis.ZP.rotation(f15 * -10.0F * (float) Math.PI / 180.0F));
                                        poseStack.mulPose(Axis.XP.rotation(f3 * -10.0F * (float) Math.PI / 180.0F));
                                        poseStack.mulPose(Axis.YP.rotation(f3 * -10.0F * (float) Math.PI / 180.0F));
                                        poseStack.mulPose(Axis.ZP.rotation(f3 * -10.0F * (float) Math.PI / 180.0F));
                                        break;
                                    default:
                                        applyItemArmAttackTransform(poseStack, humanoidarm, swingProgress);
                                }
                            } else {
                                applyItemArmAttackTransform(poseStack, humanoidarm, swingProgress);
                            }
                        }

                        renderItem(
                                player,
                                itemStack,
                                flag2 ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                                        : ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
                                !flag2,
                                poseStack,
                                multiBufferSource,
                                light);
                    }
                }
            }

            // 双剑格挡逻辑：如果是主手渲染且启用了双剑格挡
            if (flag && dualWieldBlock.getCurrentValue()) {
                ItemStack offhandStack = player.getOffhandItem();
                
                // 只在非脚手架模式下渲染双剑
                if (!isScaffoldEnabled()) {
                    // 确定要渲染的物品
                    ItemStack renderStack;
                    
                    // 检查主手物品是否符合渲染条件
                    boolean mainHandValid = itemStack.getItem() instanceof SwordItem || isAxe(itemStack.getItem()) || allItemsAnimation.getCurrentValue();
                    
                    if (mainHandValid) {
                        // 如果主手物品符合条件，直接使用主手物品作为双剑
                        renderStack = itemStack;
                        
                        // 保存当前poseStack状态
                        poseStack.pushPose();
                        
                        // 处理副手主手样式渲染
                        HumanoidArm offhandArm = player.getMainArm().getOpposite();
                        int offhandI = offhandArm == HumanoidArm.RIGHT ? 1 : -1;
                        boolean isBlocking = isBlocking();
                        boolean isKillauraBlocking = KillauraAutoBlock.getCurrentValue() && getAuraTarget() != null;
                        
                        // 应用与主手相同的动画变换
                        if (isBlocking || isKillauraBlocking) {
                            String s = BlockMods.getCurrentMode().toLowerCase();
                            switch (s) {
                                case "1.7":
                                    poseStack.translate((double) ((float) offhandI * BlockingX.getCurrentValue()),
                                            (double) (BlockingY.getCurrentValue()), -0.72F);
                                    float f17 = Mth.sin(swingProgress * swingProgress * (float) Math.PI);
                                    float f22 = Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);
                                    poseStack.mulPose(Axis.YP
                                            .rotation((float) offhandI * (45.0F + f17 * -20.0F) * (float) Math.PI / 180.0F));
                                    poseStack.mulPose(
                                            Axis.ZP.rotation((float) offhandI * f22 * -20.0F * (float) Math.PI / 180.0F));
                                    poseStack.mulPose(Axis.XP.rotation(f22 * -80.0F * (float) Math.PI / 180.0F));
                                    poseStack.mulPose(Axis.YP.rotation((float) offhandI * -45.0F * (float) Math.PI / 180.0F));
                                    poseStack.scale(0.9F, 0.9F, 0.9F);
                                    poseStack.translate(-0.2F, 0.126F, 0.2F);
                                    poseStack.mulPose(Axis.XP.rotation(-102.25F * (float) Math.PI / 180.0F));
                                    poseStack.mulPose(Axis.YP.rotation((float) offhandI * 15.0F * (float) Math.PI / 180.0F));
                                    poseStack.mulPose(Axis.ZP.rotation((float) offhandI * 80.0F * (float) Math.PI / 180.0F));
                                    break;
                                case "推":
                                    poseStack.translate((double) ((float) offhandI * BlockingX.getCurrentValue()),
                                            (double) (BlockingY.getCurrentValue()), -0.72F);
                                    poseStack.translate((double) ((float) offhandI * -0.1414214F), 0.08F, 0.1414214F);
                                    poseStack.mulPose(Axis.XP.rotation(-102.25F * (float) Math.PI / 180.0F));
                                    poseStack.mulPose(Axis.YP.rotation((float) offhandI * 13.365F * (float) Math.PI / 180.0F));
                                    poseStack.mulPose(Axis.ZP.rotation((float) offhandI * 78.05F * (float) Math.PI / 180.0F));
                                    float f15 = Mth.sin(swingProgress * swingProgress * (float) Math.PI);
                                    float f3 = Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);
                                    poseStack.mulPose(Axis.XP.rotation(f15 * -10.0F * (float) Math.PI / 180.0F));
                                    poseStack.mulPose(Axis.YP.rotation(f15 * -10.0F * (float) Math.PI / 180.0F));
                                    poseStack.mulPose(Axis.ZP.rotation(f15 * -10.0F * (float) Math.PI / 180.0F));
                                    poseStack.mulPose(Axis.XP.rotation(f3 * -10.0F * (float) Math.PI / 180.0F));
                                    poseStack.mulPose(Axis.YP.rotation(f3 * -10.0F * (float) Math.PI / 180.0F));
                                    poseStack.mulPose(Axis.ZP.rotation(f3 * -10.0F * (float) Math.PI / 180.0F));
                                    break;
                                default:
                                    applyItemArmTransform(poseStack, offhandArm, equippedProg);
                                    applyItemArmAttackTransform(poseStack, offhandArm, swingProgress);
                            }
                        } else {
                            // 非格挡状态下的动画
                            applyItemArmTransform(poseStack, offhandArm, equippedProg);
                            applyItemArmAttackTransform(poseStack, offhandArm, swingProgress);
                        }
                        
                        // 渲染物品为主要样式
                        renderItem(
                                player,
                                renderStack,
                                offhandArm == HumanoidArm.RIGHT ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                                        : ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
                                offhandArm != HumanoidArm.RIGHT,
                                poseStack,
                                multiBufferSource,
                                light);
                        
                        // 恢复poseStack状态
                        poseStack.popPose();
                    }
                }
            }

            poseStack.popPose();
        }
    }

    private LivingEntity getAuraTarget() {
        Aura aura = (Aura) Naven.getInstance().getModuleManager().getModule(Aura.class);
        if (aura != null && aura.isEnabled()) {
            try {
                java.lang.reflect.Field targetField = Aura.class.getDeclaredField("target");
                targetField.setAccessible(true);
                return (LivingEntity) targetField.get(null);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    private boolean getTargetHudEnabled() {
        Aura aura = (Aura) Naven.getInstance().getModuleManager().getModule(Aura.class);
        if (aura != null && aura.isEnabled()) {
            try {
                java.lang.reflect.Field targetHudField = Aura.class.getDeclaredField("targetHud");
                targetHudField.setAccessible(true);
                Object targetHudValue = targetHudField.get(aura);
                if (targetHudValue != null) {
                    java.lang.reflect.Method getCurrentValueMethod = targetHudValue.getClass().getMethod("getCurrentValue");
                    return (Boolean) getCurrentValueMethod.invoke(targetHudValue);
                }
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }
    
    /**
     * 查找快捷栏中第一把剑或斧子
     * @return 找到的剑或斧子物品栈，如果没有则返回空物品栈
     */
    private ItemStack findFirstWeaponInHotbar() {
        LocalPlayer player = mc.player;
        if (player == null) {
            return ItemStack.EMPTY;
        }
        
        // 遍历快捷栏（0-8槽位）
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            // 检查是否为剑或斧子
            if (!stack.isEmpty() && (stack.getItem() instanceof SwordItem || isAxe(stack.getItem()))) {
                return stack;
            }
        }
        
        return ItemStack.EMPTY;
    }
    
    /**
     * 检查物品是否为斧子
     * @param item 要检查的物品
     * @return 如果是斧子返回true，否则返回false
     */
    private boolean isAxe(net.minecraft.world.item.Item item) {
        // 使用类型检查，确保能正确识别所有斧子类型
        return item instanceof net.minecraft.world.item.AxeItem || 
               item.getClass().getSimpleName().contains("Axe") || 
               (item.getClass().getSuperclass() != null && item.getClass().getSuperclass().getSimpleName().contains("Axe"));
    }

    private void renderPlayerArm(PoseStack poseStack, MultiBufferSource bufferSource, int light, float equippedProg,
                                 float swingProgress, HumanoidArm arm) {
        boolean flag = arm == HumanoidArm.RIGHT;
        float f = flag ? 1.0F : -1.0F;
        float f1 = Mth.sqrt(swingProgress);
        float f2 = -0.3F * Mth.sin(f1 * (float) Math.PI);
        float f3 = 0.4F * Mth.sin(f1 * (float) (Math.PI * 2));
        float f4 = -0.4F * Mth.sin(swingProgress * (float) Math.PI);
        poseStack.translate((double) (f * (0.644764F + f2)), (double) (0.644764F + f3), (double) (0.644764F + f4));
        poseStack.mulPose(Axis.XP.rotation(-0.3F * Mth.sin(f1 * (float) (Math.PI * 2))));
        poseStack.mulPose(Axis.YP.rotation(f * 0.4F * Mth.sin(f1 * (float) Math.PI)));
        poseStack.mulPose(Axis.ZP.rotation(f * -0.4F * Mth.sin(swingProgress * (float) Math.PI)));
        float f5 = Mth.lerp(equippedProg, oMainHandHeight, mainHandHeight);
        float f6 = Mth.lerp(equippedProg, oOffHandHeight, offHandHeight);
        this.renderItem(mc.player, flag ? mainHandItem : offHandItem,
                flag ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND, !flag,
                poseStack, bufferSource, light);
    }

    private void renderTwoHandedMap(PoseStack poseStack, MultiBufferSource bufferSource, int light, float equipProgress,
                                    float equippedProg, float swingProgress) {
        float f = Mth.sqrt(swingProgress);
        float f1 = -0.2F * Mth.sin(swingProgress * (float) Math.PI);
        float f2 = -0.4F * Mth.sin(f * (float) Math.PI);
        poseStack.translate(0.0D, (double) (-f1 / 2.0F), (double) f2);
        float f3 = Mth.lerp(equippedProg, oMainHandHeight, mainHandHeight);
        float f4 = Mth.lerp(equippedProg, oOffHandHeight, offHandHeight);
        this.renderItem(mc.player, mainHandItem, ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, false, poseStack,
                bufferSource, light);
        this.renderItem(mc.player, offHandItem, ItemDisplayContext.FIRST_PERSON_LEFT_HAND, true, poseStack,
                bufferSource, light);
    }

    private void renderOneHandedMap(PoseStack poseStack, MultiBufferSource bufferSource, int light, float equippedProg,
                                    HumanoidArm arm, float swingProgress, ItemStack item) {
        float f = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
        poseStack.translate((double) (f * 0.125F), 0.0D, 0.0D);
        float f1 = Mth.sqrt(swingProgress);
        float f2 = -0.1F * Mth.sin(f1 * (float) Math.PI);
        float f3 = -0.3F * Mth.sin(f1 * (float) (Math.PI * 2));
        float f4 = -0.4F * Mth.sin(swingProgress * (float) Math.PI);
        poseStack.translate(0.0D, (double) (-f2 / 2.0F), (double) f4);
        poseStack.mulPose(Axis.XP.rotation(f3 * (float) Math.PI / 180.0F));
        poseStack.mulPose(Axis.YP.rotation(f * f1 * (float) Math.PI / 180.0F));
        poseStack.mulPose(Axis.ZP.rotation(f * f2 * (float) Math.PI / 180.0F));
        float f5 = Mth.lerp(equippedProg, oMainHandHeight, mainHandHeight);
        float f6 = Mth.lerp(equippedProg, oOffHandHeight, offHandHeight);
        this.renderItem(mc.player, item,
                arm == HumanoidArm.RIGHT ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                        : ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
                arm != HumanoidArm.RIGHT, poseStack, bufferSource, light);
    }

    private void applyItemArmTransform(PoseStack poseStack, HumanoidArm arm, float equippedProg) {
        int i = arm == HumanoidArm.RIGHT ? 1 : -1;
        float f = Mth.lerp(equippedProg, oMainHandHeight, mainHandHeight);
        float f1 = Mth.lerp(equippedProg, oOffHandHeight, offHandHeight);
        poseStack.translate((double) ((float) i * 0.56F), (double) (-0.52F + f * -0.6F), -0.72F);
    }

    private void applyItemArmAttackTransform(PoseStack poseStack, HumanoidArm arm, float swingProgress) {
        int i = arm == HumanoidArm.RIGHT ? 1 : -1;
        float f = Mth.sin(swingProgress * swingProgress * (float) Math.PI);
        float f1 = Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);
        poseStack.translate((double) ((float) i * 0.56F), (double) (-0.52F), -0.72F);
        poseStack.mulPose(Axis.XP.rotation(-102.25F * (float) Math.PI / 180.0F));
        poseStack.mulPose(Axis.YP.rotation((float) i * 13.365F * (float) Math.PI / 180.0F));
        poseStack.mulPose(Axis.ZP.rotation((float) i * 78.05F * (float) Math.PI / 180.0F));
        float swingFactor = Mth.clamp(swingProgress, 0.0F, 1.0F);
        poseStack.mulPose(Axis.XP.rotation(f * -15.0F * swingFactor * (float) Math.PI / 180.0F));
        poseStack.mulPose(Axis.YP.rotation(f1 * -15.0F * swingFactor * (float) Math.PI / 180.0F));
        poseStack.mulPose(Axis.ZP.rotation(f1 * -70.0F * swingFactor * (float) Math.PI / 180.0F));
    }

    private void applyEatTransform(PoseStack poseStack, float partialTicks, HumanoidArm arm, ItemStack item) {
        float f = (float) item.getUseDuration() - ((float) mc.player.getUseItemRemainingTicks() - partialTicks + 1.0F);
        float f1 = f / (float) item.getUseDuration();
        if (f1 < 0.8F) {
            float f2 = Mth.abs(Mth.cos(f / 4.0F * (float) Math.PI) * 0.1F);
            poseStack.translate(0.0D, (double) f2, 0.0D);
        }
        float f3 = 1.0F - (float) Math.pow((double) (1.0F - f1), 27.0D);
        int i = arm == HumanoidArm.RIGHT ? 1 : -1;
        poseStack.translate((double) (f3 * 0.6F * (float) i), (double) (f3 * -0.5F), (double) (f3 * 0.0F));
        poseStack.mulPose(Axis.YP.rotation((float) i * f3 * 90.0F * (float) Math.PI / 180.0F));
        poseStack.mulPose(Axis.XP.rotation(f3 * 10.0F * (float) Math.PI / 180.0F));
        poseStack.mulPose(Axis.ZP.rotation((float) i * f3 * 30.0F * (float) Math.PI / 180.0F));
    }

    private void renderItem(LivingEntity entity, ItemStack stack,
                            ItemDisplayContext transformType, boolean leftHand,
                            PoseStack poseStack, MultiBufferSource buffer, int light) {
        if (stack.isEmpty())
            return;
        ItemRenderer itemRenderer = mc.getItemRenderer();
        itemRenderer.renderStatic(entity, stack, transformType, leftHand, poseStack, buffer, entity.level(), light, 0,
                0);
    }

   public String getLocalizedName() {
      return LanguageManager.getInstance().getLocalizedString("动作", "Animations");
   }

   public String getLocalizedDescription() {
      return LanguageManager.getInstance().getLocalizedString("允许你修改游戏中的许多动作", "Allows you to modify many actions in the game");
   }
}