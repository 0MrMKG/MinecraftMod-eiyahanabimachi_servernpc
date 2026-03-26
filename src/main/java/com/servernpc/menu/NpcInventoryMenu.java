package com.servernpc.menu;

import com.servernpc.eiyahanabimachiservernpc;
import com.servernpc.entity.ReimuGoodNpcEntity;
import javax.annotation.Nullable;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class NpcInventoryMenu extends AbstractContainerMenu {
    private static final EquipmentSlot[] EQUIPMENT_ORDER = new EquipmentSlot[] {
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET,
            EquipmentSlot.MAINHAND,
            EquipmentSlot.OFFHAND
    };

    private static final int HEAD_SLOT = 0;
    private static final int CHEST_SLOT = 1;
    private static final int LEGS_SLOT = 2;
    private static final int FEET_SLOT = 3;
    private static final int MAINHAND_SLOT = 4;
    private static final int OFFHAND_SLOT = 5;

    private static final int EQUIPMENT_SLOT_COUNT = 6;
    private static final int EXTRA_SLOT_COUNT = 6;
    private static final int BACKPACK_SLOT_COUNT = 27;
    private static final int EXTRA_SLOT_START = EQUIPMENT_SLOT_COUNT;
    private static final int BACKPACK_SLOT_START = EXTRA_SLOT_START + EXTRA_SLOT_COUNT;
    private static final int NPC_SLOT_COUNT = BACKPACK_SLOT_START + BACKPACK_SLOT_COUNT;
    private static final int HOTBAR_START = NPC_SLOT_COUNT;
    private static final int HOTBAR_END = HOTBAR_START + 9;
    private static final int HAND_SLOT_Y = 62;
    private static final int OFFHAND_SLOT_X = 126;
    private static final int MAINHAND_SLOT_X = 104;
    private static final int EXTRA_SLOT_START_X = 104;
    private static final int EXTRA_SLOT_START_Y = 24;
    private static final int EXTRA_SLOT_COLS = 3;
    private static final int EXTRA_SLOT_ROWS = 2;

    private final Player owner;
    @Nullable
    private final ReimuGoodNpcEntity npc;
    private final int npcEntityId;

    public NpcInventoryMenu(int containerId, Inventory playerInventory, int npcEntityId) {
        this(containerId, playerInventory, resolveNpc(playerInventory.player, npcEntityId), npcEntityId);
    }

    public NpcInventoryMenu(int containerId, Inventory playerInventory, ReimuGoodNpcEntity npc) {
        this(containerId, playerInventory, npc, npc.getId());
    }

    private NpcInventoryMenu(int containerId, Inventory playerInventory, @Nullable ReimuGoodNpcEntity npc, int npcEntityId) {
        super(eiyahanabimachiservernpc.NPC_INVENTORY_MENU.get(), containerId);
        this.owner = playerInventory.player;
        this.npc = npc;
        this.npcEntityId = npcEntityId;

        Container equipmentContainer = this.npc != null ? new NpcEquipmentContainer(this.npc) : new DummyEquipmentContainer();
        Container extraEquipmentContainer = this.npc != null ? this.npc.getExtraEquipmentContainer() : new SimpleContainer(EXTRA_SLOT_COUNT);
        Container backpackContainer = this.npc != null ? this.npc.getBackpackContainer() : new SimpleContainer(BACKPACK_SLOT_COUNT);
        if (this.npc != null) {
            this.npc.syncBackpackLockPlaceholders();
        }

        this.addEquipmentSlot(equipmentContainer, HEAD_SLOT, 8, 8);
        this.addEquipmentSlot(equipmentContainer, CHEST_SLOT, 8, 26);
        this.addEquipmentSlot(equipmentContainer, LEGS_SLOT, 8, 44);
        this.addEquipmentSlot(equipmentContainer, FEET_SLOT, 8, 62);
        this.addEquipmentSlot(equipmentContainer, MAINHAND_SLOT, MAINHAND_SLOT_X, HAND_SLOT_Y);
        this.addEquipmentSlot(equipmentContainer, OFFHAND_SLOT, OFFHAND_SLOT_X, HAND_SLOT_Y);
        for (int row = 0; row < EXTRA_SLOT_ROWS; row++) {
            for (int col = 0; col < EXTRA_SLOT_COLS; col++) {
                int index = col + row * EXTRA_SLOT_COLS;
                this.addExtraSlot(
                        extraEquipmentContainer,
                        index,
                        EXTRA_SLOT_START_X + col * 18,
                        EXTRA_SLOT_START_Y + row * 18
                );
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int backpackIndex = col + row * 9;
                this.addSlot(new NpcBackpackSlot(backpackContainer, backpackIndex, 8 + col * 18, 101 + row * 18, this.npc));
            }
        }

        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 164));
        }
    }

    @Nullable
    public ReimuGoodNpcEntity getNpc() {
        return this.npc;
    }

    public int getNpcEntityId() {
        return this.npcEntityId;
    }

    @Override
    public boolean stillValid(Player player) {
        if (this.npc == null) {
            return true;
        }
        return this.npc.isAlive() && player.distanceToSqr(this.npc) <= 64.0D;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot sourceSlot = this.slots.get(index);
        if (!sourceSlot.hasItem()) {
            return ItemStack.EMPTY;
        }
        if (this.isLockedBackpackMenuIndex(index)) {
            return ItemStack.EMPTY;
        }

        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack sourceCopy = sourceStack.copy();

        if (index < NPC_SLOT_COUNT) {
            if (!this.moveItemStackTo(sourceStack, HOTBAR_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            int preferredEquipment = this.getPreferredEquipmentSlotIndex(sourceStack);
            if (preferredEquipment >= 0
                    && !this.slots.get(preferredEquipment).hasItem()
                    && this.slots.get(preferredEquipment).mayPlace(sourceStack)
                    && this.moveItemStackTo(sourceStack, preferredEquipment, preferredEquipment + 1, false)) {
                // moved into equipment
            } else if (!this.moveItemStackTo(sourceStack, EXTRA_SLOT_START, NPC_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (sourceStack.isEmpty()) {
            sourceSlot.set(ItemStack.EMPTY);
        } else {
            sourceSlot.setChanged();
        }

        return sourceCopy;
    }

    private void addEquipmentSlot(Container equipmentContainer, int slotIndex, int x, int y) {
        this.addSlot(new Slot(equipmentContainer, slotIndex, x, y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return NpcInventoryMenu.this.canPlaceInEquipmentSlot(slotIndex, stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
    }

    private void addExtraSlot(Container extraContainer, int slotIndex, int x, int y) {
        this.addSlot(new Slot(extraContainer, slotIndex, x, y) {
            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
    }

    private int getPreferredEquipmentSlotIndex(ItemStack stack) {
        EquipmentSlot equipmentSlot = this.resolveEquipmentSlot(stack);
        return switch (equipmentSlot) {
            case HEAD -> HEAD_SLOT;
            case CHEST -> CHEST_SLOT;
            case LEGS -> LEGS_SLOT;
            case FEET -> FEET_SLOT;
            case OFFHAND -> OFFHAND_SLOT;
            default -> MAINHAND_SLOT;
        };
    }

    private boolean canPlaceInEquipmentSlot(int equipmentIndex, ItemStack stack) {
        if (equipmentIndex == MAINHAND_SLOT || equipmentIndex == OFFHAND_SLOT) {
            return true;
        }
        return this.resolveEquipmentSlot(stack) == EQUIPMENT_ORDER[equipmentIndex];
    }

    private EquipmentSlot resolveEquipmentSlot(ItemStack stack) {
        if (this.npc != null) {
            return this.npc.getEquipmentSlotForItem(stack);
        }
        return this.owner.getEquipmentSlotForItem(stack);
    }

    private boolean isLockedBackpackMenuIndex(int menuIndex) {
        if (this.npc == null || menuIndex < BACKPACK_SLOT_START || menuIndex >= NPC_SLOT_COUNT) {
            return false;
        }
        int backpackIndex = menuIndex - BACKPACK_SLOT_START;
        return !this.npc.isBackpackSlotUnlocked(backpackIndex);
    }

    @Nullable
    private static ReimuGoodNpcEntity resolveNpc(Player player, int entityId) {
        if (entityId < 0) {
            return null;
        }
        Entity entity = player.level().getEntity(entityId);
        return entity instanceof ReimuGoodNpcEntity npcEntity ? npcEntity : null;
    }

    private static final class DummyEquipmentContainer extends SimpleContainer {
        private DummyEquipmentContainer() {
            super(EQUIPMENT_SLOT_COUNT);
        }
    }

    private static final class NpcBackpackSlot extends Slot {
        @Nullable
        private final ReimuGoodNpcEntity npc;
        private final int backpackIndex;

        private NpcBackpackSlot(Container container, int slot, int x, int y, @Nullable ReimuGoodNpcEntity npc) {
            super(container, slot, x, y);
            this.npc = npc;
            this.backpackIndex = slot;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return this.npc == null || this.npc.isBackpackSlotUnlocked(this.backpackIndex);
        }

        @Override
        public boolean mayPickup(Player player) {
            return this.npc == null || this.npc.isBackpackSlotUnlocked(this.backpackIndex);
        }
    }

    private static final class NpcEquipmentContainer implements Container {
        private final ReimuGoodNpcEntity npc;

        private NpcEquipmentContainer(ReimuGoodNpcEntity npc) {
            this.npc = npc;
        }

        @Override
        public int getContainerSize() {
            return EQUIPMENT_SLOT_COUNT;
        }

        @Override
        public boolean isEmpty() {
            for (EquipmentSlot slot : EQUIPMENT_ORDER) {
                if (!this.npc.getItemBySlot(slot).isEmpty()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public ItemStack getItem(int slot) {
            return this.npc.getItemBySlot(EQUIPMENT_ORDER[slot]);
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            ItemStack current = this.getItem(slot);
            if (current.isEmpty()) {
                return ItemStack.EMPTY;
            }

            ItemStack split = current.split(amount);
            if (current.isEmpty()) {
                this.setItem(slot, ItemStack.EMPTY);
            } else {
                this.setItem(slot, current);
            }
            return split;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            ItemStack current = this.getItem(slot);
            this.setItem(slot, ItemStack.EMPTY);
            return current;
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            this.npc.setItemSlot(EQUIPMENT_ORDER[slot], stack);
        }

        @Override
        public void setChanged() {
        }

        @Override
        public boolean stillValid(Player player) {
            return this.npc.isAlive();
        }

        @Override
        public void clearContent() {
            for (int i = 0; i < EQUIPMENT_SLOT_COUNT; i++) {
                this.setItem(i, ItemStack.EMPTY);
            }
        }
    }
}
