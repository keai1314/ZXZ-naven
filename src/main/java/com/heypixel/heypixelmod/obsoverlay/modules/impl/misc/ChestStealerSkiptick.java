package com.heypixel.heypixelmod.obsoverlay.modules.impl.misc;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMotion;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.move.Scaffold;
import com.heypixel.heypixelmod.obsoverlay.utils.InventoryUtils;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import com.heypixel.heypixelmod.obsoverlay.ui.LanguageManager;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.*;

import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 高质量的基于Skiptick的箱子自动拿取模块
 * 采用模块化设计，包含完善的反检测机制和智能批量收集功能
 */
@ModuleInfo(
   name = "自动开盒Skiptick",
   description = "基于Skiptick的智能箱子物品拿取，内置高级反检测机制",
   category = Category.MISC
)
public class ChestStealerSkiptick extends Module {
    // ==================== 配置参数 ====================
    
    /** 基础拿取延迟（Ticks） */
    private final FloatValue delay = ValueBuilder.create(this, "延迟 (Ticks)")
       .setDefaultFloatValue(3.0F)
       .setFloatStep(0.5F)
       .setMinFloatValue(1.0F)
       .setMaxFloatValue(15.0F)
       .build()
       .getFloatValue();
    
    /** 是否拿取末影箱物品 */
    private final BooleanValue pickEnderChest = ValueBuilder.create(this, "末影箱检查")
       .setDefaultBooleanValue(false)
       .build()
       .getBooleanValue();
    
    /** Skiptick倍率，控制执行频率 */
    private final FloatValue skiptickFactor = ValueBuilder.create(this, "Skiptick倍率")
       .setDefaultFloatValue(2.0F)
       .setFloatStep(0.5F)
       .setMinFloatValue(1.0F)
       .setMaxFloatValue(8.0F)
       .build()
       .getFloatValue();
    
    // ==================== 反检测配置 ====================
    
    /** 是否启用反检测优化 */
    private final BooleanValue antiDetection = ValueBuilder.create(this, "反检测优化")
       .setDefaultBooleanValue(true)
       .build()
       .getBooleanValue();
    
    /** 随机延迟范围（Ticks） */
    private final FloatValue randomDelayRange = ValueBuilder.create(this, "随机延迟范围")
       .setVisibility(this.antiDetection::getCurrentValue)
       .setDefaultFloatValue(0.8F)
       .setFloatStep(0.1F)
       .setMinFloatValue(0.0F)
       .setMaxFloatValue(3.0F)
       .build()
       .getFloatValue();
    
    /** 是否启用混淆模式（模拟人类误操作） */
    private final BooleanValue confusionMode = ValueBuilder.create(this, "混淆模式")
       .setVisibility(this.antiDetection::getCurrentValue)
       .setDefaultBooleanValue(true)
       .build()
       .getBooleanValue();
    
    /** 混淆操作间隔（次） */
    private final FloatValue confusionInterval = ValueBuilder.create(this, "混淆操作间隔")
       .setVisibility(() -> antiDetection.getCurrentValue() && confusionMode.getCurrentValue())
       .setDefaultFloatValue(12.0F)
       .setFloatStep(1.0F)
       .setMinFloatValue(5.0F)
       .setMaxFloatValue(30.0F)
       .build()
       .getFloatValue();
    
    // ==================== 智能批量收集配置 ====================
    
    /** 是否启用智能批量模式 */
    private final BooleanValue smartBatchMode = ValueBuilder.create(this, "智能批量模式")
       .setVisibility(this.antiDetection::getCurrentValue)
       .setDefaultBooleanValue(true)
       .build()
       .getBooleanValue();
    
    /** 批量收集阈值（剩余物品数量） */
    private final FloatValue batchThreshold = ValueBuilder.create(this, "批量收集阈值")
       .setVisibility(() -> antiDetection.getCurrentValue() && smartBatchMode.getCurrentValue())
       .setDefaultFloatValue(8.0F)
       .setFloatStep(1.0F)
       .setMinFloatValue(3.0F)
       .setMaxFloatValue(25.0F)
       .build()
       .getFloatValue();
    
