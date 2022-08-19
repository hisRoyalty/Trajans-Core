package com.crescentine.trajanscore.basetank;

import com.crescentine.trajanscore.TankModClient;
import com.crescentine.trajanscore.TrajansCoreConfig;
import com.crescentine.trajanscore.item.TrajansCoreItems;
import com.crescentine.trajanscore.tankshells.apcr.APCRShell;
import com.crescentine.trajanscore.tankshells.armorpiercing.ArmorPiercingShell;
import com.crescentine.trajanscore.tankshells.heat.HeatShell;
import com.crescentine.trajanscore.tankshells.highexplosive.HighExplosiveShell;
import com.crescentine.trajanscore.tankshells.standard.StandardShell;
import com.google.common.collect.ImmutableList;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;

import javax.annotation.Nullable;
import javax.swing.text.JTextComponent;

public class BaseTankEntity extends Animal implements IAnimatable {
    private final AnimationFactory factory = new AnimationFactory(this);
    public double healAmount = 0;
    public boolean armored;
    public double speed = 0;
    private static final EntityDataAccessor<Integer> FUEL_AMOUNT = SynchedEntityData.defineId(BaseTankEntity.class, EntityDataSerializers.INT);
    public int shootingCooldown = 60;
    public int time;
    public double armor = 0;
    static int shellsUsed = 1;
    public double health = 0;
    public int age;
    public double coalFuelAmount = TrajansCoreConfig.coalFuelAmount.get();
    public double lavaFuelAmount = TrajansCoreConfig.lavaFuelAmount.get();
    public double maxFuel = 12000.00;
    public boolean shootingAnimation;
    double d0 = this.random.nextGaussian() * 0.03D;
    double d1 = this.random.nextGaussian() * 0.03D;
    double d2 = this.random.nextGaussian() * 0.03D;
    private static final Ingredient COAL_FUEL = Ingredient.of(Items.COAL, Items.CHARCOAL);
    private static final Ingredient COAL_BLOCK_FUEL = Ingredient.of(Items.COAL_BLOCK);
    private static final Ingredient LAVA_FUEL = Ingredient.of(Items.LAVA_BUCKET);
    public static final Ingredient AMMO = Ingredient.of(TrajansCoreItems.APCR_SHELL.get(), TrajansCoreItems.ARMOR_PIERCING_SHELL.get(), TrajansCoreItems.HEAT_SHELL.get(), TrajansCoreItems.STANDARD_SHELL.get(), TrajansCoreItems.HIGH_EXPLOSIVE_SHELL.get());
    private ImmutableList<Entity> passengers = ImmutableList.of();
    public boolean showFuel;
    public boolean canUseAPCR;
    public boolean canUseStandard;
    public boolean canUseArmorPiercing;
    public boolean canUseHeat;
    public boolean canUseHighExplosive;
    public boolean turretFollow;

