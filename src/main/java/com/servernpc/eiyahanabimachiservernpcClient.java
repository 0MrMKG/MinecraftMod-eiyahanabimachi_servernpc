package com.servernpc;

import com.servernpc.client.render.SkinnedNpcRenderer;
import com.servernpc.client.screen.NpcInventoryScreen;
import com.servernpc.client.screen.NpcPatrolConfigScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = eiyahanabimachiservernpc.MODID, dist = Dist.CLIENT)
public class eiyahanabimachiservernpcClient {
    public eiyahanabimachiservernpcClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @EventBusSubscriber(modid = eiyahanabimachiservernpc.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            eiyahanabimachiservernpc.LOGGER.info("Client setup for {}", eiyahanabimachiservernpc.MODID);
            eiyahanabimachiservernpc.LOGGER.debug("Current player profile: {}", Minecraft.getInstance().getUser().getName());
        }

        @SubscribeEvent
        public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
            registerNpcRenderer(event, eiyahanabimachiservernpc.HAKUREI_REIMU_GOOD, "hakurei_reimu_good");
            registerNpcRenderer(event, eiyahanabimachiservernpc.HINANAWI_TENSHI, "hinanawi_tenshi");
            registerNpcRenderer(event, eiyahanabimachiservernpc.REMILIA_SCARLET, "remilia_scarlet");
            registerNpcRenderer(event, eiyahanabimachiservernpc.SAIGYOUJI_YUYUKO, "saigyouji_yuyuko");
            registerNpcRenderer(event, eiyahanabimachiservernpc.CHEN, "chen");
            registerNpcRenderer(event, eiyahanabimachiservernpc.KOCHIYA_SANAE, "kochiya_sanae");
            registerNpcRenderer(event, eiyahanabimachiservernpc.HONG_MEILING, "hong_meiling");
            registerNpcRenderer(event, eiyahanabimachiservernpc.REISEN_UDONGEIN, "reisen_udongein");
            registerNpcRenderer(event, eiyahanabimachiservernpc.CIRNO, "cirno");
            registerNpcRenderer(event, eiyahanabimachiservernpc.SHAMEIMARU_AYA, "shameimaru_aya");
            registerNpcRenderer(event, eiyahanabimachiservernpc.IZAYOI_SAKUYA, "izayoi_sakuya");
            registerNpcRenderer(event, eiyahanabimachiservernpc.KIRISAME_MARISA, "kirisame_marisa");
        }

        @SubscribeEvent
        public static void registerMenuScreens(RegisterMenuScreensEvent event) {
            event.register(eiyahanabimachiservernpc.NPC_INVENTORY_MENU.get(), NpcInventoryScreen::new);
            event.register(eiyahanabimachiservernpc.NPC_PATROL_CONFIG_MENU.get(), NpcPatrolConfigScreen::new);
        }

        private static void registerNpcRenderer(
                EntityRenderersEvent.RegisterRenderers event,
                net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.world.entity.EntityType<?>, net.minecraft.world.entity.EntityType<com.servernpc.entity.ReimuGoodNpcEntity>> entityHolder,
                String textureName
        ) {
            event.registerEntityRenderer(
                    entityHolder.get(),
                    context -> new SkinnedNpcRenderer(
                            context,
                            ResourceLocation.fromNamespaceAndPath(
                                    eiyahanabimachiservernpc.MODID,
                                    "textures/entity/npc/" + textureName + ".png"
                            )
                    )
            );
        }
    }
}