    /** 批量收集冷却时间（毫秒） */
    private final FloatValue batchCooldown = ValueBuilder.create(this, "批量冷却时间")
       .setVisibility(() -> antiDetection.getCurrentValue() && smartBatchMode.getCurrentValue())
       .setDefaultFloatValue(400.0F)
       .setFloatStep(50.0F)
       .setMinFloatValue(200.0F)
       .setMaxFloatValue(1000.0F)
       .build()
       .getFloatValue();
    
    // ==================== 内部状态 ====================
    
    /** 上次处理的屏幕 */
    private Screen lastTickScreen;
    
    /** 上次点击时间 */
    private long lastClickTime = 0;
    
    /** Tick计数器 */
    private int tickCounter = 0;
    
    // ==================== 反检测状态 ====================
    
    /** 反检测管理器实例 */
    private final AntiDetectionManager antiDetectionManager;
    
    /** 智能收集引擎实例 */
    private final SmartCollectEngine smartCollectEngine;
    
    /** 点击队列 */
    private final Queue<ClickData> clickQueue = new LinkedBlockingQueue<>();
    
    /**
     * 点击数据结构，存储待执行的点击操作
     */
    private static class ClickData {
        private final int slotId;
        private final long timestamp;
        private final ClickType clickType;
        
        /**
         * 构造点击数据
         * @param slotId 槽位ID
         * @param timestamp 生成时间戳
         * @param clickType 点击类型
         */
        public ClickData(int slotId, long timestamp, ClickType clickType) {
            this.slotId = slotId;
            this.timestamp = timestamp;
            this.clickType = clickType;
        }
        
        public int getSlotId() { return slotId; }
        public long getTimestamp() { return timestamp; }
        public ClickType getClickType() { return clickType; }
    }
    
    /**
     * 反检测管理器 - 负责所有反检测逻辑
     */
    private class AntiDetectionManager {
        // 时间窗口配置
        private static final long DETECTION_WINDOW = 1000; // 1秒检测窗口
        private static final int MAX_WINDOW_CLICKS = 12; // 窗口内最大点击次数
        
        // 统计特征配置
        private static final int INTERVAL_HISTORY_SIZE = 25; // 点击间隔历史大小
        
        // 时间窗口状态
        private long windowStartTime = System.currentTimeMillis();
        private int windowClickCount = 0;
        
        // 点击间隔历史
        private final List<Long> clickIntervals = new ArrayList<>();
        
        // 混淆行为计数器
        private int confusionCounter = 0;
        
        /**
         * 重置反检测状态
         */
        public void reset() {
            windowStartTime = System.currentTimeMillis();
            windowClickCount = 0;
            clickIntervals.clear();
            confusionCounter = 0;
        }
        
        /**
         * 检查当前是否可以执行点击操作（基于时间窗口）
         * @return 是否可以点击
         */
        public boolean canClick() {
            long currentTime = System.currentTimeMillis();
            
            // 检查并更新时间窗口
            if (currentTime - windowStartTime > DETECTION_WINDOW) {
                windowStartTime = currentTime;
                windowClickCount = 0;
            }
            
            // 检查窗口内点击次数
            return windowClickCount < MAX_WINDOW_CLICKS;
        }
        
        /**
         * 记录点击操作，更新反检测状态
         */
        public void recordClick() {
            windowClickCount++;
        }
        
        /**
         * 更新点击间隔历史
         * @param interval 点击间隔（毫秒）
         */
        public void updateClickInterval(long interval) {
            clickIntervals.add(interval);
            
            // 保持历史记录大小
            if (clickIntervals.size() > INTERVAL_HISTORY_SIZE) {
                clickIntervals.remove(0);
            }
            
            // 分析统计特征，避免过于规律的点击
            analyzeAndAdjustPattern();
        }
        
        /**
         * 分析点击模式并调整，避免过于规律
         */
        private void analyzeAndAdjustPattern() {
            if (clickIntervals.size() < INTERVAL_HISTORY_SIZE) {
                return; // 历史数据不足，无法分析
            }
            
            // 计算平均间隔和方差
            double avgInterval = clickIntervals.stream().mapToLong(Long::longValue).average().orElse(0);
            double variance = clickIntervals.stream()
                                           .mapToDouble(d -> Math.pow(d - avgInterval, 2))
                                           .average()
                                           .orElse(0);
            
            // 方差太小，点击间隔过于规律，需要打破规律
            if (variance < 80) {
                // 添加随机延迟偏移，打破规律
                lastClickTime -= (long)(Math.random() * 150);
            }
        }
        
