package com.servernpc.entity;

import com.servernpc.eiyahanabimachiservernpc;
import com.servernpc.item.NpcPatrolWandItem;
import com.servernpc.menu.NpcInventoryMenu;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Predicate;
import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedBowAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

public class ReimuGoodNpcEntity extends Monster implements RangedAttackMob {
    private static final int BACKPACK_BASE_USABLE_SLOTS = 9;
    private static final int BACKPACK_MAX_UNLOCKABLE_SLOTS = 18;
    private static final EntityDataAccessor<Boolean> MOVEMENT_STOPPED =
            SynchedEntityData.defineId(ReimuGoodNpcEntity.class, EntityDataSerializers.BOOLEAN);
    private static final String MOVEMENT_STOPPED_TAG = "MovementStopped";
    private static final String BACKPACK_ITEMS_TAG = "BackpackItems";
    private static final String BACKPACK_MAX_STORAGE_TAG = "BackpackMaxStorage";
    private static final String EXTRA_EQUIPMENT_ITEMS_TAG = "ExtraEquipmentItems";
    private static final String ACTIVITY_POINTS_TAG = "ActivityPoints";
    private static final String ACTIVITY_SCHEDULE_TAG = "ActivitySchedule";
    private static final int MAX_ACTIVITY_POINTS = 256;
    private static final int SCHEDULE_STEP_TICKS = 250;
    private static final int DEFAULT_MORNING_WORK_START = 2000;
    private static final int DEFAULT_NOON_WANDER_START = 6000;
    private static final int DEFAULT_AFTERNOON_WORK_START = 9000;
    private static final int DEFAULT_EVENING_PLAY_START = 12000;
    private static final int DEFAULT_NIGHT_SLEEP_START = 15000;
    private static final int EXTRA_SCHEDULE_SLOT_COUNT = 3;
    private static final String ACTION5_ENABLED_TAG = "Action5Enabled";
    private static final int DEFAULT_EXTRA_1_START = 17000;
    private static final int DEFAULT_EXTRA_2_START = 19000;
    private static final int DEFAULT_EXTRA_3_START = 21000;
    public static final int DEFAULT_ACTIVITY_RADIUS = 10;
    public static final int MIN_ACTIVITY_RADIUS = 1;
    public static final int MAX_ACTIVITY_RADIUS = 32;
    private static final Predicate<LivingEntity> SURVIVAL_PLAYER_TARGET =
            entity -> entity instanceof Player player && !player.isCreative() && !player.isSpectator();

    private final RangedBowAttackGoal<ReimuGoodNpcEntity> bowGoal =
            new RangedBowAttackGoal<>(this, 1.0D, 20, 15.0F);
    private final MeleeAttackGoal meleeGoal = new MeleeAttackGoal(this, 1.2D, false);
    private final SimpleContainer backpackContainer = new SimpleContainer(27);
    private final SimpleContainer extraEquipmentContainer = new SimpleContainer(6);
    private final List<ActivityPoint> activityPoints = new ArrayList<>();
    private int backpackMaxStorage = 0;
    private int morningWorkStart = DEFAULT_MORNING_WORK_START;
    private int noonWanderStart = DEFAULT_NOON_WANDER_START;
    private int afternoonWorkStart = DEFAULT_AFTERNOON_WORK_START;
    private int eveningPlayStart = DEFAULT_EVENING_PLAY_START;
    private int nightSleepStart = DEFAULT_NIGHT_SLEEP_START;
    private boolean action5Enabled = false;
    private final int[] extraScheduleStartTicks = new int[]{DEFAULT_EXTRA_1_START, DEFAULT_EXTRA_2_START, DEFAULT_EXTRA_3_START};
    private final int[] extraScheduleActivityIds = new int[]{ActivityType.ACTION6.id(), ActivityType.ACTION7.id(), ActivityType.ACTION8.id()};
    private final boolean[] extraScheduleEnabled = new boolean[]{false, false, false};

    public ReimuGoodNpcEntity(EntityType<? extends ReimuGoodNpcEntity> entityType, Level level) {
        super(entityType, level);
        this.applyDefaultIdentity();
        this.syncBackpackLockPlaceholders();
        this.reassessWeaponGoal();
    }

    private void applyDefaultIdentity() {
        this.setPersistenceRequired();
        this.setCanPickUpLoot(true);
        this.setCustomName(this.getDefaultDisplayName().copy());
        this.setCustomNameVisible(true);
    }

