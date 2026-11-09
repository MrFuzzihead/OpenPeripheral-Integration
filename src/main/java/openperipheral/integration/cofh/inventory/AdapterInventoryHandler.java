package openperipheral.integration.cofh.inventory;

import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;

import cofh.api.inventory.IInventoryHandler;
import openperipheral.api.adapter.IPeripheralAdapter;
import openperipheral.api.adapter.method.Arg;
import openperipheral.api.adapter.method.ReturnType;
import openperipheral.api.adapter.method.ScriptCallable;

public class AdapterInventoryHandler implements IPeripheralAdapter {

    @Override
    public Class<?> getTargetClass() {
        return IInventoryHandler.class;
    }

    @Override
    public String getSourceId() {
        return "cofh_inventory";
    }

    @ScriptCallable(
        returnTypes = ReturnType.TABLE,
        description = "Get the contents of the IInventoryHandler's inventory")
    public List<ItemStack> getInventoryContents(IInventoryHandler target,
        @Arg(
            name = "from",
            description = "The side to query. (north, south, east, west, up or down)") ForgeDirection from) {
        return target.getInventoryContents(from);
    }

    @ScriptCallable(
        returnTypes = ReturnType.NUMBER,
        description = "Get the number of slots of the IInventoryHandler's inventory")
    public int getSizeInventory(IInventoryHandler target,
        @Arg(
            name = "from",
            description = "The side to query. (north, south, east, west, up or down)") ForgeDirection from) {
        return target.getSizeInventory(from);
    }

    @ScriptCallable(
        returnTypes = ReturnType.BOOLEAN,
        description = "Returns true when the IInventoryHandler's inventory is empty")
    public boolean isEmpty(IInventoryHandler target,
        @Arg(
            name = "from",
            description = "The side to query. (north, south, east, west, up or down)") ForgeDirection from) {
        return target.isEmpty(from);
    }

    @ScriptCallable(
        returnTypes = ReturnType.BOOLEAN,
        description = "Returns true when the IInventoryHandler's inventory is full")
    public boolean isFull(IInventoryHandler target,
        @Arg(
            name = "from",
            description = "The side to query. (north, south, east, west, up or down)") ForgeDirection from) {
        return target.isFull(from);
    }
}