        /**
         * 执行混淆行为（模拟人类误操作）
         * @param container 当前容器屏幕
         * @param menu 箱子菜单
         */
        public void performConfusionAction(ContainerScreen container, ChestMenu menu) {
            confusionCounter++;
            
            // 达到混淆间隔时执行混淆操作
            if (confusionCounter % (int)confusionInterval.getCurrentValue() == 0) {
                // 随机选择一个无效槽位点击（如空气槽位）
                Random random = new Random();
                int randomSlot = random.nextInt(menu.getRowCount() * 9);
                ItemStack stack = menu.getSlot(randomSlot).getItem();
                
                // 只点击空槽位，避免影响正常拿取
                if (stack.isEmpty() && random.nextDouble() < 0.7) { // 70%概率执行混淆操作
                    // 模拟人类误点击 - 点击后立即撤销
                    mc.gameMode.handleInventoryMouseClick(menu.containerId, randomSlot, 1, ClickType.PICKUP, mc.player);
                    
                    // 模拟人类反应时间，100-200ms后撤销
                    long undoDelay = 100 + random.nextInt(100);
                    scheduleUndoClick(menu.containerId, randomSlot, undoDelay);
                }
            }
        }
        
        /**
         * 计算动态延迟（包含随机因素）
         * @param baseDelayMs 基础延迟（毫秒）
         * @return 动态调整后的延迟（毫秒）
         */
        public long calculateDynamicDelay(long baseDelayMs) {
            if (!antiDetection.getCurrentValue()) {
                return baseDelayMs;
            }
            
            // 添加随机延迟，范围为[-randomDelayRange, +randomDelayRange] * 50ms
            double randomFactor = (Math.random() - 0.5) * 2.0 * randomDelayRange.getCurrentValue();
            long adjustedDelay = (long)(baseDelayMs + randomFactor * 50);
            
            // 确保延迟在合理范围内
            return Math.max(50, Math.min(adjustedDelay, 1000));
        }
        
        /**
         * 重置时间窗口
         */
        public void resetWindow() {
            windowStartTime = System.currentTimeMillis();
            windowClickCount = 0;
        }
    }
    
    /**
     * 智能收集引擎 - 负责决定拿取策略
     */
    private class SmartCollectEngine {
        // 批量收集状态
        private boolean isBatchCollecting = false;
        private long lastBatchCollectTime = 0;
        
        /**
         * 决定当前应该使用的拿取策略
         * @param menu 箱子菜单
         * @return 拿取策略
         */
        public CollectStrategy decideCollectStrategy(ChestMenu menu) {
            if (!smartBatchMode.getCurrentValue()) {
                return CollectStrategy.NORMAL; // 未启用智能批量模式，使用正常拿取
            }
            
            int remainingItems = getRemainingUsefulItems(menu);
            long currentTime = System.currentTimeMillis();
            
            // 检查批量收集冷却时间
            boolean isCooldownOver = currentTime - lastBatchCollectTime > batchCooldown.getCurrentValue();
            
            // 大量物品且冷却完成，使用批量收集
            if (remainingItems >= (int)batchThreshold.getCurrentValue() && isCooldownOver) {
                return CollectStrategy.BATCH;
            }
            
            // 少量物品或冷却未完成，使用正常拿取
            return CollectStrategy.NORMAL;
        }
        
        /**
         * 执行批量收集
         * @param menu 箱子菜单
         */
        public void performBatchCollect(ChestMenu menu) {
            // 查找第一个有用物品
            for (int i = 0; i < menu.getRowCount() * 9; i++) {
                ItemStack stack = menu.getSlot(i).getItem();
                if (isItemUseful(stack)) {
                    // 使用QUICK_MOVE进行批量收集
                    mc.gameMode.handleInventoryMouseClick(menu.containerId, i, 0, ClickType.QUICK_MOVE, mc.player);
                    lastClickTime = System.currentTimeMillis();
                    lastBatchCollectTime = lastClickTime;
                    isBatchCollecting = true;
                    
                    // 批量收集后重置时间窗口，避免触发频率检测
                    antiDetectionManager.resetWindow();
                    break;
                }
            }
        }
        
