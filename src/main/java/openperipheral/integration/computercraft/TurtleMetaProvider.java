package openperipheral.integration.computercraft;

import java.util.Map;

import net.minecraft.item.ItemStack;

import com.google.common.collect.Maps;

import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.shared.turtle.items.ITurtleItem;
import openperipheral.api.meta.IItemStackMetaProvider;

public class TurtleMetaProvider implements IItemStackMetaProvider<ITurtleItem> {

    @Override
    public Class<? extends ITurtleItem> getTargetClass() {
        return ITurtleItem.class;
    }

    @Override
    public String getKey() {
        return "turtle";
    }

    @Override
    public Object getMeta(ITurtleItem target, ItemStack stack) {
        Map<String, Object> map = Maps.newHashMap();

        addSideInfo(map, "left", target.getUpgrade(stack, TurtleSide.Left));
        addSideInfo(map, "right", target.getUpgrade(stack, TurtleSide.Right));

        map.put("fuel", target.getFuelLevel(stack));

        return map;
    }

    private static void addSideInfo(Map<String, Object> map, String side, ITurtleUpgrade upgrade) {
        if (upgrade != null) {
            Map<Object, Object> upgradeMap = Maps.newHashMap();

            upgradeMap.put("adjective", upgrade.getUnlocalisedAdjective());
            upgradeMap.put(
                "type",
                upgrade.getType()
                    .toString());

            map.put(side, upgradeMap);
        }
    }
}
