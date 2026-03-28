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

public record ExecuteNpcDialogueFunctionPayload(int npcEntityId, String functionCall) implements CustomPacketPayload {
    public static final Type<ExecuteNpcDialogueFunctionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(eiyahanabimachiservernpc.MODID, "execute_npc_dialogue_function"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ExecuteNpcDialogueFunctionPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    ExecuteNpcDialogueFunctionPayload::npcEntityId,
                    ByteBufCodecs.STRING_UTF8,
                    ExecuteNpcDialogueFunctionPayload::functionCall,
                    ExecuteNpcDialogueFunctionPayload::new
            );

    @Override
    public Type<ExecuteNpcDialogueFunctionPayload> type() {
        return TYPE;
    }

    public static void handle(ExecuteNpcDialogueFunctionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer serverPlayer)) {
                return;
            }
            if (payload.functionCall() == null || payload.functionCall().isBlank()) {
                return;
            }

            Entity entity = serverPlayer.level().getEntity(payload.npcEntityId());
            if (!(entity instanceof ReimuGoodNpcEntity npc)) {
                return;
            }
            npc.executeDialogueFunction(serverPlayer, payload.functionCall());
        });
    }
}
