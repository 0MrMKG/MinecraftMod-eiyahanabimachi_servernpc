package com.servernpc.item;

import com.servernpc.eiyahanabimachiservernpc;
import com.servernpc.entity.ReimuGoodNpcEntity.ActivityPointInfo;
import com.servernpc.entity.ReimuGoodNpcEntity.ActivityType;
import com.servernpc.entity.ReimuGoodNpcEntity;
import com.servernpc.menu.NpcPatrolConfigMenu;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class NpcPatrolWandItem extends Item {
    private static final String BOUND_NPC_UUID_TAG = "BoundNpcUuid";
    private static final String BOUND_NPC_ID_TAG = "BoundNpcEntityId";
    private static final DustParticleOptions GREEN_DUST = new DustParticleOptions(new Vector3f(0.2F, 1.0F, 0.2F), 1.0F);
    private static final Map<UUID, BoundNpcSession> BOUND_SESSIONS = new ConcurrentHashMap<>();

    public NpcPatrolWandItem(Properties properties) {
        super(properties);
    }

    public static void clearBindingsForNpc(ServerLevel level, UUID npcUuid, int npcEntityId) {
        if (level == null || level.getServer() == null) {
            return;
        }

        BOUND_SESSIONS.entrySet().removeIf(entry ->
                entry.getValue().npcUuid().equals(npcUuid) || entry.getValue().npcEntityId() == npcEntityId
        );

        for (ServerPlayer serverPlayer : level.getServer().getPlayerList().getPlayers()) {
            boolean changed = false;
            for (int i = 0; i < serverPlayer.getInventory().getContainerSize(); i++) {
                ItemStack stack = serverPlayer.getInventory().getItem(i);
                if (clearBindingIfMatchedNpc(stack, npcUuid, npcEntityId)) {
                    changed = true;
                }
            }
            if (changed) {
                serverPlayer.getInventory().setChanged();
            }
        }
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity interactionTarget, InteractionHand usedHand) {
        if (!(interactionTarget instanceof ReimuGoodNpcEntity npc)) {
            return InteractionResult.PASS;
        }

        if (!player.level().isClientSide) {
            CompoundTag tag = this.readWandTag(stack);
            BoundNpcSession session = BOUND_SESSIONS.get(player.getUUID());
            boolean sameNpcInSession = session != null && session.npcUuid().equals(npc.getUUID());
            boolean sameNpcInTag = tag.hasUUID(BOUND_NPC_UUID_TAG) && npc.getUUID().equals(tag.getUUID(BOUND_NPC_UUID_TAG));

            if (sameNpcInSession || sameNpcInTag) {
                this.clearBinding(tag);
                this.writeWandTag(stack, tag);
                BOUND_SESSIONS.remove(player.getUUID());
                player.displayClientMessage(
                        Component.translatable("message." + eiyahanabimachiservernpc.MODID + ".patrol_wand.unbound", npc.getName()),
                        true
                );
            } else {
                this.writeBinding(tag, npc);
                this.writeWandTag(stack, tag);
                BOUND_SESSIONS.put(player.getUUID(), new BoundNpcSession(npc.getUUID(), npc.getId()));
                player.displayClientMessage(
                        Component.translatable("message." + eiyahanabimachiservernpc.MODID + ".patrol_wand.bound", npc.getName()),
                        true
                );
            }
        }

        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        if (player.level().isClientSide) {
            return InteractionResult.SUCCESS;
        }

        ServerLevel serverLevel = (ServerLevel) player.level();
        ItemStack stack = context.getItemInHand();
        CompoundTag tag = this.readWandTag(stack);

        ReimuGoodNpcEntity npc = this.resolveBoundNpc(serverLevel, player, stack, tag);
        if (npc == null || !npc.isAlive()) {
            BOUND_SESSIONS.remove(player.getUUID());
            player.displayClientMessage(
                    Component.translatable("message." + eiyahanabimachiservernpc.MODID + ".patrol_wand.not_bound"),
                    true
            );
            return InteractionResult.FAIL;
        }

        BlockPos clickedPos = context.getClickedPos();
        this.spawnGreenCircle(serverLevel, clickedPos);
        if (player instanceof ServerPlayer serverPlayer) {
            final ReimuGoodNpcEntity finalNpc = npc;
            final BlockPos finalPos = clickedPos;
            serverPlayer.openMenu(
                    new SimpleMenuProvider(
                            (containerId, playerInventory, ignored) -> new NpcPatrolConfigMenu(containerId, playerInventory, finalNpc, finalPos),
                            Component.translatable("container." + eiyahanabimachiservernpc.MODID + ".npc_patrol_config")
                    ),
                    buffer -> {
                        buffer.writeVarInt(finalNpc.getId());
                        buffer.writeBlockPos(finalPos);
                    }
            );
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (!(level instanceof ServerLevel serverLevel) || !(entity instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (!this.isHeldByPlayer(serverPlayer, stack)) {
            return;
        }
        if (serverPlayer.tickCount % 10 != 0) {
            return;
        }

        CompoundTag tag = this.readWandTag(stack);
        ReimuGoodNpcEntity npc = this.resolveBoundNpc(serverLevel, serverPlayer, stack, tag);
        if (npc == null || !npc.isAlive()) {
            return;
        }

        String npcName = npc.getName().getString();
        if (!npc.hasScheduleConfigured()) {
            if (serverPlayer.tickCount % 20 == 0) {
                serverPlayer.displayClientMessage(
                        Component.translatable(
                                "message." + eiyahanabimachiservernpc.MODID + ".patrol_wand.schedule_unset",
                                npc.getName()
                        ),
                        true
                );
            }
            for (ActivityPointInfo pointInfo : npc.getActivityPointInfos()) {
                BlockPos pos = pointInfo.center();
                if (serverPlayer.blockPosition().distSqr(pos) <= 64.0D * 64.0D) {
                    String markerText = this.activityTypeText(pointInfo.activityType())
                            + " | NPC:" + npcName
                            + " | 时间表:未设置"
                            + " | 半径:" + pointInfo.radius();
                    DebugPackets.sendGameTestAddMarker(serverLevel, pos, markerText, 0xFFFFFFFF, 1100);
                }
            }
            return;
        }

        int currentTickOfDay = Math.floorMod((int) serverLevel.getDayTime(), 24000);
        ActivityType currentActivity = npc.getScheduledActivity(currentTickOfDay);
        String nowText = formatTime(currentTickOfDay);
        String currentActivityText = this.activityTypeText(currentActivity);
        String currentActivityRangeText = this.currentScheduleRangeText(npc, currentTickOfDay);
        if (serverPlayer.tickCount % 20 == 0) {
            serverPlayer.displayClientMessage(
                    Component.translatable(
                            "message." + eiyahanabimachiservernpc.MODID + ".patrol_wand.now_status_with_npc",
                            npc.getName(),
                            currentActivityText,
                            nowText,
                            currentTickOfDay
                    ),
                    true
            );
        }

        for (ActivityPointInfo pointInfo : npc.getActivityPointInfos()) {
            BlockPos pos = pointInfo.center();
            if (serverPlayer.blockPosition().distSqr(pos) <= 64.0D * 64.0D) {
                String markerText = this.activityTypeText(pointInfo.activityType())
                        + " | NPC:" + npcName
                        + " | \u5f53\u524d:" + currentActivityText
                        + " " + nowText
                        + " | \u65f6\u95f4\u6bb5:" + currentActivityRangeText
                        + " | \u534a\u5f84:" + pointInfo.radius();
                DebugPackets.sendGameTestAddMarker(serverLevel, pos, markerText, 0xFFFFFFFF, 1100);
            }
        }
    }

    @Nullable
    private ReimuGoodNpcEntity resolveBoundNpc(ServerLevel serverLevel, Player player, ItemStack stack, CompoundTag tag) {
        BoundNpcSession session = BOUND_SESSIONS.get(player.getUUID());
        if (session != null) {
            Entity sessionByUuid = serverLevel.getEntity(session.npcUuid());
            if (sessionByUuid instanceof ReimuGoodNpcEntity npc) {
                this.writeBinding(tag, npc);
                this.writeWandTag(stack, tag);
                return npc;
            }
            Entity sessionById = serverLevel.getEntity(session.npcEntityId());
            if (sessionById instanceof ReimuGoodNpcEntity npc) {
                this.writeBinding(tag, npc);
                this.writeWandTag(stack, tag);
                return npc;
            }
        }

        if (tag.hasUUID(BOUND_NPC_UUID_TAG)) {
            Entity entityByUuid = serverLevel.getEntity(tag.getUUID(BOUND_NPC_UUID_TAG));
            if (entityByUuid instanceof ReimuGoodNpcEntity npc) {
                BOUND_SESSIONS.put(player.getUUID(), new BoundNpcSession(npc.getUUID(), npc.getId()));
                return npc;
            }
        }
        if (tag.contains(BOUND_NPC_ID_TAG, Tag.TAG_INT)) {
            Entity entityById = serverLevel.getEntity(tag.getInt(BOUND_NPC_ID_TAG));
            if (entityById instanceof ReimuGoodNpcEntity npc) {
                BOUND_SESSIONS.put(player.getUUID(), new BoundNpcSession(npc.getUUID(), npc.getId()));
                return npc;
            }
        }
        return null;
    }

    private CompoundTag readWandTag(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        return customData == null ? new CompoundTag() : customData.copyTag();
    }

    private void writeWandTag(ItemStack stack, CompoundTag tag) {
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private void writeBinding(CompoundTag tag, ReimuGoodNpcEntity npc) {
        tag.putUUID(BOUND_NPC_UUID_TAG, npc.getUUID());
        tag.putInt(BOUND_NPC_ID_TAG, npc.getId());
    }

    private void clearBinding(CompoundTag tag) {
        tag.remove(BOUND_NPC_UUID_TAG);
        tag.remove(BOUND_NPC_ID_TAG);
    }

    private static boolean clearBindingIfMatchedNpc(ItemStack stack, UUID npcUuid, int npcEntityId) {
        if (stack.isEmpty() || !stack.is(eiyahanabimachiservernpc.NPC_PATROL_WAND.get())) {
            return false;
        }

        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return false;
        }

        CompoundTag tag = customData.copyTag();
        boolean uuidMatched = tag.hasUUID(BOUND_NPC_UUID_TAG) && npcUuid.equals(tag.getUUID(BOUND_NPC_UUID_TAG));
        boolean idMatched = tag.contains(BOUND_NPC_ID_TAG, Tag.TAG_INT) && tag.getInt(BOUND_NPC_ID_TAG) == npcEntityId;
        if (!uuidMatched && !idMatched) {
            return false;
        }

        tag.remove(BOUND_NPC_UUID_TAG);
        tag.remove(BOUND_NPC_ID_TAG);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return true;
    }

    private boolean isHeldByPlayer(Player player, ItemStack stack) {
        return player.getMainHandItem() == stack || player.getOffhandItem() == stack;
    }

    private void spawnGreenCircle(ServerLevel level, BlockPos center) {
        Vec3 centerVec = Vec3.atCenterOf(center).add(0.0D, 0.05D, 0.0D);
        double radius = 0.55D;
        for (int i = 0; i < 28; i++) {
            double angle = (Math.PI * 2.0D * i) / 28.0D;
            double x = centerVec.x + Math.cos(angle) * radius;
            double z = centerVec.z + Math.sin(angle) * radius;
            level.sendParticles(GREEN_DUST, x, centerVec.y, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    private String activityTypeText(ActivityType type) {
        return switch (type) {
            case SLEEP -> "\u52a8\u4f5c1";
            case PLAY -> "\u52a8\u4f5c2";
            case WANDER -> "\u52a8\u4f5c3";
            case WORK -> "\u52a8\u4f5c4";
            case ACTION5 -> "\u52a8\u4f5c5";
            case ACTION6 -> "\u52a8\u4f5c6";
            case ACTION7 -> "\u52a8\u4f5c7";
            case ACTION8 -> "\u52a8\u4f5c8";
        };
    }

    private String currentScheduleRangeText(ReimuGoodNpcEntity npc, int currentTickOfDay) {
        ReimuGoodNpcEntity.ScheduleWindow window = npc.getCurrentScheduleWindow(currentTickOfDay);
        return formatRange(window.startTick(), window.endTick());
    }

    private static String formatRange(int startTick, int endTick) {
        return formatTime(startTick) + "-" + formatTime(endTick);
    }

    private static String formatTime(int tickOfDay) {
        int normalized = Math.floorMod(tickOfDay, 24000);
        int totalMinutes = (normalized * 60) / 1000;
        int hours = (totalMinutes / 60 + 6) % 24;
        int minutes = totalMinutes % 60;
        return String.format("%02d:%02d", hours, minutes);
    }

    private record BoundNpcSession(UUID npcUuid, int npcEntityId) {
    }
}
