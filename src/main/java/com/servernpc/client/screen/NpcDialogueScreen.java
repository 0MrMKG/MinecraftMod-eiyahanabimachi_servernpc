package com.servernpc.client.screen;

import com.mojang.blaze3d.platform.NativeImage;
import com.servernpc.client.dialogue.DialogueScript;
import com.servernpc.client.dialogue.DialogueScriptLoader;
import com.servernpc.eiyahanabimachiservernpc;
import com.servernpc.entity.ReimuGoodNpcEntity;
import com.servernpc.network.payload.StopNpcDialogueFocusPayload;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;
import net.neoforged.neoforge.network.PacketDistributor;

public class NpcDialogueScreen extends Screen {
    private static final int MAX_VISIBLE_OPTIONS = 3;
    private static final int OPTION_GAP = 4;
    private static final int OPTION_TEXT_H_PADDING = 8;
    private static final int OPTION_TEXT_V_PADDING = 4;
    private static final int OPTION_MIN_HEIGHT = 20;
    private static final int MAIN_PANEL_SIMPLE_BG = 0x1A000000; // 10% alpha
    private static final int MAIN_PANEL_OUTER_BORDER = 0x80D9D9D9; // 50% alpha
    private static final int MAIN_PANEL_OUTER_RADIUS = 6;
    private static final int OPTION_BORDER_NORMAL = 0x1AFFFFFF; // 10% alpha
    private static final int OPTION_BORDER_SELECTED = 0x80FFFFFF; // 50% alpha
    private static final int BOX_SHADOW_COLOR = 0x2E080808;
    private static final int BOX_GLASS_TOP = 0x84E3E3E3;
    private static final int BOX_GLASS_BOTTOM = 0x739D9D9D;
    private static final int BOX_GLASS_TOP_SELECTED = 0x96F0F0F0;
    private static final int BOX_GLASS_BOTTOM_SELECTED = 0x868D8D8D;
    private static final int BOX_OUTLINE_COLOR = 0xD0D8D8D8;
    private static final int BOX_TOP_HIGHLIGHT = 0xB4FFFFFF;
    private static final int BOX_INNER_SHADOW = 0x330B0B0B;
    private static final int BOX_BEVEL_HIGHLIGHT = 0x66FFFFFF;
    private static final int BOX_BEVEL_DARK = 0x3A303030;
    private static final int BOX_FROST_LINE_A = 0x0CCFCFCF;
    private static final int BOX_FROST_LINE_B = 0x09FFFFFF;
    private static final int OPTION_SELECTED_GLOW_A = 0x2FFFFFFF;
    private static final int OPTION_SELECTED_GLOW_B = 0x18000000;
    private static final int OPTION_SELECTED_EDGE = 0xBCE7E7E7;
    private static final int PANEL_GLASS_TOP = 0x88D4D4D4;
    private static final int PANEL_GLASS_BOTTOM = 0x767F7F7F;
    private static final int PANEL_OUTLINE = 0x92C9C9C9;
    private static final int PANEL_INNER_SHADOW = 0x36090909;
    private static final int PANEL_INNER_HIGHLIGHT = 0x26FFFFFF;
    private static final int PANEL_BEVEL_HIGHLIGHT = 0x4EFFFFFF;
    private static final int PANEL_BEVEL_DARK = 0x34303030;
    private static final int PANEL_FROST_LINE_A = 0x0CC7C7C7;
    private static final int PANEL_FROST_LINE_B = 0x07FFFFFF;
    private static final int BRACKET_COLOR_LIGHT = 0xF2F2F2F2;
    private static final int BRACKET_COLOR_DARK = 0xBDAEAEAE;
    private static final int BRACKET_SHADOW = 0x66101010;
    private static final int BRACKET_ETCH = 0xA36A6A6A;
    private static final int BRACKET_SPECULAR = 0xD3FFFFFF;
    private static final int BRACKET_LENGTH = 20;
    private static final int BRACKET_THICKNESS = 2;
    private static final int NAMEPLATE_RADIUS = 4;
    private static final int NAME_TEXT_COLOR = 0xFFFFFFFF;
    private static final int NAMEPLATE_BORDER_NORMAL = 0x1AFFFFFF; // 10% alpha
    private static final int TEXT_SHADOW_COLOR = 0x8A101010;
    private static final int TYPEWRITER_TICKS_PER_CHAR = 1;

    private final ReimuGoodNpcEntity npc;
    private final DialogueScript script;
    private final String npcEntityId;

    @Nullable
    private DialogueScript.DialogueNode currentNode;
    private int visibleCharacterCount;
    private int typewriterTickCounter;
    private int selectedOptionIndex;
    private int optionWindowStart;

