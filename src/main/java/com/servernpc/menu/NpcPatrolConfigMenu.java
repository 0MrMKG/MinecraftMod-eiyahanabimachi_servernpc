package com.servernpc.menu;

import com.servernpc.eiyahanabimachiservernpc;
import com.servernpc.entity.ReimuGoodNpcEntity;
import com.servernpc.entity.ReimuGoodNpcEntity.ActivityType;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;

public class NpcPatrolConfigMenu extends AbstractContainerMenu {
    public static final int BTN_TYPE_ACTION1 = 0;
    public static final int BTN_TYPE_ACTION2 = 1;
    public static final int BTN_TYPE_ACTION3 = 2;
    public static final int BTN_TYPE_ACTION4 = 3;
    public static final int BTN_TYPE_ACTION5 = 4;
    public static final int BTN_TYPE_ACTION6 = 5;
    public static final int BTN_TYPE_ACTION7 = 6;
    public static final int BTN_TYPE_ACTION8 = 7;

    public static final int BTN_MORNING_MINUS = 10;
    public static final int BTN_MORNING_PLUS = 11;
    public static final int BTN_NOON_MINUS = 12;
    public static final int BTN_NOON_PLUS = 13;
    public static final int BTN_AFTERNOON_MINUS = 14;
    public static final int BTN_AFTERNOON_PLUS = 15;
    public static final int BTN_EVENING_MINUS = 16;
    public static final int BTN_EVENING_PLUS = 17;
    public static final int BTN_NIGHT_MINUS = 18;
    public static final int BTN_NIGHT_PLUS = 19;

    public static final int BTN_RADIUS_MINUS = 20;
    public static final int BTN_RADIUS_PLUS = 21;

    public static final int EXTRA_SLOT_COUNT = 3;
    public static final int EXTRA_BUTTON_BASE = 40;
    public static final int EXTRA_BUTTONS_PER_SLOT = 4;
    public static final int EXTRA_OP_TOGGLE = 0;
    public static final int EXTRA_OP_TYPE = 1;
    public static final int EXTRA_OP_TIME_MINUS = 2;
    public static final int EXTRA_OP_TIME_PLUS = 3;

    public static final int BTN_SAVE = 30;
    public static final int BTN_CANCEL = 31;
    public static final int BTN_DELETE = 32;
    public static final int BTN_ACTION5_TOGGLE = 33;
    public static final int BTN_ACTION5_TIME_MINUS = 34;
    public static final int BTN_ACTION5_TIME_PLUS = 35;

    private static final int STEP_TICKS = 250;

    private final int npcEntityId;
    private final BlockPos targetPos;
    @Nullable
    private final ReimuGoodNpcEntity npc;
    private final Component npcDisplayName;

    private int selectedActivityTypeId = ActivityType.WORK.id();
    private int morningWorkStart = 2000;
    private int noonWanderStart = 6000;
    private int afternoonWorkStart = 9000;
    private int eveningPlayStart = 12000;
    private int nightSleepStart = 15000;
    private boolean scheduleConfigured = false;
    private boolean action5Enabled = false;
    private int patrolRadius = ReimuGoodNpcEntity.DEFAULT_ACTIVITY_RADIUS;
    private final boolean[] extraEnabled = new boolean[]{false, false, false};
    private final int[] extraStartTicks = new int[]{17000, 19000, 21000};
    private final int[] extraActivityTypeIds = new int[]{ActivityType.ACTION6.id(), ActivityType.ACTION7.id(), ActivityType.ACTION8.id()};

