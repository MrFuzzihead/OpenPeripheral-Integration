package openperipheral.integration.ic2;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import net.minecraftforge.common.util.ForgeDirection;

import org.junit.Assert;
import org.junit.Test;

import ic2.api.crops.CropCard;
import ic2.api.crops.ICropTile;
import ic2.api.energy.tile.IEnergyConductor;
import ic2.api.energy.tile.IEnergySink;
import ic2.api.energy.tile.IEnergySource;
import ic2.api.energy.tile.IHeatSource;
import ic2.api.energy.tile.IKineticSource;
import ic2.api.reactor.IReactor;
import ic2.api.reactor.IReactorChamber;
import ic2.api.tile.IEnergyStorage;

/**
 * IC2 adapters. With the IC2 dev jar on the test runtime classpath these can be driven with mocks; the
 * reflective internal-class adapter is covered as well because the class is now resolvable.
 */
public class Ic2AdaptersTest {

    @Test
    public void energyConductor() {
        final IEnergyConductor conductor = mock(IEnergyConductor.class);
        when(conductor.getConductionLoss()).thenReturn(1.5);
        when(conductor.getConductorBreakdownEnergy()).thenReturn(2.5);
        when(conductor.getInsulationBreakdownEnergy()).thenReturn(3.5);
        when(conductor.getInsulationEnergyAbsorption()).thenReturn(4.5);

        final AdapterEnergyConductor adapter = new AdapterEnergyConductor();
        Assert.assertEquals("eu_conductor", adapter.getSourceId());
        Assert.assertEquals(1.5, adapter.getEUConductionLoss(conductor), 0.0001);
        Assert.assertEquals(2.5, adapter.getEUConductorBreakdownEnergy(conductor), 0.0001);
        Assert.assertEquals(3.5, adapter.getEUInsulationBreakdownEnergy(conductor), 0.0001);
        Assert.assertEquals(4.5, adapter.getEUInsulationEnergyAbsorption(conductor), 0.0001);
    }

    @Test
    public void energySinkAndSource() {
        final IEnergySink sink = mock(IEnergySink.class);
        when(sink.getDemandedEnergy()).thenReturn(32.0);
        when(sink.getSinkTier()).thenReturn(2);

        final AdapterEnergySink sinkAdapter = new AdapterEnergySink();
        Assert.assertEquals(32.0, sinkAdapter.getDemandedEnergy(sink), 0.0001);
        Assert.assertEquals(2, sinkAdapter.getEUSinkTier(sink));

        final IEnergySource source = mock(IEnergySource.class);
        when(source.getOfferedEnergy()).thenReturn(16.0);
        when(source.getSourceTier()).thenReturn(3);

        final AdapterEnergySource sourceAdapter = new AdapterEnergySource();
        Assert.assertEquals(16.0, sourceAdapter.getOfferedEnergy(source), 0.0001);
        Assert.assertEquals(3, sourceAdapter.getEUSourceTier(source));
    }

    @Test
    public void energyStorage() {
        final IEnergyStorage storage = mock(IEnergyStorage.class);
        when(storage.getCapacity()).thenReturn(1000);
        when(storage.getStored()).thenReturn(250);
        when(storage.getOutputEnergyUnitsPerTick()).thenReturn(8.0);

        final AdapterEnergyStorage adapter = new AdapterEnergyStorage();
        Assert.assertEquals(1000, adapter.getEUCapacity(storage));
        Assert.assertEquals(250, adapter.getEUStored(storage));
        Assert.assertEquals(8.0, adapter.getEUOutputPerTick(storage), 0.0001);
    }

    @Test
    public void reactorAndChamber() {
        final IReactor reactor = mock(IReactor.class);
        when(reactor.getHeat()).thenReturn(100);
        when(reactor.getMaxHeat()).thenReturn(2000);
        when(reactor.getReactorEnergyOutput()).thenReturn(64.0f);
        when(reactor.produceEnergy()).thenReturn(true);

        final AdapterReactor adapter = new AdapterReactor();
        Assert.assertEquals(100, adapter.getHeat(reactor));
        Assert.assertEquals(2000, adapter.getMaxHeat(reactor));
        Assert.assertEquals(64.0f, adapter.getEUOutput(reactor), 0.0001);
        Assert.assertTrue(adapter.isActive(reactor));

        final IReactorChamber chamber = mock(IReactorChamber.class);
        when(chamber.getReactor()).thenReturn(reactor);

        final AdapterReactorChamber chamberAdapter = new AdapterReactorChamber();
        Assert.assertTrue(chamberAdapter.isValid(chamber));
        Assert.assertEquals(100, chamberAdapter.getHeat(chamber));
        Assert.assertEquals(2000, chamberAdapter.getMaxHeat(chamber));
        Assert.assertEquals(64.0f, chamberAdapter.getEUOutput(chamber), 0.0001);
        Assert.assertTrue(chamberAdapter.isActive(chamber));
    }

