package openperipheral.integration.railcraft;

import java.util.Map;

import net.minecraft.util.Vec3;

import com.google.common.collect.Maps;

import mods.railcraft.api.carts.IPaintedCart;
import openperipheral.api.helpers.EntityMetaProviderSimple;

public class PaintedCartMetaProvider extends EntityMetaProviderSimple<IPaintedCart> {

    @Override
    public String getKey() {
        return "painted_cart";
    }

    @Override
    public Object getMeta(IPaintedCart target, Vec3 relativePos) {
        Map<String, Object> map = Maps.newHashMap();
        map.put("primaryColor", target.getPrimaryColor());
        map.put("secondaryColor", target.getSecondaryColor());
        return map;
    }

}
