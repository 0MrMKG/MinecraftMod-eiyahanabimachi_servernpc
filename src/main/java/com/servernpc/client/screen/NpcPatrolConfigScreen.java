package com.servernpc.client.screen;

import com.servernpc.eiyahanabimachiservernpc;
import com.servernpc.entity.ReimuGoodNpcEntity.ActivityType;
import com.servernpc.menu.NpcPatrolConfigMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

public class NpcPatrolConfigScreen extends AbstractContainerScreen<NpcPatrolConfigMenu> {
    private static final int PANEL_BG = 0xFFC6C6C6;
    private static final int[] ACTION_COLORS = new int[]{
            0xFF5B6284, 0xFFD88A4E, 0xFF6AA9D8, 0xFF4EA662,
            0xFFB07AE8, 0xFFE87A9E, 0xFF7AD9B8, 0xFFE8C86A
    };

    private final Button[] extraToggleButtons = new Button[NpcPatrolConfigMenu.EXTRA_SLOT_COUNT];
    private final Button[] extraTypeButtons = new Button[NpcPatrolConfigMenu.EXTRA_SLOT_COUNT];

    public NpcPatrolConfigScreen(NpcPatrolConfigMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 272;
        this.imageHeight = 254;
    }

    @Override
    protected void init() {
        super.init();
        int left = this.leftPos;
        int top = this.topPos;

        this.addRenderableWidget(this.createTypeButton(left + 10, top + 36, ActivityType.SLEEP.key(), NpcPatrolConfigMenu.BTN_TYPE_ACTION1));
        this.addRenderableWidget(this.createTypeButton(left + 76, top + 36, ActivityType.PLAY.key(), NpcPatrolConfigMenu.BTN_TYPE_ACTION2));
        this.addRenderableWidget(this.createTypeButton(left + 142, top + 36, ActivityType.WANDER.key(), NpcPatrolConfigMenu.BTN_TYPE_ACTION3));
        this.addRenderableWidget(this.createTypeButton(left + 208, top + 36, ActivityType.WORK.key(), NpcPatrolConfigMenu.BTN_TYPE_ACTION4));
        this.addRenderableWidget(this.createTypeButton(left + 10, top + 56, ActivityType.ACTION5.key(), NpcPatrolConfigMenu.BTN_TYPE_ACTION5));
        this.addRenderableWidget(this.createTypeButton(left + 76, top + 56, ActivityType.ACTION6.key(), NpcPatrolConfigMenu.BTN_TYPE_ACTION6));
        this.addRenderableWidget(this.createTypeButton(left + 142, top + 56, ActivityType.ACTION7.key(), NpcPatrolConfigMenu.BTN_TYPE_ACTION7));
        this.addRenderableWidget(this.createTypeButton(left + 208, top + 56, ActivityType.ACTION8.key(), NpcPatrolConfigMenu.BTN_TYPE_ACTION8));

        int adjustY = top + 88;
        this.addRenderableWidget(this.createAdjustButton(left + 10, adjustY, "-", NpcPatrolConfigMenu.BTN_MORNING_MINUS));
        this.addRenderableWidget(this.createAdjustButton(left + 31, adjustY, "+", NpcPatrolConfigMenu.BTN_MORNING_PLUS));
        this.addRenderableWidget(this.createAdjustButton(left + 60, adjustY, "-", NpcPatrolConfigMenu.BTN_NOON_MINUS));
        this.addRenderableWidget(this.createAdjustButton(left + 81, adjustY, "+", NpcPatrolConfigMenu.BTN_NOON_PLUS));
        this.addRenderableWidget(this.createAdjustButton(left + 110, adjustY, "-", NpcPatrolConfigMenu.BTN_AFTERNOON_MINUS));
        this.addRenderableWidget(this.createAdjustButton(left + 131, adjustY, "+", NpcPatrolConfigMenu.BTN_AFTERNOON_PLUS));
        this.addRenderableWidget(this.createAdjustButton(left + 160, adjustY, "-", NpcPatrolConfigMenu.BTN_EVENING_MINUS));
        this.addRenderableWidget(this.createAdjustButton(left + 181, adjustY, "+", NpcPatrolConfigMenu.BTN_EVENING_PLUS));
        this.addRenderableWidget(this.createAdjustButton(left + 210, adjustY, "-", NpcPatrolConfigMenu.BTN_NIGHT_MINUS));
        this.addRenderableWidget(this.createAdjustButton(left + 231, adjustY, "+", NpcPatrolConfigMenu.BTN_NIGHT_PLUS));

        this.addRenderableWidget(this.createAdjustButton(left + 10, top + 146, "-", NpcPatrolConfigMenu.BTN_RADIUS_MINUS));
        this.addRenderableWidget(this.createAdjustButton(left + 31, top + 146, "+", NpcPatrolConfigMenu.BTN_RADIUS_PLUS));

        for (int i = 0; i < NpcPatrolConfigMenu.EXTRA_SLOT_COUNT; i++) {
            final int slot = i;
            int rowY = top + 170 + i * 18;
            Button toggle = Button.builder(Component.translatable("gui." + eiyahanabimachiservernpc.MODID + ".state.off"), button ->
                    this.sendButton(NpcPatrolConfigMenu.extraButtonId(slot, NpcPatrolConfigMenu.EXTRA_OP_TOGGLE))
            ).bounds(left + 10, rowY, 28, 16).build();
            Button type = Button.builder(this.activityName(this.menu.getExtraScheduleActivityId(i)), button ->
                    this.sendButton(NpcPatrolConfigMenu.extraButtonId(slot, NpcPatrolConfigMenu.EXTRA_OP_TYPE))
            ).bounds(left + 42, rowY, 54, 16).build();
            this.extraToggleButtons[i] = toggle;
            this.extraTypeButtons[i] = type;
            this.addRenderableWidget(toggle);
            this.addRenderableWidget(type);
            this.addRenderableWidget(this.createAdjustButton(left + 100, rowY, "-", NpcPatrolConfigMenu.extraButtonId(i, NpcPatrolConfigMenu.EXTRA_OP_TIME_MINUS)));
            this.addRenderableWidget(this.createAdjustButton(left + 121, rowY, "+", NpcPatrolConfigMenu.extraButtonId(i, NpcPatrolConfigMenu.EXTRA_OP_TIME_PLUS)));
        }

        this.addRenderableWidget(Button.builder(
                Component.translatable("gui." + eiyahanabimachiservernpc.MODID + ".save"),
                button -> this.sendButton(NpcPatrolConfigMenu.BTN_SAVE)
        ).bounds(left + 10, top + 232, 82, 20).build());
        this.addRenderableWidget(Button.builder(
                Component.translatable("gui." + eiyahanabimachiservernpc.MODID + ".delete"),
                button -> this.sendButton(NpcPatrolConfigMenu.BTN_DELETE)
        ).bounds(left + 95, top + 232, 82, 20).build());
        this.addRenderableWidget(Button.builder(
                Component.translatable("gui." + eiyahanabimachiservernpc.MODID + ".cancel"),
                button -> this.sendButton(NpcPatrolConfigMenu.BTN_CANCEL)
        ).bounds(left + 180, top + 232, 82, 20).build());

        this.updateDynamicButtons();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        this.updateDynamicButtons();
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int left = this.leftPos;
        int top = this.topPos;
        guiGraphics.fill(left, top, left + this.imageWidth, top + this.imageHeight, PANEL_BG);
        this.drawPanelBorder(guiGraphics, left, top, this.imageWidth, this.imageHeight);
        this.drawTimelineBar(guiGraphics, left + 10, top + 108, 252, 14);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, 10, 6, 0x404040, false);
        guiGraphics.drawString(
                this.font,
                Component.translatable(
                        "gui." + eiyahanabimachiservernpc.MODID + ".patrol_position",
                        this.menu.getTargetPos().getX(),
                        this.menu.getTargetPos().getY(),
                        this.menu.getTargetPos().getZ()
                ),
                10,
                18,
                0x404040,
                false
        );

