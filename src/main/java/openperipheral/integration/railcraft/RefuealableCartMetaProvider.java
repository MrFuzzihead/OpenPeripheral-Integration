package openperipheral.integration.railcraft;

import java.util.Map;

import net.minecraft.util.Vec3;

import com.google.common.collect.Maps;

import mods.railcraft.api.carts.IRefuelableCart;
import openperipheral.api.helpers.EntityMetaProviderSimple;

public class RefuealableCartMetaProvider extends EntityMetaProviderSimple<IRefuelableCart> {

    @Override
    public String getKey() {
        return "refuelable_cart";
    }

    @Override
    public Object getMeta(IRefuelableCart target, Vec3 relativePos) {
        Map<String, Object> map = Maps.newHashMap();
        map.put("needsRefuel", target.needsRefuel());
        return map;
    }

}
