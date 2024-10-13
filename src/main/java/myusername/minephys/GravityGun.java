package myusername.minephys;

import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class GravityGun {

    public static final Item GRAVITY_GUN = register(new Item(new Item.Settings()), "grav_gun");

    public static Item register(Item item, String id) {

        Identifier itemId = Identifier.of(Minecartphysics.MOD_ID, id);
        Item registeredItem = Registry.register(Registries.ITEM, itemId, item);

        return registeredItem;
    }

    public static void initalize() {

        // Dummy method to ensure this class is evaluated.

    }
}