        guiGraphics.drawString(this.font, Component.translatable("gui." + eiyahanabimachiservernpc.MODID + ".activity_select"), 10, 28, 0x404040, false);
        guiGraphics.drawString(this.font, Component.translatable("gui." + eiyahanabimachiservernpc.MODID + ".schedule_adjust"), 10, 76, 0x404040, false);

        guiGraphics.drawString(this.font, formatTime(this.menu.getMorningWorkStart()), 10, 126, 0x404040, false);
        guiGraphics.drawString(this.font, formatTime(this.menu.getNoonWanderStart()), 60, 126, 0x404040, false);
        guiGraphics.drawString(this.font, formatTime(this.menu.getAfternoonWorkStart()), 110, 126, 0x404040, false);
        guiGraphics.drawString(this.font, formatTime(this.menu.getEveningPlayStart()), 160, 126, 0x404040, false);
        guiGraphics.drawString(this.font, formatTime(this.menu.getNightSleepStart()), 210, 126, 0x404040, false);

        guiGraphics.drawString(
                this.font,
                Component.translatable("gui." + eiyahanabimachiservernpc.MODID + ".patrol_radius", this.menu.getPatrolRadius()),
                58,
                151,
                0x404040,
                false
        );

        guiGraphics.drawString(this.font, Component.translatable("gui." + eiyahanabimachiservernpc.MODID + ".extra_schedule"), 10, 160, 0x404040, false);
        for (int i = 0; i < NpcPatrolConfigMenu.EXTRA_SLOT_COUNT; i++) {
            int rowY = 172 + i * 18;
            String timeName = formatTime(this.menu.getExtraScheduleStartTick(i));
            guiGraphics.drawString(this.font, Component.literal("\u5f00\u59cb " + timeName), 146, rowY + 2, 0x404040, false);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    private Button createTypeButton(int x, int y, String typeKey, int buttonId) {
        return Button.builder(
                Component.translatable("gui." + eiyahanabimachiservernpc.MODID + ".activity." + typeKey),
                button -> this.sendButton(buttonId)
        ).bounds(x, y, 62, 18).build();
    }

    private Button createAdjustButton(int x, int y, String text, int buttonId) {
        return Button.builder(Component.literal(text), button -> this.sendButton(buttonId))
                .bounds(x, y, 20, 16)
                .build();
    }

    private void sendButton(int buttonId) {
        if (this.minecraft == null || this.minecraft.gameMode == null) {
            return;
        }
        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, buttonId);
    }

