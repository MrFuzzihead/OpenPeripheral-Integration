package openperipheral.integration.vanilla;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import com.google.common.base.Preconditions;

import cpw.mods.fml.common.registry.GameData;
import cpw.mods.fml.common.registry.GameRegistry.UniqueIdentifier;
import openmods.utils.ItemUtils;

/**
 * SearchNeedle is a helper class meant to allow easier communication between
 * Lua and vanilla requesting access to a specific item;
 */
public class ItemFingerprint {

    public final UniqueIdentifier id;
    public final int damage;
    public final String nbtHash;

    public ItemFingerprint(String id, int damage, String nbtHash) {
        Preconditions.checkArgument(id != null && id.indexOf(':') > 0, "Invalid item id '%s'", id);
        this.id = new UniqueIdentifier(id);
        this.damage = damage;
        this.nbtHash = nbtHash;
    }

    public ItemFingerprint(Item item, int damage, NBTTagCompound tag) {
        this.id = identify(item);
        this.damage = damage;
        this.nbtHash = tag != null ? ItemUtils.getNBTHash(tag) : null;
    }

    public ItemFingerprint(ItemStack stack) {
        Preconditions.checkNotNull(stack, "ItemStack is null");
        this.id = identify(stack.getItem());
        this.damage = stack.getItemDamage();

        NBTTagCompound tag = stack.getTagCompound();
        this.nbtHash = tag != null ? ItemUtils.getNBTHash(tag) : null;
    }

    private static UniqueIdentifier identify(Item item) {
        Preconditions.checkNotNull(item, "Item is null");
        final String itemId = GameData.getItemRegistry()
            .getNameForObject(item);
        Preconditions.checkArgument(itemId != null, "Item %s is not registered", item);
        return new UniqueIdentifier(itemId);
    }

    @Override
    public String toString() {
        return String.format("%s:%d:%s", id, damage, nbtHash);
    }
}
