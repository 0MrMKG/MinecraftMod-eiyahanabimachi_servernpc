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
    private static final int TIMELINE_X = 10;
    private static final int TIMELINE_Y = 108;
    private static final int TIMELINE_W = 252;
    private static final int TIMELINE_H = 14;
    private static final int DAY_NIGHT_BAR_OFFSET_Y = -10;
    private static final int DAY_NIGHT_BAR_H = 6;
    private static final int DAY_NIGHT_SUN_TICK = 6000;
    private static final int DAY_NIGHT_MOON_TICK = 18000;
    private static final int DAY_NIGHT_TRANSITION_HALF = 1000;
    private static final int DAY_COLOR = 0xFF9ED3FF;
    private static final int NIGHT_COLOR = 0xFF27345C;
    private static final int DAY_NIGHT_TOP_HIGHLIGHT = 0x66FFFFFF;
    private static final int HANDLE_PICK_HALF_WIDTH = 4;
    private static final int HANDLE_PICK_TOP = 5;
    private static final int HANDLE_PICK_BOTTOM = 20;
    private static final int[] BOUNDARY_MINUS_BUTTONS = new int[]{
            NpcPatrolConfigMenu.BTN_MORNING_MINUS,
            NpcPatrolConfigMenu.BTN_NOON_MINUS,
            NpcPatrolConfigMenu.BTN_AFTERNOON_MINUS,
            NpcPatrolConfigMenu.BTN_EVENING_MINUS
    };
    private static final int[] BOUNDARY_PLUS_BUTTONS = new int[]{
            NpcPatrolConfigMenu.BTN_MORNING_PLUS,
            NpcPatrolConfigMenu.BTN_NOON_PLUS,
            NpcPatrolConfigMenu.BTN_AFTERNOON_PLUS,
            NpcPatrolConfigMenu.BTN_EVENING_PLUS
    };
    private static final int STEP_TICKS = 250;
    private static final int CHAIN_SLOT_COUNT = NpcPatrolConfigMenu.EXTRA_SLOT_COUNT + 1;
    private static final int[] ACTION_COLORS = new int[]{
            0xFF5B6284, 0xFFD88A4E, 0xFF6AA9D8, 0xFF4EA662,
            0xFFB07AE8, 0xFFE87A9E, 0xFF7AD9B8, 0xFFE8C86A
    };

    private final Button[] extraToggleButtons = new Button[CHAIN_SLOT_COUNT];
    private final Button[] extraTypeButtons = new Button[CHAIN_SLOT_COUNT];
    private int draggingBoundaryIndex = -1;
    private int draggingBoundaryTick = -1;

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

        this.addRenderableWidget(this.createAdjustButton(left + 10, top + 146, "-", NpcPatrolConfigMenu.BTN_RADIUS_MINUS));
        this.addRenderableWidget(this.createAdjustButton(left + 31, top + 146, "+", NpcPatrolConfigMenu.BTN_RADIUS_PLUS));

        for (int i = 0; i < CHAIN_SLOT_COUNT; i++) {
            final int chainSlot = i;
            int rowY = top + 164 + i * 16;

            int toggleButtonId;
            int timeMinusButtonId;
            int timePlusButtonId;
            if (chainSlot == 0) {
                toggleButtonId = NpcPatrolConfigMenu.BTN_ACTION5_TOGGLE;
                timeMinusButtonId = NpcPatrolConfigMenu.BTN_ACTION5_TIME_MINUS;
                timePlusButtonId = NpcPatrolConfigMenu.BTN_ACTION5_TIME_PLUS;
            } else {
                int extraSlot = chainSlot - 1;
                toggleButtonId = NpcPatrolConfigMenu.extraButtonId(extraSlot, NpcPatrolConfigMenu.EXTRA_OP_TOGGLE);
                timeMinusButtonId = NpcPatrolConfigMenu.extraButtonId(extraSlot, NpcPatrolConfigMenu.EXTRA_OP_TIME_MINUS);
                timePlusButtonId = NpcPatrolConfigMenu.extraButtonId(extraSlot, NpcPatrolConfigMenu.EXTRA_OP_TIME_PLUS);
            }

            Button toggle = Button.builder(Component.translatable("gui." + eiyahanabimachiservernpc.MODID + ".state.off"), button ->
                    this.sendButton(toggleButtonId)
            ).bounds(left + 10, rowY, 28, 15).build();
            Button type = Button.builder(this.activityName(this.menu.getScheduleChainActivityId(chainSlot)), button -> {
            }).bounds(left + 42, rowY, 54, 15).build();
            this.extraToggleButtons[chainSlot] = toggle;
            this.extraTypeButtons[chainSlot] = type;
            this.addRenderableWidget(toggle);
            this.addRenderableWidget(type);
            this.addRenderableWidget(this.createAdjustButton(left + 100, rowY, "-", timeMinusButtonId));
            this.addRenderableWidget(this.createAdjustButton(left + 121, rowY, "+", timePlusButtonId));
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
        this.drawDayNightBar(guiGraphics, left + TIMELINE_X, top + TIMELINE_Y + DAY_NIGHT_BAR_OFFSET_Y, TIMELINE_W, DAY_NIGHT_BAR_H);
        this.drawTimelineBar(guiGraphics, left + TIMELINE_X, top + TIMELINE_Y, TIMELINE_W, TIMELINE_H);
        this.drawTimelineHandles(guiGraphics, left + TIMELINE_X, top + TIMELINE_Y, TIMELINE_W, TIMELINE_H);
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

        guiGraphics.drawString(
                this.font,
                Component.translatable("gui." + eiyahanabimachiservernpc.MODID + ".patrol_radius", this.menu.getPatrolRadius()),
                58,
                151,
                0x404040,
                false
        );

        guiGraphics.drawString(this.font, Component.translatable("gui." + eiyahanabimachiservernpc.MODID + ".extra_schedule"), 10, 160, 0x404040, false);
        for (int i = 0; i < CHAIN_SLOT_COUNT; i++) {
            int rowY = 166 + i * 16;
            String timeName = formatTime(this.menu.getScheduleChainStartTick(i));
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

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int picked = this.pickBoundaryHandle(mouseX, mouseY);
            if (picked >= 0) {
                this.draggingBoundaryIndex = picked;
                int[] ticks = this.getDisplayBoundaryTicks();
                this.draggingBoundaryTick = this.clampBoundaryTick(picked, this.snapTick(this.mouseXToTick(mouseX)), ticks);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && this.draggingBoundaryIndex >= 0) {
            int[] ticks = this.getDisplayBoundaryTicks();
            this.draggingBoundaryTick = this.clampBoundaryTick(
                    this.draggingBoundaryIndex,
                    this.snapTick(this.mouseXToTick(mouseX)),
                    ticks
            );
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && this.draggingBoundaryIndex >= 0) {
            int boundary = this.draggingBoundaryIndex;
            int targetTick = this.draggingBoundaryTick;
            int currentTick = this.getMenuBoundaryTick(boundary);
            int deltaSteps = (targetTick - currentTick) / STEP_TICKS;
            this.applyBoundaryDeltaByButtons(boundary, deltaSteps);
            this.draggingBoundaryIndex = -1;
            this.draggingBoundaryTick = -1;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void updateDynamicButtons() {
        for (int i = 0; i < CHAIN_SLOT_COUNT; i++) {
            if (this.extraToggleButtons[i] != null) {
                this.extraToggleButtons[i].setMessage(Component.translatable(
                        "gui." + eiyahanabimachiservernpc.MODID + (this.menu.isScheduleChainEnabled(i) ? ".state.on" : ".state.off")
                ));
                this.extraToggleButtons[i].active = this.menu.canToggleScheduleChainSlot(i);
            }
            if (this.extraTypeButtons[i] != null) {
                this.extraTypeButtons[i].setMessage(this.activityName(this.menu.getScheduleChainActivityId(i)));
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
        this.drawTimelineSegments(guiGraphics, x, y, width, height, this.getDisplayStartTicks(), this.getDisplayActionIds());
    }

    private void drawTimelineHandles(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        int[] ticks = this.getDisplayBoundaryTicks();
        for (int i = 0; i < ticks.length; i++) {
            int markerX = x + (Mth.positiveModulo(ticks[i], 24000) * width) / 24000;
            int top = y - 3;
            int bottom = y + height + 3;
            int left = markerX - 1;
            int right = markerX + 1;
            int color = (i == this.draggingBoundaryIndex) ? 0xFFFFE38A : 0xFFFFFFFF;
            guiGraphics.fill(left, top, right, bottom, color);
        }
        if (this.draggingBoundaryIndex >= 0) {
            String dragText = formatTime(this.draggingBoundaryTick);
            guiGraphics.drawString(this.font, Component.literal(dragText), x, y - 12, 0x404040, false);
        }
    }

    private void drawTimelineSegments(GuiGraphics guiGraphics, int x, int y, int width, int height, int[] starts, int[] actions) {
        for (int i = 0; i < starts.length; i++) {
            int start = Mth.positiveModulo(starts[i], 24000);
            int end = Mth.positiveModulo(starts[(i + 1) % starts.length], 24000);
            int color = actionColor(actions[i]);
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

    private static String formatTime(int tickOfDay) {
        int normalized = Math.floorMod(tickOfDay, 24000);
        int totalMinutes = (normalized * 60) / 1000;
        int hours = (totalMinutes / 60 + 6) % 24;
        int minutes = totalMinutes % 60;
        return String.format("%02d:%02d", hours, minutes);
    }

    private void drawDayNightBar(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        for (int px = 0; px < width; px++) {
            int tick = (px * 24000) / width;
            guiGraphics.fill(x + px, y, x + px + 1, y + height, this.dayNightColor(tick));
        }
        guiGraphics.fill(x, y, x + width, y + 1, DAY_NIGHT_TOP_HIGHLIGHT);
        this.drawSunIcon(guiGraphics, x + (DAY_NIGHT_SUN_TICK * width) / 24000 - 3, y - 1);
        this.drawMoonIcon(guiGraphics, x + (DAY_NIGHT_MOON_TICK * width) / 24000 - 3, y - 1);
    }

    private int dayNightColor(int tickOfDay) {
        int tick = Mth.positiveModulo(tickOfDay, 24000);
        int sunriseStart = 24000 - DAY_NIGHT_TRANSITION_HALF;
        int sunriseEnd = DAY_NIGHT_TRANSITION_HALF;
        int sunsetStart = 12000 - DAY_NIGHT_TRANSITION_HALF;
        int sunsetEnd = 12000 + DAY_NIGHT_TRANSITION_HALF;

        if (tick >= sunriseEnd && tick < sunsetStart) {
            return DAY_COLOR;
        }
        if (tick >= sunsetEnd && tick < sunriseStart) {
            return NIGHT_COLOR;
        }
        if (tick >= sunsetStart && tick < sunsetEnd) {
            float t = (tick - sunsetStart) / (float) (sunsetEnd - sunsetStart);
            return this.lerpColor(DAY_COLOR, NIGHT_COLOR, t);
        }
        int sunrisePos = tick >= sunriseStart ? tick - sunriseStart : tick + (24000 - sunriseStart);
        float t = sunrisePos / (float) (DAY_NIGHT_TRANSITION_HALF * 2);
        return this.lerpColor(NIGHT_COLOR, DAY_COLOR, t);
    }

    private int lerpColor(int from, int to, float t) {
        float clamped = Mth.clamp(t, 0.0F, 1.0F);
        int a = (int) (this.lerpChannel((from >>> 24) & 0xFF, (to >>> 24) & 0xFF, clamped));
        int r = (int) (this.lerpChannel((from >>> 16) & 0xFF, (to >>> 16) & 0xFF, clamped));
        int g = (int) (this.lerpChannel((from >>> 8) & 0xFF, (to >>> 8) & 0xFF, clamped));
        int b = (int) (this.lerpChannel(from & 0xFF, to & 0xFF, clamped));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private float lerpChannel(int from, int to, float t) {
        return from + (to - from) * t;
    }

    private void drawSunIcon(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.fill(x + 2, y + 1, x + 5, y + 4, 0xFFFFDC5E);
        guiGraphics.fill(x + 1, y + 2, x + 2, y + 3, 0xFFFFDC5E);
        guiGraphics.fill(x + 5, y + 2, x + 6, y + 3, 0xFFFFDC5E);
        guiGraphics.fill(x + 3, y, x + 4, y + 1, 0xFFFFDC5E);
        guiGraphics.fill(x + 3, y + 4, x + 4, y + 5, 0xFFFFDC5E);
    }

    private void drawMoonIcon(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.fill(x + 2, y + 1, x + 5, y + 4, 0xFFE6ECFF);
        guiGraphics.fill(x + 4, y + 1, x + 5, y + 4, DAY_COLOR);
    }

    private int[] getDisplayStartTicks() {
        int actionCount = this.menu.getEnabledActionCount();
        int[] starts = new int[actionCount];
        starts[0] = this.menu.getMorningWorkStart();
        starts[1] = this.menu.getNoonWanderStart();
        starts[2] = this.menu.getAfternoonWorkStart();
        starts[3] = this.menu.getEveningPlayStart();

        int next = 4;
        if (this.menu.isAction5Enabled() && next < actionCount) {
            starts[next++] = this.menu.getNightSleepStart();
        }
        for (int i = 0; i < NpcPatrolConfigMenu.EXTRA_SLOT_COUNT && next < actionCount; i++) {
            if (!this.menu.isExtraScheduleEnabled(i)) {
                break;
            }
            starts[next++] = this.menu.getExtraScheduleStartTick(i);
        }

        if (this.draggingBoundaryIndex >= 0 && this.draggingBoundaryIndex + 1 < starts.length) {
            starts[this.draggingBoundaryIndex + 1] = this.draggingBoundaryTick;
        }
        return starts;
    }

    private int[] getDisplayActionIds() {
        int actionCount = this.menu.getEnabledActionCount();
        int[] actions = new int[actionCount];
        actions[0] = ActivityType.SLEEP.id();
        actions[1] = ActivityType.PLAY.id();
        actions[2] = ActivityType.WANDER.id();
        actions[3] = ActivityType.WORK.id();

        int next = 4;
        if (this.menu.isAction5Enabled() && next < actionCount) {
            actions[next++] = ActivityType.ACTION5.id();
        }
        for (int i = 0; i < NpcPatrolConfigMenu.EXTRA_SLOT_COUNT && next < actionCount; i++) {
            if (!this.menu.isExtraScheduleEnabled(i)) {
                break;
            }
            actions[next++] = ActivityType.ACTION6.id() + i;
        }
        return actions;
    }

    private int pickBoundaryHandle(double mouseX, double mouseY) {
        int x = this.leftPos + TIMELINE_X;
        int y = this.topPos + TIMELINE_Y;
        if (mouseY < y - HANDLE_PICK_TOP || mouseY > y + HANDLE_PICK_BOTTOM) {
            return -1;
        }
        int[] ticks = this.getDisplayBoundaryTicks();
        int bestIndex = -1;
        int bestDistance = Integer.MAX_VALUE;
        for (int i = 0; i < ticks.length; i++) {
            int markerX = x + (Mth.positiveModulo(ticks[i], 24000) * TIMELINE_W) / 24000;
            int distance = (int) Math.abs(mouseX - markerX);
            if (distance <= HANDLE_PICK_HALF_WIDTH && distance < bestDistance) {
                bestDistance = distance;
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    private int[] getDisplayBoundaryTicks() {
        int actionCount = this.menu.getEnabledActionCount();
        int[] ticks = new int[actionCount];
        int index = 0;
        ticks[index++] = this.menu.getMorningWorkStart();
        ticks[index++] = this.menu.getNoonWanderStart();
        ticks[index++] = this.menu.getAfternoonWorkStart();
        ticks[index++] = this.menu.getEveningPlayStart();
        if (this.menu.isAction5Enabled() && index < actionCount) {
            ticks[index++] = this.menu.getNightSleepStart();
        }
        for (int i = 0; i < NpcPatrolConfigMenu.EXTRA_SLOT_COUNT && index < actionCount; i++) {
            if (!this.menu.isExtraScheduleEnabled(i)) {
                break;
            }
            ticks[index++] = this.menu.getExtraScheduleStartTick(i);
        }
        if (this.draggingBoundaryIndex >= 0 && this.draggingBoundaryIndex < ticks.length) {
            ticks[this.draggingBoundaryIndex] = this.draggingBoundaryTick;
        }
        return ticks;
    }

    private int getMenuBoundaryTick(int boundaryIndex) {
        return switch (boundaryIndex) {
            case 0 -> this.menu.getMorningWorkStart();
            case 1 -> this.menu.getNoonWanderStart();
            case 2 -> this.menu.getAfternoonWorkStart();
            case 3 -> this.menu.getEveningPlayStart();
            case 4 -> this.menu.getNightSleepStart();
            case 5 -> this.menu.getExtraScheduleStartTick(0);
            case 6 -> this.menu.getExtraScheduleStartTick(1);
            case 7 -> this.menu.getExtraScheduleStartTick(2);
            default -> 0;
        };
    }

    private void applyBoundaryDeltaByButtons(int boundaryIndex, int deltaSteps) {
        if (deltaSteps == 0) {
            return;
        }

        int plusButtonId;
        int minusButtonId;
        if (boundaryIndex <= 3) {
            plusButtonId = BOUNDARY_PLUS_BUTTONS[boundaryIndex];
            minusButtonId = BOUNDARY_MINUS_BUTTONS[boundaryIndex];
        } else if (boundaryIndex == 4) {
            plusButtonId = NpcPatrolConfigMenu.BTN_ACTION5_TIME_PLUS;
            minusButtonId = NpcPatrolConfigMenu.BTN_ACTION5_TIME_MINUS;
        } else {
            int extraSlot = boundaryIndex - 5;
            plusButtonId = NpcPatrolConfigMenu.extraButtonId(extraSlot, NpcPatrolConfigMenu.EXTRA_OP_TIME_PLUS);
            minusButtonId = NpcPatrolConfigMenu.extraButtonId(extraSlot, NpcPatrolConfigMenu.EXTRA_OP_TIME_MINUS);
        }

        if (deltaSteps > 0) {
            for (int i = 0; i < deltaSteps; i++) {
                this.sendButton(plusButtonId);
            }
            return;
        }
        for (int i = 0; i < -deltaSteps; i++) {
            this.sendButton(minusButtonId);
        }
    }

    private int mouseXToTick(double mouseX) {
        int timelineLeft = this.leftPos + TIMELINE_X;
        double ratio = (mouseX - timelineLeft) / TIMELINE_W;
        return Mth.clamp((int) Math.round(ratio * 24000.0D), 0, 23999);
    }

    private int snapTick(int tick) {
        int snapped = Math.round(tick / (float) STEP_TICKS) * STEP_TICKS;
        return Mth.clamp(snapped, 0, 23999);
    }

    private int clampBoundaryTick(int boundaryIndex, int tick, int[] displayTicks) {
        int min = 0;
        int max = 23999;
        if (boundaryIndex > 0) {
            min = displayTicks[boundaryIndex - 1] + STEP_TICKS;
        }
        if (boundaryIndex < displayTicks.length - 1) {
            max = displayTicks[boundaryIndex + 1] - STEP_TICKS;
        }
        return Mth.clamp(tick, min, max);
    }
}