    private void updateDynamicButtons() {
        for (int i = 0; i < NpcPatrolConfigMenu.EXTRA_SLOT_COUNT; i++) {
            if (this.extraToggleButtons[i] != null) {
                this.extraToggleButtons[i].setMessage(Component.translatable(
                        "gui." + eiyahanabimachiservernpc.MODID + (this.menu.isExtraScheduleEnabled(i) ? ".state.on" : ".state.off")
                ));
                this.extraToggleButtons[i].active = this.menu.canToggleExtraScheduleSlot(i);
            }
            if (this.extraTypeButtons[i] != null) {
                this.extraTypeButtons[i].setMessage(this.activityName(this.menu.getExtraScheduleActivityId(i)));
                this.extraTypeButtons[i].active = false;
            }
        }
    }

    private Component activityName(int typeId) {
        ActivityType type = ActivityType.byId(typeId);
        return Component.translatable("gui." + eiyahanabimachiservernpc.MODID + ".activity." + type.key());
    }

    private void drawPanelBorder(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        int x2 = x + width;
        int y2 = y + height;
        guiGraphics.fill(x, y, x2, y + 1, 0xFFE0E0E0);
        guiGraphics.fill(x, y, x + 1, y2, 0xFFE0E0E0);
        guiGraphics.fill(x2 - 1, y, x2, y2, 0xFF4C4C4C);
        guiGraphics.fill(x, y2 - 1, x2, y2, 0xFF4C4C4C);
    }

