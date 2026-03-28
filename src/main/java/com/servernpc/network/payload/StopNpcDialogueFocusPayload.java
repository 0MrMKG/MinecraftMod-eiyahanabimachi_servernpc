package com.servernpc.network.payload;

import com.servernpc.eiyahanabimachiservernpc;
import com.servernpc.entity.ReimuGoodNpcEntity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record StopNpcDialogueFocusPayload(int npcEntityId) implements CustomPacketPayload {
    public static final Type<StopNpcDialogueFocusPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(eiyahanabimachiservernpc.MODID, "stop_npc_dialogue_focus"));
    public static final StreamCodec<RegistryFriendlyByteBuf, StopNpcDialogueFocusPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    StopNpcDialogueFocusPayload::npcEntityId,
                    StopNpcDialogueFocusPayload::new
            );

    @Override
    public Type<StopNpcDialogueFocusPayload> type() {
        return TYPE;
    }

    public static void handle(StopNpcDialogueFocusPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer serverPlayer)) {
                return;
            }
            Entity entity = serverPlayer.level().getEntity(payload.npcEntityId());
            if (entity instanceof ReimuGoodNpcEntity npc) {
                npc.endDialogueFocus(serverPlayer);
            }
        });
    }
}
