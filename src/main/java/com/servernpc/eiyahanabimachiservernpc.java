package com.servernpc;

import com.mojang.logging.LogUtils;
import com.servernpc.entity.ReimuGoodNpcEntity;
import com.servernpc.item.NpcInventoryToolItem;
import com.servernpc.item.NpcPatrolWandItem;
import com.servernpc.item.StopMovementToolItem;
import com.servernpc.item.TimeAccelerationToolItem;
import com.servernpc.menu.NpcInventoryMenu;
import com.servernpc.menu.NpcPatrolConfigMenu;
import com.servernpc.network.payload.StopNpcDialogueFocusPayload;
import org.slf4j.Logger;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(eiyahanabimachiservernpc.MODID)
public class eiyahanabimachiservernpc {
    public static final String MODID = "eiyahanabimachiservernpc";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<ReimuGoodNpcEntity>> HAKUREI_REIMU_GOOD = registerNpcEntity("hakurei_reimu_good");
    public static final DeferredHolder<EntityType<?>, EntityType<ReimuGoodNpcEntity>> HINANAWI_TENSHI = registerNpcEntity("hinanawi_tenshi");
    public static final DeferredHolder<EntityType<?>, EntityType<ReimuGoodNpcEntity>> REMILIA_SCARLET = registerNpcEntity("remilia_scarlet");
    public static final DeferredHolder<EntityType<?>, EntityType<ReimuGoodNpcEntity>> SAIGYOUJI_YUYUKO = registerNpcEntity("saigyouji_yuyuko");
    public static final DeferredHolder<EntityType<?>, EntityType<ReimuGoodNpcEntity>> CHEN = registerNpcEntity("chen");
    public static final DeferredHolder<EntityType<?>, EntityType<ReimuGoodNpcEntity>> KOCHIYA_SANAE = registerNpcEntity("kochiya_sanae");
    public static final DeferredHolder<EntityType<?>, EntityType<ReimuGoodNpcEntity>> HONG_MEILING = registerNpcEntity("hong_meiling");
    public static final DeferredHolder<EntityType<?>, EntityType<ReimuGoodNpcEntity>> REISEN_UDONGEIN = registerNpcEntity("reisen_udongein");
    public static final DeferredHolder<EntityType<?>, EntityType<ReimuGoodNpcEntity>> CIRNO = registerNpcEntity("cirno");
    public static final DeferredHolder<EntityType<?>, EntityType<ReimuGoodNpcEntity>> SHAMEIMARU_AYA = registerNpcEntity("shameimaru_aya");
    public static final DeferredHolder<EntityType<?>, EntityType<ReimuGoodNpcEntity>> IZAYOI_SAKUYA = registerNpcEntity("izayoi_sakuya");
    public static final DeferredHolder<EntityType<?>, EntityType<ReimuGoodNpcEntity>> KIRISAME_MARISA = registerNpcEntity("kirisame_marisa");