    private void drawTimelineBar(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        boolean anyExtraEnabled = this.menu.isExtraScheduleEnabled(0)
                || this.menu.isExtraScheduleEnabled(1)
                || this.menu.isExtraScheduleEnabled(2);

        if (!anyExtraEnabled) {
            int[] starts = new int[]{
                    this.menu.getMorningWorkStart(),
                    this.menu.getNoonWanderStart(),
                    this.menu.getAfternoonWorkStart(),
                    this.menu.getEveningPlayStart()
            };
            int[] actions = new int[]{
                    ActivityType.SLEEP.id(),
                    ActivityType.PLAY.id(),
                    ActivityType.WANDER.id(),
                    ActivityType.WORK.id()
            };
            this.drawTimelineSegments(guiGraphics, x, y, width, height, starts, actions, false);
            return;
        }

        int[] starts = new int[]{
                this.menu.getMorningWorkStart(),
                this.menu.getNoonWanderStart(),
                this.menu.getAfternoonWorkStart(),
                this.menu.getEveningPlayStart(),
                this.menu.getNightSleepStart(),
                this.menu.getExtraScheduleStartTick(0),
                this.menu.getExtraScheduleStartTick(1),
                this.menu.getExtraScheduleStartTick(2)
        };
        int[] actions = new int[]{
                ActivityType.SLEEP.id(),
                ActivityType.PLAY.id(),
                ActivityType.WANDER.id(),
                ActivityType.WORK.id(),
                ActivityType.ACTION5.id(),
                ActivityType.ACTION6.id(),
                ActivityType.ACTION7.id(),
                ActivityType.ACTION8.id()
        };
        this.drawTimelineSegments(guiGraphics, x, y, width, height, starts, actions, true);
    }

    private void drawTimelineSegments(GuiGraphics guiGraphics, int x, int y, int width, int height, int[] starts, int[] actions, boolean allowDim) {
        for (int i = 0; i < starts.length; i++) {
            int start = Mth.positiveModulo(starts[i], 24000);
            int end = Mth.positiveModulo(starts[(i + 1) % starts.length], 24000);
            int color = actionColor(actions[i]);
            if (allowDim && i >= 5 && !this.menu.isExtraScheduleEnabled(i - 5)) {
                color = dimColor(color);
            }
            this.fillTickRangeWrap(guiGraphics, x, y, width, height, start, end, color);
            this.drawTickMarker(guiGraphics, x, y, height, width, start);
        }
    }

    private void fillTickRangeWrap(GuiGraphics guiGraphics, int x, int y, int width, int height, int startTick, int endTick, int color) {
        int start = Mth.positiveModulo(startTick, 24000);
        int end = Mth.positiveModulo(endTick, 24000);
        if (start == end) {
            this.fillTickRange(guiGraphics, x, y, width, height, 0, 24000, color);
            return;
        }
        if (end > start) {
            this.fillTickRange(guiGraphics, x, y, width, height, start, end, color);
            return;
        }
        this.fillTickRange(guiGraphics, x, y, width, height, start, 24000, color);
        this.fillTickRange(guiGraphics, x, y, width, height, 0, end, color);
    }

    private void fillTickRange(GuiGraphics guiGraphics, int x, int y, int width, int height, int startTick, int endTick, int color) {
        int startX = x + (startTick * width) / 24000;
        int endX = x + (endTick * width) / 24000;
        if (endX <= startX) {
            endX = startX + 1;
        }
        guiGraphics.fill(startX, y, endX, y + height, color);
    }

    private void drawTickMarker(GuiGraphics guiGraphics, int x, int y, int height, int width, int tick) {
        int markerX = x + (Mth.positiveModulo(tick, 24000) * width) / 24000;
        guiGraphics.fill(markerX, y - 1, markerX + 1, y + height + 1, 0xFFFFFFFF);
    }

    private static int actionColor(int typeId) {
        int index = Mth.clamp(typeId, 0, ACTION_COLORS.length - 1);
        return ACTION_COLORS[index];
    }

    private static int dimColor(int color) {
        int a = (color >>> 24) & 0xFF;
        int r = (int) (((color >>> 16) & 0xFF) * 0.45F);
        int g = (int) (((color >>> 8) & 0xFF) * 0.45F);
        int b = (int) ((color & 0xFF) * 0.45F);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static String formatTime(int tickOfDay) {
        int normalized = Math.floorMod(tickOfDay, 24000);
        int totalMinutes = (normalized * 60) / 1000;
        int hours = (totalMinutes / 60 + 6) % 24;
        int minutes = totalMinutes % 60;
        return String.format("%02d:%02d", hours, minutes);
    }
}
