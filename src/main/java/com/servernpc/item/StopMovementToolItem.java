package com.servernpc.item;

import com.servernpc.entity.ReimuGoodNpcEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class StopMovementToolItem extends Item {
    private static final Component FREEZE_MSG = Component.literal("\u5DF2\u6682\u505C\u76EE\u6807\u884C\u52A8");
    private static final Component UNFREEZE_MSG = Component.literal("\u5DF2\u6062\u590D\u76EE\u6807\u884C\u52A8");

    public StopMovementToolItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity interactionTarget, InteractionHand usedHand) {
        if (!(interactionTarget instanceof ReimuGoodNpcEntity npc)) {
            return InteractionResult.PASS;
        }

        if (!player.level().isClientSide) {
            boolean nextStopped = !npc.isMovementStopped();
            npc.setMovementStopped(nextStopped);
            player.displayClientMessage(nextStopped ? FREEZE_MSG : UNFREEZE_MSG, true);
        }
        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }
}
