package openperipheral.integration.forestry;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import forestry.api.apiculture.EnumBeeChromosome;
import forestry.api.apiculture.IBee;
import forestry.api.apiculture.IBeeGenome;
import forestry.api.apiculture.IBeeHousing;
import forestry.api.apiculture.IBeeHousingInventory;
import forestry.api.apiculture.IBeekeepingLogic;
import forestry.api.arboriculture.ITree;
import forestry.api.arboriculture.ITreeGenome;
import forestry.api.genetics.AlleleManager;
import forestry.api.genetics.IAllele;
import forestry.api.genetics.IAlleleRegistry;
import forestry.api.genetics.IAlleleSpecies;
import forestry.api.genetics.IChromosomeType;
import forestry.api.genetics.IGenome;
import forestry.api.genetics.IIndividual;
import forestry.api.genetics.IMutation;
import forestry.api.genetics.ISpeciesRoot;
import forestry.api.lepidopterology.IButterfly;
import forestry.api.lepidopterology.IButterflyGenome;
import openperipheral.integration.Config;

public class ForestryAdaptersTest {

    private IAlleleRegistry originalRegistry;

    @Before
    public void installRegistry() {
        originalRegistry = AlleleManager.alleleRegistry;
        AlleleManager.alleleRegistry = mock(IAlleleRegistry.class);
    }

    @After
    public void restoreRegistry() {
        AlleleManager.alleleRegistry = originalRegistry;
    }

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
    @SuppressWarnings("unchecked")
    public void beeHousingSpeciesListsAndMutations() {
        final ISpeciesRoot root = mock(ISpeciesRoot.class);
        when(AlleleManager.alleleRegistry.getSpeciesRoot("rootBees")).thenReturn(root);

        final IAlleleSpecies parentA = species("parentA", "Parent A");
        final IAlleleSpecies parentB = species("parentB", "Parent B");
        final IAlleleSpecies child = species("child", "Child");

        final IMutation mutation = mutation(root, parentA, parentB, child);
        final IMutation secret = mutation(root, parentA, child, child);
        when(secret.isSecret()).thenReturn(true);

        final List<IMutation> mutations = new ArrayList<IMutation>();
        mutations.add(mutation);
        mutations.add(secret);
        doReturn(mutations).when(root)
            .getMutations(false);

        final IIndividual template = mock(IIndividual.class);
        final IGenome templateGenome = mock(IGenome.class);
        when(template.getGenome()).thenReturn(templateGenome);
        when(templateGenome.getPrimary()).thenReturn(child);
        final ArrayList<IIndividual> templates = new ArrayList<IIndividual>();
        templates.add(template);
        doReturn(templates).when(root)
            .getIndividualTemplates();

        final IBeeHousing housing = mock(IBeeHousing.class);
        final AdapterBeeHousing adapter = new AdapterBeeHousing();

        final List<Map<String, Object>> breeding = adapter.getBeeBreedingData(housing);
        Assert.assertEquals("secret mutations are skipped by default", 1, breeding.size());
        Assert.assertEquals(
            0.5f,
            breeding.get(0)
                .get("chance"));
        Assert.assertEquals(
            child.getName(),
            breeding.get(0)
                .get("result"));

        final List<Map<String, String>> all = adapter.listAllSpecies(housing);
        Assert.assertEquals(3, all.size());

        final List<Map<String, Object>> parents = adapter.getBeeParents(housing, child.getName());
        Assert.assertEquals(1, parents.size());
        Assert.assertEquals(
            parentA.getName(),
            ((Map<String, Object>) parents.get(0)
                .get("allele1")).get("name"));

        final List<Map<String, Object>> children = adapter.getBeeChildren(housing, parentA.getName());
        Assert.assertEquals(1, children.size());
        Assert.assertNotNull(
            children.get(0)
                .get("result"));

        final boolean oldShowHidden = Config.showHiddenMutations;
        try {
            Config.showHiddenMutations = true;
            Assert.assertEquals(
                2,
                adapter.getBeeBreedingData(housing)
                    .size());
        } finally {
            Config.showHiddenMutations = oldShowHidden;
        }

        when(AlleleManager.alleleRegistry.getSpeciesRoot("rootBees")).thenReturn(null);
        final AdapterBeeHousing noRoot = new AdapterBeeHousing();
        Assert.assertNull(noRoot.listAllSpecies(housing));
        Assert.assertNull(noRoot.getBeeParents(housing, "child"));
        Assert.assertNull(noRoot.getBeeChildren(housing, "parent"));
        Assert.assertNull(noRoot.getBeeBreedingData(housing));
    }

    @Test
    public void beeHousingSkipsMalformedMutations() {
        final ISpeciesRoot root = mock(ISpeciesRoot.class);
        when(AlleleManager.alleleRegistry.getSpeciesRoot("rootBees")).thenReturn(root);

        final IAlleleSpecies parentA = species("parentA", "Parent A");
        final IAlleleSpecies parentB = species("parentB", "Parent B");
        final IAlleleSpecies child = species("child", "Child");

        final IMutation good = mutation(root, parentA, parentB, child);
        final IMutation broken = mock(IMutation.class);
        when(broken.getAllele0()).thenThrow(new RuntimeException("broken mutation"));

        final List<IMutation> mutations = new ArrayList<IMutation>();
        mutations.add(broken);
        mutations.add(good);
        doReturn(mutations).when(root)
            .getMutations(false);
        doReturn(new ArrayList<IIndividual>()).when(root)
            .getIndividualTemplates();

        final List<Map<String, Object>> breeding = new AdapterBeeHousing().getBeeBreedingData(mock(IBeeHousing.class));
        Assert.assertEquals(1, breeding.size());
    }