    @Nullable
    private ResourceLocation portraitTextureLocation;
    @Nullable
    private DynamicTexture portraitTexture;
    private int portraitTextureWidth = 0;
    private int portraitTextureHeight = 0;
    private int portraitCropU = 0;
    private int portraitCropV = 0;
    private int portraitCropWidth = 0;
    private int portraitCropHeight = 0;
    private boolean previousHideGui;
    private boolean hideGuiStateCaptured;

    public static void open(ReimuGoodNpcEntity npc) {
        DialogueScriptLoader.LoadedDialogue loadedDialogue = DialogueScriptLoader.loadForNpc(npc);
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.setScreen(new NpcDialogueScreen(npc, loadedDialogue));
    }

    private NpcDialogueScreen(ReimuGoodNpcEntity npc, DialogueScriptLoader.LoadedDialogue loadedDialogue) {
        super(Component.literal("npc_dialogue"));
        this.npc = npc;
        this.script = loadedDialogue.script();
        this.npcEntityId = loadedDialogue.scriptPath().getFileName().toString().replace(".txt", "");
        this.enterNode(this.script.startNodeId());
    }

    @Override
    protected void init() {
        this.captureAndHideVanillaHud();
        this.loadPortraitTexture();
    }

    @Override
    public void tick() {
        if (this.minecraft == null || this.minecraft.player == null || !this.npc.isAlive()) {
            this.onClose();
            return;
        }

        if (!this.isCurrentTextFullyVisible()) {
            this.typewriterTickCounter++;
            if (this.typewriterTickCounter >= TYPEWRITER_TICKS_PER_CHAR) {
                this.visibleCharacterCount++;
                this.typewriterTickCounter = 0;
            }
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        Rect dialogueRect = this.getDialogueRect();
        Rect portraitRect = this.getPortraitRect(dialogueRect);
        Rect optionsRect = this.getOptionsRect(dialogueRect);
        Rect textRect = this.getTextRect(dialogueRect, portraitRect, optionsRect);
        String displayName = this.script.npcName().isBlank() ? this.npc.getDisplayName().getString() : this.script.npcName();
        Rect nameplateRect = this.getNameplateRect(dialogueRect, optionsRect, displayName);

        this.drawMainPanel(guiGraphics, dialogueRect);
        this.drawNameplate(guiGraphics, nameplateRect, displayName);
        this.drawPortrait(guiGraphics, portraitRect);
        this.drawDialogueText(guiGraphics, textRect);

        if (this.isCurrentTextFullyVisible() && this.hasOptions()) {
            this.drawOptions(guiGraphics, optionsRect);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.onClose();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            return this.handleConfirm();
        }

        if (this.isCurrentTextFullyVisible() && this.hasOptions()) {
            if (keyCode == GLFW.GLFW_KEY_UP || keyCode == GLFW.GLFW_KEY_W) {
                this.moveSelection(-1);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_DOWN || keyCode == GLFW.GLFW_KEY_S) {
                this.moveSelection(1);
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!this.isCurrentTextFullyVisible() || !this.hasOptions()) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        if (scrollY > 0) {
            this.moveSelection(-1);
            return true;
        }
        if (scrollY < 0) {
            this.moveSelection(1);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            this.onClose();
            return true;
        }
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        if (!this.isCurrentTextFullyVisible()) {
            this.revealWholeText();
            return true;
        }

        if (this.hasOptions()) {
            int optionIndex = this.findOptionIndexAt(mouseX, mouseY);
            if (optionIndex >= 0) {
                this.selectedOptionIndex = optionIndex;
                this.ensureSelectionInView();
                this.applySelectedOption();
                return true;
            }
        }

        return this.handleConfirm();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void removed() {
        this.restoreVanillaHudVisibility();
        this.releasePortraitTexture();
        super.removed();
    }

    @Override
    public void onClose() {
        this.restoreVanillaHudVisibility();
        PacketDistributor.sendToServer(new StopNpcDialogueFocusPayload(this.npc.getId()));
        super.onClose();
    }

    private void captureAndHideVanillaHud() {
        if (this.minecraft == null || this.hideGuiStateCaptured) {
            return;
        }
        this.previousHideGui = this.minecraft.options.hideGui;
        this.minecraft.options.hideGui = true;
        this.hideGuiStateCaptured = true;
    }

    private void restoreVanillaHudVisibility() {
        if (this.minecraft == null || !this.hideGuiStateCaptured) {
            return;
        }
        this.minecraft.options.hideGui = this.previousHideGui;
        this.hideGuiStateCaptured = false;
    }

    private boolean handleConfirm() {
        if (this.currentNode == null) {
            this.onClose();
            return true;
        }
        if (!this.isCurrentTextFullyVisible()) {
            this.revealWholeText();
            return true;
        }
        if (this.hasOptions()) {
            this.applySelectedOption();
            return true;
        }
        this.advanceToNextOrClose();
        return true;
    }

    private void applySelectedOption() {
        if (this.currentNode == null || this.currentNode.options().isEmpty()) {
            this.advanceToNextOrClose();
            return;
        }
        int index = Mth.clamp(this.selectedOptionIndex, 0, this.currentNode.options().size() - 1);
        DialogueScript.DialogueOption option = this.currentNode.options().get(index);
        this.enterNode(option.nextNodeId());
    }

    private void advanceToNextOrClose() {
        if (this.currentNode == null || this.currentNode.nextNodeId() == null || this.currentNode.nextNodeId().isBlank()) {
            this.onClose();
            return;
        }
        this.enterNode(this.currentNode.nextNodeId());
    }

    private void enterNode(String nodeId) {
        if (nodeId == null || nodeId.isBlank() || DialogueScript.END_NODE.equalsIgnoreCase(nodeId)) {
            this.onClose();
            return;
        }

        DialogueScript.DialogueNode nextNode = this.script.node(nodeId);
        if (nextNode == null) {
            this.onClose();
            return;
        }

        this.currentNode = nextNode;
        this.visibleCharacterCount = 0;
        this.typewriterTickCounter = 0;
        this.selectedOptionIndex = 0;
        this.optionWindowStart = 0;
    }

    private void drawNameplate(GuiGraphics guiGraphics, Rect nameplateRect, String name) {
        this.drawRoundedNameplate(guiGraphics, nameplateRect);
        int textY = nameplateRect.y + (nameplateRect.height - this.font.lineHeight) / 2;
        int textX = nameplateRect.x + 12;
        guiGraphics.drawString(this.font, name, textX + 1, textY, NAME_TEXT_COLOR, false);
        guiGraphics.drawString(this.font, name, textX, textY, NAME_TEXT_COLOR, false);
    }

    private void drawPortrait(GuiGraphics guiGraphics, Rect portraitRect) {
        if (this.portraitTextureLocation != null && this.portraitTextureWidth > 0 && this.portraitTextureHeight > 0) {
            int sourceWidth = this.portraitCropWidth > 0 ? this.portraitCropWidth : this.portraitTextureWidth;
            int sourceHeight = this.portraitCropHeight > 0 ? this.portraitCropHeight : this.portraitTextureHeight;
            int sourceU = this.portraitCropU;
            int sourceV = this.portraitCropV;

            float scale = Math.min(
                    portraitRect.width / (float) sourceWidth,
                    portraitRect.height / (float) sourceHeight
            );
            int drawWidth = Math.max(1, Math.round(sourceWidth * scale));
            int drawHeight = Math.max(1, Math.round(sourceHeight * scale));
            int drawX = portraitRect.x + (portraitRect.width - drawWidth) / 2;
            int drawY = portraitRect.y + (portraitRect.height - drawHeight) / 2;

            guiGraphics.blit(
                    this.portraitTextureLocation,
                    drawX,
                    drawY,
                    drawWidth,
                    drawHeight,
                    sourceU,
                    sourceV,
                    sourceWidth,
                    sourceHeight,
                    this.portraitTextureWidth,
                    this.portraitTextureHeight
            );
            return;
        }
        guiGraphics.fill(portraitRect.x, portraitRect.y, portraitRect.x + portraitRect.width, portraitRect.y + portraitRect.height, 0x55444444);
        String hint = "Portrait Missing";
        int textX = portraitRect.x + (portraitRect.width - this.font.width(hint)) / 2;
        int textY = portraitRect.y + (portraitRect.height - this.font.lineHeight) / 2;
        this.drawReadableString(guiGraphics, hint, Math.max(portraitRect.x + 2, textX), textY, 0xFFDADADA);
    }

    private void drawDialogueText(GuiGraphics guiGraphics, Rect textRect) {
        if (this.currentNode == null) {
            return;
        }

        String fullText = this.currentNode.text();
        int visibleCount = Math.min(this.visibleCharacterCount, fullText.length());
        String visibleText = fullText.substring(0, visibleCount);

        int contentW = Math.max(1, textRect.width);
        int contentH = Math.max(1, textRect.height);
        int lineHeight = this.font.lineHeight + 2;
        int maxLines = Math.max(1, contentH / lineHeight);

        List<FormattedCharSequence> lines = this.font.split(Component.literal(visibleText), contentW);
        for (int i = 0; i < lines.size() && i < maxLines; i++) {
            this.drawReadableString(guiGraphics, lines.get(i), textRect.x, textRect.y + i * lineHeight, 0xFFF2F2F2);
        }

    }

    private void drawOptions(GuiGraphics guiGraphics, Rect optionsRect) {
        if (this.currentNode == null) {
            return;
        }
        List<DialogueScript.DialogueOption> options = this.currentNode.options();
        if (options.isEmpty()) {
            return;
        }
        this.ensureSelectionInView();

        List<OptionCell> cells = this.layoutVisibleOptionCells(optionsRect, options);
        for (OptionCell optionCell : cells) {
            int optionIndex = optionCell.optionIndex();
            Rect cell = optionCell.rect();
            boolean selected = optionIndex == this.selectedOptionIndex;
            this.drawFrostedGlassBox(guiGraphics, cell, selected);

            int lineStep = this.font.lineHeight + 1;
            int maxLines = Math.max(1, (cell.height - OPTION_TEXT_V_PADDING * 2) / lineStep);
            int renderLines = Math.min(maxLines, optionCell.lines().size());
            int textBlockHeight = renderLines * lineStep - 1;
            int textStartY = cell.y + (cell.height - textBlockHeight) / 2;
            int textX = cell.x + OPTION_TEXT_H_PADDING;

            for (int i = 0; i < renderLines; i++) {
                this.drawReadableString(
                        guiGraphics,
                        optionCell.lines().get(i),
                        textX,
                        textStartY + i * lineStep,
                        selected ? 0xFFFFFFFF : 0xFFE8E8E8
                );
            }
        }
    }

    private int findOptionIndexAt(double mouseX, double mouseY) {
        if (this.currentNode == null || this.currentNode.options().isEmpty()) {
            return -1;
        }
        Rect optionsRect = this.getOptionsRect(this.getDialogueRect());
        List<DialogueScript.DialogueOption> options = this.currentNode.options();
        List<OptionCell> cells = this.layoutVisibleOptionCells(optionsRect, options);

        for (OptionCell optionCell : cells) {
            if (optionCell.rect().contains(mouseX, mouseY)) {
                return optionCell.optionIndex();
            }
        }
        return -1;
    }

    private void moveSelection(int delta) {
        if (this.currentNode == null || this.currentNode.options().isEmpty()) {
            return;
        }
        int optionCount = this.currentNode.options().size();
        this.selectedOptionIndex = Math.floorMod(this.selectedOptionIndex + delta, optionCount);
        this.ensureSelectionInView();
    }

    private void ensureSelectionInView() {
        if (this.currentNode == null || this.currentNode.options().isEmpty()) {
            this.optionWindowStart = 0;
            return;
        }
        int optionCount = this.currentNode.options().size();
        int maxWindowStart = Math.max(0, optionCount - MAX_VISIBLE_OPTIONS);
        if (this.selectedOptionIndex < this.optionWindowStart) {
            this.optionWindowStart = this.selectedOptionIndex;
        } else if (this.selectedOptionIndex >= this.optionWindowStart + MAX_VISIBLE_OPTIONS) {
            this.optionWindowStart = this.selectedOptionIndex - MAX_VISIBLE_OPTIONS + 1;
        }
        this.optionWindowStart = Mth.clamp(this.optionWindowStart, 0, maxWindowStart);
    }

    private void revealWholeText() {
        if (this.currentNode == null) {
            return;
        }
        this.visibleCharacterCount = this.currentNode.text().length();
        this.typewriterTickCounter = 0;
    }

    private boolean isCurrentTextFullyVisible() {
        if (this.currentNode == null) {
            return true;
        }
        return this.visibleCharacterCount >= this.currentNode.text().length();
    }

    private boolean hasOptions() {
        return this.currentNode != null && !this.currentNode.options().isEmpty();
    }

    private void drawFrostedGlassBox(GuiGraphics guiGraphics, Rect rect, boolean selected) {
        int radius = Math.min(4, Math.min(rect.width / 2, rect.height / 2));
        int x1 = rect.x;
        int y1 = rect.y;
        int x2 = rect.x + rect.width;
        int y2 = rect.y + rect.height;
        int borderColor = selected ? OPTION_BORDER_SELECTED : OPTION_BORDER_NORMAL;
        this.drawRoundedOutline(guiGraphics, x1, y1, x2, y2, radius, borderColor);
    }

    private void drawMainPanel(GuiGraphics guiGraphics, Rect rect) {
        // Simple 10% transparent background panel.
        guiGraphics.fill(rect.x, rect.y, rect.x + rect.width, rect.y + rect.height, MAIN_PANEL_SIMPLE_BG);
        this.drawRoundedOutline(
                guiGraphics,
                rect.x,
                rect.y,
                rect.x + rect.width,
                rect.y + rect.height,
                MAIN_PANEL_OUTER_RADIUS,
                MAIN_PANEL_OUTER_BORDER
        );
    }

    private void drawFrostedGlassLayer(GuiGraphics guiGraphics, Rect rect, int topColor, int bottomColor, int lineColorA, int lineColorB) {
        int x1 = rect.x;
        int y1 = rect.y;
        int x2 = rect.x + rect.width;
        int y2 = rect.y + rect.height;
        guiGraphics.fillGradient(x1, y1, x2, y2, topColor, bottomColor);
        guiGraphics.fill(x1 + 1, y1 + 1, x2 - 1, y2 - 1, 0x10FFFFFF);
    }

    private void drawBevelHighlights(GuiGraphics guiGraphics, Rect rect, int highlight, int dark) {
        int x1 = rect.x;
        int y1 = rect.y;
        int x2 = rect.x + rect.width;
        int y2 = rect.y + rect.height;
        guiGraphics.fill(x1 + 1, y1 + 1, x2 - 1, y1 + 2, highlight);
        guiGraphics.fill(x1 + 1, y1 + 1, x1 + 2, y2 - 1, highlight);
        guiGraphics.fill(x1 + 1, y2 - 2, x2 - 1, y2 - 1, dark);
        guiGraphics.fill(x2 - 2, y1 + 1, x2 - 1, y2 - 1, dark);
        guiGraphics.fill(x1 + 2, y1 + 2, x2 - 2, y1 + 3, this.scaleAlpha(highlight, 0.55F));
        guiGraphics.fill(x1 + 2, y2 - 3, x2 - 2, y2 - 2, this.scaleAlpha(dark, 0.7F));
    }

    private void drawInnerDepthShadow(GuiGraphics guiGraphics, Rect rect, int baseColor, int thickness) {
        int x1 = rect.x;
        int y1 = rect.y;
        int x2 = rect.x + rect.width;
        int y2 = rect.y + rect.height;
        for (int i = 1; i <= thickness; i++) {
            float fade = 1.0F - (i - 1) / (float) (thickness + 1);
            int bottomRight = this.scaleAlpha(baseColor, fade);
            int topLeft = this.scaleAlpha(PANEL_INNER_HIGHLIGHT, fade * 0.7F);
            guiGraphics.fill(x1 + i, y2 - i, x2 - i, y2 - i + 1, bottomRight);
            guiGraphics.fill(x2 - i, y1 + i, x2 - i + 1, y2 - i, bottomRight);
            guiGraphics.fill(x1 + i, y1 + i, x2 - i, y1 + i + 1, topLeft);
            guiGraphics.fill(x1 + i, y1 + i, x1 + i + 1, y2 - i, topLeft);
        }
    }

    private void drawThinOutline(GuiGraphics guiGraphics, Rect rect, int outlineColor, int topHighlight) {
        int x1 = rect.x;
        int y1 = rect.y;
        int x2 = rect.x + rect.width;
        int y2 = rect.y + rect.height;
        guiGraphics.fill(x1, y1, x2, y1 + 1, outlineColor);
        guiGraphics.fill(x1, y2 - 1, x2, y2, this.scaleAlpha(outlineColor, 0.9F));
        guiGraphics.fill(x1, y1, x1 + 1, y2, outlineColor);
        guiGraphics.fill(x2 - 1, y1, x2, y2, this.scaleAlpha(outlineColor, 0.85F));
        guiGraphics.fill(x1 + 1, y1, x2 - 1, y1 + 1, topHighlight);
    }

    private void drawSelectedCrystalGlow(GuiGraphics guiGraphics, Rect rect) {
        int x1 = rect.x + 3;
        int y1 = rect.y + 3;
        int x2 = rect.x + rect.width - 3;
        int y2 = rect.y + rect.height - 3;
        if (x2 <= x1 || y2 <= y1) {
            return;
        }
        guiGraphics.fillGradient(x1, y1, x2, y2, OPTION_SELECTED_GLOW_A, OPTION_SELECTED_GLOW_B);
        guiGraphics.fill(x1 + 1, y1 + 1, x2 - 1, y1 + 2, OPTION_SELECTED_EDGE);
        guiGraphics.fill(x1 + 1, y2 - 2, x2 - 1, y2 - 1, this.scaleAlpha(OPTION_SELECTED_EDGE, 0.7F));
        guiGraphics.fill(x1 + 1, y1 + 1, x1 + 2, y2 - 1, this.scaleAlpha(OPTION_SELECTED_EDGE, 0.85F));
        guiGraphics.fill(x2 - 2, y1 + 1, x2 - 1, y2 - 1, this.scaleAlpha(OPTION_SELECTED_EDGE, 0.62F));
        guiGraphics.fill(rect.x - 1, rect.y + 2, rect.x + rect.width + 1, rect.y + rect.height - 2, 0x12000000);
    }

    private void drawPanelBrackets(GuiGraphics guiGraphics, Rect rect) {
        int x1 = rect.x;
        int y1 = rect.y;
        int x2 = rect.x + rect.width;
        int y2 = rect.y + rect.height;
        int t = BRACKET_THICKNESS;
        int l = BRACKET_LENGTH;

        this.drawCornerBracket(guiGraphics, x1, y1, l, t, true, true);
        this.drawCornerBracket(guiGraphics, x2, y1, l, t, false, true);
        this.drawCornerBracket(guiGraphics, x1, y2, l, t, true, false);
        this.drawCornerBracket(guiGraphics, x2, y2, l, t, false, false);
    }

    private void drawCornerBracket(GuiGraphics guiGraphics, int cornerX, int cornerY, int length, int thickness, boolean toRight, boolean toBottom) {
        int hx1 = toRight ? cornerX : cornerX - length;
        int hx2 = toRight ? cornerX + length : cornerX;
        int hy1 = toBottom ? cornerY : cornerY - thickness;
        int hy2 = toBottom ? cornerY + thickness : cornerY;

        int vx1 = toRight ? cornerX : cornerX - thickness;
        int vx2 = toRight ? cornerX + thickness : cornerX;
        int vy1 = toBottom ? cornerY : cornerY - length;
        int vy2 = toBottom ? cornerY + length : cornerY;

        guiGraphics.fill(hx1 - 2, hy1 - 2, hx2 + 2, hy2 + 2, BRACKET_SHADOW);
        guiGraphics.fill(vx1 - 2, vy1 - 2, vx2 + 2, vy2 + 2, BRACKET_SHADOW);

        guiGraphics.fillGradient(hx1, hy1, hx2, hy2, BRACKET_COLOR_LIGHT, BRACKET_COLOR_DARK);
        guiGraphics.fillGradient(vx1, vy1, vx2, vy2, BRACKET_COLOR_LIGHT, BRACKET_COLOR_DARK);

        guiGraphics.fill(hx1, hy1, hx2, hy1 + 1, BRACKET_SPECULAR);
        guiGraphics.fill(vx1, vy1, vx1 + 1, vy2, BRACKET_SPECULAR);

        int etchPadding = 5;
        for (int i = etchPadding; i < length - etchPadding; i += 6) {
            int etchX = toRight ? cornerX + i : cornerX - i - 1;
            int etchY1 = toBottom ? cornerY + 1 : cornerY - thickness + 1;
            int etchY2 = toBottom ? cornerY + thickness - 1 : cornerY - 1;
            guiGraphics.fill(etchX, etchY1, etchX + 1, etchY2, BRACKET_ETCH);
        }
        for (int i = etchPadding; i < length - etchPadding; i += 6) {
            int etchX1 = toRight ? cornerX + 1 : cornerX - thickness + 1;
            int etchX2 = toRight ? cornerX + thickness - 1 : cornerX - 1;
            int etchY = toBottom ? cornerY + i : cornerY - i - 1;
            guiGraphics.fill(etchX1, etchY, etchX2, etchY + 1, BRACKET_ETCH);
        }
    }

    private int scaleAlpha(int color, float alphaFactor) {
        int alpha = (color >>> 24) & 0xFF;
        int rgb = color & 0x00FFFFFF;
        int scaledAlpha = Mth.clamp((int) (alpha * alphaFactor), 0, 255);
        return (scaledAlpha << 24) | rgb;
    }

    private void drawReadableString(GuiGraphics guiGraphics, String text, int x, int y, int color) {
        guiGraphics.drawString(this.font, text, x + 1, y + 1, TEXT_SHADOW_COLOR, false);
        guiGraphics.drawString(this.font, text, x, y, color, false);
    }

    private void drawReadableString(GuiGraphics guiGraphics, FormattedCharSequence text, int x, int y, int color) {
        guiGraphics.drawString(this.font, text, x + 1, y + 1, TEXT_SHADOW_COLOR, false);
        guiGraphics.drawString(this.font, text, x, y, color, false);
    }

    private List<OptionCell> layoutVisibleOptionCells(Rect optionsRect, List<DialogueScript.DialogueOption> options) {
        List<OptionCell> cells = new ArrayList<>();
        if (options.isEmpty()) {
            return cells;
        }

        int contentWidth = Math.max(32, optionsRect.width - OPTION_TEXT_H_PADDING * 2);
        int y = optionsRect.y;
        int bottom = optionsRect.y + optionsRect.height;

        for (int i = 0; i < MAX_VISIBLE_OPTIONS; i++) {
            int optionIndex = this.optionWindowStart + i;
            if (optionIndex >= options.size()) {
                break;
            }

            List<FormattedCharSequence> lines = this.font.split(Component.literal(options.get(optionIndex).text()), contentWidth);
            int lineCount = Math.max(1, lines.size());
            int desiredHeight = lineCount * (this.font.lineHeight + 1) + OPTION_TEXT_V_PADDING * 2;
            int optionHeight = Math.max(OPTION_MIN_HEIGHT, desiredHeight);

            if (y + optionHeight > bottom) {
                int remaining = bottom - y;
                if (remaining < OPTION_MIN_HEIGHT) {
                    break;
                }
                optionHeight = remaining;
            }

            Rect cellRect = new Rect(optionsRect.x, y, optionsRect.width, optionHeight);
            cells.add(new OptionCell(optionIndex, cellRect, lines));

            y += optionHeight + OPTION_GAP;
            if (y >= bottom) {
                break;
            }
        }
        return cells;
    }

    private Rect getDialogueRect() {
        int width = (int) (this.width * 0.88F);
        int height = Math.max(102, (int) (this.height * 0.20F));
        int x = (this.width - width) / 2;
        int y = this.height - height;
        return new Rect(x, y, width, height);
    }

    private Rect getPortraitRect(Rect dialogueRect) {
        int size = Math.max(58, (int) (dialogueRect.height * 0.56F));
        int x = dialogueRect.x + 12;
        int y = dialogueRect.y + dialogueRect.height - size - 10;
        return new Rect(x, y, size, size);
    }

    private Rect getOptionsRect(Rect dialogueRect) {
        int width = Math.max(132, (int) (dialogueRect.width * 0.23F));
        int x = dialogueRect.x + dialogueRect.width - width - 12;
        int y = dialogueRect.y + 16;
        int height = Mth.clamp(dialogueRect.height - 26, 72, 96);
        return new Rect(x, y, width, height);
    }

    private Rect getTextRect(Rect dialogueRect, Rect portraitRect, Rect optionsRect) {
        int x = portraitRect.x + portraitRect.width + 12;
        int y = dialogueRect.y + 34;
        int width = optionsRect.x - x - 10;
        int height = dialogueRect.height - 44;
        return new Rect(x, y, Math.max(82, width), Math.max(34, height));
    }

    private Rect getNameplateRect(Rect dialogueRect, Rect optionsRect, String displayName) {
        int x = dialogueRect.x + 14;
        int y = dialogueRect.y + 8;
        int height = this.font.lineHeight + 10;
        int textPadding = 12;
        int desiredWidth = this.font.width(displayName) + textPadding * 2;
        int maxWidth = Math.max(90, optionsRect.x - x - 18);
        int width = Mth.clamp(desiredWidth, 90, maxWidth);
        return new Rect(x, y, width, height);
    }

    private void drawRoundedNameplate(GuiGraphics guiGraphics, Rect rect) {
        int r = Math.min(NAMEPLATE_RADIUS, Math.min(rect.width / 2, rect.height / 2));
        int x1 = rect.x;
        int y1 = rect.y;
        int x2 = rect.x + rect.width;
        int y2 = rect.y + rect.height;

        this.drawRoundedOutline(guiGraphics, x1, y1, x2, y2, r, NAMEPLATE_BORDER_NORMAL);
    }

    private void fillRoundedRect(GuiGraphics guiGraphics, int x1, int y1, int x2, int y2, int radius, int color) {
        guiGraphics.fill(x1 + radius, y1, x2 - radius, y2, color);
        guiGraphics.fill(x1, y1 + radius, x2, y2 - radius, color);
        // 4 corners (pixel rounded)
        guiGraphics.fill(x1 + 1, y1 + 1, x1 + radius, y1 + radius, color);
        guiGraphics.fill(x2 - radius, y1 + 1, x2 - 1, y1 + radius, color);
        guiGraphics.fill(x1 + 1, y2 - radius, x1 + radius, y2 - 1, color);
        guiGraphics.fill(x2 - radius, y2 - radius, x2 - 1, y2 - 1, color);
    }

    private void fillRoundedGradient(GuiGraphics guiGraphics, int x1, int y1, int x2, int y2, int radius, int topColor, int bottomColor) {
        guiGraphics.fillGradient(x1 + radius, y1, x2 - radius, y2, topColor, bottomColor);
        guiGraphics.fillGradient(x1, y1 + radius, x2, y2 - radius, topColor, bottomColor);
        guiGraphics.fillGradient(x1 + 1, y1 + 1, x1 + radius, y1 + radius, topColor, bottomColor);
        guiGraphics.fillGradient(x2 - radius, y1 + 1, x2 - 1, y1 + radius, topColor, bottomColor);
        guiGraphics.fillGradient(x1 + 1, y2 - radius, x1 + radius, y2 - 1, topColor, bottomColor);
        guiGraphics.fillGradient(x2 - radius, y2 - radius, x2 - 1, y2 - 1, topColor, bottomColor);
    }

    private void drawRoundedOutline(GuiGraphics guiGraphics, int x1, int y1, int x2, int y2, int radius, int color) {
        guiGraphics.fill(x1 + radius, y1, x2 - radius, y1 + 1, color);
        guiGraphics.fill(x1 + radius, y2 - 1, x2 - radius, y2, color);
        guiGraphics.fill(x1, y1 + radius, x1 + 1, y2 - radius, color);
        guiGraphics.fill(x2 - 1, y1 + radius, x2, y2 - radius, color);

        guiGraphics.fill(x1 + 1, y1 + 1, x1 + radius, y1 + 2, color);
        guiGraphics.fill(x1 + 1, y1 + 1, x1 + 2, y1 + radius, color);

        guiGraphics.fill(x2 - radius, y1 + 1, x2 - 1, y1 + 2, color);
        guiGraphics.fill(x2 - 2, y1 + 1, x2 - 1, y1 + radius, color);

        guiGraphics.fill(x1 + 1, y2 - 2, x1 + radius, y2 - 1, color);
        guiGraphics.fill(x1 + 1, y2 - radius, x1 + 2, y2 - 1, color);

        guiGraphics.fill(x2 - radius, y2 - 2, x2 - 1, y2 - 1, color);
        guiGraphics.fill(x2 - 2, y2 - radius, x2 - 1, y2 - 1, color);
    }

    private void loadPortraitTexture() {
        this.releasePortraitTexture();
        @Nullable ResourceLocation portraitResource = this.resolvePortraitResourceLocation();
        if (portraitResource == null) {
            return;
        }
        var optionalResource = Minecraft.getInstance().getResourceManager().getResource(portraitResource);
        if (optionalResource.isEmpty()) {
            return;
        }

        try (InputStream inputStream = optionalResource.get().open()) {
            NativeImage image = NativeImage.read(inputStream);
            this.portraitTextureWidth = image.getWidth();
            this.portraitTextureHeight = image.getHeight();
            this.computePortraitCropBounds(image);
            this.portraitTexture = new DynamicTexture(image);
            this.portraitTextureLocation = ResourceLocation.fromNamespaceAndPath(
                    eiyahanabimachiservernpc.MODID,
                    "dialogue/portrait/" + this.npcEntityId + "/" + System.nanoTime()
            );
            Minecraft.getInstance().getTextureManager().register(this.portraitTextureLocation, this.portraitTexture);
        } catch (IOException ex) {
            eiyahanabimachiservernpc.LOGGER.warn("Failed to load dialogue portrait resource {}", portraitResource, ex);
        }
    }

    @Nullable
    private ResourceLocation resolvePortraitResourceLocation() {
        ResourceLocation portrait = ResourceLocation.fromNamespaceAndPath(
                eiyahanabimachiservernpc.MODID,
                "textures/dialogue/portrait/" + this.npcEntityId + ".png"
        );
        if (Minecraft.getInstance().getResourceManager().getResource(portrait).isPresent()) {
            return portrait;
        }
        return null;
    }

    private void releasePortraitTexture() {
        if (this.portraitTextureLocation != null) {
            Minecraft.getInstance().getTextureManager().release(this.portraitTextureLocation);
            this.portraitTextureLocation = null;
        }
        this.portraitTexture = null;
        this.portraitTextureWidth = 0;
        this.portraitTextureHeight = 0;
        this.portraitCropU = 0;
        this.portraitCropV = 0;
        this.portraitCropWidth = 0;
        this.portraitCropHeight = 0;
    }

    private void computePortraitCropBounds(NativeImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int minX = width;
        int minY = height;
        int maxX = -1;
        int maxY = -1;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = image.getPixelRGBA(x, y);
                int alpha = (argb >>> 24) & 0xFF;
                if (alpha > 0) {
                    if (x < minX) minX = x;
                    if (y < minY) minY = y;
                    if (x > maxX) maxX = x;
                    if (y > maxY) maxY = y;
                }
            }
        }

        if (maxX >= minX && maxY >= minY) {
            this.portraitCropU = minX;
            this.portraitCropV = minY;
            this.portraitCropWidth = maxX - minX + 1;
            this.portraitCropHeight = maxY - minY + 1;
            return;
        }
        this.portraitCropU = 0;
        this.portraitCropV = 0;
        this.portraitCropWidth = width;
        this.portraitCropHeight = height;
    }

    private record OptionCell(int optionIndex, Rect rect, List<FormattedCharSequence> lines) {
    }

    private record Rect(int x, int y, int width, int height) {
        private boolean contains(double mouseX, double mouseY) {
            return mouseX >= this.x
                    && mouseX <= this.x + this.width
                    && mouseY >= this.y
                    && mouseY <= this.y + this.height;
        }
    }
}
