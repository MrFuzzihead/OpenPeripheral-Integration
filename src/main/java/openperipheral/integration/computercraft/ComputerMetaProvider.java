package openperipheral.integration.computercraft;

import java.util.Map;

import net.minecraft.item.ItemStack;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;

import dan200.computercraft.shared.computer.items.IComputerItem;
import openperipheral.api.meta.IItemStackMetaProvider;

public class ComputerMetaProvider implements IItemStackMetaProvider<IComputerItem> {

    @Override
    public Class<? extends IComputerItem> getTargetClass() {
        return IComputerItem.class;
    }

    @Override
    public String getKey() {
        return "computer";
    }

    @Override
    public Object getMeta(IComputerItem target, ItemStack stack) {
        Map<String, Object> computerInfo = Maps.newHashMap();
        int computerID = target.getComputerID(stack);
        if (computerID >= 0) {
            computerInfo.put("id", computerID);
            computerInfo.put("label", Strings.nullToEmpty(target.getLabel(stack)));
        }

        computerInfo.put("type", target.getFamily(stack));

        return computerInfo;
    }
}
