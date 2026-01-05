package com.heypixel.heypixelmod.obsoverlay.modules.impl.combat;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventAttackSlowdown;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventAttackYaw;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventClick;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventDestroyBlock;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMotion;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventPacket;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventPositionItem;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRender;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventUseItemRayTrace;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventUpdateHeldItem;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.utils.NetworkUtils;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundResourcePackPacket;
import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import net.minecraft.network.protocol.login.ServerboundHelloPacket;
import net.minecraft.network.protocol.login.ServerboundKeyPacket;
import net.minecraft.network.protocol.status.ServerboundPingRequestPacket;
import net.minecraft.network.protocol.status.ServerboundStatusRequestPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import com.heypixel.heypixelmod.obsoverlay.utils.RenderUtils;

@ModuleInfo(
        name = "FakeLag",
        description = "延迟发送数据包，使玩家在服务器上的位置与客户端显示位置产生差异",
        category = Category.COMBAT
)
public class FakeLag extends Module {
    // 白名单数据包类型
    public static final Set<Class<?>> whitelist = new HashSet<Class<?>>() {
        {
            this.add(ClientIntentionPacket.class);
            this.add(ServerboundStatusRequestPacket.class);
            this.add(ServerboundPingRequestPacket.class);
            this.add(ServerboundHelloPacket.class);
            this.add(ServerboundKeyPacket.class);
        }
    };
    
    // 数据包队列
    private final ConcurrentLinkedQueue<Packet<?>> packets = new ConcurrentLinkedQueue<>();
    
    // 配置选项
    private final FloatValue maxTicks = ValueBuilder.create(this, "最大Tick")
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(25.0F)
            .setDefaultFloatValue(20.0F)
            .setFloatStep(1.0F)
            .build()
            .getFloatValue();
    
    private final FloatValue tntDistance = ValueBuilder.create(this, "TNT距离")
            .setMinFloatValue(3.0F)
            .setMaxFloatValue(10.0F)
            .setDefaultFloatValue(5.0F)
            .setFloatStep(0.1F)
            .build()
            .getFloatValue();
    
    private final FloatValue playerDistance = ValueBuilder.create(this, "玩家检测范围")
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(20.0F)
            .setDefaultFloatValue(4.0F)
            .setFloatStep(0.1F)
            .build()
            .getFloatValue();
    
    private final FloatValue recoilDelay = ValueBuilder.create(this, "后座延迟 (ms)")
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(2000.0F)
            .setDefaultFloatValue(500.0F)
            .setFloatStep(50.0F)
            .build()
            .getFloatValue();
    
    private final FloatValue packetDelay = ValueBuilder.create(this, "数据包延迟 (ms)")
            .setMinFloatValue(50.0F)
            .setMaxFloatValue(2000.0F)
            .setDefaultFloatValue(500.0F)
            .setFloatStep(50.0F)
            .build()
            .getFloatValue();
    
    private final FloatValue fov = ValueBuilder.create(this, "检测FOV")
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(360.0F)
            .setDefaultFloatValue(180.0F)
            .setFloatStep(5.0F)
            .build()
            .getFloatValue();
    
    // 状态变量
    private int releasedTicks = 0;
    private long lastAttackTime = 0;
    private boolean inRecoilDelay = false;
    private long packetStartTime = 0; // 记录队列中最早的数据包时间
    private long packetsAccumulatedStartTime = 0; // 记录开始积攒数据包的时间
    private float lastHealth = 20.0F; // 用于检测玩家是否受到伤害
    private int movePacketCount = 0; // 移动数据包计数器
    private long lastPlayerCheckTime = 0; // 记录上次检查玩家的时间
    private boolean cachedHasNearbyPlayersInFOV = false; // 缓存的玩家检测结果
    private static final long PLAYER_CHECK_INTERVAL = 50; // 玩家检测缓存间隔（毫秒）
    
