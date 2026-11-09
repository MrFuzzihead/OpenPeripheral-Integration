package openperipheral.integration.vanilla;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.UUID;

import net.minecraft.block.BlockJukebox.TileEntityJukebox;
import net.minecraft.block.BlockNote;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.MobSpawnerBaseLogic;
import net.minecraft.tileentity.TileEntityBeacon;
import net.minecraft.tileentity.TileEntityBrewingStand;
import net.minecraft.tileentity.TileEntityComparator;
import net.minecraft.tileentity.TileEntityDaylightDetector;
import net.minecraft.tileentity.TileEntityFlowerPot;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraft.tileentity.TileEntityNote;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.tileentity.TileEntitySkull;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;
import net.minecraftforge.fluids.IFluidTank;

import org.junit.Assert;
import org.junit.Test;

import com.mojang.authlib.GameProfile;

import openperipheral.api.helpers.Index;

/**
 * Unit coverage for the vanilla adapters that only need a tile entity and (sometimes) a world. All items
 * are constructed directly instead of being looked up from {@code net.minecraft.init.Items}: the global
 * registries need FML's launch class loader, which a plain JUnit JVM does not have.
 */
public class VanillaTileEntityAdaptersTest {

    private static World world() {
        return mock(World.class);
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Class<?> type = target.getClass();
        while (type != null) {
            try {
                final Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                field.set(target, value);
                return;
            } catch (NoSuchFieldException e) {
                type = type.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }

    @Test
    public void signLinesAreTrimmedPaddedAndRead() {
        final World world = world();
        final TileEntitySign sign = new TileEntitySign();
        sign.setWorldObj(world);
        sign.xCoord = 1;
        sign.yCoord = 2;
        sign.zCoord = 3;
        sign.signText = new String[] { "", "", "", "" };

        final AdapterSign adapter = new AdapterSign();
        adapter.setLine(sign, Index.fromJava(0, 0), "0123456789abcdefghij");
        Assert.assertEquals("0123456789abcde", sign.signText[0]);
        Assert.assertEquals("0123456789abcde", adapter.getLine(sign, Index.fromJava(0, 0)));

        adapter.setLines(sign, new String[] { "one", "two" });
        Assert.assertArrayEquals(new String[] { "one", "two", "", "" }, sign.signText);
        Assert.assertEquals("one\ntwo\n\n", adapter.getText(sign));

        adapter.setText(sign, "hello\nworld");
        Assert.assertArrayEquals(new String[] { "hello", "world", "", "" }, sign.signText);

        verify(world, org.mockito.Mockito.times(3)).markBlockForUpdate(1, 2, 3);
    }

    @Test
    public void signTextAcceptsFourLines() {
        final TileEntitySign sign = new TileEntitySign();
        sign.setWorldObj(world());
        sign.signText = new String[] { "", "", "", "" };

        final AdapterSign adapter = new AdapterSign();
        adapter.setText(sign, "one\ntwo\nthree\nfour");

        Assert.assertArrayEquals(new String[] { "one", "two", "three", "four" }, sign.signText);
        Assert.assertEquals("one\ntwo\nthree\nfour", adapter.getText(sign));
    }

    @Test(expected = IllegalArgumentException.class)
    public void signTextRejectsMoreThanFourLines() {
        final TileEntitySign sign = new TileEntitySign();
        sign.setWorldObj(world());
        sign.signText = new String[] { "", "", "", "" };
        new AdapterSign().setText(sign, "a\nb\nc\nd\ne");
    }

    @Test
    public void setLinesTruncatesToTheVanillaLimit() {
        final TileEntitySign sign = new TileEntitySign();
        sign.setWorldObj(world());
        sign.signText = new String[] { "", "", "", "" };

        new AdapterSign().setLines(sign, new String[] { "0123456789abcdefghij", "short" });

        Assert.assertEquals("0123456789abcde", sign.signText[0]);
        Assert.assertEquals("short", sign.signText[1]);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void signLineIndexIsChecked() {
        final TileEntitySign sign = new TileEntitySign();
        sign.setWorldObj(world());
        sign.signText = new String[] { "", "", "", "" };
        new AdapterSign().getLine(sign, Index.fromJava(4, 0));
    }

    @Test
    public void notePitchIsNormalisedToTheValidRange() {
        Assert.assertEquals(0, AdapterNoteBlock.normalizePitch(0));
        Assert.assertEquals(24, AdapterNoteBlock.normalizePitch(24));
        Assert.assertEquals(0, AdapterNoteBlock.normalizePitch(25));
        Assert.assertEquals(5, AdapterNoteBlock.normalizePitch(30));
        Assert.assertEquals(24, AdapterNoteBlock.normalizePitch(-1));
        Assert.assertEquals(0, AdapterNoteBlock.normalizePitch(-25));
    }

    @Test
    public void noteBlockPlaySoundUsesRelativeCoordinates() {
        final World world = world();
        final TileEntityNote note = new TileEntityNote();
        note.setWorldObj(world);
        note.xCoord = 1;
        note.yCoord = 2;
        note.zCoord = 3;

        new AdapterNoteBlock().playSound(note, "sound", 1.0f, 0.5f, 0.5, -0.5, null);
        verify(world).playSoundEffect(2.0, 2.0, 3.5, "sound", 0.5f, 1.0f);
    }

    @Test
    public void noteBlockNoteIsExposed() {
        final TileEntityNote note = new TileEntityNote();
        note.note = 17;
        // Both setPitch() and changePitch() fire ForgeHooks.onNoteChange, whose static initialiser
        // needs the item registry; only the getter is testable in a plain JVM.
        Assert.assertEquals(17, new AdapterNoteBlock().getNote(note));
    }

    @Test
    public void noteBlockTriggerSendsABlockEvent() {
        final World world = world();
        final BlockNote woodenBlock = new BlockNote();
        final AirBlock air = new AirBlock();
        // triggerNote() looks at the block above (must be air) and below (wood -> instrument 4).
        when(world.getBlock(0, 1, 0)).thenReturn(air);
        when(world.getBlock(0, -1, 0)).thenReturn(woodenBlock);

        final TileEntityNote note = new TileEntityNote();
        note.setWorldObj(world);
        note.note = 7;
        new AdapterNoteBlock().triggerNote(note);

        // Blocks.noteblock is null without the FML registries, so the block argument is null here.
        verify(world).addBlockEvent(0, 0, 0, (net.minecraft.block.Block) null, 4, 7);
    }

    /** A block whose material is air, standing in for the registry-backed {@code Blocks.air}. */
    private static class AirBlock extends net.minecraft.block.Block {

        AirBlock() {
            super(net.minecraft.block.material.Material.air);
        }
    }

    @Test
    public void comparatorOutputSignalRoundTrips() {
        final TileEntityComparator comparator = new TileEntityComparator();
        final AdapterComparator adapter = new AdapterComparator();
        comparator.setOutputSignal(11);
        Assert.assertEquals(11, adapter.getOutputSignal(comparator));
    }

    @Test
    public void furnaceFieldsAreExposed() {
        final TileEntityFurnace furnace = new TileEntityFurnace();
        final AdapterFurnace adapter = new AdapterFurnace();

        Assert.assertFalse(adapter.isBurning(furnace));

        furnace.furnaceBurnTime = 200;
        furnace.currentItemBurnTime = 1600;
        furnace.furnaceCookTime = 42;

        Assert.assertTrue(adapter.isBurning(furnace));
        Assert.assertEquals(200, adapter.getBurnTime(furnace));
        Assert.assertEquals(1600, adapter.getCurrentItemBurnTime(furnace));
        Assert.assertEquals(42, adapter.getCookTime(furnace));
    }

    @Test
    public void brewingStandBrewTimeIsExposed() throws Exception {
        final TileEntityBrewingStand stand = new TileEntityBrewingStand();
        setField(stand, "brewTime", 123);
        Assert.assertEquals(123, new AdapterBrewingStand().getBrewTime(stand));
    }

    @Test
    public void skullTypesAndPlayerProfile() throws Exception {
        final AdapterSkull adapter = new AdapterSkull();
        final TileEntitySkull skull = new TileEntitySkull();

        skull.func_152107_a(0);
        Assert.assertEquals("skeleton", adapter.getType(skull));
        skull.func_152107_a(1);
        Assert.assertEquals("wither_skeleton", adapter.getType(skull));
        skull.func_152107_a(2);
        Assert.assertEquals("zombie", adapter.getType(skull));
        skull.func_152107_a(4);
        Assert.assertEquals("creeper", adapter.getType(skull));
        skull.func_152107_a(99);
        Assert.assertEquals("unknown", adapter.getType(skull));

        skull.func_152107_a(3);
        Assert.assertArrayEquals(new Object[] { null, null }, adapter.getPlayer(skull));

        // func_152106_a() resolves the profile through MinecraftServer#getPlayerProfileCache,
        // which does not exist headlessly, so inject the private field instead.
        final UUID id = UUID.randomUUID();
        final GameProfile profile = new GameProfile(id, "TestPlayer");
        final Field profileField = TileEntitySkull.class.getDeclaredField("field_152110_j");
        profileField.setAccessible(true);
        profileField.set(skull, profile);
        Assert.assertArrayEquals(new Object[] { id, "TestPlayer" }, adapter.getPlayer(skull));
    }

    @Test
    public void flowerPotContentsAreWrappedInAnItemStack() {
        final AdapterFlowerPot adapter = new AdapterFlowerPot();
        final Item item = new Item();

        Assert.assertNull(adapter.getContents(new TileEntityFlowerPot()));

        final TileEntityFlowerPot pot = new TileEntityFlowerPot(item, 3);
        final ItemStack contents = adapter.getContents(pot);
        Assert.assertSame(item, contents.getItem());
        Assert.assertEquals(1, contents.stackSize);
        Assert.assertEquals(3, contents.getItemDamage());
    }

    @Test
    public void recordPlayerReturnsTheInsertedRecord() {
        final TileEntityJukebox jukebox = new TileEntityJukebox();
        final ItemStack record = new ItemStack(new Item());

        Assert.assertNull(new AdapterRecordPlayer().getRecord(jukebox));

        jukebox.func_145857_a(record);
        Assert.assertSame(record, new AdapterRecordPlayer().getRecord(jukebox));
    }

    @Test
    public void beaconEffectsAndLevels() {
        final TileEntityBeacon beacon = new TileEntityBeacon();
        final AdapterBeacon adapter = new AdapterBeacon();

        Assert.assertEquals("None", adapter.getPrimaryEffect(beacon));
        Assert.assertEquals("None", adapter.getSecondaryEffect(beacon));

        beacon.func_146005_c(4);
        beacon.setPrimaryEffect(1);
        beacon.setSecondaryEffect(3);

        Assert.assertEquals("potion.moveSpeed", adapter.getPrimaryEffect(beacon));
        Assert.assertEquals("potion.digSpeed", adapter.getSecondaryEffect(beacon));
        Assert.assertEquals(4, adapter.getLevels(beacon));
    }

    @Test
    public void mobSpawnerReadsTheSpawnedEntityName() {
        final TileEntityMobSpawner spawner = mock(TileEntityMobSpawner.class);
        final MobSpawnerBaseLogic logic = mock(MobSpawnerBaseLogic.class);
        when(spawner.func_145881_a()).thenReturn(logic);
        when(logic.getEntityNameToSpawn()).thenReturn("Zombie");

        Assert.assertEquals("Zombie", new AdapterMobSpawner().getSpawningMobName(spawner));
    }

    @Test
    public void fluidTankInfoIsPassedThrough() {
        final IFluidTank tank = mock(IFluidTank.class);
        final FluidTankInfo info = new FluidTankInfo(mock(FluidStack.class), 2000);
        when(tank.getInfo()).thenReturn(info);

        Assert.assertSame(info, new AdapterFluidTank().getInfo(tank));
    }

    @Test
    public void fluidHandlerDefaultsToUnknownDirection() {
        final IFluidHandler handler = mock(IFluidHandler.class);
        final FluidTankInfo[] tanks = new FluidTankInfo[] { new FluidTankInfo(null, 1000) };
        when(handler.getTankInfo(ForgeDirection.UNKNOWN)).thenReturn(tanks);
        when(handler.getTankInfo(ForgeDirection.NORTH)).thenReturn(tanks);

        final AdapterFluidHandler adapter = new AdapterFluidHandler();
        Assert.assertSame(tanks, adapter.getTankInfo(handler, null));
        Assert.assertSame(tanks, adapter.getTankInfo(handler, ForgeDirection.NORTH));

        verify(handler).getTankInfo(ForgeDirection.UNKNOWN);
        verify(handler).getTankInfo(ForgeDirection.NORTH);
    }

    @Test
    public void daylightSensorLightLevels() throws Exception {
        final World world = world();
        final WorldProvider provider = mock(WorldProvider.class);
        setField(world, "provider", provider);
        setField(world, "skylightSubtracted", 2);

        final TileEntityDaylightDetector sensor = new TileEntityDaylightDetector();
        sensor.setWorldObj(world);
        sensor.xCoord = 4;
        sensor.yCoord = 5;
        sensor.zCoord = 6;

        when(world.getSavedLightValue(EnumSkyBlock.Sky, 4, 5, 6)).thenReturn(15);
        when(world.getSavedLightValue(EnumSkyBlock.Block, 4, 5, 6)).thenReturn(7);
        when(world.getCelestialAngle(1)).thenReturn(0.5f);

        final AdapterDaylightSensor adapter = new AdapterDaylightSensor();
        Assert.assertTrue(adapter.hasSky(sensor));
        Assert.assertEquals(13, adapter.getSkyLight(sensor));
        Assert.assertEquals(7, adapter.getBlockLight(sensor));
        Assert.assertEquals(180.0f, adapter.getCelestialAngle(sensor), 0.0001f);

        provider.hasNoSky = true;
        Assert.assertFalse(adapter.hasSky(sensor));
    }

    @Test(expected = IllegalArgumentException.class)
    public void daylightSensorWithoutSkyCannotReportSkyLight() throws Exception {
        final World world = world();
        final WorldProvider provider = mock(WorldProvider.class);
        provider.hasNoSky = true;
        setField(world, "provider", provider);

        final TileEntityDaylightDetector sensor = new TileEntityDaylightDetector();
        sensor.setWorldObj(world);

        new AdapterDaylightSensor().getSkyLight(sensor);
    }

    @Test
    public void noteBlockMaterialIsWood() {
        Assert.assertEquals(net.minecraft.block.material.Material.wood, new BlockNote().getMaterial());
    }
}