    @Test
    public void beeHousingResolvesDroneAndQueenIndividuals() {
        final IBeeHousing housing = mock(IBeeHousing.class);
        final IBeeHousingInventory inventory = mock(IBeeHousingInventory.class);
        when(housing.getBeeInventory()).thenReturn(inventory);

        final ItemStack drone = new ItemStack(new Item());
        final ItemStack queen = new ItemStack(new Item());
        when(inventory.getDrone()).thenReturn(drone);
        when(inventory.getQueen()).thenReturn(queen);

        final IIndividual droneIndividual = mock(IIndividual.class);
        final IIndividual queenIndividual = mock(IIndividual.class);
        when(AlleleManager.alleleRegistry.getIndividual(drone)).thenReturn(droneIndividual);
        when(AlleleManager.alleleRegistry.getIndividual(queen)).thenReturn(queenIndividual);

        final AdapterBeeHousing adapter = new AdapterBeeHousing();
        Assert.assertSame(droneIndividual, adapter.getDrone(housing));
        Assert.assertSame(queenIndividual, adapter.getQueen(housing));
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
    public void butterflyAndTreeDescriptions() {
        final IButterfly butterfly = mock(IButterfly.class);
        when(butterfly.getDisplayName()).thenReturn("Butterfly");
        when(butterfly.getSize()).thenReturn(0.5f);
        when(butterfly.isAnalyzed()).thenReturn(false);

        final Map<String, Object> butterflyMap = ConverterIIndividual.describeIndividual(butterfly);
        Assert.assertEquals("butterfly", butterflyMap.get("type"));
        Assert.assertEquals(0.5f, butterflyMap.get("size"));

        final IButterflyGenome butterflyGenome = mock(IButterflyGenome.class);
        when(butterfly.isAnalyzed()).thenReturn(true);
        when(butterfly.getGenome()).thenReturn(butterflyGenome);
        when(butterflyGenome.getActiveAllele(any(IChromosomeType.class))).thenReturn(null);
        when(butterflyGenome.getInactiveAllele(any(IChromosomeType.class))).thenReturn(null);

        final Map<String, Object> analyzedButterfly = ConverterIIndividual.describeIndividual(butterfly);
        Assert.assertNotNull(analyzedButterfly.get("active"));
        Assert.assertEquals(14, ((Map<?, ?>) analyzedButterfly.get("active")).size());

        final ITree tree = mock(ITree.class);
        when(tree.getDisplayName()).thenReturn("Tree");
        when(tree.getPlantTypes()).thenReturn(java.util.EnumSet.noneOf(net.minecraftforge.common.EnumPlantType.class));
        when(tree.isAnalyzed()).thenReturn(false);

        final Map<String, Object> treeMap = ConverterIIndividual.describeIndividual(tree);
        Assert.assertEquals("tree", treeMap.get("type"));

        final ITreeGenome treeGenome = mock(ITreeGenome.class);
        when(tree.isAnalyzed()).thenReturn(true);
        when(tree.getGenome()).thenReturn(treeGenome);
        when(treeGenome.getActiveAllele(any(IChromosomeType.class))).thenReturn(null);
        when(treeGenome.getInactiveAllele(any(IChromosomeType.class))).thenReturn(null);

        final Map<String, Object> analyzedTree = ConverterIIndividual.describeIndividual(tree);
        Assert.assertNotNull(analyzedTree.get("active"));
        Assert.assertEquals(12, ((Map<?, ?>) analyzedTree.get("active")).size());
    }

    @Test
    public void individualMetadataOnlyAppliesToIndividuals() {
        final IndividualMetaProvider provider = new IndividualMetaProvider();
        Assert.assertEquals("individual", provider.getKey());
        Assert.assertFalse(provider.canApply(new Item(), new ItemStack(new Item())));
        Assert.assertNull(provider.getMeta(new Item(), new ItemStack(new Item())));
    }

    private static IAlleleSpecies species(String uid, String name) {
        final IAlleleSpecies species = mock(IAlleleSpecies.class);
        when(species.getUID()).thenReturn(uid);
        when(species.getName()).thenReturn(name);
        when(species.isSecret()).thenReturn(false);
        return species;
    }

    private static IMutation mutation(ISpeciesRoot root, IAlleleSpecies first, IAlleleSpecies second,
        IAlleleSpecies offspringSpecies) {
        final IMutation mutation = mock(IMutation.class);
        when(mutation.getRoot()).thenReturn(root);
        when(mutation.getTemplate()).thenReturn(new IAllele[0]);
        when(mutation.getAllele0()).thenReturn(first);
        when(mutation.getAllele1()).thenReturn(second);
        when(mutation.getBaseChance()).thenReturn(0.5f);
        when(mutation.getSpecialConditions()).thenReturn(Collections.emptyList());
        when(mutation.isSecret()).thenReturn(false);

        final IGenome offspring = mock(IGenome.class);
        when(offspring.getActiveAllele(EnumBeeChromosome.SPECIES)).thenReturn(offspringSpecies);
        when(root.templateAsGenome(any(IAllele[].class))).thenReturn(offspring);
        return mutation;
    }
}