    protected Component getDefaultDisplayName() {
        return Component.translatable(this.getType().getDescriptionId());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(MOVEMENT_STOPPED, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(3, new SeekAndPickupItemsGoal(this, 1.05D, 12.0D));
        this.goalSelector.addGoal(4, new PatrolRouteGoal(this, 0.95D));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(
                2,
                new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, SURVIVAL_PLAYER_TARGET)
        );
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Zombie.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, AbstractSkeleton.class, true));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.32D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D);
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return SoundEvents.VILLAGER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource damageSource) {
        return SoundEvents.VILLAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.VILLAGER_DEATH;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack heldItem = player.getItemInHand(hand);
        if (player.isShiftKeyDown() && heldItem.isEmpty() && player.getOffhandItem().isEmpty()) {
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.openMenu(
                        new SimpleMenuProvider(
                                (containerId, playerInventory, ignored) -> new NpcInventoryMenu(containerId, playerInventory, this),
                                Component.translatable("container." + eiyahanabimachiservernpc.MODID + ".npc_inventory")
                        ),
                        buffer -> buffer.writeVarInt(this.getId())
                );
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        if (heldItem.is(eiyahanabimachiservernpc.STOP_MOVEMENT_TOOL.get())
                || heldItem.is(eiyahanabimachiservernpc.NPC_INVENTORY_TOOL.get())
                || heldItem.is(eiyahanabimachiservernpc.NPC_PATROL_WAND.get())) {
            return InteractionResult.PASS;
        }

        this.playSound(SoundEvents.VILLAGER_YES, 1.0F, 1.0F);
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    @Override
    public void performRangedAttack(LivingEntity target, float pullProgress) {
        ItemStack weapon = this.getItemInHand(ProjectileUtil.getWeaponHoldingHand(this, item -> item instanceof BowItem));
        if (!(weapon.getItem() instanceof BowItem)) {
            return;
        }
        // Pick projectile type from the 6-slot equipment area (row-major priority), without consuming it.
        ItemStack projectile = this.resolvePreferredArrowProjectile(weapon);
        AbstractArrow arrow = ProjectileUtil.getMobArrow(this, projectile, pullProgress, weapon);
        Item weaponItem = weapon.getItem();
        if (weaponItem instanceof ProjectileWeaponItem projectileWeaponItem) {
            arrow = projectileWeaponItem.customArrow(arrow, projectile, weapon);
        }

        double dx = target.getX() - this.getX();
        double dy = target.getY(1.0D / 3.0D) - arrow.getY();
        double dz = target.getZ() - this.getZ();
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
        arrow.shoot(dx, dy + horizontalDistance * 0.2D, dz, 1.6F, (float) (14 - this.level().getDifficulty().getId() * 4));

        this.playSound(SoundEvents.SKELETON_SHOOT, 1.0F, 1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
        this.level().addFreshEntity(arrow);
    }

    @Override
    public boolean canFireProjectileWeapon(ProjectileWeaponItem weapon) {
        return weapon instanceof BowItem;
    }

    private ItemStack resolvePreferredArrowProjectile(ItemStack weapon) {
        Item weaponItem = weapon.getItem();
        if (weaponItem instanceof ProjectileWeaponItem projectileWeaponItem) {
            Predicate<ItemStack> supportedProjectiles = projectileWeaponItem.getSupportedHeldProjectiles();
            for (int i = 0; i < this.extraEquipmentContainer.getContainerSize(); i++) {
                ItemStack candidate = this.extraEquipmentContainer.getItem(i);
                if (!candidate.isEmpty() && supportedProjectiles.test(candidate)) {
                    return candidate.copyWithCount(1);
                }
            }
        }
        return new ItemStack(Items.ARROW);
    }

    @Override
    public boolean wantsToPickUp(ItemStack stack) {
        if (!this.isValidCombatOrArmorItem(stack)) {
            return false;
        }
        return super.wantsToPickUp(stack);
    }

    @Override
    public boolean canTakeItem(ItemStack stack) {
        if (!this.isValidCombatOrArmorItem(stack)) {
            return false;
        }
        return super.canTakeItem(stack);
    }

    public boolean isMovementStopped() {
        return this.entityData.get(MOVEMENT_STOPPED);
    }

    public void setMovementStopped(boolean stopped) {
        this.entityData.set(MOVEMENT_STOPPED, stopped);
        this.setNoAi(stopped);
        this.setNoGravity(stopped);

        if (stopped) {
            this.setTarget(null);
            this.getNavigation().stop();
            this.setDeltaMovement(Vec3.ZERO);
            this.xxa = 0.0F;
            this.zza = 0.0F;
        } else if (this.level() != null && !this.level().isClientSide) {
            this.reassessWeaponGoal();
        }
    }

    public SimpleContainer getBackpackContainer() {
        return this.backpackContainer;
    }

    public int getBackpackMaxStorage() {
        return this.backpackMaxStorage;
    }

    public void setBackpackMaxStorage(int maxStorage) {
        this.backpackMaxStorage = Mth.clamp(maxStorage, 0, BACKPACK_MAX_UNLOCKABLE_SLOTS);
        this.syncBackpackLockPlaceholders();
    }

    public void addBackpackMaxStorage(int amount) {
        if (amount <= 0) {
            return;
        }
        this.setBackpackMaxStorage(this.backpackMaxStorage + amount);
    }

    public boolean isBackpackSlotUnlocked(int backpackSlotIndex) {
        if (backpackSlotIndex < 0 || backpackSlotIndex >= this.backpackContainer.getContainerSize()) {
            return false;
        }
        int unlockedSlots = BACKPACK_BASE_USABLE_SLOTS + this.backpackMaxStorage;
        return backpackSlotIndex < unlockedSlots;
    }

    public void syncBackpackLockPlaceholders() {
        for (int i = 0; i < this.backpackContainer.getContainerSize(); i++) {
            ItemStack current = this.backpackContainer.getItem(i);
            if (this.isBackpackSlotUnlocked(i)) {
                if (isLockedPlaceholder(current)) {
                    this.backpackContainer.setItem(i, ItemStack.EMPTY);
                }
            } else if (!isLockedPlaceholder(current)) {
                this.backpackContainer.setItem(i, createLockedPlaceholder());
            }
        }
    }

    public SimpleContainer getExtraEquipmentContainer() {
        return this.extraEquipmentContainer;
    }

    public void setOrUpdateActivityPoint(BlockPos center, ActivityType activityType) {
        this.setOrUpdateActivityPoint(center, activityType, DEFAULT_ACTIVITY_RADIUS);
    }

    public void setOrUpdateActivityPoint(BlockPos center, ActivityType activityType, int radius) {
        if (center == null || activityType == null) {
            return;
        }
        int clampedRadius = Mth.clamp(radius, MIN_ACTIVITY_RADIUS, MAX_ACTIVITY_RADIUS);
        BlockPos immutableCenter = center.immutable();
        for (int i = 0; i < this.activityPoints.size(); i++) {
            ActivityPoint existing = this.activityPoints.get(i);
            if (existing.center().equals(immutableCenter)) {
                this.activityPoints.set(i, new ActivityPoint(immutableCenter, activityType, clampedRadius));
                return;
            }
        }
        if (this.activityPoints.size() >= MAX_ACTIVITY_POINTS) {
            this.activityPoints.remove(0);
        }
        this.activityPoints.add(new ActivityPoint(immutableCenter, activityType, clampedRadius));
    }

    public int getActivityPointCount() {
        return this.activityPoints.size();
    }

    public boolean removeActivityPoint(BlockPos center) {
        if (center == null) {
            return false;
        }
        BlockPos immutableCenter = center.immutable();
        return this.activityPoints.removeIf(point -> point.center().equals(immutableCenter));
    }

    public List<BlockPos> getActivityPointPositions() {
        return this.activityPoints.stream()
                .map(ActivityPoint::center)
                .toList();
    }

    public List<ActivityPointInfo> getActivityPointInfos() {
        return this.activityPoints.stream()
                .map(point -> new ActivityPointInfo(point.center(), point.activityType(), point.radius()))
                .toList();
    }

    public int getActivityPointRadius(BlockPos center) {
        if (center == null) {
            return DEFAULT_ACTIVITY_RADIUS;
        }
        BlockPos immutableCenter = center.immutable();
        for (ActivityPoint point : this.activityPoints) {
            if (point.center().equals(immutableCenter)) {
                return point.radius();
            }
        }
        return DEFAULT_ACTIVITY_RADIUS;
    }

    public ActivityType getScheduledActivity(int timeOfDay) {
        return this.getCurrentScheduleWindow(timeOfDay).activityType();
    }

    public ScheduleWindow getCurrentScheduleWindow(int timeOfDay) {
        List<ScheduleEntry> entries = this.buildScheduleEntries();
        if (entries.isEmpty()) {
            return new ScheduleWindow(0, 24000, ActivityType.SLEEP);
        }

        int time = normalizePatrolTime(timeOfDay);
        int activeIndex = entries.size() - 1;
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).startTick() <= time) {
                activeIndex = i;
            } else {
                break;
            }
        }

        ScheduleEntry active = entries.get(activeIndex);
        int nextIndex = (activeIndex + 1) % entries.size();
        int endTick = entries.get(nextIndex).startTick();
        if (endTick == active.startTick()) {
            endTick = (active.startTick() + 1) % 24000;
        }
        return new ScheduleWindow(active.startTick(), endTick, active.activityType());
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

    public int getExtraScheduleSlotCount() {
        return EXTRA_SCHEDULE_SLOT_COUNT;
    }

    public boolean isAction5Enabled() {
        return this.action5Enabled;
    }

    public void setAction5Enabled(boolean enabled) {
        this.action5Enabled = enabled;
        if (!enabled) {
            for (int i = 0; i < EXTRA_SCHEDULE_SLOT_COUNT; i++) {
                this.extraScheduleEnabled[i] = false;
            }
        }
        this.normalizeExtraScheduleEnabledPrefix();
    }

    public boolean isExtraScheduleEnabled(int slot) {
        return this.isValidExtraSlot(slot) && this.extraScheduleEnabled[slot];
    }

    public int getExtraScheduleStartTick(int slot) {
        if (!this.isValidExtraSlot(slot)) {
            return DEFAULT_EXTRA_1_START;
        }
        return this.extraScheduleStartTicks[slot];
    }

    public int getExtraScheduleActivityId(int slot) {
        if (!this.isValidExtraSlot(slot)) {
            return ActivityType.ACTION6.id();
        }
        return this.extraScheduleActivityIds[slot];
    }

    public void setExtraScheduleSlot(int slot, boolean enabled, int startTick, int activityId) {
        if (!this.isValidExtraSlot(slot)) {
            return;
        }
        if (!this.action5Enabled) {
            enabled = false;
        }
        if (enabled && slot > 0 && !this.extraScheduleEnabled[slot - 1]) {
            enabled = false;
        }
        this.extraScheduleEnabled[slot] = enabled;
        this.extraScheduleStartTicks[slot] = normalizePatrolTime(startTick);
        this.extraScheduleActivityIds[slot] = this.normalizeExtraActivityId(activityId);
        this.normalizeExtraScheduleEnabledPrefix();
    }

    public void setScheduleBoundaries(int morningWork, int noonWander, int afternoonWork, int eveningPlay, int nightSleep) {
        int morning = normalizePatrolTime(morningWork);
        morning = Mth.clamp(morning, 0, 23999 - 4 * SCHEDULE_STEP_TICKS);
        int noon = normalizePatrolTime(noonWander);
        noon = Mth.clamp(noon, morning + SCHEDULE_STEP_TICKS, 23999 - 3 * SCHEDULE_STEP_TICKS);
        int afternoon = normalizePatrolTime(afternoonWork);
        afternoon = Mth.clamp(afternoon, noon + SCHEDULE_STEP_TICKS, 23999 - 2 * SCHEDULE_STEP_TICKS);
        int evening = normalizePatrolTime(eveningPlay);
        evening = Mth.clamp(evening, afternoon + SCHEDULE_STEP_TICKS, 23999 - SCHEDULE_STEP_TICKS);
        int night = normalizePatrolTime(nightSleep);
        night = Mth.clamp(night, evening + SCHEDULE_STEP_TICKS, 23999);

        this.morningWorkStart = morning;
        this.noonWanderStart = noon;
        this.afternoonWorkStart = afternoon;
        this.eveningPlayStart = evening;
        this.nightSleepStart = night;
    }

    private List<ScheduleEntry> buildScheduleEntries() {
        List<ScheduleEntry> entries = new ArrayList<>();
        entries.add(new ScheduleEntry(this.morningWorkStart, ActivityType.SLEEP));
        entries.add(new ScheduleEntry(this.noonWanderStart, ActivityType.PLAY));
        entries.add(new ScheduleEntry(this.afternoonWorkStart, ActivityType.WANDER));
        entries.add(new ScheduleEntry(this.eveningPlayStart, ActivityType.WORK));
        if (this.action5Enabled) {
            entries.add(new ScheduleEntry(this.nightSleepStart, ActivityType.ACTION5));
        }
        for (int i = 0; i < EXTRA_SCHEDULE_SLOT_COUNT; i++) {
            if (!this.extraScheduleEnabled[i]) {
                continue;
            }
            entries.add(new ScheduleEntry(
                    this.extraScheduleStartTicks[i],
                    ActivityType.byId(this.normalizeExtraActivityId(this.extraScheduleActivityIds[i]))
            ));
        }
        entries.sort((a, b) -> {
            if (a.startTick() != b.startTick()) {
                return Integer.compare(a.startTick(), b.startTick());
            }
            return Integer.compare(a.activityType().id(), b.activityType().id());
        });
        return entries;
    }

    private int normalizeExtraActivityId(int activityId) {
        int clamped = Mth.clamp(activityId, ActivityType.ACTION6.id(), ActivityType.ACTION8.id());
        return ActivityType.byId(clamped).id();
    }

    private boolean isValidExtraSlot(int slot) {
        return slot >= 0 && slot < EXTRA_SCHEDULE_SLOT_COUNT;
    }

    public void reassessWeaponGoal() {
        if (this.level() == null || this.level().isClientSide) {
            return;
        }

        this.goalSelector.removeGoal(this.meleeGoal);
        this.goalSelector.removeGoal(this.bowGoal);

        ItemStack mainHand = this.getItemInHand(ProjectileUtil.getWeaponHoldingHand(this, item -> item instanceof BowItem));
        if (mainHand.getItem() instanceof BowItem) {
            int interval = this.level().getDifficulty() == Difficulty.HARD ? 20 : 40;
            this.bowGoal.setMinAttackInterval(interval);
            this.goalSelector.addGoal(2, this.bowGoal);
        } else {
            this.goalSelector.addGoal(2, this.meleeGoal);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.reassessWeaponGoal();
        this.setMovementStopped(compound.getBoolean(MOVEMENT_STOPPED_TAG));
        this.backpackMaxStorage = Mth.clamp(compound.getInt(BACKPACK_MAX_STORAGE_TAG), 0, BACKPACK_MAX_UNLOCKABLE_SLOTS);
        if (compound.contains(BACKPACK_ITEMS_TAG, Tag.TAG_LIST)) {
            this.backpackContainer.fromTag(compound.getList(BACKPACK_ITEMS_TAG, Tag.TAG_COMPOUND), this.registryAccess());
        } else {
            this.backpackContainer.clearContent();
        }
        if (compound.contains(EXTRA_EQUIPMENT_ITEMS_TAG, Tag.TAG_LIST)) {
            this.extraEquipmentContainer.fromTag(compound.getList(EXTRA_EQUIPMENT_ITEMS_TAG, Tag.TAG_COMPOUND), this.registryAccess());
        } else {
            this.extraEquipmentContainer.clearContent();
        }
        this.activityPoints.clear();
        if (compound.contains(ACTIVITY_POINTS_TAG, Tag.TAG_LIST)) {
            ListTag activityPointTags = compound.getList(ACTIVITY_POINTS_TAG, Tag.TAG_COMPOUND);
            for (int i = 0; i < activityPointTags.size() && this.activityPoints.size() < MAX_ACTIVITY_POINTS; i++) {
                CompoundTag activityPointTag = activityPointTags.getCompound(i);
                ActivityPoint activityPoint = ActivityPoint.fromTag(activityPointTag);
                if (activityPoint != null) {
                    this.activityPoints.add(activityPoint);
                }
            }
        }
        if (compound.contains(ACTIVITY_SCHEDULE_TAG, Tag.TAG_COMPOUND)) {
            CompoundTag scheduleTag = compound.getCompound(ACTIVITY_SCHEDULE_TAG);
            this.setScheduleBoundaries(
                    scheduleTag.getInt("MorningWorkStart"),
                    scheduleTag.getInt("NoonWanderStart"),
                    scheduleTag.getInt("AfternoonWorkStart"),
                    scheduleTag.getInt("EveningPlayStart"),
                    scheduleTag.getInt("NightSleepStart")
            );
            boolean action5 = scheduleTag.contains(ACTION5_ENABLED_TAG, Tag.TAG_BYTE)
                    ? scheduleTag.getBoolean(ACTION5_ENABLED_TAG)
                    : true;
            this.setAction5Enabled(action5);
            for (int i = 0; i < EXTRA_SCHEDULE_SLOT_COUNT; i++) {
                String enabledKey = "ExtraSlot" + i + "Enabled";
                String startKey = "ExtraSlot" + i + "Start";
                String activityKey = "ExtraSlot" + i + "Activity";
                boolean enabled = scheduleTag.contains(enabledKey, Tag.TAG_BYTE)
                        ? scheduleTag.getBoolean(enabledKey)
                        : true;
                int start = scheduleTag.contains(startKey, Tag.TAG_INT)
                        ? scheduleTag.getInt(startKey)
                        : (i == 0 ? DEFAULT_EXTRA_1_START : i == 1 ? DEFAULT_EXTRA_2_START : DEFAULT_EXTRA_3_START);
                int activity = scheduleTag.contains(activityKey, Tag.TAG_INT)
                        ? scheduleTag.getInt(activityKey)
                        : (i == 0 ? ActivityType.ACTION6.id() : i == 1 ? ActivityType.ACTION7.id() : ActivityType.ACTION8.id());
                this.setExtraScheduleSlot(
                        i,
                        enabled,
                        start,
                        activity
                );
            }
        } else {
            this.setScheduleBoundaries(
                    DEFAULT_MORNING_WORK_START,
                    DEFAULT_NOON_WANDER_START,
                    DEFAULT_AFTERNOON_WORK_START,
                    DEFAULT_EVENING_PLAY_START,
                    DEFAULT_NIGHT_SLEEP_START
            );
            this.setAction5Enabled(false);
            this.setExtraScheduleSlot(0, false, DEFAULT_EXTRA_1_START, ActivityType.ACTION6.id());
            this.setExtraScheduleSlot(1, false, DEFAULT_EXTRA_2_START, ActivityType.ACTION7.id());
            this.setExtraScheduleSlot(2, false, DEFAULT_EXTRA_3_START, ActivityType.ACTION8.id());
        }
        this.syncBackpackLockPlaceholders();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean(MOVEMENT_STOPPED_TAG, this.isMovementStopped());
        compound.putInt(BACKPACK_MAX_STORAGE_TAG, this.backpackMaxStorage);
        compound.put(BACKPACK_ITEMS_TAG, this.backpackContainer.createTag(this.registryAccess()));
        compound.put(EXTRA_EQUIPMENT_ITEMS_TAG, this.extraEquipmentContainer.createTag(this.registryAccess()));
        ListTag activityPointTags = new ListTag();
        for (ActivityPoint activityPoint : this.activityPoints) {
            activityPointTags.add(activityPoint.toTag());
        }
        compound.put(ACTIVITY_POINTS_TAG, activityPointTags);
        CompoundTag scheduleTag = new CompoundTag();
        scheduleTag.putInt("MorningWorkStart", this.morningWorkStart);
        scheduleTag.putInt("NoonWanderStart", this.noonWanderStart);
        scheduleTag.putInt("AfternoonWorkStart", this.afternoonWorkStart);
        scheduleTag.putInt("EveningPlayStart", this.eveningPlayStart);
        scheduleTag.putInt("NightSleepStart", this.nightSleepStart);
        scheduleTag.putBoolean(ACTION5_ENABLED_TAG, this.action5Enabled);
        for (int i = 0; i < EXTRA_SCHEDULE_SLOT_COUNT; i++) {
            scheduleTag.putBoolean("ExtraSlot" + i + "Enabled", this.extraScheduleEnabled[i]);
            scheduleTag.putInt("ExtraSlot" + i + "Start", this.extraScheduleStartTicks[i]);
            scheduleTag.putInt("ExtraSlot" + i + "Activity", this.extraScheduleActivityIds[i]);
        }
        compound.put(ACTIVITY_SCHEDULE_TAG, scheduleTag);
    }

    private void normalizeExtraScheduleEnabledPrefix() {
        if (!this.action5Enabled) {
            for (int i = 0; i < EXTRA_SCHEDULE_SLOT_COUNT; i++) {
                this.extraScheduleEnabled[i] = false;
            }
            return;
        }
        for (int i = 1; i < EXTRA_SCHEDULE_SLOT_COUNT; i++) {
            if (this.extraScheduleEnabled[i] && !this.extraScheduleEnabled[i - 1]) {
                this.extraScheduleEnabled[i] = false;
            }
        }
    }

    @Override
    public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
        super.setItemSlot(slot, stack);
        if (this.level() != null && !this.level().isClientSide) {
            this.reassessWeaponGoal();
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.isMovementStopped()) {
            this.setDeltaMovement(Vec3.ZERO);
            this.xxa = 0.0F;
            this.zza = 0.0F;
            this.setTarget(null);
        }
    }

    @Override
    public void die(DamageSource damageSource) {
        if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel) {
            NpcPatrolWandItem.clearBindingsForNpc(serverLevel, this.getUUID(), this.getId());
        }
        super.die(damageSource);
    }

    private boolean isValidPickupItemEntity(ItemEntity itemEntity) {
        if (!itemEntity.isAlive() || itemEntity.hasPickUpDelay()) {
            return false;
        }
        ItemStack stack = itemEntity.getItem();
        return !stack.isEmpty() && this.wantsToPickUp(stack) && this.canTakeItem(stack);
    }

    private boolean isValidCombatOrArmorItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        EquipmentSlot slot = this.getEquipmentSlotForItem(stack);
        if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
            return true;
        }

        if (slot.getType() != EquipmentSlot.Type.HAND) {
            return false;
        }

        Item item = stack.getItem();
        return item instanceof ProjectileWeaponItem
                || item instanceof SwordItem
                || item instanceof AxeItem
                || stack.is(Items.TRIDENT)
                || stack.is(Items.SHIELD);
    }

    private static class SeekAndPickupItemsGoal extends Goal {
        private final ReimuGoodNpcEntity mob;
        private final double speedModifier;
        private final double searchRadius;
        @Nullable
        private ItemEntity targetItem;
        private int repathDelay;

        private SeekAndPickupItemsGoal(ReimuGoodNpcEntity mob, double speedModifier, double searchRadius) {
            this.mob = mob;
            this.speedModifier = speedModifier;
            this.searchRadius = searchRadius;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (!this.mob.canPickUpLoot() || this.mob.getTarget() != null) {
                return false;
            }

            List<ItemEntity> candidates = this.mob.level().getEntitiesOfClass(
                    ItemEntity.class,
                    this.mob.getBoundingBox().inflate(this.searchRadius),
                    this.mob::isValidPickupItemEntity
            );
            if (candidates.isEmpty()) {
                return false;
            }

            this.targetItem = candidates.stream()
                    .min(Comparator.comparingDouble(this.mob::distanceToSqr))
                    .orElse(null);
            return this.targetItem != null;
        }

        @Override
        public boolean canContinueToUse() {
            return this.targetItem != null
                    && this.targetItem.isAlive()
                    && !this.targetItem.hasPickUpDelay()
                    && this.mob.getTarget() == null
                    && this.mob.isValidPickupItemEntity(this.targetItem);
        }

        @Override
        public void start() {
            this.repathDelay = 0;
            if (this.targetItem != null) {
                this.mob.getNavigation().moveTo(this.targetItem, this.speedModifier);
            }
        }

        @Override
        public void stop() {
            this.targetItem = null;
            this.mob.getNavigation().stop();
        }

        @Override
        public void tick() {
            if (this.targetItem == null) {
                return;
            }

            if (--this.repathDelay <= 0) {
                this.repathDelay = 10;
                this.mob.getNavigation().moveTo(this.targetItem, this.speedModifier);
            }

            if (this.mob.distanceToSqr(this.targetItem) <= 2.0D) {
                this.mob.pickUpItem(this.targetItem);
            }
        }
    }

    private static int normalizePatrolTime(int value) {
        return Mth.positiveModulo(value, 24000);
    }

    private static class PatrolRouteGoal extends Goal {
        private static final double ARRIVAL_RADIUS = 1.0D;
        private final ReimuGoodNpcEntity mob;
        private final double speedModifier;
        @Nullable
        private ActivityType currentActivity;
        @Nullable
        private ActivityPoint currentPoint;
        @Nullable
        private BlockPos navigateTarget;
        private boolean pendingArrival;
        private int repathDelay;

        private PatrolRouteGoal(ReimuGoodNpcEntity mob, double speedModifier) {
            this.mob = mob;
            this.speedModifier = speedModifier;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (this.mob.isMovementStopped() || this.mob.getTarget() != null) {
                return false;
            }
            if (this.mob.activityPoints.isEmpty()) {
                this.currentActivity = null;
                this.currentPoint = null;
                this.navigateTarget = null;
                this.pendingArrival = false;
                this.mob.clearRestriction();
                return false;
            }

            if (this.currentPoint != null && !this.mob.activityPoints.contains(this.currentPoint)) {
                this.currentPoint = null;
                this.pendingArrival = false;
            }

            ActivityType activeActivity = this.mob.getScheduledActivity((int) this.mob.level().getDayTime());
            if (this.currentPoint == null || this.currentActivity != activeActivity) {
                this.currentActivity = activeActivity;
                this.currentPoint = this.pickPointForActivity(activeActivity);
                if (this.currentPoint == null) {
                    this.navigateTarget = null;
                    this.pendingArrival = false;
                    this.mob.clearRestriction();
                    return false;
                }
                this.pendingArrival = true;
                this.navigateTarget = this.currentPoint.center();
                if (this.isInsideArrivalRadius(this.currentPoint)) {
                    this.onArrivedAtPoint();
                    return false;
                }
                this.applyApproachRestriction(this.currentPoint);
                return true;
            }

            if (this.pendingArrival) {
                this.navigateTarget = this.currentPoint.center();
                if (this.isInsideArrivalRadius(this.currentPoint)) {
                    this.onArrivedAtPoint();
                    return false;
                }
                this.applyApproachRestriction(this.currentPoint);
                return true;
            }

            this.applyRestriction(this.currentPoint);
            this.navigateTarget = null;
            return false;
        }

        @Override
        public boolean canContinueToUse() {
            return !this.mob.isMovementStopped()
                    && this.mob.getTarget() == null
                    && this.navigateTarget != null
                    && this.currentPoint != null
                    && this.pendingArrival
                    && !this.isInsideArrivalRadius(this.currentPoint);
        }

        @Override
        public void start() {
            this.repathDelay = 0;
            if (this.navigateTarget != null) {
                this.mob.getNavigation().moveTo(
                        this.navigateTarget.getX() + 0.5D,
                        this.navigateTarget.getY(),
                        this.navigateTarget.getZ() + 0.5D,
                        this.speedModifier
                );
            }
        }

        @Override
        public void stop() {
            this.navigateTarget = null;
            this.repathDelay = 0;
        }

        @Override
        public void tick() {
            if (this.currentPoint == null || this.navigateTarget == null) {
                this.mob.getNavigation().stop();
                return;
            }
            if (this.isInsideArrivalRadius(this.currentPoint)) {
                this.onArrivedAtPoint();
                return;
            }
            if (--this.repathDelay <= 0) {
                this.repathDelay = 20;
                this.mob.getNavigation().moveTo(
                        this.navigateTarget.getX() + 0.5D,
                        this.navigateTarget.getY(),
                        this.navigateTarget.getZ() + 0.5D,
                        this.speedModifier
                );
            }
        }

        private void onArrivedAtPoint() {
            this.pendingArrival = false;
            this.navigateTarget = null;
            if (this.currentPoint != null) {
                this.applyRestriction(this.currentPoint);
            }
            this.mob.getNavigation().stop();
        }

        @Nullable
        private ActivityPoint pickPointForActivity(ActivityType activityType) {
            List<ActivityPoint> candidatePoints = this.mob.activityPoints.stream()
                    .filter(point -> point.activityType() == activityType)
                    .toList();
            if (candidatePoints.isEmpty()) {
                candidatePoints = this.mob.activityPoints;
            }
            if (candidatePoints.isEmpty()) {
                return null;
            }
            return candidatePoints.get(this.mob.getRandom().nextInt(candidatePoints.size()));
        }

        private void applyRestriction(ActivityPoint point) {
            this.mob.restrictTo(point.center(), point.radius());
        }

        private void applyApproachRestriction(ActivityPoint point) {
            this.mob.restrictTo(point.center(), (int) Math.ceil(ARRIVAL_RADIUS));
        }

        private boolean isInsideArrivalRadius(ActivityPoint point) {
            double radius = ARRIVAL_RADIUS;
            return this.mob.distanceToSqr(Vec3.atCenterOf(point.center())) <= radius * radius;
        }
    }

    public enum ActivityType {
        SLEEP(0, "action1"),
        PLAY(1, "action2"),
        WANDER(2, "action3"),
        WORK(3, "action4"),
        ACTION5(4, "action5"),
        ACTION6(5, "action6"),
        ACTION7(6, "action7"),
        ACTION8(7, "action8");

        private final int id;
        private final String key;

        ActivityType(int id, String key) {
            this.id = id;
            this.key = key;
        }

        public int id() {
            return this.id;
        }

        public String key() {
            return this.key;
        }

        public static ActivityType byId(int id) {
            for (ActivityType type : values()) {
                if (type.id == id) {
                    return type;
                }
            }
            return WORK;
        }
    }

    private record ScheduleEntry(int startTick, ActivityType activityType) {
    }

    public record ScheduleWindow(int startTick, int endTick, ActivityType activityType) {
    }

    public record ActivityPointInfo(BlockPos center, ActivityType activityType, int radius) {
    }

    private record ActivityPoint(BlockPos center, ActivityType activityType, int radius) {
        private static final String CENTER_TAG = "Center";
        private static final String TYPE_TAG = "Type";
        private static final String RADIUS_TAG = "Radius";

        private CompoundTag toTag() {
            CompoundTag tag = new CompoundTag();
            tag.putLong(CENTER_TAG, this.center.asLong());
            tag.putInt(TYPE_TAG, this.activityType.id());
            tag.putInt(RADIUS_TAG, this.radius);
            return tag;
        }

        @Nullable
        private static ActivityPoint fromTag(CompoundTag tag) {
            if (!tag.contains(CENTER_TAG, Tag.TAG_LONG)) {
                return null;
            }

            BlockPos center = BlockPos.of(tag.getLong(CENTER_TAG));
            ActivityType type = ActivityType.byId(tag.getInt(TYPE_TAG));
            int radius = Mth.clamp(tag.getInt(RADIUS_TAG), MIN_ACTIVITY_RADIUS, MAX_ACTIVITY_RADIUS);
            if (!tag.contains(RADIUS_TAG, Tag.TAG_INT)) {
                radius = DEFAULT_ACTIVITY_RADIUS;
            }
            return new ActivityPoint(center, type, radius);
        }
    }

    private static ItemStack createLockedPlaceholder() {
        ItemStack placeholder = new ItemStack(Items.BLACK_STAINED_GLASS_PANE);
        placeholder.set(DataComponents.CUSTOM_NAME, Component.translatable("gui." + eiyahanabimachiservernpc.MODID + ".locked_slot"));
        return placeholder;
    }

    private static boolean isLockedPlaceholder(ItemStack stack) {
        return stack.is(Items.BLACK_STAINED_GLASS_PANE) && stack.has(DataComponents.CUSTOM_NAME);
    }
}
