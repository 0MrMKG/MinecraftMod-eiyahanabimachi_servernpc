package com.servernpc.item;

import com.servernpc.eiyahanabimachiservernpc;
import com.servernpc.entity.ReimuGoodNpcEntity;
import com.servernpc.menu.NpcInventoryMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class NpcInventoryToolItem extends Item {
    public NpcInventoryToolItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity interactionTarget, InteractionHand usedHand) {
        if (!(interactionTarget instanceof ReimuGoodNpcEntity npc)) {
            return InteractionResult.PASS;
        }

        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(
                    new SimpleMenuProvider(
                            (containerId, playerInventory, ignored) -> new NpcInventoryMenu(containerId, playerInventory, npc),
                            Component.translatable("container." + eiyahanabimachiservernpc.MODID + ".npc_inventory")
                    ),
                    buffer -> buffer.writeVarInt(npc.getId())
            );
        }

        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }
}
