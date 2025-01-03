package com.crescentine.trajanscore.container;

import com.crescentine.trajanscore.TrajansCoreMod;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.network.IContainerFactory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.awt.*;

public class TankModContainers {
    public static final DeferredRegister<MenuType<?>> CONTAINERS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, TrajansCoreMod.MOD_ID);

    public static final RegistryObject<MenuType<CrafterContainer>> CRAFTER_CONTAINER
            = CONTAINERS.register("crafter_container",
            () -> IForgeMenuType.create(((windowId, inv, data) -> {
                BlockPos pos = data.readBlockPos();
                Level level = inv.player.getCommandSenderWorld();
                return new CrafterContainer(windowId, level, pos, inv, inv.player);
            })));
    public static final RegistryObject<MenuType<PlatingPressContainer>> PLATING_PRESS_CONTAINER =
            registerMenuType(PlatingPressContainer::new, "plating_press_container");
    public static final RegistryObject<MenuType<EngineFabricatorContainer>> ENGINE_FABRICATOR_CONTAINER =
            registerMenuType(EngineFabricatorContainer::new, "engine_fabricator_container");
    public static final RegistryObject<MenuType<SteelManufacturerContainer>> STEEL_MANUFACTURER_CONTAINER =
            registerMenuType(SteelManufacturerContainer::new, "steel_manufacturer_container");
    public static final RegistryObject<MenuType<TurretFactoryContainer>> TURRET_FACTORY_CONTAINER =
            registerMenuType(TurretFactoryContainer::new, "turret_factory_container");
    public static final RegistryObject<MenuType<ShellWorkbenchContainer>> SHELL_WORKBENCH_CONTAINER =
            registerMenuType(ShellWorkbenchContainer::new, "shell_workbench_container");




    private static <T extends AbstractContainerMenu>RegistryObject<MenuType<T>> registerMenuType(IContainerFactory<T> factory,
                                                                                                 String name) {
        return CONTAINERS.register(name, () -> IForgeMenuType.create(factory));
    }


    public static void register(IEventBus eventBus) {
        CONTAINERS.register(eventBus);
    }
}
