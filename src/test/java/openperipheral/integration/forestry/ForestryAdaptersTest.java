package openperipheral.integration.forestry;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import net.minecraft.item.ItemStack;

import org.junit.Assert;
import org.junit.Test;

import forestry.api.apiculture.IBee;
import forestry.api.apiculture.IBeeGenome;
import forestry.api.apiculture.IBeeHousing;
import forestry.api.apiculture.IBeeHousingInventory;
import forestry.api.apiculture.IBeekeepingLogic;
import forestry.api.genetics.IChromosomeType;

public class ForestryAdaptersTest {

    @Test
    public void beeHousing() {
        final IBeeHousing housing = mock(IBeeHousing.class);
        final IBeekeepingLogic logic = mock(IBeekeepingLogic.class);
        final IBeeHousingInventory inventory = mock(IBeeHousingInventory.class);
        when(housing.getBeekeepingLogic()).thenReturn(logic);
        when(housing.getBeeInventory()).thenReturn(inventory);
        when(logic.canWork()).thenReturn(true);
        when(logic.getBeeProgressPercent()).thenReturn(40);
        when(inventory.getDrone()).thenReturn(null);
        when(inventory.getQueen()).thenReturn(null);

        final AdapterBeeHousing adapter = new AdapterBeeHousing();
        Assert.assertTrue(adapter.canBreed(housing));
        Assert.assertEquals(40, adapter.breedingProgress(housing));
        Assert.assertNull(adapter.getDrone(housing));
        Assert.assertNull(adapter.getQueen(housing));
    }

    @Test
    public void individualDescriptionWithoutGenome() {
        final IBee bee = mock(IBee.class);
        when(bee.getDisplayName()).thenReturn("Meadows Bee");
        when(bee.getIdent()).thenReturn("forestry.bee.meadows");
        when(bee.isAnalyzed()).thenReturn(false);
        when(bee.isSecret()).thenReturn(false);
        when(bee.hasEffect()).thenReturn(false);
        when(bee.canSpawn()).thenReturn(true);
        when(bee.getGeneration()).thenReturn(2);
        when(bee.isNatural()).thenReturn(true);
        when(bee.getHealth()).thenReturn(10);
        when(bee.getMaxHealth()).thenReturn(20);
        when(bee.isAlive()).thenReturn(true);

        final Map<String, Object> map = ConverterIIndividual.describeIndividual(bee);
        Assert.assertEquals("Meadows Bee", map.get("displayName"));
        Assert.assertEquals("forestry.bee.meadows", map.get("ident"));
        Assert.assertEquals("bee", map.get("type"));
        Assert.assertEquals(Boolean.TRUE, map.get("canSpawn"));
        Assert.assertEquals(2, map.get("generation"));
        Assert.assertEquals(10, map.get("health"));
        Assert.assertFalse(map.containsKey("active"));
    }

    @Test
    public void individualDescriptionWithGenome() {
        final IBee bee = mock(IBee.class);
        final IBeeGenome genome = mock(IBeeGenome.class);
        when(bee.isAnalyzed()).thenReturn(true);
        when(bee.getGenome()).thenReturn(genome);
        when(genome.getActiveAllele(any(IChromosomeType.class))).thenReturn(null);
        when(genome.getInactiveAllele(any(IChromosomeType.class))).thenReturn(null);

        final Map<String, Object> map = ConverterIIndividual.describeIndividual(bee);
        @SuppressWarnings("unchecked")
        final Map<String, Object> active = (Map<String, Object>) map.get("active");
        Assert.assertNotNull(active);
        Assert.assertEquals("missing", active.get("species"));
        Assert.assertEquals(13, active.size());

        @SuppressWarnings("unchecked")
        final Map<String, Object> inactive = (Map<String, Object>) map.get("inactive");
        Assert.assertNotNull(inactive);
        Assert.assertEquals(13, inactive.size());
    }

    @Test
    public void individualMetadataOnlyAppliesToIndividuals() {
        final IndividualMetaProvider provider = new IndividualMetaProvider();
        Assert.assertEquals("individual", provider.getKey());
        Assert.assertFalse(
            provider.canApply(new net.minecraft.item.Item(), new ItemStack(new net.minecraft.item.Item())));
        Assert
            .assertNull(provider.getMeta(new net.minecraft.item.Item(), new ItemStack(new net.minecraft.item.Item())));
    }
}