    // 检查基本状态条件
    private boolean shouldReleaseDueToBasicConditions() {
        Player player = mc.player;
        if (player == null) return false;
        
        long currentTime = System.currentTimeMillis();
        
        // 玩家死亡时
        if (player.isDeadOrDying() || player.getHealth() <= 0.0F) {
            return true;
        }
        
        // 玩家在水中时
        if (player.isInWater()) {
            return true;
        }
        
        // 打开了游戏界面时
        if (mc.screen != null) {
            return true;
        }
        
        // 后坐力恢复时间未到
        if (this.inRecoilDelay) {
            return false;
        }
        
        // 暂时注释掉这个条件，避免数据包被立即释放
        // 队列中最早的数据包超过了配置的延迟时间
        // if (this.packets.size() > 0 && packetStartTime > 0 && currentTime - packetStartTime >= this.packetDelay.getCurrentValue()) {
        //     return true;
        // }
        
        return false;
    }
    
    // 检查物品使用触发条件
    private boolean isUsingConsumable(ItemStack stack) {
        if (stack.isEmpty()) return false;
        
        // 食物
        if (stack.isEdible()) return true;
        
        // 药水
        if (stack.getItem() == Items.POTION || stack.getItem() == Items.SPLASH_POTION || stack.getItem() == Items.LINGERING_POTION) {
            return true;
        }
        
        // 牛奶桶
        if (stack.getItem() == Items.MILK_BUCKET) {
            return true;
        }
        
        return false;
    }
    