    public static final DeferredItem<Item> HAKUREI_REIMU_GOOD_SPAWN_EGG = registerNpcSpawnEgg("hakurei_reimu_good", HAKUREI_REIMU_GOOD, 0xD92D4C, 0xFFF2A8);
    public static final DeferredItem<Item> HINANAWI_TENSHI_SPAWN_EGG = registerNpcSpawnEgg("hinanawi_tenshi", HINANAWI_TENSHI, 0x4F9EDE, 0xF5D77C);
    public static final DeferredItem<Item> REMILIA_SCARLET_SPAWN_EGG = registerNpcSpawnEgg("remilia_scarlet", REMILIA_SCARLET, 0xD8485A, 0xA7D7F5);
    public static final DeferredItem<Item> SAIGYOUJI_YUYUKO_SPAWN_EGG = registerNpcSpawnEgg("saigyouji_yuyuko", SAIGYOUJI_YUYUKO, 0xEAB4E2, 0x5DBD85);
    public static final DeferredItem<Item> CHEN_SPAWN_EGG = registerNpcSpawnEgg("chen", CHEN, 0xF7A64A, 0x4D2B17);
    public static final DeferredItem<Item> KOCHIYA_SANAE_SPAWN_EGG = registerNpcSpawnEgg("kochiya_sanae", KOCHIYA_SANAE, 0x2F8F5D, 0xEDEDED);
    public static final DeferredItem<Item> HONG_MEILING_SPAWN_EGG = registerNpcSpawnEgg("hong_meiling", HONG_MEILING, 0xD83A3A, 0x3EA36A);
    public static final DeferredItem<Item> REISEN_UDONGEIN_SPAWN_EGG = registerNpcSpawnEgg("reisen_udongein", REISEN_UDONGEIN, 0xA778C9, 0xF0F0F0);
    public static final DeferredItem<Item> CIRNO_SPAWN_EGG = registerNpcSpawnEgg("cirno", CIRNO, 0x5AC9F2, 0x2E6FA9);
    public static final DeferredItem<Item> SHAMEIMARU_AYA_SPAWN_EGG = registerNpcSpawnEgg("shameimaru_aya", SHAMEIMARU_AYA, 0x2A2A2A, 0xE13D3D);
    public static final DeferredItem<Item> IZAYOI_SAKUYA_SPAWN_EGG = registerNpcSpawnEgg("izayoi_sakuya", IZAYOI_SAKUYA, 0x8AA3C9, 0xEAEAEA);
    public static final DeferredItem<Item> KIRISAME_MARISA_SPAWN_EGG = registerNpcSpawnEgg("kirisame_marisa", KIRISAME_MARISA, 0xF0D23B, 0x141414);
    public static final DeferredItem<Item> STOP_MOVEMENT_TOOL = ITEMS.register(
            "stop_movement_tool",
            () -> new StopMovementToolItem(new Item.Properties().stacksTo(1))
    );
    public static final DeferredItem<Item> NPC_INVENTORY_TOOL = ITEMS.register(
            "npc_inventory_tool",
            () -> new NpcInventoryToolItem(new Item.Properties().stacksTo(1))
    );
    public static final DeferredItem<Item> NPC_PATROL_WAND = ITEMS.register(
            "npc_patrol_wand",
            () -> new NpcPatrolWandItem(new Item.Properties().stacksTo(1))
    );
    public static final DeferredItem<Item> TIME_ACCELERATION_TOOL = ITEMS.register(
            "time_acceleration_tool",
            () -> new TimeAccelerationToolItem(new Item.Properties().stacksTo(1))
    );
    public static final DeferredHolder<MenuType<?>, MenuType<NpcInventoryMenu>> NPC_INVENTORY_MENU = MENU_TYPES.register(
            "npc_inventory",
            () -> IMenuTypeExtension.create(
                    (containerId, playerInventory, data) -> new NpcInventoryMenu(
                            containerId,
                            playerInventory,
                            data != null ? data.readVarInt() : -1
                    )
            )
    );
    public static final DeferredHolder<MenuType<?>, MenuType<NpcPatrolConfigMenu>> NPC_PATROL_CONFIG_MENU = MENU_TYPES.register(
            "npc_patrol_config",
            () -> IMenuTypeExtension.create(
                    (containerId, playerInventory, data) -> new NpcPatrolConfigMenu(
                            containerId,
                            playerInventory,
                            data
                    )
            )
    );
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> NPC_SPAWN_EGGS_TAB = CREATIVE_MODE_TABS.register(
            "npc_spawn_eggs",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + MODID + ".npc_spawn_eggs"))
                    .icon(() -> new ItemStack(Items.CAKE))
                    .displayItems((parameters, output) -> {
                        output.accept(STOP_MOVEMENT_TOOL.get());
                        output.accept(NPC_INVENTORY_TOOL.get());
                        output.accept(NPC_PATROL_WAND.get());
                        output.accept(TIME_ACCELERATION_TOOL.get());
                        output.accept(HAKUREI_REIMU_GOOD_SPAWN_EGG.get());
                        output.accept(HINANAWI_TENSHI_SPAWN_EGG.get());
                        output.accept(REMILIA_SCARLET_SPAWN_EGG.get());
                        output.accept(SAIGYOUJI_YUYUKO_SPAWN_EGG.get());
                        output.accept(CHEN_SPAWN_EGG.get());
                        output.accept(KOCHIYA_SANAE_SPAWN_EGG.get());
                        output.accept(HONG_MEILING_SPAWN_EGG.get());
                        output.accept(REISEN_UDONGEIN_SPAWN_EGG.get());
                        output.accept(CIRNO_SPAWN_EGG.get());
                        output.accept(SHAMEIMARU_AYA_SPAWN_EGG.get());
                        output.accept(IZAYOI_SAKUYA_SPAWN_EGG.get());
                        output.accept(KIRISAME_MARISA_SPAWN_EGG.get());
                    })
                    .build()
    );

    private static DeferredHolder<EntityType<?>, EntityType<ReimuGoodNpcEntity>> registerNpcEntity(String id) {
        return ENTITY_TYPES.register(
                id,
                () -> EntityType.Builder.of(ReimuGoodNpcEntity::new, MobCategory.MONSTER)
                        .sized(0.6F, 1.95F)
                        .eyeHeight(1.62F)
                        .build(ResourceLocation.fromNamespaceAndPath(MODID, id).toString())
        );
    }

    private static DeferredItem<Item> registerNpcSpawnEgg(
            String id,
            DeferredHolder<EntityType<?>, EntityType<ReimuGoodNpcEntity>> entityHolder,
            int primaryColor,
            int secondaryColor
    ) {
        return ITEMS.register(
                id + "_spawn_egg",
                () -> new SpawnEggItem(entityHolder.get(), primaryColor, secondaryColor, new Item.Properties())
        );
    }

    public eiyahanabimachiservernpc(IEventBus modEventBus, ModContainer modContainer) {
        ENTITY_TYPES.register(modEventBus);
        ITEMS.register(modEventBus);
        MENU_TYPES.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        modEventBus.addListener(this::registerEntityAttributes);
        modEventBus.addListener(this::registerPayloadHandlers);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void registerPayloadHandlers(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(
                StopNpcDialogueFocusPayload.TYPE,
                StopNpcDialogueFocusPayload.STREAM_CODEC,
                StopNpcDialogueFocusPayload::handle
        );
    }

    private void registerEntityAttributes(EntityAttributeCreationEvent event) {
        event.put(HAKUREI_REIMU_GOOD.get(), ReimuGoodNpcEntity.createAttributes().build());
        event.put(HINANAWI_TENSHI.get(), ReimuGoodNpcEntity.createAttributes().build());
        event.put(REMILIA_SCARLET.get(), ReimuGoodNpcEntity.createAttributes().build());
        event.put(SAIGYOUJI_YUYUKO.get(), ReimuGoodNpcEntity.createAttributes().build());
        event.put(CHEN.get(), ReimuGoodNpcEntity.createAttributes().build());
        event.put(KOCHIYA_SANAE.get(), ReimuGoodNpcEntity.createAttributes().build());
        event.put(HONG_MEILING.get(), ReimuGoodNpcEntity.createAttributes().build());
        event.put(REISEN_UDONGEIN.get(), ReimuGoodNpcEntity.createAttributes().build());
        event.put(CIRNO.get(), ReimuGoodNpcEntity.createAttributes().build());
        event.put(SHAMEIMARU_AYA.get(), ReimuGoodNpcEntity.createAttributes().build());
        event.put(IZAYOI_SAKUYA.get(), ReimuGoodNpcEntity.createAttributes().build());
        event.put(KIRISAME_MARISA.get(), ReimuGoodNpcEntity.createAttributes().build());
    }
}
