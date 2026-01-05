package com.heypixel.heypixelmod.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public final class DamageUtils {

    private static final Minecraft mc = Minecraft.getInstance();

    public enum ExplosionType {
        ANCHOR(5.0F),
        CRYSTAL(6.0F);

        private final float power;

        ExplosionType(float power) {
            this.power = power;
        }

        public float getPower() {
            return this.power;
        }
    }

    public static float getExplosionDamage(LivingEntity entity, BlockPos explosionCenter, ExplosionType type, @Nullable BlockPos simulatedBlockPos) {
        if (mc.level == null || mc.player == null) {
            return 0.0f;
        }

        Vec3 explosionVec = Vec3.atCenterOf(explosionCenter);
        float explosionPower = type.getPower();

        double distance = entity.position().distanceTo(explosionVec) / (double) (explosionPower * 2.0F);
        if (distance > 1.0) {
            return 0.0f;
        }

        float exposure = getExplosionExposure(mc.level, explosionVec, entity, simulatedBlockPos);
        float impact = (1.0F - (float) distance) * exposure;
        float baseDamage = (impact * impact + impact) / 2.0F * 7.0F * (explosionPower * 2.0F) + 1.0F;

        switch (mc.level.getDifficulty()) {
            case PEACEFUL -> baseDamage = 0.0F;
            case EASY -> baseDamage = Math.min(baseDamage / 2.0F + 1.0F, baseDamage);
            case HARD -> baseDamage = baseDamage * 1.5F;
        }

        return getDamageAfterProtection(entity, baseDamage, mc.level.damageSources().explosion(null, null));
    }

    private static float getExplosionExposure(BlockGetter world, Vec3 source, LivingEntity entity, @Nullable BlockPos simulatedBlockPos) {
        AABB aabb = entity.getBoundingBox();
        double step = 1.0 / (Math.max(aabb.getXsize(), Math.max(aabb.getYsize(), aabb.getZsize())) * 2.0);
        if (step <= 0) return 0.0f;

        int totalRays = 0;
        int unobstructedRays = 0;

        for (double dx = 0.0; dx <= 1.0; dx += step) {
            for (double dy = 0.0; dy <= 1.0; dy += step) {
                for (double dz = 0.0; dz <= 1.0; dz += step) {
                    double x = Mth.lerp(dx, aabb.minX, aabb.maxX);
                    double y = Mth.lerp(dy, aabb.minY, aabb.maxY);
                    double z = Mth.lerp(dz, aabb.minZ, aabb.maxZ);
                    Vec3 target = new Vec3(x, y, z);

                    if (!isObstructed(world, source, target, simulatedBlockPos)) {
                        unobstructedRays++;
                    }
                    totalRays++;
                }
            }
        }

        return totalRays == 0 ? 0.0f : (float) unobstructedRays / (float) totalRays;
    }

    private static boolean isObstructed(BlockGetter world, Vec3 start, Vec3 end, @Nullable BlockPos simulatedBlockPos) {
        BlockGetter customWorld = new BlockGetter() {

            @Override
            public BlockState getBlockState(BlockPos pos) {
                if (pos.equals(simulatedBlockPos)) {
                    return Blocks.OBSIDIAN.defaultBlockState();
                }
                return world.getBlockState(pos);
            }

            @Override
            public FluidState getFluidState(BlockPos pos) {
                return world.getFluidState(pos);
            }

            @Override
            public int getHeight() {
                return world.getHeight();
            }

            @Override
            public int getMinBuildHeight() {
                return world.getMinBuildHeight();
            }

            @Nullable
            @Override
            public BlockEntity getBlockEntity(BlockPos pos) {

                if (pos.equals(simulatedBlockPos)) {
                    return null;
                }
                return world.getBlockEntity(pos);
            }
        };

        ClipContext context = new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player);

        return BlockGetter.traverseBlocks(start, end, context,
                (ctx, pos) -> {
                    BlockState state = customWorld.getBlockState(pos);
                    if (!state.isAir() && state.getBlock().getExplosionResistance() >= 600) {
                        return state.getCollisionShape(customWorld, pos).clip(start, end, pos);
                    }
                    return null;
                },
                (ctx) -> null
        ) != null;
    }

    private static float getDamageAfterProtection(LivingEntity entity, float damage, DamageSource source) {
        if (entity.hasEffect(MobEffects.DAMAGE_RESISTANCE) && !source.is(DamageTypeTags.BYPASSES_RESISTANCE)) {
            int level = entity.getEffect(MobEffects.DAMAGE_RESISTANCE).getAmplifier() + 1;
            damage *= (1.0f - level * 0.2f);
        }

        if (damage <= 0.0f) return 0.0f;

        damage = net.minecraft.world.damagesource.CombatRules.getDamageAfterAbsorb(
                damage,
                (float) entity.getArmorValue(),
                (float) entity.getAttributeValue(Attributes.ARMOR_TOUGHNESS)
        );

        int protectionLevel = EnchantmentHelper.getDamageProtection(entity.getArmorSlots(), source);
        if (protectionLevel > 0) {
            damage = net.minecraft.world.damagesource.CombatRules.getDamageAfterMagicAbsorb(damage, (float) protectionLevel);
        }

        return Math.max(damage, 0.0f);
    }
}