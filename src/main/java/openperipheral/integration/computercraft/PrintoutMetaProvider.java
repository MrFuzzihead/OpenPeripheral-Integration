package openperipheral.integration.computercraft;

import java.util.Map;

import net.minecraft.item.ItemStack;

import com.google.common.collect.Maps;

import dan200.computercraft.shared.media.items.ItemPrintout;
import openperipheral.api.meta.IItemStackMetaProvider;

public class PrintoutMetaProvider implements IItemStackMetaProvider<ItemPrintout> {

    @Override
    public Class<? extends ItemPrintout> getTargetClass() {
        return ItemPrintout.class;
    }

    @Override
    public String getKey() {
        return "printout";
    }

    @Override
    public Object getMeta(ItemPrintout target, ItemStack stack) {
        Map<String, Object> printoutMap = Maps.newHashMap();

        printoutMap.put("title", ItemPrintout.getTitle(stack));
        printoutMap.put("pages", ItemPrintout.getPageCount(stack));
        printoutMap.put("text", ItemPrintout.getText(stack));

        return printoutMap;
    }

}