        /**
         * 重置批量收集状态
         */
        public void reset() {
            isBatchCollecting = false;
            lastBatchCollectTime = 0;
        }
    }
    
    /**
     * 拿取策略枚举
     */
    private enum CollectStrategy {
        NORMAL,  // 正常拿取
        BATCH    // 批量收集
    }
    
    /**
     * 构造函数 - 初始化模块
     */
    public ChestStealerSkiptick() {
        this.antiDetectionManager = new AntiDetectionManager();
        this.smartCollectEngine = new SmartCollectEngine();
    }
    
    public String getLocalizedName() {
        return LanguageManager.getInstance().getLocalizedString("自动开盒Skiptick", "Chest Stealer Skiptick");
    }
    
    public String getLocalizedDescription() {
        return LanguageManager.getInstance().getLocalizedString(
            "基于Skiptick的智能箱子物品拿取，内置高级反检测机制",
            "Skiptick-based smart chest stealer with advanced anti-detection mechanisms"
        );
    }
    
    /**
     * 事件监听 - 处理游戏tick
     * @param e 运动事件
     */
    @EventTarget(1)
    public void onMotion(EventMotion e) {
        if (e.getType() != EventType.PRE) {
            return;
        }
        
        Screen currentScreen = mc.screen;
        tickCounter++;
        
        // 处理点击队列
        processClickQueue();
        
        // 检查是否在容器界面中
        if (!(currentScreen instanceof ContainerScreen container)) {
            // 不在容器界面，清空队列
            clickQueue.clear();
            return;
        }
        
        // 检查是否为箱子菜单
        if (!(container.getMenu() instanceof ChestMenu)) {
            return;
        }
        ChestMenu menu = (ChestMenu) container.getMenu();
        
        // 新打开的箱子，重置状态
        if (currentScreen != lastTickScreen) {
            resetState();
        } else {
            // 检查是否为有效的箱子类型
            if (isValidChest(container)) {
                // 检查箱子是否为空
                if (isChestEmpty(menu)) {
                    // 箱子为空，延迟关闭
                    if (System.currentTimeMillis() - lastClickTime > 400) {
                        mc.player.closeContainer();
                    }
                } else {
                    // 使用Skiptick逻辑：只在特定tick执行拿取操作
                    if (tickCounter % (int)skiptickFactor.getCurrentValue() == 0) {
                        // 反检测：检查时间窗口
                        if (antiDetection.getCurrentValue() && !antiDetectionManager.canClick()) {
                            return;
                        }
                        
                        // 决定拿取策略
                        CollectStrategy strategy = smartCollectEngine.decideCollectStrategy(menu);
                        
                        // 执行拿取操作
                        switch (strategy) {
                            case BATCH:
                                // 批量收集
                                smartCollectEngine.performBatchCollect(menu);
                                if (antiDetection.getCurrentValue()) {
                                    antiDetectionManager.recordClick();
                                }
                                break;
                            case NORMAL:
                                // 正常拿取，查找最佳物品并加入队列
                                findAndQueueBestItem(menu);
                                break;
                        }
                        
                        // 执行混淆行为
                        if (antiDetection.getCurrentValue() && confusionMode.getCurrentValue()) {
                            antiDetectionManager.performConfusionAction(container, menu);
                        }
                    }
                }
            }
        }
        
        lastTickScreen = currentScreen;
    }
    
    /**
     * 重置模块状态
     */
    private void resetState() {
        lastClickTime = System.currentTimeMillis();
        clickQueue.clear();
        tickCounter = 0;
        antiDetectionManager.reset();
        smartCollectEngine.reset();
    }
    
    /**
     * 检查是否为有效的箱子类型
     * @param container 容器屏幕
     * @return 是否为有效箱子
     */
    private boolean isValidChest(ContainerScreen container) {
        String chestTitle = container.getTitle().getString();
        String chest = Component.translatable("container.chest").getString();
        String largeChest = Component.translatable("container.chestDouble").getString();
        String enderChest = Component.translatable("container.enderchest").getString();
        
        return chestTitle.equals(chest) ||
               chestTitle.equals(largeChest) ||
               chestTitle.equals("Chest") ||
               (pickEnderChest.getCurrentValue() && chestTitle.equals(enderChest));
    }
    
