package dev.xinxin.module.modules.world;

import dev.xinxin.SilenceFix;
import dev.xinxin.event.EventTarget;
import dev.xinxin.event.rendering.EventRender2D;
import dev.xinxin.event.rendering.EventRender3D;
import dev.xinxin.event.world.*;
import dev.xinxin.module.Category;
import dev.xinxin.module.Module;
import dev.xinxin.module.values.BoolValue;
import dev.xinxin.module.values.ColorValue;
import dev.xinxin.utils.FallingPlayer;
import dev.xinxin.utils.RotationComponent;
import dev.xinxin.utils.client.HelperUtil;
import dev.xinxin.utils.client.PacketUtil;
import dev.xinxin.utils.misc.MathUtils;
import dev.xinxin.utils.player.BlockUtil;
import dev.xinxin.utils.player.PlaceInfo;
import dev.xinxin.utils.player.RotationNew;
import dev.xinxin.utils.player.RotationUtil;
import dev.xinxin.utils.render.ProgressManager.ProgressBarManager;
import dev.xinxin.utils.render.RenderUtil;
import dev.xinxin.utils.render.ScaffoldCounter;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.util.*;

import java.awt.*;
import java.util.*;
import java.util.List;

import static dev.xinxin.utils.InventoryUtil.swap;

public class Scaffold extends Module {
    private final BoolValue bwValue = new BoolValue("Bedwars", false);
    private final BoolValue swing = new BoolValue("Swing", true);
    private final BoolValue esp = new BoolValue("ESP", true);
    private final ColorValue espColor = new ColorValue("ESP Color",new Color(46, 239, 232,70).getRGB());
    private static final List<Block> invalidBlocks = Arrays.asList(Blocks.enchanting_table, Blocks.chest, Blocks.ender_chest,
            Blocks.trapped_chest, Blocks.anvil, Blocks.sand, Blocks.web, Blocks.torch,
            Blocks.crafting_table, Blocks.furnace, Blocks.waterlily, Blocks.dispenser,
            Blocks.stone_pressure_plate, Blocks.wooden_pressure_plate, Blocks.noteblock,
            Blocks.dropper, Blocks.tnt, Blocks.standing_banner, Blocks.wall_banner, Blocks.redstone_torch, Blocks.crafting_table);

    public int baseY = -1;
    private int slot;
    private int lastslot;
    private boolean canPlace;
    public int bigVelocityTick = 0;
    float lastCount;

    public Scaffold() {
        super("Scaffold", Category.Movement,"自动搭路");
    }

    private int maxObservedBlocks = 1;

    @EventTarget
    public void onRender2D(EventRender2D event) {
        int count = getBlockCount();
        if (count > maxObservedBlocks) {
            maxObservedBlocks = count;
        }
        lastCount = (float) (lastCount + (count - lastCount) * 0.25f);
        float present = Math.max(0f, Math.min(1f, lastCount / (float) maxObservedBlocks));
        String title = "Scaffold";
        String description = "Blocks: " + count;
        SilenceFix.instance.island.addIsland(null, title, description, present);
    }



    //Dis MulitAction
    @EventTarget
    public void onR2d(EventPlace eventPlace){
        eventPlace.setCancelled();
    }

    @Override
    public void onEnable() {
        if (mc.thePlayer == null) return;
        blockPos = null;
        lastslot = mc.thePlayer.inventory.currentItem;
        SilenceFix.instance.slotSpoofManager.startSpoofing(lastslot);
        baseY = -1;
        canPlace = true;
        lastCount = getBlockCount();
        bigVelocityTick = 0;
    }

    public static boolean reachable;

