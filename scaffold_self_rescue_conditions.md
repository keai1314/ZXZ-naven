# Scaffold自救启动条件分析

## 核心启动条件

Scaffold模块的自救功能启动条件主要由以下因素决定，无论是否启用**速度调试**选项：

### 1. 基础条件
- **自救开关已开启** (`selfRescue.getCurrentValue() == true`)
- **不在空档期内** (`currentTick - this.lastReleaseTick > cooldownTicks`)
- **可以放置方块** (`canPlaceBlocks() == true`)

### 2. 触发条件（满足任一即可）

#### A. 掉落触发
- 玩家处于**掉落状态** (`isFalling() == true`)
- 玩家**不在地面上** (`!mc.player.onGround()`)
- 玩家**向下移动** (`mc.player.getDeltaMovement().y < 0`)
- 使用`FallingPlayer`计算玩家将**继续掉落** (`fallingPlayer.y < mc.player.getY()`)

#### B. 速度触发
- 玩家3D移动速度**超过14.0 Bps** (`speed3D > 14.0`)

#### C. 加速度触发
- 玩家加速度**超过设定阈值** (`currentAcceleration > accelerationThreshold.getCurrentValue()`)
- 加速度阈值可通过配置调整，默认值为**20.0 Bps²**

## 速度调试选项的作用

**速度调试** (`debugSpeed`) 选项仅影响**显示功能**，不会改变自救的启动逻辑：

1. 启用后在游戏内**准心下方显示**当前速度和加速度信息
2. 显示格式：`Speed: X.XX Bps` 和 `Accel: X.XX Bps²`
3. 方便玩家**观察和调试**自救触发条件
4. 不影响实际的自救启动逻辑

## 自救启动的完整流程

1. 检查基础条件是否满足
2. 检查是否处于掉落状态
3. 计算玩家当前速度和加速度
4. 检查速度和加速度是否超过阈值
5. 检查是否可以放置方块
6. 如果满足条件，调用`triggerPlayerStuck()`触发自救

## 自救功能的实际效果

- 触发后增加`PlayerUtils.playerStuckTicks`计数器
- 持续1个游戏刻的卡住状态
- 如果Blink模块启用，释放所有积攒的数据包
- 自救时可选择忽略放置条件 (`ignoreConditionsOnSelfRescue`)

## 总结

启用**速度调试**选项后，Scaffold自救启动的**核心条件不变**，只是增加了速度和加速度的可视化显示，方便玩家观察和调试自救触发时机。

自救仍然由以下条件触发：
- 掉落状态
- 过高的移动速度（>14.0 Bps）
- 过高的加速度（>配置阈值）

速度调试选项仅作为辅助工具，帮助玩家了解当前游戏状态，不会改变自救功能的实际行为。