    /**
     * 查找并队列最佳物品
     * @param menu 箱子菜单
     */
    private void findAndQueueBestItem(ChestMenu menu) {
        // 生成随机排序的槽位列表
        List<Integer> slots = IntStream.range(0, menu.getRowCount() * 9)
                                      .boxed()
                                      .collect(Collectors.toList());
        Collections.shuffle(slots);
        
        // 查找最佳物品
        for (int slotId : slots) {
            ItemStack stack = menu.getSlot(slotId).getItem();
            if (isItemUseful(stack) && isBestItemInChest(menu, stack)) {
                // 将点击请求加入队列
                clickQueue.offer(new ClickData(slotId, System.currentTimeMillis(), ClickType.QUICK_MOVE));
                
                // 记录点击
                if (antiDetection.getCurrentValue()) {
                    antiDetectionManager.recordClick();
                }
                break;
            }
        }
    }
    
    /**
     * 处理点击队列 - 智能调度点击操作
     */
    private void processClickQueue() {
        if (clickQueue.isEmpty()) {
            return;
        }
        
        long currentTime = System.currentTimeMillis();
        long baseDelayMs = (long)(delay.getCurrentValue() * 50); // 转换为毫秒
        
        // 计算动态延迟（包含随机因素）
        long dynamicDelay = antiDetection.getCurrentValue() 
                ? antiDetectionManager.calculateDynamicDelay(baseDelayMs)
                : baseDelayMs;
        
        // 检查是否可以执行下一个点击
        if (currentTime - lastClickTime >= dynamicDelay) {
            ClickData clickData = clickQueue.poll();
            if (clickData != null) {
                // 执行点击操作
                Screen currentScreen = mc.screen;
                if (currentScreen instanceof ContainerScreen) {
                    ContainerScreen container = (ContainerScreen) currentScreen;
                    if (container.getMenu() instanceof ChestMenu) {
                        ChestMenu menu = (ChestMenu) container.getMenu();
                        mc.gameMode.handleInventoryMouseClick(
                                menu.containerId,
                                clickData.getSlotId(),
                                0,
                                clickData.getClickType(),
                                mc.player
                        );
                        
                        // 更新点击间隔历史
                        if (antiDetection.getCurrentValue()) {
                            antiDetectionManager.updateClickInterval(currentTime - lastClickTime);
                        }
                        
                        lastClickTime = currentTime;
                    }
                }
            }
        }
    }
    