    public NpcPatrolConfigMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf data) {
        this(
                containerId,
                playerInventory,
                data != null ? data.readVarInt() : -1,
                data != null ? data.readBlockPos() : BlockPos.ZERO
        );
    }

    public NpcPatrolConfigMenu(int containerId, Inventory playerInventory, ReimuGoodNpcEntity npc, BlockPos targetPos) {
        this(containerId, playerInventory, npc.getId(), targetPos);
    }

    private NpcPatrolConfigMenu(int containerId, Inventory playerInventory, int npcEntityId, BlockPos targetPos) {
        super(eiyahanabimachiservernpc.NPC_PATROL_CONFIG_MENU.get(), containerId);
        this.npcEntityId = npcEntityId;
        this.targetPos = targetPos.immutable();
        this.npc = resolveNpc(playerInventory.player, npcEntityId);
        this.npcDisplayName = this.npc != null
                ? this.npc.getName().copy()
                : Component.literal("#" + npcEntityId);

        if (this.npc != null) {
            this.scheduleConfigured = this.npc.hasScheduleConfigured();
            this.morningWorkStart = this.npc.getMorningWorkStart();
            this.noonWanderStart = this.npc.getNoonWanderStart();
            this.afternoonWorkStart = this.npc.getAfternoonWorkStart();
            this.eveningPlayStart = this.npc.getEveningPlayStart();
            this.nightSleepStart = this.npc.getNightSleepStart();
            this.action5Enabled = this.npc.isAction5Enabled();
            this.patrolRadius = this.npc.getActivityPointRadius(this.targetPos);
            if (this.scheduleConfigured) {
                this.selectedActivityTypeId = this.npc.getScheduledActivity((int) this.npc.level().getDayTime()).id();
            }
            for (int i = 0; i < EXTRA_SLOT_COUNT; i++) {
                this.extraEnabled[i] = this.npc.isExtraScheduleEnabled(i);
                this.extraStartTicks[i] = this.npc.getExtraScheduleStartTick(i);
                this.extraActivityTypeIds[i] = this.fixedExtraActivityId(i);
            }
        }
        this.normalizeExtraScheduleTicks();

        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return NpcPatrolConfigMenu.this.selectedActivityTypeId;
            }

            @Override
            public void set(int value) {
                NpcPatrolConfigMenu.this.selectedActivityTypeId = ActivityType.byId(value).id();
            }
        });
        this.addDataSlot(intDataSlot(() -> this.morningWorkStart, value -> this.morningWorkStart = value));
        this.addDataSlot(intDataSlot(() -> this.noonWanderStart, value -> this.noonWanderStart = value));
        this.addDataSlot(intDataSlot(() -> this.afternoonWorkStart, value -> this.afternoonWorkStart = value));
        this.addDataSlot(intDataSlot(() -> this.eveningPlayStart, value -> this.eveningPlayStart = value));
        this.addDataSlot(intDataSlot(() -> this.nightSleepStart, value -> this.nightSleepStart = value));
        this.addDataSlot(intDataSlot(() -> this.scheduleConfigured ? 1 : 0, value -> this.scheduleConfigured = value != 0));
        this.addDataSlot(intDataSlot(() -> this.action5Enabled ? 1 : 0, value -> this.action5Enabled = value != 0));
        this.addDataSlot(intDataSlot(() -> this.patrolRadius, value -> this.patrolRadius = value));
        for (int i = 0; i < EXTRA_SLOT_COUNT; i++) {
            final int slot = i;
            this.addDataSlot(intDataSlot(() -> this.extraEnabled[slot] ? 1 : 0, value -> this.extraEnabled[slot] = value != 0));
            this.addDataSlot(intDataSlot(() -> this.extraStartTicks[slot], value -> this.extraStartTicks[slot] = normalizeTick(value)));
            this.addDataSlot(intDataSlot(() -> this.fixedExtraActivityId(slot), value -> this.extraActivityTypeIds[slot] = this.fixedExtraActivityId(slot)));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.npc != null && this.npc.isAlive();
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id >= BTN_TYPE_ACTION1 && id <= BTN_TYPE_ACTION8) {
            this.selectedActivityTypeId = ActivityType.byId(id).id();
            this.broadcastChanges();
            return true;
        }

        if (id >= EXTRA_BUTTON_BASE && id < EXTRA_BUTTON_BASE + EXTRA_SLOT_COUNT * EXTRA_BUTTONS_PER_SLOT) {
            int relative = id - EXTRA_BUTTON_BASE;
            int slot = relative / EXTRA_BUTTONS_PER_SLOT;
            int op = relative % EXTRA_BUTTONS_PER_SLOT;
            switch (op) {
                case EXTRA_OP_TOGGLE -> this.toggleExtraScheduleSlot(player, slot);
                case EXTRA_OP_TYPE -> this.extraActivityTypeIds[slot] = this.fixedExtraActivityId(slot);
                case EXTRA_OP_TIME_MINUS -> this.adjustExtraScheduleTime(slot, -STEP_TICKS);
                case EXTRA_OP_TIME_PLUS -> this.adjustExtraScheduleTime(slot, STEP_TICKS);
                default -> {
                    return false;
                }
            }
            this.broadcastChanges();
            return true;
        }

        switch (id) {
            case BTN_ACTION5_TOGGLE -> this.toggleAction5Slot();
            case BTN_ACTION5_TIME_MINUS -> this.adjustBoundary(4, -STEP_TICKS);
            case BTN_ACTION5_TIME_PLUS -> this.adjustBoundary(4, STEP_TICKS);
            case BTN_MORNING_MINUS -> this.adjustBoundary(0, -STEP_TICKS);
            case BTN_MORNING_PLUS -> this.adjustBoundary(0, STEP_TICKS);
            case BTN_NOON_MINUS -> this.adjustBoundary(1, -STEP_TICKS);
            case BTN_NOON_PLUS -> this.adjustBoundary(1, STEP_TICKS);
            case BTN_AFTERNOON_MINUS -> this.adjustBoundary(2, -STEP_TICKS);
            case BTN_AFTERNOON_PLUS -> this.adjustBoundary(2, STEP_TICKS);
            case BTN_EVENING_MINUS -> this.adjustBoundary(3, -STEP_TICKS);
            case BTN_EVENING_PLUS -> this.adjustBoundary(3, STEP_TICKS);
            case BTN_NIGHT_MINUS -> this.adjustBoundary(4, -STEP_TICKS);
            case BTN_NIGHT_PLUS -> this.adjustBoundary(4, STEP_TICKS);
            case BTN_RADIUS_MINUS -> this.adjustRadius(-1);
            case BTN_RADIUS_PLUS -> this.adjustRadius(1);
            case BTN_SAVE -> {
                this.saveActivityPoint(player);
                return true;
            }
            case BTN_DELETE -> {
                this.deleteActivityPoint(player);
                return true;
            }
            case BTN_CANCEL -> {
                player.closeContainer();
                return true;
            }
            default -> {
                return false;
            }
        }

        this.broadcastChanges();
        return true;
    }

    public int getSelectedActivityTypeId() {
        return this.selectedActivityTypeId;
    }

    public BlockPos getTargetPos() {
        return this.targetPos;
    }

    public int getMorningWorkStart() {
        return this.morningWorkStart;
    }

    public int getNoonWanderStart() {
        return this.noonWanderStart;
    }

    public int getAfternoonWorkStart() {
        return this.afternoonWorkStart;
    }

    public int getEveningPlayStart() {
        return this.eveningPlayStart;
    }

    public int getNightSleepStart() {
        return this.nightSleepStart;
    }

    public int getPatrolRadius() {
        return this.patrolRadius;
    }

    public Component getNpcDisplayName() {
        return this.npcDisplayName;
    }

    public boolean hasScheduleConfigured() {
        return this.scheduleConfigured;
    }

    public boolean isExtraScheduleEnabled(int slot) {
        return this.isValidExtraSlot(slot) && this.extraEnabled[slot];
    }

    public boolean isAction5Enabled() {
        return this.action5Enabled;
    }

    public boolean canToggleAction5() {
        return true;
    }

    public int getExtraScheduleStartTick(int slot) {
        if (!this.isValidExtraSlot(slot)) {
            return 0;
        }
        return this.extraStartTicks[slot];
    }

    public int getExtraScheduleActivityId(int slot) {
        if (!this.isValidExtraSlot(slot)) {
            return ActivityType.ACTION6.id();
        }
        return this.fixedExtraActivityId(slot);
    }

    public boolean canToggleExtraScheduleSlot(int slot) {
        if (!this.isValidExtraSlot(slot)) {
            return false;
        }
        if (!this.action5Enabled) {
            return false;
        }
        if (this.extraEnabled[slot]) {
            return true;
        }
        for (int i = 0; i < slot; i++) {
            if (!this.extraEnabled[i]) {
                return false;
            }
        }
        return true;
    }

    public int getScheduleChainSlotCount() {
        return EXTRA_SLOT_COUNT + 1;
    }

    public boolean isScheduleChainEnabled(int chainSlot) {
        if (chainSlot == 0) {
            return this.isAction5Enabled();
        }
        int extraSlot = chainSlot - 1;
        return this.isExtraScheduleEnabled(extraSlot);
    }

    public int getScheduleChainStartTick(int chainSlot) {
        if (chainSlot == 0) {
            return this.nightSleepStart;
        }
        int extraSlot = chainSlot - 1;
        return this.getExtraScheduleStartTick(extraSlot);
    }

    public int getScheduleChainActivityId(int chainSlot) {
        return ActivityType.ACTION5.id() + Mth.clamp(chainSlot, 0, EXTRA_SLOT_COUNT);
    }

    public boolean canToggleScheduleChainSlot(int chainSlot) {
        if (chainSlot == 0) {
            return this.canToggleAction5();
        }
        int extraSlot = chainSlot - 1;
        return this.canToggleExtraScheduleSlot(extraSlot);
    }

    public int getEnabledActionCount() {
        int count = 4;
        if (this.action5Enabled) {
            count++;
            for (int i = 0; i < EXTRA_SLOT_COUNT; i++) {
                if (this.extraEnabled[i]) {
                    count++;
                } else {
                    break;
                }
            }
        }
        return count;
    }

    public static int extraButtonId(int slot, int op) {
        return EXTRA_BUTTON_BASE + slot * EXTRA_BUTTONS_PER_SLOT + op;
    }

    private void saveActivityPoint(Player player) {
        if (this.npc == null || !this.npc.isAlive()) {
            player.displayClientMessage(
                    Component.translatable("message." + eiyahanabimachiservernpc.MODID + ".patrol_wand.npc_unavailable"),
                    true
            );
            player.closeContainer();
            return;
        }

        ActivityType selectedType = ActivityType.byId(this.selectedActivityTypeId);
        this.npc.setOrUpdateActivityPoint(this.targetPos, selectedType, this.patrolRadius);
        this.npc.setScheduleBoundaries(
                this.morningWorkStart,
                this.noonWanderStart,
                this.afternoonWorkStart,
                this.eveningPlayStart,
                this.nightSleepStart
        );
        this.npc.setScheduleConfigured(true);
        this.scheduleConfigured = true;
        this.npc.setAction5Enabled(this.action5Enabled);
        for (int i = 0; i < EXTRA_SLOT_COUNT; i++) {
            this.npc.setExtraScheduleSlot(i, this.extraEnabled[i], this.extraStartTicks[i], this.fixedExtraActivityId(i));
        }

        player.displayClientMessage(
                Component.translatable(
                        "message." + eiyahanabimachiservernpc.MODID + ".activity_point.saved",
                        this.targetPos.getX(),
                        this.targetPos.getY(),
                        this.targetPos.getZ(),
                        Component.translatable("gui." + eiyahanabimachiservernpc.MODID + ".activity." + selectedType.key()),
                        this.patrolRadius,
                        this.npc.getActivityPointCount()
                ),
                false
        );
        player.closeContainer();
    }

    private void deleteActivityPoint(Player player) {
        if (this.npc == null || !this.npc.isAlive()) {
            player.displayClientMessage(
                    Component.translatable("message." + eiyahanabimachiservernpc.MODID + ".patrol_wand.npc_unavailable"),
                    true
            );
            player.closeContainer();
            return;
        }

        boolean removed = this.npc.removeActivityPoint(this.targetPos);
        if (removed) {
            player.displayClientMessage(
                    Component.translatable(
                            "message." + eiyahanabimachiservernpc.MODID + ".activity_point.deleted",
                            this.targetPos.getX(),
                            this.targetPos.getY(),
                            this.targetPos.getZ(),
                            this.npc.getActivityPointCount()
                    ),
                    false
            );
        } else {
            player.displayClientMessage(
                    Component.translatable(
                            "message." + eiyahanabimachiservernpc.MODID + ".activity_point.not_found",
                            this.targetPos.getX(),
                            this.targetPos.getY(),
                            this.targetPos.getZ()
                    ),
                    true
            );
        }
        player.closeContainer();
    }

    private void adjustBoundary(int index, int delta) {
        switch (index) {
            case 0 -> this.morningWorkStart = Mth.clamp(this.morningWorkStart + delta, 0, this.noonWanderStart - STEP_TICKS);
            case 1 -> this.noonWanderStart = Mth.clamp(this.noonWanderStart + delta, this.morningWorkStart + STEP_TICKS, this.afternoonWorkStart - STEP_TICKS);
            case 2 -> this.afternoonWorkStart = Mth.clamp(this.afternoonWorkStart + delta, this.noonWanderStart + STEP_TICKS, this.eveningPlayStart - STEP_TICKS);
            case 3 -> {
                int maxEvening = this.action5Enabled ? this.nightSleepStart - STEP_TICKS : 23999;
                this.eveningPlayStart = Mth.clamp(this.eveningPlayStart + delta, this.afternoonWorkStart + STEP_TICKS, maxEvening);
            }
            case 4 -> {
                if (this.action5Enabled) {
                    int maxNight = this.extraEnabled[0] ? this.extraStartTicks[0] - STEP_TICKS : 23999;
                    this.nightSleepStart = Mth.clamp(this.nightSleepStart + delta, this.eveningPlayStart + STEP_TICKS, maxNight);
                }
            }
            default -> {
            }
        }
        this.normalizeExtraScheduleTicks();
    }

    private void adjustRadius(int delta) {
        this.patrolRadius = Mth.clamp(
                this.patrolRadius + delta,
                ReimuGoodNpcEntity.MIN_ACTIVITY_RADIUS,
                ReimuGoodNpcEntity.MAX_ACTIVITY_RADIUS
        );
    }

    private void adjustExtraScheduleTime(int slot, int delta) {
        if (!this.isValidExtraSlot(slot)) {
            return;
        }
        if (!this.action5Enabled || !this.extraEnabled[slot]) {
            return;
        }
        int nextValue = this.extraStartTicks[slot] + delta;
        int min = this.getExtraStartMin(slot);
        int max = this.getExtraStartMax(slot);
        this.extraStartTicks[slot] = Mth.clamp(nextValue, min, max);
        this.normalizeExtraScheduleTicks();
    }

    private void toggleExtraScheduleSlot(Player player, int slot) {
        if (!this.isValidExtraSlot(slot)) {
            return;
        }

        if (this.extraEnabled[slot]) {
            for (int i = slot; i < EXTRA_SLOT_COUNT; i++) {
                this.extraEnabled[i] = false;
            }
            this.rebalanceOptionalScheduleStarts();
            return;
        }

        if (!this.action5Enabled) {
            player.displayClientMessage(
                    Component.translatable(
                            "message." + eiyahanabimachiservernpc.MODID + ".extra_schedule_lock_order",
                            5
                    ),
                    true
            );
            return;
        }

        for (int i = 0; i < slot; i++) {
            if (!this.extraEnabled[i]) {
                player.displayClientMessage(
                        Component.translatable(
                                "message." + eiyahanabimachiservernpc.MODID + ".extra_schedule_lock_order",
                                i + 6
                        ),
                        true
                );
                return;
            }
        }
        this.extraEnabled[slot] = true;
        this.rebalanceOptionalScheduleStarts();
    }

    private void toggleAction5Slot() {
        this.action5Enabled = !this.action5Enabled;
        if (!this.action5Enabled) {
            this.disableAllExtraSchedules();
        }
        this.rebalanceOptionalScheduleStarts();
    }

    private void disableAllExtraSchedules() {
        for (int i = 0; i < EXTRA_SLOT_COUNT; i++) {
            this.extraEnabled[i] = false;
        }
    }

    private boolean isAnyExtraEnabled() {
        for (int i = 0; i < EXTRA_SLOT_COUNT; i++) {
            if (this.extraEnabled[i]) {
                return true;
            }
        }
        return false;
    }

    private boolean isValidExtraSlot(int slot) {
        return slot >= 0 && slot < EXTRA_SLOT_COUNT;
    }

    private int fixedExtraActivityId(int slot) {
        return ActivityType.ACTION6.id() + Mth.clamp(slot, 0, EXTRA_SLOT_COUNT - 1);
    }

    private static int normalizeTick(int tick) {
        return Mth.positiveModulo(tick, 24000);
    }

    private int getExtraStartMin(int slot) {
        if (slot <= 0) {
            return Mth.clamp(this.nightSleepStart + STEP_TICKS, 0, 23999);
        }
        int prev = slot - 1;
        while (prev >= 0 && !this.extraEnabled[prev]) {
            prev--;
        }
        if (prev < 0) {
            return Mth.clamp(this.nightSleepStart + STEP_TICKS, 0, 23999);
        }
        return Mth.clamp(this.extraStartTicks[prev] + STEP_TICKS, 0, 23999);
    }

    private int getExtraStartMax(int slot) {
        int next = slot + 1;
        while (next < EXTRA_SLOT_COUNT && !this.extraEnabled[next]) {
            next++;
        }
        if (next >= EXTRA_SLOT_COUNT) {
            return 23999;
        }
        int bound = this.extraStartTicks[next] - STEP_TICKS;
        return Mth.clamp(bound, 0, 23999);
    }

    private void normalizeExtraScheduleTicks() {
        if (!this.action5Enabled) {
            this.nightSleepStart = 23999;
            this.disableAllExtraSchedules();
            for (int i = 0; i < EXTRA_SLOT_COUNT; i++) {
                this.extraStartTicks[i] = 23999;
            }
            return;
        }

        this.nightSleepStart = Mth.clamp(this.nightSleepStart, this.eveningPlayStart + STEP_TICKS, 23999);
        for (int i = 0; i < EXTRA_SLOT_COUNT; i++) {
            if (i > 0 && this.extraEnabled[i] && !this.extraEnabled[i - 1]) {
                this.extraEnabled[i] = false;
            }
        }

        int previous = this.nightSleepStart;
        for (int i = 0; i < EXTRA_SLOT_COUNT; i++) {
            if (!this.extraEnabled[i]) {
                this.extraStartTicks[i] = 23999;
                continue;
            }
            int min = Mth.clamp(previous + STEP_TICKS, 0, 23999);
            this.extraStartTicks[i] = Mth.clamp(this.extraStartTicks[i], min, 23999);
            previous = this.extraStartTicks[i];
        }
    }

    private void rebalanceOptionalScheduleStarts() {
        if (!this.action5Enabled) {
            this.nightSleepStart = 23999;
            for (int i = 0; i < EXTRA_SLOT_COUNT; i++) {
                this.extraStartTicks[i] = 23999;
            }
            return;
        }

        int enabledExtraCount = 0;
        while (enabledExtraCount < EXTRA_SLOT_COUNT && this.extraEnabled[enabledExtraCount]) {
            enabledExtraCount++;
        }
        int optionalCount = 1 + enabledExtraCount;
        int minStart = Mth.clamp(this.eveningPlayStart + STEP_TICKS, 0, 23999);
        int range = Math.max(0, 23999 - minStart);

        int[] starts = new int[optionalCount];
        for (int i = 0; i < optionalCount; i++) {
            int target;
            if (optionalCount == 1) {
                target = minStart + range / 2;
            } else {
                float ratio = i / (float) (optionalCount - 1);
                target = minStart + Math.round(range * ratio);
            }
            target = this.snapToStep(target);
            int minBound = Mth.clamp(minStart + i * STEP_TICKS, 0, 23999);
            int maxBound = Mth.clamp(23999 - (optionalCount - 1 - i) * STEP_TICKS, 0, 23999);
            starts[i] = Mth.clamp(target, minBound, maxBound);
        }

        this.nightSleepStart = starts[0];
        for (int i = 0; i < enabledExtraCount; i++) {
            this.extraStartTicks[i] = starts[i + 1];
        }
        for (int i = enabledExtraCount; i < EXTRA_SLOT_COUNT; i++) {
            this.extraStartTicks[i] = 23999;
        }
        this.normalizeExtraScheduleTicks();
    }

    private int snapToStep(int tick) {
        int snapped = Math.round(tick / (float) STEP_TICKS) * STEP_TICKS;
        return Mth.clamp(snapped, 0, 23999);
    }

    private static DataSlot intDataSlot(IntGetter getter, IntSetter setter) {
        return new DataSlot() {
            @Override
            public int get() {
                return getter.get();
            }

            @Override
            public void set(int value) {
                setter.set(value);
            }
        };
    }

    @Nullable
    private static ReimuGoodNpcEntity resolveNpc(Player player, int entityId) {
        if (entityId < 0) {
            return null;
        }
        Entity entity = player.level().getEntity(entityId);
        return entity instanceof ReimuGoodNpcEntity npcEntity ? npcEntity : null;
    }

    @FunctionalInterface
    private interface IntGetter {
        int get();
    }

    @FunctionalInterface
    private interface IntSetter {
        void set(int value);
    }
}
