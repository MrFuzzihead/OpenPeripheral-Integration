package openperipheral.integration.vanilla;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityFurnace;

import org.junit.Assert;
import org.junit.Test;

/**
 * Smoke test that proves the headless JUnit environment can touch Minecraft classes. Everything else in
 * this suite builds on it, so keep it small and dependency-free.
 */
public class HeadlessEnvironmentTest {

    @Test
    public void minecraftClassesAreUsable() {
        final ItemStack stack = new ItemStack(Items.diamond, 3);
        Assert.assertSame(Items.diamond, stack.getItem());
        Assert.assertEquals(3, stack.stackSize);
        // Fuel values come from FML's FuelManager and are empty in a headless JVM; only sanity-check
        // that the call is wired up at all.
        Assert.assertTrue(TileEntityFurnace.getItemBurnTime(new ItemStack(Items.coal)) > 0);
    }
}
