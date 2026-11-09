package openperipheral.integration.thaumcraft;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;

import org.junit.Assert;
import org.junit.Test;

import thaumcraft.api.IRunicArmor;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;
import thaumcraft.api.aspects.IEssentiaContainerItem;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.api.nodes.INode;
import thaumcraft.api.nodes.NodeModifier;
import thaumcraft.api.nodes.NodeType;
import thaumcraft.api.wands.IWandFocus;
import thaumcraft.common.tiles.TileArcaneBore;
import thaumcraft.common.tiles.TileDeconstructionTable;
import thaumcraft.common.tiles.TileJarBrain;
import thaumcraft.common.tiles.TileJarFillable;
import thaumcraft.common.tiles.TileSensor;

public class ThaumcraftAdaptersTest {

    @Test
    @SuppressWarnings("unchecked")
    public void aspectContainer() {
        final AspectList aspects = new AspectList().add(Aspect.AIR, 5)
            .add(Aspect.FIRE, 2);
        final IAspectContainer container = mock(IAspectContainer.class);
        when(container.getAspects()).thenReturn(aspects);

        final AdapterAspectContainer adapter = new AdapterAspectContainer();
        Assert.assertSame(aspects, adapter.getAspects(container));

        final Map<String, Integer> summed = adapter.getAspectsSum(container);
        Assert.assertEquals(Integer.valueOf(5), summed.get(Aspect.AIR.getName()));
        Assert.assertEquals(Integer.valueOf(2), summed.get(Aspect.FIRE.getName()));
        Assert.assertEquals(5, adapter.getAspectCount(container, Aspect.AIR.getTag()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void aspectContainerEdgeCases() {
        final AdapterAspectContainer adapter = new AdapterAspectContainer();
        final IAspectContainer empty = mock(IAspectContainer.class);
        when(empty.getAspects()).thenReturn(null);
        Assert.assertNull(adapter.getAspectsSum(empty));
        Assert.assertEquals(0, adapter.getAspectCount(empty, Aspect.AIR.getTag()));

        try {
            adapter.getAspectCount(empty, "definitely_not_an_aspect");
            Assert.fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    @Test
    public void essentiaTransport() {
        final IEssentiaTransport transport = mock(IEssentiaTransport.class);
        when(transport.getSuctionAmount(ForgeDirection.UP)).thenReturn(8);
        when(transport.getSuctionType(ForgeDirection.UP)).thenReturn(Aspect.WATER);
        when(transport.getEssentiaAmount(ForgeDirection.DOWN)).thenReturn(4);
        when(transport.getEssentiaType(ForgeDirection.DOWN)).thenReturn(Aspect.EARTH);

        final AdapterEssentiaTransport adapter = new AdapterEssentiaTransport();
        Assert.assertEquals(8, adapter.getSuctionAmount(transport, ForgeDirection.UP));
        Assert.assertEquals(Aspect.WATER.getTag(), adapter.getSuctionType(transport, ForgeDirection.UP));
        Assert.assertEquals(4, adapter.getEssentiaAmount(transport, ForgeDirection.DOWN));
        Assert.assertEquals(Aspect.EARTH.getTag(), adapter.getEssentiaType(transport, ForgeDirection.DOWN));

        when(transport.getSuctionType(ForgeDirection.UP)).thenReturn(null);
        Assert.assertEquals("", adapter.getSuctionType(transport, ForgeDirection.UP));
    }

    @Test
    public void node() {
        final INode node = mock(INode.class);
        when(node.getNodeType()).thenReturn(NodeType.NORMAL);
        when(node.getNodeModifier()).thenReturn(NodeModifier.BRIGHT);

        final AdapterNode adapter = new AdapterNode();
        Assert.assertEquals(NodeType.NORMAL.name(), adapter.getNodeType(node));
        Assert.assertEquals(NodeModifier.BRIGHT.name(), adapter.getNodeModifier(node));

        when(node.getNodeType()).thenReturn(null);
        when(node.getNodeModifier()).thenReturn(null);
        Assert.assertEquals("NONE", adapter.getNodeType(node));
        Assert.assertEquals("NONE", adapter.getNodeModifier(node));
    }

    @Test
    public void arcaneBore() {
        final TileArcaneBore bore = mock(TileArcaneBore.class);
        bore.hasPickaxe = true;
        bore.hasFocus = true;
        bore.area = 2;
        bore.maxRadius = 3;
        bore.speed = 7;
        when(bore.gettingPower()).thenReturn(true);
        when(bore.getStackInSlot(1))
            .thenReturn(new ItemStack(new net.minecraft.item.ItemSword(net.minecraft.item.Item.ToolMaterial.EMERALD)));

        final AdapterArcaneBore adapter = new AdapterArcaneBore();
        Assert.assertTrue(adapter.hasPickaxe(bore));
        Assert.assertTrue(adapter.hasFocus(bore));
        Assert.assertEquals(11, adapter.getRadius(bore));
        Assert.assertEquals(7, adapter.getSpeed(bore));
        Assert.assertFalse(adapter.isPickaxeBroken(bore));
        Assert.assertTrue(adapter.isWorking(bore));
        Assert.assertFalse(adapter.hasNativeClusters(bore));
        Assert.assertEquals(0, adapter.getFortune(bore));
        Assert.assertFalse(adapter.hasSilkTouch(bore));

        bore.hasPickaxe = false;
        Assert.assertFalse(adapter.isWorking(bore));
    }

    @Test
    public void arcaneEar() {
        final TileSensor sensor = mock(TileSensor.class);
        sensor.note = 5;
        sensor.tone = 2;

        final AdapterArcaneEar adapter = new AdapterArcaneEar();
        Assert.assertEquals(5, adapter.getNote(sensor));
        Assert.assertEquals(2, adapter.getTone(sensor));

        adapter.setNote(sensor, (byte) 9);
        adapter.setTone(sensor, (byte) 4);
        Assert.assertEquals(9, sensor.note);
        Assert.assertEquals(4, sensor.tone);
    }

    @Test
    public void brainJar() {
        final TileJarBrain jar = mock(TileJarBrain.class);
        jar.xp = 42;
        Assert.assertEquals(42, new AdapterBrainJar().getXP(jar));
    }

    @Test
    public void deconstructor() {
        final TileDeconstructionTable table = mock(TileDeconstructionTable.class);
        Assert.assertFalse(new AdapterDeconstructor().hasAspect(table));
        Assert.assertEquals("", new AdapterDeconstructor().getAspect(table));

        table.aspect = Aspect.ENERGY;
        when(table.getStackInSlot(0)).thenReturn(new ItemStack(new Item()));

        final AdapterDeconstructor adapter = new AdapterDeconstructor();
        Assert.assertTrue(adapter.hasAspect(table));
        Assert.assertEquals(Aspect.ENERGY.getTag(), adapter.getAspect(table));
        Assert.assertTrue(adapter.hasItem(table));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void jar() {
        final TileJarFillable jar = mock(TileJarFillable.class);
        jar.aspectFilter = Aspect.FIRE;
        when(jar.getAspects()).thenReturn(new AspectList());

        final AdapterJar adapter = new AdapterJar();
        Assert.assertEquals(Aspect.FIRE.getName(), adapter.getAspectFilter(jar));

        final List<Map<String, Object>> aspects = adapter.getAspects(jar);
        Assert.assertEquals(1, aspects.size());
        Assert.assertEquals(
            Aspect.FIRE.getName(),
            aspects.get(0)
                .get("name"));
        Assert.assertEquals(
            0,
            aspects.get(0)
                .get("quantity"));

        when(jar.getAspects()).thenReturn(new AspectList().add(Aspect.AIR, 3));
        final List<Map<String, Object>> filled = adapter.getAspects(jar);
        Assert.assertEquals(1, filled.size());
        Assert.assertEquals(
            3,
            filled.get(0)
                .get("quantity"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void itemMetadataProviders() {
        final IWandFocus focus = mock(IWandFocus.class);
        final AspectList cost = new AspectList().add(Aspect.AIR, 10);
        when(focus.getVisCost()).thenReturn(cost);
        when(focus.isVisCostPerTick()).thenReturn(true);
        when(focus.getFocusColor()).thenReturn(0x123456);
        final Map<String, Object> focusMap = (Map<String, Object>) new WandFocusMetaProvider()
            .getMeta(focus, new ItemStack(new Item()));
        Assert.assertSame(cost, focusMap.get("cost"));
        Assert.assertEquals(Boolean.TRUE, focusMap.get("isPerTick"));
        Assert.assertEquals(0x123456, focusMap.get("color"));

        final IRunicArmor runic = mock(IRunicArmor.class);
        final ItemStack runicStack = new ItemStack(new Item());
        when(runic.getRunicCharge(runicStack)).thenReturn(7);
        final Map<String, Object> runicMap = (Map<String, Object>) new RunicItemMetaProvider()
            .getMeta(runic, runicStack);
        Assert.assertEquals(7, runicMap.get("charge"));

        final AspectList aspects = new AspectList().add(Aspect.WATER, 4);
        final IEssentiaContainerItem container = mock(IEssentiaContainerItem.class);
        when(container.getAspects(org.mockito.ArgumentMatchers.any(ItemStack.class))).thenReturn(aspects);
        Assert.assertSame(aspects, new EssentiaContainerMetaProvider().getMeta(container, new ItemStack(new Item())));
    }

    @Test
    public void aspectListConverter() {
        final AspectList list = new AspectList().add(Aspect.AIR, 2)
            .add(Aspect.FIRE, 3);
        final List<Map<String, Object>> map = ConverterAspectList.aspectsToMap(list);
        Assert.assertEquals(2, map.size());
        Assert.assertTrue(
            ConverterAspectList.aspectsToMap(null)
                .isEmpty());

        final java.util.List<Map<String, Object>> entries = new java.util.ArrayList<Map<String, Object>>();
        ConverterAspectList.appendAspectEntry(entries, Aspect.WATER, 9);
        Assert.assertEquals(
            Aspect.WATER.getName(),
            entries.get(0)
                .get("name"));
        Assert.assertEquals(
            9,
            entries.get(0)
                .get("quantity"));
    }
}
