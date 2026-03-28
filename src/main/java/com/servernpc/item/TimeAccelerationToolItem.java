package com.servernpc.item;

import com.servernpc.eiyahanabimachiservernpc;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;

public class TimeAccelerationToolItem extends Item {
    private static final int TIME_MULTIPLIER = 60;
    private static final int ACTIONBAR_UPDATE_INTERVAL = 10;

    public TimeAccelerationToolItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        player.startUsingItem(usedHand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public void onUseTick(Level level, LivingEntity livingEntity, ItemStack stack, int remainingUseDuration) {
        if (!(level instanceof ServerLevel serverLevel) || !(livingEntity instanceof ServerPlayer)) {
            return;
        }

        int extraTicks = serverLevel.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT)
                ? TIME_MULTIPLIER - 1
                : TIME_MULTIPLIER;
        serverLevel.setDayTime(serverLevel.getDayTime() + extraTicks);
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
        if (serverPlayer.tickCount % ACTIONBAR_UPDATE_INTERVAL != 0) {
            return;
        }

        int tickOfDay = Math.floorMod((int) serverLevel.getDayTime(), 24000);
        serverPlayer.displayClientMessage(
                Component.translatable(
                        "message." + eiyahanabimachiservernpc.MODID + ".time_acceleration_tool.now_time",
                        formatTime(tickOfDay),
                        tickOfDay
                ),
                true
        );
    }

    private boolean isHeldByPlayer(Player player, ItemStack stack) {
        return player.getMainHandItem() == stack || player.getOffhandItem() == stack;
    }

    private static String formatTime(int tickOfDay) {
        int normalized = Math.floorMod(tickOfDay, 24000);
        int totalMinutes = (normalized * 60) / 1000;
        int hours = (totalMinutes / 60 + 6) % 24;
        int minutes = totalMinutes % 60;
        return String.format("%02d:%02d", hours, minutes);
    }
}
