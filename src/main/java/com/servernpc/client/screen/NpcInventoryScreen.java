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
import net.minecraft.util.Mth;
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
    private static final int EXTRA_SLOT_EXPAND = 1;
    private static final float SLOT_TEXTURE_U = 77.0F;
    private static final float SLOT_TEXTURE_V = 62.0F;
    private static final int PREVIEW_X = 27;
    private static final int PREVIEW_Y = 8;
    private static final int PREVIEW_WIDTH = 48;
    private static final int PREVIEW_HEIGHT = 70;
    private static final int AFFECTION_PANEL_X = 188;
    private static final int AFFECTION_PANEL_Y = 8;
    private static final int AFFECTION_PANEL_WIDTH = 98;
    private static final int AFFECTION_PANEL_HEIGHT = 72;
    private static final int AFFECTION_BAR_X = AFFECTION_PANEL_X + 8;
    private static final int AFFECTION_BAR_Y = AFFECTION_PANEL_Y + 34;
    private static final int AFFECTION_BAR_WIDTH = AFFECTION_PANEL_WIDTH - 16;
    private static final int AFFECTION_BAR_HEIGHT = 8;
    private static final int AFFECTION_TITLE_HEART_X = AFFECTION_PANEL_X + 58;
    private static final int AFFECTION_TITLE_HEART_Y = AFFECTION_PANEL_Y + 6;
    private static final int AFFECTION_SCORE_HEART_X = AFFECTION_PANEL_X + 8;
    private static final int AFFECTION_SCORE_HEART_Y = AFFECTION_PANEL_Y + 47;
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
        this.drawAffectionPanel(guiGraphics, left, top, partialTick);
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

        int affectionLevel = this.menu.getAffectionLevel();
        AffectionTheme affectionTheme = this.resolveAffectionTheme(affectionLevel);
        guiGraphics.drawString(
                this.font,
                Component.translatable("gui." + eiyahanabimachiservernpc.MODID + ".affection"),
                AFFECTION_PANEL_X + 8,
                AFFECTION_PANEL_Y + 8,
                0x404040,
                false
        );
        this.drawMechanicalHeartBadge(guiGraphics, AFFECTION_TITLE_HEART_X, AFFECTION_TITLE_HEART_Y, true);
        guiGraphics.drawString(
                this.font,
                Component.translatable("gui." + eiyahanabimachiservernpc.MODID + ".affection_level", affectionLevel),
                AFFECTION_PANEL_X + 8,
                AFFECTION_PANEL_Y + 20,
                affectionTheme.levelColor(),
                false
        );

        Component progressText = this.menu.shouldDisplayAffectionAsFull()
                ? Component.translatable("gui." + eiyahanabimachiservernpc.MODID + ".affection_full")
                : Component.translatable(
                        "gui." + eiyahanabimachiservernpc.MODID + ".affection_progress",
                        this.menu.getAffectionProgressInLevel(),
                        this.menu.getAffectionRequiredForNextLevel()
                );
        this.drawMechanicalHeartBadge(guiGraphics, AFFECTION_SCORE_HEART_X, AFFECTION_SCORE_HEART_Y, false);
        guiGraphics.drawString(this.font, progressText, AFFECTION_PANEL_X + 21, AFFECTION_PANEL_Y + 48, 0x505050, false);
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
        this.drawExpandedSlot(guiGraphics, x, y, SLOT_SHADOW_COLOR);
    }

    private void drawExtraEquipmentSlots(GuiGraphics guiGraphics, int left, int top) {
        for (int row = 0; row < EXTRA_SLOT_ROWS; row++) {
            for (int col = 0; col < EXTRA_SLOT_COLS; col++) {
                int x = left + EXTRA_SLOT_START_X + col * 18;
                int y = top + EXTRA_SLOT_START_Y + row * 18;
                this.drawExpandedExtraSlot(guiGraphics, x, y);
            }
        }
    }

    private void drawExpandedExtraSlot(GuiGraphics guiGraphics, int x, int y) {
        this.drawExpandedSlot(guiGraphics, x, y, SLOT_SHADOW_COLOR);
    }

    private void drawExpandedSlot(GuiGraphics guiGraphics, int x, int y, int shadowColor) {
        int drawX = x - EXTRA_SLOT_EXPAND;
        int drawY = y - EXTRA_SLOT_EXPAND;
        int drawSize = 18 + EXTRA_SLOT_EXPAND * 2;
        float textureU = SLOT_TEXTURE_U - EXTRA_SLOT_EXPAND;
        float textureV = SLOT_TEXTURE_V - EXTRA_SLOT_EXPAND;
        guiGraphics.blit(INVENTORY_TEXTURE, drawX, drawY, textureU, textureV, drawSize, drawSize, 256, 256);
        guiGraphics.fill(drawX + 1, drawY + 1, drawX + drawSize - 1, drawY + drawSize - 1, shadowColor);
    }

    private void drawHotbarSlotShadows(GuiGraphics guiGraphics, int left, int hotbarTop) {
        for (int col = 0; col < 9; col++) {
            int x = left + 8 + col * 18;
            int y = hotbarTop + 1;
            this.drawExpandedSlot(guiGraphics, x, y, HOTBAR_SLOT_SHADOW_COLOR);
        }
    }

    private void drawAffectionPanel(GuiGraphics guiGraphics, int left, int top, float partialTick) {
        int affectionLevel = this.menu.getAffectionLevel();
        AffectionTheme affectionTheme = this.resolveAffectionTheme(affectionLevel);

        int panelLeft = left + AFFECTION_PANEL_X;
        int panelTop = top + AFFECTION_PANEL_Y;
        guiGraphics.fill(panelLeft, panelTop, panelLeft + AFFECTION_PANEL_WIDTH, panelTop + AFFECTION_PANEL_HEIGHT, SECTION_GRAY);
        this.drawPanelBorder(guiGraphics, panelLeft, panelTop, AFFECTION_PANEL_WIDTH, AFFECTION_PANEL_HEIGHT);

        int barLeft = left + AFFECTION_BAR_X;
        int barTop = top + AFFECTION_BAR_Y;
        int barRight = barLeft + AFFECTION_BAR_WIDTH;
        int barBottom = barTop + AFFECTION_BAR_HEIGHT;
        int innerLeft = barLeft + 2;
        int innerTop = barTop + 2;
        int innerRight = barRight - 2;
        int innerBottom = barBottom - 2;

        // Recessed dark-metal frame with bevel.
        guiGraphics.fill(barLeft - 1, barTop - 1, barRight + 1, barBottom + 1, 0x68000000);
        guiGraphics.fill(barLeft, barTop, barRight, barBottom, 0xFF2F3238);
        guiGraphics.fill(barLeft + 1, barTop + 1, barRight - 1, barBottom - 1, 0xFF171A1F);
        guiGraphics.fill(barLeft, barTop, barRight, barTop + 1, 0xFF5A5F69);
        guiGraphics.fill(barLeft, barTop, barLeft + 1, barBottom, 0xFF4A4F58);
        guiGraphics.fill(barLeft, barBottom - 1, barRight, barBottom, 0xFF0C0D11);
        guiGraphics.fill(barRight - 1, barTop, barRight, barBottom, 0xFF0A0B0F);

        guiGraphics.fillGradient(innerLeft, innerTop, innerRight, innerBottom, 0xFF262A30, 0xFF13161B);

        int fillWidth = Mth.clamp(Math.round((innerRight - innerLeft) * this.menu.getAffectionProgressPercent()), 0, innerRight - innerLeft);
        if (fillWidth > 0) {
            int energyRight = innerLeft + fillWidth;
            long ticks = this.minecraft != null && this.minecraft.level != null
                    ? this.minecraft.level.getGameTime()
                    : System.currentTimeMillis() / 50L;
            float phase = (ticks + partialTick) * 0.35F;

            // Core energy fill.
            guiGraphics.fillGradient(
                    innerLeft,
                    innerTop,
                    energyRight,
                    innerBottom,
                    affectionTheme.barTopColor(),
                    affectionTheme.barBottomColor()
            );

            // Moving energy lines.
            for (int x = innerLeft; x < energyRight; x++) {
                int wave = (int) ((x + phase) % 10.0F);
                if (wave < 2) {
                    guiGraphics.fill(x, innerTop, x + 1, innerBottom, 0x35FFFFFF);
                }
            }

            // Tiny particle sparkles.
            int sparkleCount = Math.max(1, fillWidth / 16);
            for (int i = 0; i < sparkleCount; i++) {
                int sx = innerLeft + (int) (((phase * 3.0F) + i * 11.0F) % Math.max(1, fillWidth));
                int sy = innerTop + (i % Math.max(1, innerBottom - innerTop));
                guiGraphics.fill(sx, sy, sx + 1, sy + 1, 0x88EFFFF0);
            }

            // Glowing indicator at energy head.
            int markerX = Math.max(innerLeft, energyRight - 1);
            guiGraphics.fill(markerX - 1, innerTop - 1, markerX + 2, innerBottom + 1, 0x48B8FFD1);
            guiGraphics.fill(markerX, innerTop, markerX + 1, innerBottom, 0xE8D6FFE4);
        }

        // Glass-like cover plate.
        guiGraphics.fillGradient(innerLeft, innerTop, innerRight, innerBottom, 0x2AFFFFFF, 0x08FFFFFF);
        guiGraphics.fill(innerLeft, innerTop, innerRight, innerTop + 1, 0x42FFFFFF);
    }

    private AffectionTheme resolveAffectionTheme(int level) {
        if (level <= 5) {
            return new AffectionTheme(0xFF2F6B2F, 0xFF71D96E, 0xFF2E9D45);
        }
        if (level <= 7) {
            return new AffectionTheme(0xFF9B1E1E, 0xFFE06A6A, 0xFFB02A2A);
        }
        return new AffectionTheme(0xFFC4368A, 0xFFFF85CD, 0xFFD6499E);
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

    private void drawMechanicalHeartBadge(GuiGraphics guiGraphics, int x, int y, boolean large) {
        int w = large ? 11 : 9;
        int h = large ? 10 : 8;
        int glow = large ? 0x3898FFE2 : 0x2E98FFE2;
        int plate = 0xFF3C424B;
        int edgeLight = 0xFFB9C0CA;
        int edgeDark = 0xFF1D2025;
        int coreA = 0xFFFF76C5;
        int coreB = 0xFFFF4FB2;

        guiGraphics.fill(x - 1, y - 1, x + w + 1, y + h + 1, glow);
        guiGraphics.fill(x, y, x + w, y + h, plate);
        guiGraphics.fill(x, y, x + w, y + 1, edgeLight);
        guiGraphics.fill(x, y, x + 1, y + h, edgeLight);
        guiGraphics.fill(x, y + h - 1, x + w, y + h, edgeDark);
        guiGraphics.fill(x + w - 1, y, x + w, y + h, edgeDark);

        int cx = x + w / 2;
        int cy = y + h / 2;
        guiGraphics.fill(cx - 2, cy - 1, cx + 2, cy + 2, coreA);
        guiGraphics.fill(cx - 3, cy - 2, cx - 1, cy, coreA);
        guiGraphics.fill(cx + 1, cy - 2, cx + 3, cy, coreA);
        guiGraphics.fill(cx - 1, cy + 2, cx + 1, cy + 3, coreB);
        guiGraphics.fill(cx - 2, cy + 1, cx + 2, cy + 2, coreB);
        guiGraphics.fill(cx - 1, cy - 1, cx + 1, cy, 0x66FFFFFF);
    }

    private record AffectionTheme(int levelColor, int barTopColor, int barBottomColor) {
    }
}
