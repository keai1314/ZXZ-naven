package com.heypixel.heypixelmod.obsoverlay.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

public class ItemUtil {
    private static final Minecraft mc = Minecraft.getInstance();

    public static boolean hasRawUnbreakingEnchant() {
        ItemStack itemStack = mc.player.getMainHandItem();
        if (itemStack == null) {
            return false;
        }
        if (EnchantmentHelper.getItemEnchantmentLevel(Enchantments.UNBREAKING, itemStack) > 0) {
            return true;
        }
        return itemStack.getItem() == Items.DIAMOND_SWORD || itemStack.getItem() == Items.IRON_SWORD || itemStack.getItem() == Items.GOLDEN_SWORD || itemStack.getItem() == Items.NETHERITE_SWORD || itemStack.getItem() == Items.WOODEN_SWORD;
    }

    public static boolean isHoldingTool() {
        ItemStack itemStack = mc.player.getMainHandItem();
        if (itemStack == null) {
            return false;
        }
        return itemStack.getItem() == Items.DIAMOND_SWORD || itemStack.getItem() == Items.IRON_SWORD || itemStack.getItem() == Items.GOLDEN_SWORD || itemStack.getItem() == Items.NETHERITE_SWORD || itemStack.getItem() == Items.WOODEN_SWORD;
    }

    public static boolean isHoldingSword() {
        ItemStack itemStack = mc.player.getMainHandItem();
        if (itemStack == null) {
            return false;
        }
        return itemStack.getItem() == Items.DIAMOND_SWORD || itemStack.getItem() == Items.IRON_SWORD || itemStack.getItem() == Items.GOLDEN_SWORD || itemStack.getItem() == Items.NETHERITE_SWORD || itemStack.getItem() == Items.WOODEN_SWORD;
    }
}