    // 检查是否有可攻击的实体在范围内
    private boolean hasAttackableEntities() {
        int attackableEntities = 0;
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof Player && entity != mc.player && entity.distanceTo(mc.player) <= this.playerDistance.getCurrentValue()) {
                attackableEntities++;
                break; // 找到一个即可返回，无需遍历所有
            }
        }
        return attackableEntities > 0;
    }
    
    // 检查是否应该释放数据包
    private boolean shouldReleasePackets() {
        return shouldReleaseDueToBasicConditions();
    }
    
    // 获取当前缓存的移动数据包数量
    private long getBlinkTicks() {
        return this.movePacketCount;
    }
    
    // 释放单个数据包
    private void releaseTick() {
        while (!this.packets.isEmpty()) {
            Packet<?> poll = this.packets.poll();
            NetworkUtils.sendPacketNoEvent(poll);
            if (poll instanceof ServerboundMovePlayerPacket) {
                this.releasedTicks++;
                this.movePacketCount--;
                break;
            }
        }
    }
    
    // 释放所有数据包
    private void releaseAllPackets() {
        while (!this.packets.isEmpty()) {
            this.releaseTick();
        }
        // 重置数据包开始时间
        this.packetStartTime = 0;
        this.packetsAccumulatedStartTime = 0; // 重置积攒开始时间
        this.movePacketCount = 0; // 重置移动数据包计数器
    }
    
    // 检查是否有TNT靠近
    private boolean isTNTNear(double distance) {
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof PrimedTnt && entity.distanceTo(mc.player) <= distance) {
                return true; // 找到一个即可返回，无需遍历所有
            }
        }
        return false;
    }
    
    // 检查实体是否在玩家的视野FOV范围内
    private boolean isEntityInFOV(Entity entity) {
        if (mc.player == null || entity == null) return false;
        
        // 计算实体相对于玩家的角度
        double deltaX = entity.getX() - mc.player.getX();
        double deltaZ = entity.getZ() - mc.player.getZ();
        double entityYaw = Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0D;
        
        // 处理角度范围
        if (entityYaw < 0.0D) {
            entityYaw += 360.0D;
        }
        
        double playerYaw = mc.player.getYRot();
        if (playerYaw < 0.0D) {
            playerYaw += 360.0D;
        }
        
        // 计算角度差
        double deltaYaw = Math.abs(entityYaw - playerYaw);
        if (deltaYaw > 180.0D) {
            deltaYaw = 360.0D - deltaYaw;
        }
        
        // 检查是否在FOV范围内
        return deltaYaw <= this.fov.getCurrentValue() / 2.0D;
    }
    
    // 检查周围是否有玩家，且至少有一个在视野FOV范围内
    private boolean isPlayerNearAndInFOV(double distance) {
        long currentTime = System.currentTimeMillis();
        // 检查缓存是否有效
        if (currentTime - this.lastPlayerCheckTime < PLAYER_CHECK_INTERVAL) {
            return this.cachedHasNearbyPlayersInFOV;
        }
        
        // 缓存无效，重新检测
        boolean result = false;
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof Player && entity != mc.player && entity.distanceTo(mc.player) <= distance && isEntityInFOV(entity)) {
                result = true;
                break; // 找到一个即可返回，无需遍历所有
            }
        }
        
        // 更新缓存
        this.cachedHasNearbyPlayersInFOV = result;
        this.lastPlayerCheckTime = currentTime;
        return result;
    }
    
    @EventTarget
    public void onRender(EventRender e) {
        // 减少渲染频率：只在移动数据包较多时才渲染
        if (!this.packets.isEmpty() && this.movePacketCount > 5 && mc.player != null) {
            long currentTime = System.currentTimeMillis();
            // 当有数据包积攒且未超过2秒时，渲染蓝色半透明box
            if (packetsAccumulatedStartTime > 0 && currentTime - packetsAccumulatedStartTime < 2000) {
                // 获取玩家的AABB
                AABB playerBox = mc.player.getBoundingBox();
                
                // 设置渲染状态
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                RenderSystem.disableDepthTest();
                RenderSystem.setShaderColor(0.0F, 0.0F, 1.0F, 0.5F); // 蓝色半透明
                
                // 绘制蓝色半透明box
                RenderUtils.drawSolidBox(playerBox, e.getPMatrixStack());
                
                // 恢复渲染状态
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                RenderSystem.enableDepthTest();
                RenderSystem.disableBlend();
            }
        }
    }
    
    @Override
    public void onEnable() {
        this.packets.clear();
        this.releasedTicks = 0;
        this.lastAttackTime = 0;
        this.inRecoilDelay = false;
    }
    
    @Override
    public void onDisable() {
        // 释放所有缓存的数据包
        this.releaseAllPackets();
    }
    
    @EventTarget
    public void onMotion(EventMotion e) {
        if (e.getType() == EventType.PRE && mc.player != null) {
            long currentTime = System.currentTimeMillis();
            
            // 检测玩家是否受到伤害
            float currentHealth = mc.player.getHealth();
            if (currentHealth < this.lastHealth) {
                // 玩家受到伤害，释放所有数据包
                this.releaseAllPackets();
            }
            this.lastHealth = currentHealth;
            
            // 更新后座延迟状态
            if (this.inRecoilDelay && currentTime - this.lastAttackTime >= this.recoilDelay.getCurrentValue()) {
                this.inRecoilDelay = false;
            }
            
            this.setSuffix(this.getBlinkTicks() + " Ticks Behind");
            this.releasedTicks = 0;
            
            // 检查后坐力延迟状态
            if (!this.inRecoilDelay) {
                // 只有在后坐力延迟结束后才释放数据包
                // 达到最大Tick数时释放数据包
                while ((float) this.releasedTicks < 20.0F && (float) this.getBlinkTicks() >= this.maxTicks.getCurrentValue() && !this.packets.isEmpty()) {
                    this.releaseTick();
                }
            }
        }
    }
    
    @EventTarget
    public void onAttackSlowdown(EventAttackSlowdown e) {
        // 攻击时释放所有数据包
        if (!this.inRecoilDelay) {
            this.releaseAllPackets();
            this.lastAttackTime = System.currentTimeMillis();
            this.inRecoilDelay = true;
        }
    }
    
    @EventTarget
    public void onAttackYaw(EventAttackYaw e) {
        // 攻击时释放所有数据包
        if (!this.inRecoilDelay) {
            this.releaseAllPackets();
            this.lastAttackTime = System.currentTimeMillis();
            this.inRecoilDelay = true;
        }
    }
    
    // 暂时注释掉这些事件处理器，避免数据包被立即释放
    /*
    // 点击事件（实体交互和方块交互）
    @EventTarget
    public void onEventClick(EventClick e) {
        if (!this.inRecoilDelay) {
            this.releaseAllPackets();
            this.lastAttackTime = System.currentTimeMillis();
            this.inRecoilDelay = true;
        }
    }
    
    // 方块破坏事件
    @EventTarget
    public void onDestroyBlock(EventDestroyBlock e) {
        if (!this.inRecoilDelay) {
            this.releaseAllPackets();
            this.lastAttackTime = System.currentTimeMillis();
            this.inRecoilDelay = true;
        }
    }
    
    // 物品使用事件
    @EventTarget
    public void onUseItemRayTrace(EventUseItemRayTrace e) {
        Player player = mc.player;
        if (player == null) return;
        
        ItemStack stack = player.getMainHandItem();
        
        // 玩家使用消耗品时
        if (this.isUsingConsumable(stack)) {
            this.releaseAllPackets();
            this.lastAttackTime = System.currentTimeMillis();
            this.inRecoilDelay = true;
        } else {
            // 动作时（使用物品等）
            if (!this.inRecoilDelay) {
                this.releaseAllPackets();
                this.lastAttackTime = System.currentTimeMillis();
                this.inRecoilDelay = true;
            }
        }
    }
    
    // 物品切换事件
    @EventTarget
    public void onUpdateHeldItem(EventUpdateHeldItem e) {
        if (!this.inRecoilDelay) {
            this.releaseAllPackets();
            this.lastAttackTime = System.currentTimeMillis();
            this.inRecoilDelay = true;
        }
    }
    
    // 物品位置更新事件
    @EventTarget
    public void onPositionItem(EventPositionItem e) {
        if (!this.inRecoilDelay) {
            this.releaseAllPackets();
            this.lastAttackTime = System.currentTimeMillis();
            this.inRecoilDelay = true;
        }
    }
    */
    
    // 处理来自NoSlow模块的数据包
    public void handlePacketFromNoSlow(Packet<?> packet) {
        // 检查是否为白名单数据包
        if (whitelist.contains(packet.getClass())) {
            // 白名单数据包直接发送
            if (mc.getConnection() != null) {
                mc.getConnection().getConnection().send(packet);
            }
            return;
        }
        
        // 检查是否为资源包状态数据包
        if (packet instanceof ServerboundResourcePackPacket) {
            this.releaseAllPackets();
            return;
        }
        
        // 检查是否有玩家在范围内且在FOV内
        boolean hasNearbyPlayersInFOV = this.isPlayerNearAndInFOV((double) this.playerDistance.getCurrentValue());
        
        // 只有当周围有玩家且在FOV内时才暂存数据包
        if (hasNearbyPlayersInFOV) {
            this.packets.offer(packet);
            if (packet instanceof ServerboundMovePlayerPacket) {
                this.movePacketCount++;
            }
            
            // 更新队列中最早的数据包时间
            if (this.packets.size() == 1) {
                this.packetStartTime = System.currentTimeMillis();
                this.packetsAccumulatedStartTime = System.currentTimeMillis(); // 开始积攒数据包的时间
            }
        } else {
            // 离开玩家范围或不在FOV内时直接发送数据包
            if (mc.getConnection() != null) {
                mc.getConnection().getConnection().send(packet);
            }
        }
    }
    
    @EventTarget
    public void onPacket(EventPacket event) {
        if (!isEnabled())
            return;
        
        // 处理接收的数据包
        if (event.getType() == EventType.RECEIVE) {
            // 暂时移除被攻击检测，避免编译错误
            return;
        }
        
        // 处理发送的数据包
        if (event.getType() == EventType.SEND && mc.player != null && !event.isCancelled()) {
            // 白名单数据包直接发送
            if (whitelist.contains(event.getPacket().getClass())) {
                return;
            }
            
            // 资源包状态数据包
            if (event.getPacket() instanceof ServerboundResourcePackPacket) {
                this.releaseAllPackets();
                return;
            }
            
            // 检查是否有玩家在范围内且在FOV内
            boolean hasNearbyPlayersInFOV = this.isPlayerNearAndInFOV((double) this.playerDistance.getCurrentValue());
            
            // 只有当周围有玩家且在FOV内时才暂存数据包
            if (hasNearbyPlayersInFOV) {
                event.setCancelled(true);
                this.packets.offer(event.getPacket());
                if (event.getPacket() instanceof ServerboundMovePlayerPacket) {
                    this.movePacketCount++;
                }
                
                // 更新队列中最早的数据包时间
                if (this.packets.size() == 1) {
                    this.packetStartTime = System.currentTimeMillis();
                    this.packetsAccumulatedStartTime = System.currentTimeMillis(); // 开始积攒数据包的时间
                }
            } else {
                // 离开玩家范围或不在FOV内时释放所有数据包，避免被反作弊拉回
                this.releaseAllPackets();
                this.packetsAccumulatedStartTime = 0; // 重置积攒开始时间
            }
        }
    }
}