    /**
     * 计算剩余有用物品数量
     * @param menu 箱子菜单
     * @return 剩余有用物品数量
     */
    private int getRemainingUsefulItems(ChestMenu menu) {
        int count = 0;
        for (int i = 0; i < menu.getRowCount() * 9; i++) {
            ItemStack stack = menu.getSlot(i).getItem();
            if (isItemUseful(stack)) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * 检查物品是否为游戏内最佳物品
     * @param menu 箱子菜单
     * @param stack 待检查物品
     * @return 是否为最佳物品
     */
    private boolean isBestItemInChest(ChestMenu menu, ItemStack stack) {
        // 神装和锋利斧直接视为最佳
        if (InventoryUtils.isGodItem(stack) || InventoryUtils.isSharpnessAxe(stack)) {
            return true;
        }
        
        // 遍历所有槽位，检查是否有更好的物品
        for (int i = 0; i < menu.getRowCount() * 9; i++) {
            ItemStack checkStack = menu.getSlot(i).getItem();
            
            // 同类型物品比较
            if (stack.getItem() instanceof ArmorItem && checkStack.getItem() instanceof ArmorItem) {
                ArmorItem armor = (ArmorItem) stack.getItem();
                ArmorItem checkArmor = (ArmorItem) checkStack.getItem();
                
                // 同部位护甲比较防御值
                if (armor.getEquipmentSlot() == checkArmor.getEquipmentSlot() &&
                    InventoryUtils.getProtection(checkStack) > InventoryUtils.getProtection(stack)) {
                    return false;
                }
            } 
            // 剑类物品比较伤害
            else if (stack.getItem() instanceof SwordItem && checkStack.getItem() instanceof SwordItem) {
                if (InventoryUtils.getSwordDamage(checkStack) > InventoryUtils.getSwordDamage(stack)) {
                    return false;
                }
            } 
            // 镐类物品比较工具评分
            else if (stack.getItem() instanceof PickaxeItem && checkStack.getItem() instanceof PickaxeItem) {
                if (InventoryUtils.getToolScore(checkStack) > InventoryUtils.getToolScore(stack)) {
                    return false;
                }
            } 
            // 斧类物品比较工具评分
            else if (stack.getItem() instanceof AxeItem && checkStack.getItem() instanceof AxeItem) {
                if (InventoryUtils.getToolScore(checkStack) > InventoryUtils.getToolScore(stack)) {
                    return false;
                }
            }
        }
        
        return true;
    }
    
    /**
     * 检查箱子是否为空
     * @param menu 箱子菜单
     * @return 是否为空
     */
    private boolean isChestEmpty(ChestMenu menu) {
        for (int i = 0; i < menu.getRowCount() * 9; i++) {
            ItemStack item = menu.getSlot(i).getItem();
            if (!item.isEmpty() && isItemUseful(item) && isBestItemInChest(menu, item)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * 检查物品是否有用
     * @param stack 待检查物品
     * @return 是否有用
     */
    public static boolean isItemUseful(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        
        // 神装和锋利斧直接视为有用
        if (InventoryUtils.isGodItem(stack) || InventoryUtils.isSharpnessAxe(stack)) {
            return true;
        }
        
        // 排除无用物品
        if (stack.getItem() == Items.FISHING_ROD ||
            stack.getItem() == Items.COMPASS ||
            stack.getItem() == Items.CHEST ||
            stack.getItem() == Items.ANVIL ||
            stack.getItem() == Items.WHEAT_SEEDS ||
            stack.getItem() instanceof ShovelItem ||
            stack.getItem() instanceof HoeItem) {
            return false;
        }
        
        // 护甲类物品
        if (stack.getItem() instanceof ArmorItem) {
            ArmorItem armor = (ArmorItem) stack.getItem();
            float protection = InventoryUtils.getProtection(stack);
            float bestArmor = InventoryUtils.getBestArmorScore(armor.getEquipmentSlot());
            return protection > bestArmor;
        }
        
        // 剑类物品
        if (stack.getItem() instanceof SwordItem) {
            float damage = InventoryUtils.getSwordDamage(stack);
            float bestDamage = InventoryUtils.getBestSwordDamage();
            return damage > bestDamage;
        }
        
        // 镐类物品
        if (stack.getItem() instanceof PickaxeItem) {
            float score = InventoryUtils.getToolScore(stack);
            float bestScore = InventoryUtils.getBestPickaxeScore();
            return score > bestScore;
        }
        
        // 斧类物品
        if (stack.getItem() instanceof AxeItem) {
            float score = InventoryUtils.getToolScore(stack);
            float bestScore = InventoryUtils.getBestAxeScore();
            return score > bestScore;
        }
        
        // 弩类物品
        if (stack.getItem() instanceof CrossbowItem) {
            float score = InventoryUtils.getCrossbowScore(stack);
            float bestScore = InventoryUtils.getBestCrossbowScore();
            return score > bestScore;
        }
        
        // 弓类物品
        if (stack.getItem() instanceof BowItem) {
            if (InventoryUtils.isPunchBow(stack)) {
                float score = InventoryUtils.getPunchBowScore(stack);
                float bestScore = InventoryUtils.getBestPunchBowScore();
                return score > bestScore;
            } else if (InventoryUtils.isPowerBow(stack)) {
                float score = InventoryUtils.getPowerBowScore(stack);
                float bestScore = InventoryUtils.getBestPowerBowScore();
                return score > bestScore;
            }
        }
        
        // 其他物品类型的检查...
        return false;
    }
    
    /**
     * 调度撤销点击操作（用于混淆行为）
     * @param containerId 容器ID
     * @param slotId 槽位ID
     * @param delayMs 延迟时间（毫秒）
     */
    private void scheduleUndoClick(int containerId, int slotId, long delayMs) {
        // 注意：此处简化实现，实际可使用定时器
        // 由于Minecraft单线程特性，此处仅作演示
    }
}
