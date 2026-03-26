package com.servernpc.entity;

import com.servernpc.eiyahanabimachiservernpc;
import com.servernpc.menu.NpcInventoryMenu;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Predicate;
import javax.annotation.Nullable;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
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
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.Level;
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
    private static final Predicate<LivingEntity> SURVIVAL_PLAYER_TARGET =
            entity -> entity instanceof Player player && !player.isCreative() && !player.isSpectator();

    private final RangedBowAttackGoal<ReimuGoodNpcEntity> bowGoal =
            new RangedBowAttackGoal<>(this, 1.0D, 20, 15.0F);
    private final MeleeAttackGoal meleeGoal = new MeleeAttackGoal(this, 1.2D, false);
    private final SimpleContainer backpackContainer = new SimpleContainer(27);
    private final SimpleContainer extraEquipmentContainer = new SimpleContainer(6);
    private int backpackMaxStorage = 0;

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
                || heldItem.is(eiyahanabimachiservernpc.NPC_INVENTORY_TOOL.get())) {
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
        this.syncBackpackLockPlaceholders();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean(MOVEMENT_STOPPED_TAG, this.isMovementStopped());
        compound.putInt(BACKPACK_MAX_STORAGE_TAG, this.backpackMaxStorage);
        compound.put(BACKPACK_ITEMS_TAG, this.backpackContainer.createTag(this.registryAccess()));
        compound.put(EXTRA_EQUIPMENT_ITEMS_TAG, this.extraEquipmentContainer.createTag(this.registryAccess()));
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

    private static ItemStack createLockedPlaceholder() {
        ItemStack placeholder = new ItemStack(Items.BLACK_STAINED_GLASS_PANE);
        placeholder.set(DataComponents.CUSTOM_NAME, Component.translatable("gui." + eiyahanabimachiservernpc.MODID + ".locked_slot"));
        return placeholder;
    }

    private static boolean isLockedPlaceholder(ItemStack stack) {
        return stack.is(Items.BLACK_STAINED_GLASS_PANE) && stack.has(DataComponents.CUSTOM_NAME);
    }
}
