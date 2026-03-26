package com.servernpc.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.servernpc.eiyahanabimachiservernpc;
import com.servernpc.entity.ReimuGoodNpcEntity;
import com.servernpc.menu.NpcInventoryMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class NpcInventoryScreen extends AbstractContainerScreen<NpcInventoryMenu> {
    private static final ResourceLocation INVENTORY_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/gui/container/inventory.png");
    private static final ResourceLocation GENERIC_54_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");
    private static final int TOP_SECTION_HEIGHT = 84;
    private static final int BACKPACK_HEADER_HEIGHT = 17;
    private static final int BACKPACK_ROWS_HEIGHT = 54;
    private static final int BACKPACK_SECTION_HEIGHT = BACKPACK_HEADER_HEIGHT + BACKPACK_ROWS_HEIGHT;
    private static final int HOTBAR_GAP = 9;
    private static final int HOTBAR_SECTION_HEIGHT = 24;
    private static final int RIGHT_PANEL_X = 76;
    private static final int RIGHT_PANEL_Y = 8;
    private static final int RIGHT_PANEL_WIDTH = 98;
    private static final int RIGHT_PANEL_HEIGHT = 72;
    private static final int SECTION_GRAY = 0xFFC6C6C6;
    private static final int OFFHAND_SLOT_X = 126;
    private static final int MAINHAND_SLOT_X = 104;
    private static final int HAND_SLOT_Y = 62;
    private static final int EXTRA_SLOT_START_X = 104;
    private static final int EXTRA_SLOT_START_Y = 24;
    private static final int EXTRA_SLOT_COLS = 3;
    private static final int EXTRA_SLOT_ROWS = 2;
    private static final int PREVIEW_X = 27;
    private static final int PREVIEW_Y = 8;
    private static final int PREVIEW_WIDTH = 48;
    private static final int PREVIEW_HEIGHT = 70;
    private static final int SLOT_SHADOW_COLOR = 0x22000000;
    private static final int HOTBAR_SLOT_SHADOW_COLOR = 0x20000000;

    public NpcInventoryScreen(NpcInventoryMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = TOP_SECTION_HEIGHT + BACKPACK_SECTION_HEIGHT + HOTBAR_GAP + HOTBAR_SECTION_HEIGHT;
        this.titleLabelX = 8;
        this.titleLabelY = 6;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int left = this.leftPos;
        int top = this.topPos;

        guiGraphics.blit(INVENTORY_TEXTURE, left, top, 0.0F, 0.0F, this.imageWidth, TOP_SECTION_HEIGHT, 256, 256);
        // Use a clean right-side panel to remove vanilla craft-area remnants and stray lines.
        guiGraphics.fill(
                left + RIGHT_PANEL_X,
                top + RIGHT_PANEL_Y,
                left + RIGHT_PANEL_X + RIGHT_PANEL_WIDTH,
                top + RIGHT_PANEL_Y + RIGHT_PANEL_HEIGHT,
                SECTION_GRAY
        );

        int backpackTop = top + TOP_SECTION_HEIGHT;
        guiGraphics.blit(GENERIC_54_TEXTURE, left, backpackTop, 0.0F, 0.0F, this.imageWidth, BACKPACK_HEADER_HEIGHT, 256, 256);
        guiGraphics.blit(GENERIC_54_TEXTURE, left, backpackTop + BACKPACK_HEADER_HEIGHT, 0.0F, 17.0F, this.imageWidth, BACKPACK_ROWS_HEIGHT, 256, 256);
        this.drawTopBottomTransition(guiGraphics, left, top + TOP_SECTION_HEIGHT - 1);

        int gapTop = backpackTop + BACKPACK_SECTION_HEIGHT;
        this.drawHotbarGap(guiGraphics, left, gapTop);
        int hotbarTop = gapTop + HOTBAR_GAP;
        guiGraphics.blit(INVENTORY_TEXTURE, left, hotbarTop, 0.0F, 142.0F, this.imageWidth, HOTBAR_SECTION_HEIGHT, 256, 256);
        this.drawHotbarSlotShadows(guiGraphics, left, hotbarTop);

        int previewLeft = left + PREVIEW_X;
        int previewTop = top + PREVIEW_Y;
        guiGraphics.fillGradient(
                previewLeft,
                previewTop,
                previewLeft + PREVIEW_WIDTH,
                previewTop + PREVIEW_HEIGHT,
                0xFF7FB4FF,
                0xFFFFFFFF
        );
        this.drawPanelBorder(guiGraphics, previewLeft, previewTop, PREVIEW_WIDTH, PREVIEW_HEIGHT);
        this.drawExtraEquipmentSlots(guiGraphics, left, top);
        this.drawHandSlot(guiGraphics, left + MAINHAND_SLOT_X, top + HAND_SLOT_Y);
        this.drawHandSlot(guiGraphics, left + OFFHAND_SLOT_X, top + HAND_SLOT_Y);
        this.renderNpcPreview(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(
                this.font,
                Component.translatable("container." + eiyahanabimachiservernpc.MODID + ".npc_backpack"),
                8,
                90,
                0x404040,
                false
        );
        guiGraphics.drawString(this.font, Component.translatable("gui." + eiyahanabimachiservernpc.MODID + ".equipment_area"), 104, 14, 0x505050, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    private void drawPanelBorder(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        int x2 = x + width;
        int y2 = y + height;
        guiGraphics.fill(x - 1, y - 1, x2 + 1, y, 0xFF373737);
        guiGraphics.fill(x - 1, y - 1, x, y2 + 1, 0xFF373737);
        guiGraphics.fill(x2, y - 1, x2 + 1, y2 + 1, 0xFFF0F0F0);
        guiGraphics.fill(x - 1, y2, x2 + 1, y2 + 1, 0xFFF0F0F0);
    }

    private void drawTopBottomTransition(GuiGraphics guiGraphics, int left, int seamY) {
        guiGraphics.fill(left + 4, seamY, left + this.imageWidth - 4, seamY + 1, SECTION_GRAY);
        guiGraphics.fill(left + 2, seamY + 1, left + this.imageWidth - 2, seamY + 2, SECTION_GRAY);
    }

    private void drawHotbarGap(GuiGraphics guiGraphics, int left, int gapTop) {
        if (HOTBAR_GAP <= 0) {
            return;
        }
        guiGraphics.fill(left + 3, gapTop, left + this.imageWidth - 3, gapTop + HOTBAR_GAP, SECTION_GRAY);
        // Keep side borders continuous instead of being cut by the spacer.
        guiGraphics.fill(left + 1, gapTop, left + 3, gapTop + HOTBAR_GAP, 0xFFE0E0E0);
        guiGraphics.fill(left + this.imageWidth - 3, gapTop, left + this.imageWidth - 1, gapTop + HOTBAR_GAP, 0xFF4C4C4C);
    }

    private void drawHandSlot(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.blit(INVENTORY_TEXTURE, x, y, 77.0F, 62.0F, 18, 18, 256, 256);
        this.drawSlotShadow(guiGraphics, x, y);
    }

    private void drawExtraEquipmentSlots(GuiGraphics guiGraphics, int left, int top) {
        for (int row = 0; row < EXTRA_SLOT_ROWS; row++) {
            for (int col = 0; col < EXTRA_SLOT_COLS; col++) {
                int x = left + EXTRA_SLOT_START_X + col * 18;
                int y = top + EXTRA_SLOT_START_Y + row * 18;
                guiGraphics.blit(INVENTORY_TEXTURE, x, y, 77.0F, 62.0F, 18, 18, 256, 256);
                this.drawSlotShadow(guiGraphics, x, y);
            }
        }
    }

    private void drawSlotShadow(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.fill(x + 1, y + 1, x + 17, y + 17, SLOT_SHADOW_COLOR);
    }

    private void drawHotbarSlotShadows(GuiGraphics guiGraphics, int left, int hotbarTop) {
        for (int col = 0; col < 9; col++) {
            int x = left + 8 + col * 18;
            int y = hotbarTop + 1;
            guiGraphics.fill(x + 1, y + 1, x + 17, y + 17, HOTBAR_SLOT_SHADOW_COLOR);
        }
    }

    private void renderNpcPreview(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        ReimuGoodNpcEntity npc = this.menu.getNpc();
        if (npc == null) {
            return;
        }
        int previewLeft = this.leftPos + PREVIEW_X;
        int previewTop = this.topPos + PREVIEW_Y;
        boolean wasNameVisible = npc.isCustomNameVisible();
        npc.setCustomNameVisible(false);
        InventoryScreen.renderEntityInInventoryFollowsMouse(
                guiGraphics,
                previewLeft,
                previewTop,
                previewLeft + PREVIEW_WIDTH,
                previewTop + PREVIEW_HEIGHT,
                30,
                0.0625F,
                mouseX,
                mouseY,
                npc
        );
        npc.setCustomNameVisible(wasNameVisible);
    }
}
