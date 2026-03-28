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
import net.minecraft.core.Direction;
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
import net.minecraft.world.entity.Entity;
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
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
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
    private static final String SCHEDULE_CONFIGURED_TAG = "ScheduleConfigured";
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
    private static final int SLEEP_FALLBACK_SEARCH_RADIUS = 16;
    private static final int SLEEP_FALLBACK_VERTICAL_RANGE = 4;
    private static final int SLEEP_FALLBACK_CHECK_COOLDOWN = 20;
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
    private boolean scheduleConfigured = false;
    private boolean action5Enabled = false;
    private final int[] extraScheduleStartTicks = new int[]{DEFAULT_EXTRA_1_START, DEFAULT_EXTRA_2_START, DEFAULT_EXTRA_3_START};
    private final int[] extraScheduleActivityIds = new int[]{ActivityType.ACTION6.id(), ActivityType.ACTION7.id(), ActivityType.ACTION8.id()};
    private final boolean[] extraScheduleEnabled = new boolean[]{false, false, false};
    @Nullable
    private BlockPos sleepFallbackBed;
    private int sleepFallbackCooldown;

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
            return new ScheduleWindow(0, 24000, ActivityType.WORK);
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

    public boolean isInLastScheduleWindow(int timeOfDay) {
        List<ScheduleEntry> entries = this.buildScheduleEntries();
        if (entries.isEmpty()) {
            return false;
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
        return activeIndex == entries.size() - 1;
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

    public boolean hasScheduleConfigured() {
        return this.scheduleConfigured;
    }

    public void setScheduleConfigured(boolean configured) {
        this.scheduleConfigured = configured;
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
        if (!this.scheduleConfigured) {
            return List.of();
        }
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
            this.scheduleConfigured = scheduleTag.contains(SCHEDULE_CONFIGURED_TAG, Tag.TAG_BYTE)
                    ? scheduleTag.getBoolean(SCHEDULE_CONFIGURED_TAG)
                    : true;
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
            this.scheduleConfigured = false;
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
        scheduleTag.putBoolean(SCHEDULE_CONFIGURED_TAG, this.scheduleConfigured);
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
        if (!this.level().isClientSide && this.isSleeping()) {
            boolean inLastWindow = this.hasScheduleConfigured()
                    && this.isInLastScheduleWindow((int) this.level().getDayTime());
            if (!inLastWindow || this.isMovementStopped()) {
                this.stopSleeping();
                this.sleepFallbackBed = null;
                this.sleepFallbackCooldown = 0;
            }
        }
        if (this.isSleeping()) {
            this.setDeltaMovement(Vec3.ZERO);
            this.getNavigation().stop();
            this.getSleepingPos().ifPresent(bedPos -> {
                BlockState bedState = this.level().getBlockState(bedPos);
                if (!(bedState.getBlock() instanceof BedBlock)) {
                    this.stopSleeping();
                    return;
                }
                Vec3 bedCenter = Vec3.atBottomCenterOf(bedPos).add(0.0D, 0.5625D, 0.0D);
                if (this.distanceToSqr(bedCenter) > 0.04D) {
                    this.setPos(bedCenter.x, bedCenter.y, bedCenter.z);
                }
            });
        }
        if (!this.level().isClientSide) {
            this.updateSleepFallbackState();
        }
        if (this.isMovementStopped()) {
            this.setDeltaMovement(Vec3.ZERO);
            this.xxa = 0.0F;
            this.zza = 0.0F;
            this.setTarget(null);
        }
    }

    private void updateSleepFallbackState() {
        this.sleepFallbackBed = null;
        this.sleepFallbackCooldown = 0;
    }

    private void forceSleepAt(BlockPos bedPos) {
        BlockPos headPos = this.resolveBedHeadPos(bedPos);
        if (headPos == null) {
            return;
        }
        Vec3 bedCenter = Vec3.atBottomCenterOf(headPos).add(0.0D, 0.5625D, 0.0D);
        this.getNavigation().stop();
        this.setDeltaMovement(Vec3.ZERO);
        this.moveTo(bedCenter.x, bedCenter.y, bedCenter.z, this.getYRot(), this.getXRot());
        this.startSleeping(headPos);
        this.sleepFallbackBed = headPos;
    }

    @Nullable
    private BlockPos findNearestBedAround(BlockPos center, int horizontalRadius, int verticalRadius) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;

        int minY = Math.max(this.level().getMinBuildHeight(), center.getY() - verticalRadius);
        int maxY = Math.min(this.level().getMaxBuildHeight() - 1, center.getY() + verticalRadius);
        for (int y = minY; y <= maxY; y++) {
            for (int x = center.getX() - horizontalRadius; x <= center.getX() + horizontalRadius; x++) {
                for (int z = center.getZ() - horizontalRadius; z <= center.getZ() + horizontalRadius; z++) {
                    cursor.set(x, y, z);
                    BlockPos headPos = this.resolveBedHeadPos(cursor);
                    if (headPos == null || !this.isUsableBed(headPos)) {
                        continue;
                    }
                    double dist = this.distanceToSqr(Vec3.atCenterOf(headPos));
                    if (dist < bestDist) {
                        bestDist = dist;
                        best = headPos.immutable();
                    }
                }
            }
        }
        return best;
    }

    @Nullable
    private BlockPos resolveBedHeadPos(BlockPos bedPos) {
        BlockState state = this.level().getBlockState(bedPos);
        if (!(state.getBlock() instanceof BedBlock)) {
            return null;
        }
        if (!state.hasProperty(BedBlock.PART) || !state.hasProperty(BedBlock.FACING)) {
            return bedPos.immutable();
        }
        if (state.getValue(BedBlock.PART) == BedPart.HEAD) {
            return bedPos.immutable();
        }
        return bedPos.relative(state.getValue(BedBlock.FACING)).immutable();
    }

    private boolean isUsableBed(BlockPos bedPos) {
        BlockPos headPos = this.resolveBedHeadPos(bedPos);
        if (headPos == null) {
            return false;
        }
        BlockState state = this.level().getBlockState(headPos);
        if (!(state.getBlock() instanceof BedBlock)) {
            return false;
        }
        return !state.hasProperty(BedBlock.OCCUPIED)
                || !state.getValue(BedBlock.OCCUPIED)
                || this.isSleeping();
    }

    @Override
    public boolean isPushable() {
        return !this.isSleeping() && super.isPushable();
    }

    @Override
    protected void doPush(Entity entity) {
        if (this.isSleeping()) {
            return;
        }
        super.doPush(entity);
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
            if (this.mob.isInLastScheduleWindow((int) this.mob.level().getDayTime())) {
                return false;
            }
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
            if (this.mob.isInLastScheduleWindow((int) this.mob.level().getDayTime())) {
                return false;
            }
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
        private static final double ARRIVAL_RADIUS = 3.0D;
        private static final int BED_SEARCH_RADIUS = 16;
        private static final int BED_SEARCH_VERTICAL_RANGE = 4;
        private static final int BED_SCAN_INTERVAL = 20;
        private static final int REPATH_INTERVAL = 20;
        private final ReimuGoodNpcEntity mob;
        private final double speedModifier;
        @Nullable
        private ActivityType currentActivity;
        @Nullable
        private ActivityPoint currentPoint;
        @Nullable
        private BlockPos navigateTarget;
        @Nullable
        private BlockPos sleepBedTarget;
        private boolean navigatingToPoint;
        private boolean seekingBed;
        private int currentWindowStartTick = -1;
        private int repathDelay;
        private int bedScanDelay;

        private PatrolRouteGoal(ReimuGoodNpcEntity mob, double speedModifier) {
            this.mob = mob;
            this.speedModifier = speedModifier;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (this.mob.isMovementStopped()) {
                if (this.mob.isSleeping()) {
                    this.mob.stopSleeping();
                }
                this.resetRuntimeState();
                return false;
            }

            if (!this.mob.hasScheduleConfigured() || this.mob.activityPoints.isEmpty()) {
                if (this.mob.isSleeping()) {
                    this.mob.stopSleeping();
                }
                this.resetAllState();
                this.mob.getNavigation().stop();
                this.mob.clearRestriction();
                return false;
            }

            if (this.currentPoint != null && !this.mob.activityPoints.contains(this.currentPoint)) {
                this.currentPoint = null;
                this.navigatingToPoint = false;
                this.clearBedState();
            }

            int now = (int) this.mob.level().getDayTime();
            ScheduleWindow activeWindow = this.mob.getCurrentScheduleWindow(now);
            ActivityType activeActivity = activeWindow.activityType();
            int activeWindowStartTick = activeWindow.startTick();
            boolean switchedWindow = this.currentWindowStartTick != activeWindowStartTick;

            if (this.mob.getTarget() != null) {
                if (this.shouldSeekBedNow(now)) {
                    this.mob.setTarget(null);
                } else {
                    return false;
                }
            }

            if (this.currentPoint == null || this.currentActivity != activeActivity || switchedWindow) {
                if (this.mob.isSleeping()) {
                    this.mob.stopSleeping();
                }
                this.currentActivity = activeActivity;
                this.currentWindowStartTick = activeWindowStartTick;
                this.currentPoint = this.pickPointForActivity(activeActivity);
                if (this.currentPoint == null) {
                    this.resetRuntimeState();
                    this.mob.getNavigation().stop();
                    this.mob.clearRestriction();
                    return false;
                }

                this.navigatingToPoint = true;
                this.clearBedState();
                this.navigateTarget = this.currentPoint.center();
                this.applyApproachRestriction(this.currentPoint);

                if (this.isInsideArrivalRadius(this.currentPoint)) {
                    this.onArrivedAtPoint(now);
                    if (this.seekingBed) {
                        this.prepareBedNavigation(this.currentPoint);
                        return !this.mob.isSleeping();
                    }
                    return false;
                }
                return true;
            }

            if (this.navigatingToPoint) {
                this.navigateTarget = this.currentPoint.center();
                if (this.isInsideArrivalRadius(this.currentPoint)) {
                    this.onArrivedAtPoint(now);
                    if (this.seekingBed) {
                        this.prepareBedNavigation(this.currentPoint);
                        return !this.mob.isSleeping();
                    }
                    return false;
                }
                this.applyApproachRestriction(this.currentPoint);
                return true;
            }

            if (this.seekingBed) {
                if (!this.shouldSeekBedNow(now)) {
                    this.clearBedState();
                    return false;
                }
                if (this.mob.isSleeping()) {
                    return false;
                }
                this.prepareBedNavigation(this.currentPoint);
                return !this.mob.isSleeping();
            }

            this.applyRestriction(this.currentPoint);
            return false;
        }

        @Override
        public boolean canContinueToUse() {
            if (this.currentPoint == null || this.mob.isMovementStopped()) {
                return false;
            }

            int now = (int) this.mob.level().getDayTime();
            ScheduleWindow activeWindow = this.mob.getCurrentScheduleWindow(now);
            if (this.currentActivity != activeWindow.activityType()
                    || this.currentWindowStartTick != activeWindow.startTick()) {
                return false;
            }

            if (this.mob.getTarget() != null) {
                if (this.shouldSeekBedNow(now)) {
                    this.mob.setTarget(null);
                } else {
                    return false;
                }
            }

            if (this.navigatingToPoint) {
                return this.navigateTarget != null && !this.isInsideArrivalRadius(this.currentPoint);
            }

            return this.seekingBed && !this.mob.isSleeping() && this.shouldSeekBedNow(now);
        }

        @Override
        public void start() {
            this.repathDelay = 0;
            this.bedScanDelay = 0;
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
            if (this.currentPoint == null) {
                this.mob.getNavigation().stop();
                return;
            }

            if (this.navigatingToPoint) {
                if (this.navigateTarget == null) {
                    this.navigateTarget = this.currentPoint.center();
                }
                if (this.isInsideArrivalRadius(this.currentPoint)) {
                    this.onArrivedAtPoint((int) this.mob.level().getDayTime());
                    return;
                }
                if (--this.repathDelay <= 0) {
                    this.repathDelay = REPATH_INTERVAL;
                    this.mob.getNavigation().moveTo(
                            this.navigateTarget.getX() + 0.5D,
                            this.navigateTarget.getY(),
                            this.navigateTarget.getZ() + 0.5D,
                            this.speedModifier
                    );
                }
                return;
            }

            if (this.seekingBed) {
                int now = (int) this.mob.level().getDayTime();
                if (!this.shouldSeekBedNow(now)) {
                    this.clearBedState();
                    this.mob.getNavigation().stop();
                    return;
                }
                if (this.mob.isSleeping()) {
                    this.clearBedState();
                    this.mob.getNavigation().stop();
                    return;
                }

                this.prepareBedNavigation(this.currentPoint);
                if (this.mob.isSleeping()) {
                    this.clearBedState();
                    this.mob.getNavigation().stop();
                    return;
                }

                if (this.navigateTarget == null) {
                    this.mob.getNavigation().stop();
                    return;
                }

                if (--this.repathDelay <= 0) {
                    this.repathDelay = REPATH_INTERVAL;
                    this.mob.getNavigation().moveTo(
                            this.navigateTarget.getX() + 0.5D,
                            this.navigateTarget.getY(),
                            this.navigateTarget.getZ() + 0.5D,
                            this.speedModifier
                    );
                }
                return;
            }

            this.mob.getNavigation().stop();
        }

        private void onArrivedAtPoint(int now) {
            this.navigatingToPoint = false;
            this.navigateTarget = null;
            this.mob.getNavigation().stop();
            if (this.currentPoint == null) {
                this.clearBedState();
                return;
            }

            if (this.shouldSeekBedNow(now)) {
                this.seekingBed = true;
                this.sleepBedTarget = null;
                this.repathDelay = 0;
                this.bedScanDelay = 0;
                this.mob.clearRestriction();
                return;
            }

            this.clearBedState();
            this.applyRestriction(this.currentPoint);
        }

        private boolean shouldSeekBedNow(int dayTime) {
            return this.mob.isInLastScheduleWindow(dayTime);
        }

        private void prepareBedNavigation(ActivityPoint point) {
            if (this.sleepBedTarget != null && !this.isValidBedTarget(this.sleepBedTarget, point)) {
                this.sleepBedTarget = null;
            }

            if (this.sleepBedTarget == null) {
                if (this.bedScanDelay > 0) {
                    this.bedScanDelay--;
                    this.navigateTarget = null;
                    return;
                }
                this.bedScanDelay = BED_SCAN_INTERVAL;
                this.sleepBedTarget = this.findNearestBed(point);
            }

            if (this.sleepBedTarget == null) {
                this.navigateTarget = null;
                return;
            }

            this.navigateTarget = this.sleepBedTarget;
            if (!this.isAtBed(this.sleepBedTarget)) {
                return;
            }

            if (this.tryStartSleeping(this.sleepBedTarget, point)) {
                this.navigateTarget = null;
                return;
            }

            this.sleepBedTarget = null;
            this.navigateTarget = null;
            this.repathDelay = 0;
            this.bedScanDelay = 0;
        }

        private boolean tryStartSleeping(BlockPos bedPos, ActivityPoint point) {
            if (this.mob.level().isClientSide || this.mob.isSleeping()) {
                return this.mob.isSleeping();
            }

            BlockState state = this.mob.level().getBlockState(bedPos);
            BlockPos headPos = this.resolveBedHeadPos(bedPos, state);
            if (headPos == null || !this.isValidBedTarget(headPos, point)) {
                return false;
            }

            if (this.isBedOccupiedByOther(headPos)) {
                return false;
            }

            this.clearGhostOccupiedFlag(headPos);
            this.mob.getNavigation().stop();
            this.mob.startSleeping(headPos);
            if (this.mob.isSleeping()) {
                return true;
            }

            Vec3 bedCenter = Vec3.atBottomCenterOf(headPos).add(0.0D, 0.5625D, 0.0D);
            this.mob.setDeltaMovement(Vec3.ZERO);
            this.mob.moveTo(bedCenter.x, bedCenter.y, bedCenter.z, this.mob.getYRot(), this.mob.getXRot());
            this.mob.startSleeping(headPos);
            return this.mob.isSleeping();
        }

        private boolean isAtBed(BlockPos bedPos) {
            return this.mob.distanceToSqr(Vec3.atCenterOf(bedPos)) <= 2.25D;
        }

        @Nullable
        private BlockPos findNearestBed(ActivityPoint point) {
            BlockPos center = point.center();
            int radius = Math.max(2, Math.min(BED_SEARCH_RADIUS, point.radius()));
            double radiusSqr = (radius + 0.5D) * (radius + 0.5D);
            int minY = Math.max(this.mob.level().getMinBuildHeight(), center.getY() - BED_SEARCH_VERTICAL_RANGE);
            int maxY = Math.min(this.mob.level().getMaxBuildHeight() - 1, center.getY() + BED_SEARCH_VERTICAL_RANGE);
            BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

            BlockPos best = null;
            double bestDist = Double.MAX_VALUE;
            for (int y = minY; y <= maxY; y++) {
                for (int x = center.getX() - radius; x <= center.getX() + radius; x++) {
                    for (int z = center.getZ() - radius; z <= center.getZ() + radius; z++) {
                        cursor.set(x, y, z);
                        BlockState state = this.mob.level().getBlockState(cursor);
                        if (!(state.getBlock() instanceof BedBlock)) {
                            continue;
                        }
                        BlockPos headPos = this.resolveBedHeadPos(cursor, state);
                        if (headPos == null) {
                            continue;
                        }
                        if (Vec3.atCenterOf(center).distanceToSqr(Vec3.atCenterOf(headPos)) > radiusSqr) {
                            continue;
                        }
                        if (!this.isValidBedTarget(headPos, point)) {
                            continue;
                        }
                        double dist = this.mob.distanceToSqr(Vec3.atCenterOf(headPos));
                        if (dist < bestDist) {
                            bestDist = dist;
                            best = headPos.immutable();
                        }
                    }
                }
            }
            return best;
        }

        @Nullable
        private BlockPos resolveBedHeadPos(BlockPos bedPos, BlockState state) {
            if (!(state.getBlock() instanceof BedBlock)) {
                return null;
            }
            if (!state.hasProperty(BedBlock.PART) || !state.hasProperty(BedBlock.FACING)) {
                return bedPos.immutable();
            }
            BedPart part = state.getValue(BedBlock.PART);
            return (part == BedPart.HEAD ? bedPos : bedPos.relative(state.getValue(BedBlock.FACING))).immutable();
        }

        private boolean isValidBedTarget(BlockPos bedPos, ActivityPoint point) {
            if (bedPos == null || point == null) {
                return false;
            }
            BlockState state = this.mob.level().getBlockState(bedPos);
            if (!(state.getBlock() instanceof BedBlock)) {
                return false;
            }

            int radius = Math.max(2, Math.min(BED_SEARCH_RADIUS, point.radius()));
            double radiusSqr = (radius + 0.5D) * (radius + 0.5D);
            if (Vec3.atCenterOf(point.center()).distanceToSqr(Vec3.atCenterOf(bedPos)) > radiusSqr) {
                return false;
            }
            return !this.isBedOccupiedByOther(bedPos);
        }

        private boolean isBedOccupiedByOther(BlockPos bedPos) {
            BlockState state = this.mob.level().getBlockState(bedPos);
            BlockPos headPos = this.resolveBedHeadPos(bedPos, state);
            if (headPos == null) {
                return true;
            }

            AABB checkBox = new AABB(headPos).inflate(1.5D, 1.5D, 1.5D);
            for (LivingEntity living : this.mob.level().getEntitiesOfClass(
                    LivingEntity.class,
                    checkBox,
                    entity -> entity != this.mob && entity.isSleeping()
            )) {
                BlockPos sleepingPos = living.getSleepingPos().orElse(null);
                if (sleepingPos == null) {
                    continue;
                }
                BlockState sleepingState = this.mob.level().getBlockState(sleepingPos);
                BlockPos sleepingHead = this.resolveBedHeadPos(sleepingPos, sleepingState);
                if (headPos.equals(sleepingHead != null ? sleepingHead : sleepingPos)) {
                    return true;
                }
            }
            return false;
        }

        private void clearGhostOccupiedFlag(BlockPos bedPos) {
            BlockState state = this.mob.level().getBlockState(bedPos);
            if (!(state.getBlock() instanceof BedBlock) || !state.hasProperty(BedBlock.OCCUPIED)) {
                return;
            }
            if (!state.getValue(BedBlock.OCCUPIED) || this.isBedOccupiedByOther(bedPos)) {
                return;
            }
            this.mob.level().setBlock(bedPos, state.setValue(BedBlock.OCCUPIED, false), 3);
        }

        @Nullable
        private ActivityPoint pickPointForActivity(ActivityType activityType) {
            List<ActivityPoint> candidatePoints = this.getCandidatePoints(activityType);
            if (candidatePoints.isEmpty()) {
                return null;
            }
            return candidatePoints.get(this.mob.getRandom().nextInt(candidatePoints.size()));
        }

        private List<ActivityPoint> getCandidatePoints(ActivityType activityType) {
            return this.mob.activityPoints.stream()
                    .filter(point -> point.activityType() == activityType)
                    .toList();
        }

        private void resetRuntimeState() {
            this.navigatingToPoint = false;
            this.clearBedState();
            this.navigateTarget = null;
            this.repathDelay = 0;
            this.bedScanDelay = 0;
        }

        private void resetAllState() {
            this.currentActivity = null;
            this.currentPoint = null;
            this.currentWindowStartTick = -1;
            this.resetRuntimeState();
        }

        private void clearBedState() {
            this.seekingBed = false;
            this.sleepBedTarget = null;
            this.bedScanDelay = 0;
        }

        private void applyRestriction(ActivityPoint point) {
            this.mob.restrictTo(point.center(), point.radius());
        }

        private void applyApproachRestriction(ActivityPoint point) {
            this.mob.restrictTo(point.center(), (int) Math.ceil(ARRIVAL_RADIUS));
        }

        private boolean isInsideArrivalRadius(ActivityPoint point) {
            return this.mob.distanceToSqr(Vec3.atCenterOf(point.center())) <= ARRIVAL_RADIUS * ARRIVAL_RADIUS;
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