    @Test
    public void reactorChamberWithoutReactor() {
        final IReactorChamber chamber = mock(IReactorChamber.class);
        final AdapterReactorChamber adapter = new AdapterReactorChamber();
        Assert.assertFalse(adapter.isValid(chamber));

        try {
            adapter.getHeat(chamber);
            Assert.fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    @Test
    public void crop() {
        final CropCard crop = mock(CropCard.class);
        when(crop.name()).thenReturn("wheat");
        final ICropTile tile = mock(ICropTile.class);
        when(tile.getCrop()).thenReturn(crop);
        when(tile.getSize()).thenReturn((byte) 4);
        when(tile.getGrowth()).thenReturn((byte) 5);
        when(tile.getGain()).thenReturn((byte) 6);
        when(tile.getResistance()).thenReturn((byte) 7);
        when(tile.getScanLevel()).thenReturn((byte) 8);
        when(tile.getNutrientStorage()).thenReturn(9);
        when(tile.getHydrationStorage()).thenReturn(10);
        when(tile.getWeedExStorage()).thenReturn(11);
        when(tile.getHumidity()).thenReturn((byte) 12);
        when(tile.getNutrients()).thenReturn((byte) 13);
        when(tile.getAirQuality()).thenReturn((byte) 14);

        final AdapterCrop adapter = new AdapterCrop();
        Assert.assertEquals("wheat", adapter.getID(tile));
        Assert.assertEquals(4, adapter.getSize(tile));
        Assert.assertEquals(5, adapter.getGrowth(tile));
        Assert.assertEquals(6, adapter.getGain(tile));
        Assert.assertEquals(7, adapter.getResistance(tile));
        Assert.assertEquals(8, adapter.getScanLevel(tile));
        Assert.assertEquals(9, adapter.getNutrientStorage(tile));
        Assert.assertEquals(10, adapter.getHydrationStorage(tile));
        Assert.assertEquals(11, adapter.getWeedExStorage(tile));
        Assert.assertEquals(12, adapter.getHumidity(tile));
        Assert.assertEquals(13, adapter.getNutrients(tile));
        Assert.assertEquals(14, adapter.getAirQuality(tile));

        when(tile.getCrop()).thenReturn(null);
        Assert.assertEquals("", adapter.getID(tile));
    }

    @Test
    public void heatAndKineticSources() {
        final IHeatSource heat = mock(IHeatSource.class);
        when(heat.maxrequestHeatTick(ForgeDirection.UP)).thenReturn(7);
        Assert.assertEquals(7, new AdapterHeatSource().getMaxKineticEnergy(heat, ForgeDirection.UP));

        final IKineticSource kinetic = mock(IKineticSource.class);
        when(kinetic.maxrequestkineticenergyTick(ForgeDirection.DOWN)).thenReturn(9);
        Assert.assertEquals(9, new AdapterKineticSource().getMaxKineticEnergy(kinetic, ForgeDirection.DOWN));
    }

    @Test
    public void electricMachineProgress() {
        final ic2.core.block.machine.tileentity.TileEntityElectricMachine machine = mock(
            ic2.core.block.machine.tileentity.TileEntityElectricMachine.class);
        machine.maxEnergy = 100;
        machine.energy = 50.0;

        final AdapterElecticMachine adapter = new AdapterElecticMachine();
        Assert.assertNotNull(adapter.getTargetClass());
        Assert.assertEquals(50.0, adapter.getProgress(machine), 0.0001);

        machine.maxEnergy = 0;
        Assert.assertEquals(0.0, adapter.getProgress(machine), 0.0001);
    }
}