    @Override
    public void onDisable() {
        if (mc.thePlayer == null) return;
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), false);
        mc.thePlayer.inventory.currentItem = lastslot;
        SilenceFix.instance.slotSpoofManager.stopSpoofing();
    }

    private int blockCheckCooldown = 0;
    private int lastBlockCount = 0;

    @EventTarget
    public void onUpdate(EventMotion event) {
        if (blockCheckCooldown > 0) {
            blockCheckCooldown--;
            if (blockCheckCooldown == 0) {
                int currentCount = getBlockCount();
                if (currentCount == 0 && lastBlockCount == 0 && this.getState()) {
                    HelperUtil.sendMessage("§c确认方块已用完，自动关闭");
                    this.setState(false);
                }
                lastBlockCount = currentCount;
            }
            return;
        }
        int currentBlockCount = getBlockCount();
        if (currentBlockCount == 0) {
            if (this.getState()) {
                blockCheckCooldown = 10;
                HelperUtil.sendMessage("§e检测到方块可能被服务器刷新，等待确认...");
            }
            lastBlockCount = 0;
            return;
        }
        lastBlockCount = currentBlockCount;
    }

    @EventTarget
    public void onRender3D(EventRender3D eventRender3D){
        if (blockPos == null) return;
        for (int i = 0; i < 2; i++) {
            final PlaceInfo placeInfo = PlaceInfo.get(blockPos);

            if (BlockUtil.isValidBock(blockPos) && placeInfo != null && esp.getValue()) {
                RenderUtil.drawBlockBox(blockPos, espColor.getColorC(), false);
            }
        }
    }

    @EventTarget
    public void onMoveInput(EventMoveInput event) {
        if (mc.thePlayer.onGround && event.getForward() > 0 && !mc.gameSettings.keyBindJump.isKeyDown()) {
            event.setJump(true);
        }
    }

    public void place(boolean canreset) {
        if (!canPlace) {
            return;
        }
        if (blockPos != null) {
            EnumFacing enumFacing = mc.gameSettings.keyBindJump.isKeyDown() ? getPlaceSide(blockPos) : this.enumFacing;
            if (enumFacing == null) {
                return;
            }
            Vec3 hitvec = getVec3(blockPos, enumFacing);
            if (validateBlockRange(hitvec)) {
                // 实际放置方块时使用正确的槽位
                if (mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, mc.thePlayer.getCurrentEquippedItem(), this.blockPos, enumFacing, getVec3(blockPos, enumFacing))) {
                    if (swing.getValue()) {
                        mc.thePlayer.swingItem();
                    } else {
                        mc.thePlayer.sendQueue.addToSendQueue(new C0APacketAnimation());
                    }
                }
            }
        }
        if (canreset) {
            blockPos = null;
        }
    }

    private EnumFacing getPlaceSide(BlockPos blockPos) {
        ArrayList<Vec3> positions = new ArrayList();
        HashMap<Vec3, EnumFacing> hashMap = new HashMap();
        BlockPos playerPos = new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);
        if (bwValue.getValue()) {
            if ((double)mc.thePlayer.fallDistance > 0.2 && BlockUtil.isAirBlock(blockPos.add(0, 1, 0)) && !blockPos.add(0, 1, 0).equals(playerPos) && !mc.thePlayer.onGround) {
                BlockPos bp = blockPos.add(0, 1, 0);
                Vec3 vec3 = this.getBestHitFeet(bp);
                positions.add(vec3);
                hashMap.put(vec3, EnumFacing.UP);
            }
        } else if (BlockUtil.isAirBlock(blockPos.add(0, 1, 0)) && !blockPos.add(0, 1, 0).equals(playerPos) && !mc.thePlayer.onGround) {
            BlockPos bp = blockPos.add(0, 1, 0);
            Vec3 vec3 = this.getBestHitFeet(bp);
            positions.add(vec3);
            hashMap.put(vec3, EnumFacing.UP);
        }

        if (BlockUtil.isAirBlock(blockPos.add(1, 0, 0)) && !blockPos.add(1, 0, 0).equals(playerPos)) {
            BlockPos bp = blockPos.add(1, 0, 0);
            Vec3 vec3 = this.getBestHitFeet(bp);
            positions.add(vec3);
            hashMap.put(vec3, EnumFacing.EAST);
        }

        if (BlockUtil.isAirBlock(blockPos.add(-1, 0, 0)) && !blockPos.add(-1, 0, 0).equals(playerPos)) {
            BlockPos bp = blockPos.add(-1, 0, 0);
            Vec3 vec3 = this.getBestHitFeet(bp);
            positions.add(vec3);
            hashMap.put(vec3, EnumFacing.WEST);
        }

        if (BlockUtil.isAirBlock(blockPos.add(0, 0, 1)) && !blockPos.add(0, 0, 1).equals(playerPos)) {
            BlockPos bp = blockPos.add(0, 0, 1);
            Vec3 vec3 = this.getBestHitFeet(bp);
            positions.add(vec3);
            hashMap.put(vec3, EnumFacing.SOUTH);
        }

        if (BlockUtil.isAirBlock(blockPos.add(0, 0, -1)) && !blockPos.add(0, 0, -1).equals(playerPos)) {
            BlockPos bp = blockPos.add(0, 0, -1);
            Vec3 vec3 = this.getBestHitFeet(bp);
            positions.add(vec3);
            hashMap.put(vec3, EnumFacing.NORTH);
        }

        positions.sort(Comparator.comparingDouble((vec3x) -> mc.thePlayer.getDistance(vec3x.xCoord, vec3x.yCoord, vec3x.zCoord)));
        if (!positions.isEmpty()) {
            Vec3 vec3 = this.getBestHitFeet(this.blockPos);
            if (mc.thePlayer.getDistance(vec3.xCoord, vec3.yCoord, vec3.zCoord) >= mc.thePlayer.getDistance(((Vec3)positions.get(0)).xCoord, ((Vec3)positions.get(0)).yCoord, ((Vec3)positions.get(0)).zCoord)) {
                return hashMap.get(positions.get(0));
            }
        }

        return null;
    }

    private Vec3 getBestHitFeet(BlockPos blockPos) {
        Block block = mc.theWorld.getBlockState(blockPos).getBlock();
        double ex = MathHelper.clamp_double(mc.thePlayer.posX, blockPos.getX(), (double) blockPos.getX() + block.getBlockBoundsMaxX());
        double ey = MathHelper.clamp_double(!mc.gameSettings.keyBindJump.isKeyDown() ? baseY : mc.thePlayer.posY, blockPos.getY(), (double) blockPos.getY() + block.getBlockBoundsMaxY());
        double ez = MathHelper.clamp_double(mc.thePlayer.posZ, blockPos.getZ(), (double) blockPos.getZ() + block.getBlockBoundsMaxZ());
        return new Vec3(ex, ey, ez);
    }


    private static boolean validateBlockRange(final Vec3 pos) {
        if (pos == null)
            return false;
        final EntityPlayerSP player = mc.thePlayer;
        final double x = (pos.xCoord - player.posX);
        final double y = (pos.yCoord - (player.posY + player.getEyeHeight()));
        final double z = (pos.zCoord - player.posZ);
        return StrictMath.sqrt(x * x + y * y + z * z) <= 5.0D;
    }

    @EventTarget
    public void onPacket(EventPacketReceive event) {
        if (event.getPacket() instanceof S12PacketEntityVelocity velocity && velocity.getEntityID() == mc.thePlayer.getEntityId()) {
            double strength = new Vec3(velocity.getMotionX() / 8000D, 0, velocity.getMotionZ() / 8000D).lengthVector();
            if (strength >= 1.5D) {
                bigVelocityTick = 60;
            }
        }
    }

    private int rotateCount = 0;
    @EventTarget
    public void onUpdate(EventUpdate event) {
        setSuffix(getBlockCount());
        slot = getBlockSlot();
        mc.thePlayer.inventory.currentItem = slot;

        BlockPos thePlayerPos = new BlockPos(mc.thePlayer);
        IBlockState state = mc.theWorld.getBlockState(thePlayerPos);
        if (state.getBlock() != Blocks.air && state.getBlock().isPassable(mc.theWorld, thePlayerPos)) return;
        if (mc.thePlayer.ticksExisted <= 10) return;
        if (bigVelocityTick > 0) {
            bigVelocityTick--;
        }
        if (mc.thePlayer.onGround && bigVelocityTick <= 30) {
            bigVelocityTick = 0;
        }
        double motion = Math.max(mc.thePlayer.motionX, mc.thePlayer.motionZ);
        place(true);
        if (motion <= 0.4) {
            if (Math.abs(mc.thePlayer.motionX) < 0.03 || Math.abs(mc.thePlayer.motionZ) < 0.03) {
                if (!mc.thePlayer.onGround && mc.thePlayer.offGroundTicks <= 2) return;
            } else {
                if (!mc.thePlayer.onGround && mc.thePlayer.offGroundTicks <= 1) return;
            }
        }
        if (baseY == -1 || baseY > (int) mc.thePlayer.posY - 1 || bigVelocityTick > 0 || mc.thePlayer.onGround || mc.gameSettings.keyBindJump.isKeyDown()) {
            baseY = (int) mc.thePlayer.posY - 1;
        }

        getBestBlocks();
        this.findBlock();

        float tolerance = 10.0f;
        float yaw = (mc.thePlayer.rotationYaw % 360 + 360) % 360;
        boolean check = !(Math.abs(yaw - 0) <= tolerance || Math.abs(yaw - 360) <= tolerance || Math.abs(yaw - 90) <= tolerance || Math.abs(yaw - 180) <= tolerance || Math.abs(yaw - 270) <= tolerance);


        canPlace = mc.gameSettings.keyBindJump.isKeyDown() ?
                check ? mc.thePlayer.offGroundTicks >= 1 : mc.thePlayer.offGroundTicks >= 2 :
                check ? mc.thePlayer.offGroundTicks >= 3 : mc.thePlayer.offGroundTicks >= 4 ;

        if (!canPlace) return;

        if (blockPos != null) {
            reachable = true;
            if (mc.thePlayer.motionY < -0.1) {
                FallingPlayer fallingPlayer = new FallingPlayer(mc.thePlayer);
                fallingPlayer.calculate(2);
                if (blockPos.getY() > fallingPlayer.getY()) {
                    reachable = false;
                }
            }
            if ((!reachable || bigVelocityTick > 0) && rotateCount <= 4 && getBlockCount() > 0) {
                RotationNew rotation = RotationUtil.getRotationBlock(blockPos, 0F);
                mc.theWorld.skiptick++;
                rotateCount++;
                PacketUtil.sendPacket(new C03PacketPlayer.C05PacketPlayerLook(
                        rotation.getYaw(), rotation.getPitch(), mc.thePlayer.onGround
                ));
                //place(false);
                this.onUpdate(event);
            } else {
                RotationNew rotation = RotationUtil.getRotationBlock(blockPos, 1f);
                rotateCount = 0;
                RotationComponent.setRotation(rotation,360f,true);
            }
        }
    }

    public int getBlockSlot() {
        for (int i = 0; i < 9; ++i) {
            if (!mc.thePlayer.inventoryContainer.getSlot(i + 36).getHasStack()) continue;
            ItemStack stack = mc.thePlayer.inventoryContainer.getSlot(i + 36).getStack();
            if (stack != null && isValid(stack.getItem())) {
                return i;
            }
        }
        return 0;
    }


    private void findBlock() {
        Vec3 baseVec = mc.thePlayer.getPositionEyes(2F);
        BlockPos base = new BlockPos(baseVec.xCoord, baseY + 0.1f, baseVec.zCoord);
        int baseX = base.getX();
        int baseZ = base.getZ();
        if (mc.theWorld.getBlockState(base).getBlock().isBlockSolid(mc.theWorld,base,EnumFacing.DOWN)) return;
        if (checkBlock(baseVec, base)) {
            return;
        }
        for (int d = 1; d <= 6; d++) {
            if (checkBlock(baseVec, new BlockPos(
                    baseX,
                    baseY - d,
                    baseZ
            ))) {
                return;
            }

            for (int x = 0; x <= d; x++) {
                for (int z = 0; z <= d - x; z++) {
                    int y = d - x - z;
                    for (int rev1 = 0; rev1 <= 1; rev1++) {
                        for (int rev2 = 0; rev2 <= 1; rev2++) {
                            if (checkBlock(baseVec, new BlockPos(
                                    baseX + (rev1 == 0 ? x : -x),
                                    baseY - y,
                                    baseZ + (rev2 == 0 ? z : -z)
                            ))) return;
                        }
                    }
                }
            }
        }
    }

    public BlockPos blockPos;
    private EnumFacing enumFacing;

    private boolean checkBlock(Vec3 baseVec, BlockPos pos) {
        if (!(mc.theWorld.getBlockState(pos).getBlock() instanceof BlockAir)) return false;
        Vec3 center = new Vec3(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        for (EnumFacing 脸 : EnumFacing.values()) {
            Vec3 hit = center.add(new Vec3(脸.getDirectionVec()).scale(0.5));
            Vec3i baseBlock = pos.add(脸.getDirectionVec());
            if (!mc.theWorld.getBlockState(new BlockPos(baseBlock.getX(), baseBlock.getY(), baseBlock.getZ())).getBlock().isSca())
                continue;
            Vec3 relevant = hit.subtract(baseVec);
            if (relevant.lengthSquared() <= 4.5 * 4.5 && relevant.dotProduct(
                    new Vec3(脸.getDirectionVec())) >= 0) {
                blockPos = new BlockPos(baseBlock);
                enumFacing = 脸.getOpposite();
                return true;
            }
        }
        return false;
    }

    public static Vec3 getVec3(BlockPos pos, EnumFacing face) {
        double x = (double) pos.getX() + 0.5;
        double y = (double) pos.getY() + 0.5;
        double z = (double) pos.getZ() + 0.5;
        if (face == EnumFacing.UP || face == EnumFacing.DOWN) {
            x += MathUtils.getRandomInRange(0.3, -0.3);
            z += MathUtils.getRandomInRange(0.3, -0.3);
        } else {
            y += MathUtils.getRandomInRange(0.3, -0.3);
        }
        if (face == EnumFacing.WEST || face == EnumFacing.EAST) {
            z += MathUtils.getRandomInRange(0.3, -0.3);
        }
        if (face == EnumFacing.SOUTH || face == EnumFacing.NORTH) {
            x += MathUtils.getRandomInRange(0.3, -0.3);
        }
        return new Vec3(x, y, z);
    }

    public boolean isValid(final Item item) {
        return item instanceof ItemBlock && !invalidBlocks.contains(((ItemBlock) (item)).getBlock());
    }

    public int getBiggestBlockSlotHotbar() {
        int slot = -1;
        int size = 0;
        if (getBlockCount() == 0)
            return -1;
        for (int i = 36; i < 45; i++) {
            if (mc.thePlayer.inventoryContainer.getSlot(i).getHasStack()) {
                Item item = mc.thePlayer.inventoryContainer.getSlot(i).getStack().getItem();
                ItemStack is = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
                if (isValid(item)) {
                    if (is.stackSize > size) {
                        size = is.stackSize;
                        slot = i;
                    }
                }
            }
        }
        return slot;
    }

    public int getBiggestBlockSlotInv() {
        int slot = -1;
        int size = 0;
        if (getBlockCount() == 0)
            return -1;
        for (int i = 9; i < 36; i++) {
            if (mc.thePlayer.inventoryContainer.getSlot(i).getHasStack()) {
                Item item = mc.thePlayer.inventoryContainer.getSlot(i).getStack().getItem();
                ItemStack is = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
                if (isValid(item)) {
                    if (is.stackSize > size) {
                        size = is.stackSize;
                        slot = i;
                    }
                }
            }
        }
        return slot;
    }

    public void getBestBlocks() {
        if (getBlockCount() == 0)
            return;
        ItemStack is = new ItemStack(Item.getItemById(261));
        int bestInvSlot = getBiggestBlockSlotInv();
        int bestHotbarSlot = getBiggestBlockSlotHotbar();
        int bestSlot = getBiggestBlockSlotHotbar() > 0 ? getBiggestBlockSlotHotbar() : getBiggestBlockSlotInv();
        int spoofSlot = 42;
        if (bestHotbarSlot > 0 && bestInvSlot > 0) {
            if (mc.thePlayer.inventoryContainer.getSlot(bestInvSlot).getHasStack() && mc.thePlayer.inventoryContainer.getSlot(bestHotbarSlot).getHasStack()) {
                if (mc.thePlayer.inventoryContainer.getSlot(bestHotbarSlot).getStack().stackSize < mc.thePlayer.inventoryContainer.getSlot(bestInvSlot).getStack().stackSize) {
                    bestSlot = bestInvSlot;
                }
            }
        }
        if (hotbarContainBlock()) {
            for (int a = 36; a < 45; a++) {
                if (mc.thePlayer.inventoryContainer.getSlot(a).getHasStack()) {
                    Item item = mc.thePlayer.inventoryContainer.getSlot(a).getStack().getItem();
                    if (isValid(item)) {
                        spoofSlot = a;
                        break;
                    }
                }
            }
        } else {
            for (int a = 36; a < 45; a++) {
                if (!mc.thePlayer.inventoryContainer.getSlot(a).getHasStack()) {
                    spoofSlot = a;
                    break;
                }
            }
        }
        if (mc.thePlayer.inventoryContainer.getSlot(spoofSlot).slotNumber != bestSlot) {
            swap(bestSlot, spoofSlot - 36);
            mc.playerController.updateController();
        }

    }

    private boolean hotbarContainBlock() {
        int i = 36;
        while (i < 45) {
            try {
                ItemStack stack = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
                if ((stack == null) || (stack.getItem() == null) || !(stack.getItem() instanceof ItemBlock) || !isValid(stack.getItem())) {
                    i++;
                    continue;
                }
                return true;
            } catch (Exception e) {
            }
        }
        return false;
    }

    public int getBlockCount() {
        int blockCount = 0;
        for (int i = 9; i < 45; ++i) {
            if (mc.thePlayer.inventoryContainer.getSlot(i).getHasStack()) {
                ItemStack is = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
                if (is.getItem() instanceof ItemBlock) {
                    ItemBlock block = (ItemBlock) is.getItem();
                    if (isValid(block)) {
                        blockCount += is.stackSize;
                    }
                }
            }
        }
        return blockCount;
    }



}