    public BaseTankEntity(EntityType<?> entityType, Level world) {
        super((EntityType<? extends Animal>) entityType, world);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Pig.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, 250.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 10.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.5)
                .add(Attributes.FOLLOW_RANGE, 0.0D);
    }

    @Override
    public InteractionResult interactAt(Player player, Vec3 hitPos, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        if (itemstack.is(Items.IRON_BLOCK) && this.getHealth() < this.health) {
            healTank(healAmount);
            itemstack.shrink(1);
            return InteractionResult.SUCCESS;
        }
        if (itemstack.is(Items.IRON_INGOT) && this.getHealth() < this.health) {
            healTank(healAmount / 9);
            itemstack.shrink(1);
            return InteractionResult.SUCCESS;
        }
        if (TrajansCoreConfig.fuelSystemEnabled.get() && getFuelAmount() < maxFuel) {
            if (COAL_FUEL.test(itemstack) && getFuelAmount() < maxFuel && TrajansCoreConfig.fuelSystemEnabled.get()) {
                fuelTankWithItem((int) coalFuelAmount * 20);
                itemstack.shrink(1);
                return InteractionResult.SUCCESS;
            }
            if (COAL_BLOCK_FUEL.test(itemstack) && getFuelAmount() < maxFuel && TrajansCoreConfig.fuelSystemEnabled.get()) {
                fuelTankWithItem((int) coalFuelAmount * 180);
                itemstack.shrink(1);
                return InteractionResult.SUCCESS;
            }
            if (LAVA_FUEL.test(itemstack) && getFuelAmount() < maxFuel && TrajansCoreConfig.fuelSystemEnabled.get()) {
                fuelTankWithItem((int) lavaFuelAmount * 20);
                itemstack.shrink(1);
                player.setItemInHand(hand, new ItemStack(Items.BUCKET));
                return InteractionResult.SUCCESS;
            }
        }
        if (canAddPassenger(player)) {
            player.startRiding(this, true);
            return InteractionResult.FAIL;
        }
        return InteractionResult.FAIL;
    }
    public void healTank(double healAmount) {
        this.heal((float) healAmount);
        this.level.addParticle(ParticleTypes.FLAME, this.getRandomX(1.0D), this.getRandomY() + 0.5D, this.getRandomZ(1.0D), d0, d1, d2);
    }
    public void fuelTankWithItem(int fuelAmount) {
        addFuel(fuelAmount);
        this.level.addParticle(ParticleTypes.LARGE_SMOKE, this.getX() + 1.0D, this.getY() + 1.0D, this.getZ(), d0, d1, d2);
    }

    @Override
    protected boolean canAddPassenger(Entity entity) {
        return this.passengers.isEmpty();
    }

    @Override
    public void thunderHit(ServerLevel p_29473_, LightningBolt p_29474_) {
    }

    @Override
    public boolean rideableUnderWater() {
        return false;
    }

    @Override
    protected int calculateFallDamage(float fallDistance, float damageMultiplier) {
        return 0;
    }

    @Override
    protected boolean isImmobile() {
        return false;
    }


    @Override
    public int getMaxFallDistance() {
        return 30;
    }
    @Override
    public void travel(Vec3 pos) {
        if (this.isAlive()) {
            if (this.isVehicle()) {
                LivingEntity livingentity = (LivingEntity) this.getControllingPassenger();
                if (level.isClientSide) {

                    if (TankModClient.SYNC_TURRET_WITH_TANK.consumeClick() && level.isClientSide)
                    {
                        turretFollow = !turretFollow;
                    }

                    if (turretFollow) {
                        this.setYRot(livingentity.getYRot());
                        this.yRotO = this.getYRot();
                        this.setXRot(livingentity.getXRot() * 0.5F);
                        this.setRot(this.getYRot(), this.getXRot());
                        this.yBodyRot = this.getYRot();
                        this.yHeadRot = this.yBodyRot;
                    }
                }
                float f = livingentity.xxa * 0.5F;
                float f1 = livingentity.zza;
                if (f1 <= 0.0F) {
                    f1 *= 0.25F;
                }

                if (getFuelAmount() > 0) {
                    this.setSpeed((float) speed);
                }
                super.travel(new Vec3((double) f, pos.y, (double) f1));
            }
            super.travel(pos);
            this.setSpeed(0);
            this.flyingSpeed = 0.02f;
        }
    }

    @Override
    public boolean shouldRiderSit() {
        return false;
    }

    @Nullable
    public Entity getControllingPassenger() {
        return this.getPassengers().isEmpty() ? null : this.getPassengers().get(0);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor p_146746_, DifficultyInstance p_146747_, MobSpawnType p_146748_, @Nullable SpawnGroupData p_146749_, @Nullable CompoundTag p_146750_) {
        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue((float) health);
        this.getAttribute(Attributes.ARMOR).setBaseValue(armor);
        this.setHealth((float) health);
        return super.finalizeSpawn(p_146746_, p_146747_, p_146748_, p_146749_, p_146750_);
    }

    @Override
    public boolean isNoGravity() {
        return false;
    }


    @org.jetbrains.annotations.Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel p_146743_, AgeableMob p_146744_) {
        return null;
    }

    @Override
    public boolean requiresCustomPersistence() {
        return true;
    }

    @Override
    public ItemStack getItemBySlot(EquipmentSlot slot) {
        return ItemStack.EMPTY;
    }

    @Override
    protected void registerGoals() {
    }

    @Override
    public boolean startRiding(Entity p_21396_, boolean p_21397_) {
        this.time = 0;
        return super.startRiding(p_21396_, p_21397_);
    }

    @Override
    public boolean isBaby() {
        return false;
    }

    private boolean isMoving() {
            return this.onGround && this.getDeltaMovement().horizontalDistanceSqr() > 1.0E-6D;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.GENERIC_EXPLODE;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ARMOR_EQUIP_IRON;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ARMOR_EQUIP_IRON;
    }

    @Override
    protected SoundEvent getSwimSplashSound() {
        return SoundEvents.PLAYER_SPLASH;
    }

    @Override
    protected SoundEvent getSwimSound() {
        return SoundEvents.GENERIC_SWIM;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.isMoving()) {
            fuelTick();
        }
        age++;
        if (time < shootingCooldown) time++;
        if (level.isClientSide() && this.isVehicle() && this.age % 10 == 0 && getFuelAmount() > 0) {
            this.level.addParticle(ParticleTypes.LARGE_SMOKE, this.getX() + 1.0D, this.getY() + 1.0D, this.getZ(), d0, d1, d2);
            this.level.addParticle(ParticleTypes.LARGE_SMOKE, this.getX() + 1.0D, this.getY() + 1.0D, this.getZ(), d0, d1, d2);
        }
    }

    protected void fuelTick() {
        int fuel = getFuelAmount();
        if (level.isClientSide) {
            if (this.isVehicle()) {
                removeFuel(1);
            }
        }
    }

    private void removeFuel(int amount) {
        int fuel = getFuelAmount();
        int newFuel = fuel - amount;
        setFuelAmount(newFuel);
    }

    private void addFuel(int amount) {
        int fuel = getFuelAmount();
        int newFuel = fuel + amount;
        setFuelAmount((newFuel));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(FUEL_AMOUNT, 0);
    }

    public void setFuelAmount(int fuel) {
        this.entityData.set(FUEL_AMOUNT, fuel);
    }

    public int getFuelAmount() {
        return this.entityData.get(FUEL_AMOUNT);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        setFuelAmount(pCompound.getInt("fuel"));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putInt("fuel", getFuelAmount());
    }


    public boolean shoot(Player player, BaseTankEntity tank, Level world) {
        Player playerEntity = (Player) player;
        ItemStack itemStack = ItemStack.EMPTY;
        BaseTankEntity tankEntity = (BaseTankEntity) tank;
        for (int i = 0; i < playerEntity.getInventory().getContainerSize(); ++i) {
            ItemStack stack = playerEntity.getInventory().offhand.get(i);
            if (stack.getCount() >= shellsUsed) {
                itemStack = stack;
                break;
            }
            player.displayClientMessage(Component.literal("You don't have any ammo!").withStyle(ChatFormatting.RED, ChatFormatting.BOLD), false);
        }
        if (time < shootingCooldown) {
            player.displayClientMessage(Component.literal("Please wait " + (shootingCooldown - time) / 20 + " s !").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), false);
            world.playSound(null, player.blockPosition(), SoundEvents.DISPENSER_FAIL, SoundSource.BLOCKS, 1.0f, 1.0f);
            return false;
        }
        if (getFuelAmount() < 1 && TrajansCoreConfig.fuelSystemEnabled.get()) {
            player.displayClientMessage(Component.literal("You don't have any fuel!").withStyle(ChatFormatting.RED, ChatFormatting.BOLD), false);
            world.playSound(null, player.blockPosition(), SoundEvents.DISPENSER_FAIL, SoundSource.BLOCKS, 1.0f, 1.0f);
            return false;
        }
        if (!AMMO.test(itemStack)) {
            player.displayClientMessage(Component.literal("You don't have any ammo!").withStyle(ChatFormatting.RED, ChatFormatting.BOLD), false);
            world.playSound(null, player.blockPosition(), SoundEvents.DISPENSER_FAIL, SoundSource.BLOCKS, 1.0f, 1.0f);
            return false;
        }

        if (itemStack.is(TrajansCoreItems.STANDARD_SHELL.get()) && canUseStandard) {
            StandardShell shellEntity = new StandardShell(tankEntity, world);
            shellEntity.shootFromRotation(tankEntity, playerEntity.getXRot(), playerEntity.getYRot(), 0.0F, 3.5F, 0F);
            world.addFreshEntity(shellEntity);
            itemStack.shrink(shellsUsed);
        }
        if (itemStack.is(TrajansCoreItems.STANDARD_SHELL.get()) && !canUseStandard) {
            player.displayClientMessage(Component.literal("Shell Type disabled with this vehicle!").withStyle(ChatFormatting.RED, ChatFormatting.BOLD), false);
            world.playSound(null, player.blockPosition(), SoundEvents.DISPENSER_FAIL, SoundSource.BLOCKS, 1.0f, 1.0f);
            return false;
        }

        if (itemStack.is(TrajansCoreItems.ARMOR_PIERCING_SHELL.get()) && canUseArmorPiercing) {
            ArmorPiercingShell shellEntity = new ArmorPiercingShell(tankEntity, world);
            shellEntity.shootFromRotation(tankEntity, playerEntity.getXRot(), playerEntity.getYRot(), 0.0F, 3.5F, 0F);
            world.addFreshEntity(shellEntity);
            itemStack.shrink(shellsUsed);
        }
        if (itemStack.is(TrajansCoreItems.ARMOR_PIERCING_SHELL.get()) && !canUseArmorPiercing) {
            player.displayClientMessage(Component.literal("Shell Type disabled with this vehicle!").withStyle(ChatFormatting.RED, ChatFormatting.BOLD), false);
            world.playSound(null, player.blockPosition(), SoundEvents.DISPENSER_FAIL, SoundSource.BLOCKS, 1.0f, 1.0f);
            return false;
        }

        if (itemStack.is(TrajansCoreItems.HIGH_EXPLOSIVE_SHELL.get()) && canUseHighExplosive) {
            HighExplosiveShell shellEntity = new HighExplosiveShell(tankEntity, world);
            shellEntity.shootFromRotation(tankEntity, playerEntity.getXRot(), playerEntity.getYRot(), 0.0F, 3.5F, 0F);
            world.addFreshEntity(shellEntity);
            itemStack.shrink(shellsUsed);
        }
        if (itemStack.is(TrajansCoreItems.HIGH_EXPLOSIVE_SHELL.get()) && !canUseHighExplosive) {
            player.displayClientMessage(Component.literal("Shell Type disabled with this vehicle!").withStyle(ChatFormatting.RED, ChatFormatting.BOLD), false);
            world.playSound(null, player.blockPosition(), SoundEvents.DISPENSER_FAIL, SoundSource.BLOCKS, 1.0f, 1.0f);
            return false;
        }

        if (itemStack.is(TrajansCoreItems.HEAT_SHELL.get()) && canUseHeat) {
            HeatShell shellEntity = new HeatShell(tankEntity, world);
            shellEntity.shootFromRotation(tankEntity, playerEntity.getXRot(), playerEntity.getYRot(), 0.0F, 3.5F, 0F);
            world.addFreshEntity(shellEntity);
            itemStack.shrink(shellsUsed);
        }
        if (itemStack.is(TrajansCoreItems.HEAT_SHELL.get()) && !canUseHeat) {
            player.displayClientMessage(Component.literal("Shell Type disabled with this vehicle!").withStyle(ChatFormatting.RED, ChatFormatting.BOLD), false);
            world.playSound(null, player.blockPosition(), SoundEvents.DISPENSER_FAIL, SoundSource.BLOCKS, 1.0f, 1.0f);
            return false;
        }

        if (itemStack.is(TrajansCoreItems.APCR_SHELL.get()) && canUseAPCR) {
            APCRShell shellEntity = new APCRShell(tankEntity, world);
            shellEntity.shootFromRotation(tankEntity, playerEntity.getXRot(), playerEntity.getYRot(), 0.0F, 3.5F, 0F);
            world.addFreshEntity(shellEntity);
            itemStack.shrink(shellsUsed);
        }
        if (itemStack.is(TrajansCoreItems.APCR_SHELL.get()) && !canUseAPCR) {
            player.displayClientMessage(Component.literal("Shell Type disabled with this vehicle!").withStyle(ChatFormatting.RED, ChatFormatting.BOLD), false);
            world.playSound(null, player.blockPosition(), SoundEvents.DISPENSER_FAIL, SoundSource.BLOCKS, 1.0f, 1.0f);
            return false;
        }
        time = 0;
        return true;
    }
    public boolean fuelLeft(Player player) {
        double fuel = getFuelAmount();
        if (fuel < 1200 && fuel > 1 && TrajansCoreConfig.fuelSystemEnabled.get()) {
            player.displayClientMessage(Component.literal("Low fuel! Amount of time before fuel runs out " + fuel / 20 + " seconds or " + String.format("%.2f", fuel / 1200) + " minutes ").withStyle(ChatFormatting.RED, ChatFormatting.BOLD), false);
            return true;
        }
        if (fuel > 1200 && TrajansCoreConfig.fuelSystemEnabled.get()) {
            player.displayClientMessage(Component.literal("The amount of fuel remaining: " + fuel / 20 + " seconds, or " + String.format("%.2f", fuel / 1200) + " minutes ").withStyle(ChatFormatting.BLUE, ChatFormatting.BOLD), false);
            return true;
        }
        if (getFuelAmount() < 1 && TrajansCoreConfig.fuelSystemEnabled.get()) {
            player.displayClientMessage(Component.literal("No fuel remaining!").withStyle(ChatFormatting.RED, ChatFormatting.BOLD), false);
            return true;
        }
        if (!TrajansCoreConfig.fuelSystemEnabled.get()) {
            player.displayClientMessage(Component.literal("Fuel is not enabled! Enable fuel in the config, or contact your server administrator.").withStyle(ChatFormatting.RED, ChatFormatting.BOLD), false);
            return true;
        }
        return false;
    }

    @Override
    public AnimationFactory getFactory() {
        return this.factory;
    }

    protected <E extends IAnimatable> PlayState predicate(AnimationEvent<E> event) {
        return PlayState.STOP;
    }

    @Override
    public void registerControllers(AnimationData animationData) {
        animationData.addAnimationController(new AnimationController<>(this, "controller", 0, this::predicate));
    }

    @Override
    public float getStepHeight() {
        return 1.0f;
    }
}