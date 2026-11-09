package openperipheral.integration.mystcraft.v2;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.ForgeDirection;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.xcompwiz.mystcraft.api.APIInstanceProvider;
import com.xcompwiz.mystcraft.api.exception.APIUndefined;
import com.xcompwiz.mystcraft.api.exception.APIVersionRemoved;
import com.xcompwiz.mystcraft.api.exception.APIVersionUndefined;
import com.xcompwiz.mystcraft.api.hook.LinkPropertyAPI;
import com.xcompwiz.mystcraft.api.hook.LinkingAPI;
import com.xcompwiz.mystcraft.api.hook.PageAPI;
import com.xcompwiz.mystcraft.api.item.IItemPageCollection;
import com.xcompwiz.mystcraft.api.linking.ILinkInfo;
import com.xcompwiz.mystcraft.tileentity.TileEntityDesk;

import openmods.fakeplayer.FakePlayerPool;
import openperipheral.api.helpers.Index;

public class MystcraftAdaptersTest {

    private static final Item LINK_BOOK = named("item.myst.linkbook");
    private static final Item AGE_BOOK = named("item.myst.agebook");
    private static final Item PAGE = named("item.myst.page");

    private static Item named(final String name) {
        return new Item() {

            @Override
            public String getUnlocalizedName() {
                return name;
            }

            @Override
            public String getUnlocalizedName(ItemStack stack) {
                return name;
            }
        };
    }

    /** Minimal item that satisfies the page-collection contract used by the desk wrapper. */
    private static class PageCollectionItem extends Item implements IItemPageCollection {

        ItemStack page;

        @Override
        public String getDisplayName(net.minecraft.entity.player.EntityPlayer player, ItemStack stack) {
            return null;
        }

        @Override
        public void setDisplayName(net.minecraft.entity.player.EntityPlayer player, ItemStack stack, String name) {}

        @Override
        public ItemStack addPage(net.minecraft.entity.player.EntityPlayer player, ItemStack stack, ItemStack page) {
            return null;
        }

        @Override
        public ItemStack remove(net.minecraft.entity.player.EntityPlayer player, ItemStack stack, ItemStack page) {
            return null;
        }

        @Override
        public java.util.List<ItemStack> getItems(net.minecraft.entity.player.EntityPlayer player, ItemStack stack) {
            return page == null ? java.util.Collections.<ItemStack>emptyList()
                : java.util.Collections.singletonList(page);
        }
    }

    private WorldServer worldServer;
    private Map<Object, Object> worldPools;

    @Before
    public void resetApi() {
        MystcraftAccess.linkingApi = null;
        MystcraftAccess.linkPropertiesApi = null;
        MystcraftAccess.pageApi = null;
        MystcraftAccess.symbolApi = null;
    }

    @Before
    @SuppressWarnings("unchecked")
    public void installFakePlayerPool() throws Exception {
        worldServer = mock(WorldServer.class);
        final Field field = FakePlayerPool.class.getDeclaredField("worldPools");
        field.setAccessible(true);
        worldPools = (Map<Object, Object>) field.get(null);

        final Class<?> worldPoolClass = Class.forName("openmods.fakeplayer.FakePlayerPool$WorldPool");
        final Object worldPool = mock(worldPoolClass, invocation -> {
            if ("executeOnPlayer".equals(
                invocation.getMethod()
                    .getName())) {
                final FakePlayerPool.PlayerUserReturning<?> user = invocation.getArgument(1);
                return user.usePlayer(null);
            }
            return null;
        });
        worldPools.put(worldServer, worldPool);
    }

    @After
    public void removeFakePlayerPool() {
        worldPools.remove(worldServer);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void v1BookMetadata() {
        final ItemStack stack = new ItemStack(LINK_BOOK);
        final NBTTagCompound tag = new NBTTagCompound();
        tag.setString("agename", "Age 1");
        tag.setInteger("Dimension", 7);
        tag.setInteger("SpawnX", 1);
        tag.setInteger("SpawnY", 2);
        tag.setInteger("SpawnZ", 3);
        tag.setFloat("SpawnYaw", 90.0f);
        stack.setTagCompound(tag);

        final Map<String, Object> map = (Map<String, Object>) new openperipheral.integration.mystcraft.v1.BookMetaProvider()
            .getMeta(LINK_BOOK, stack);
        Assert.assertEquals("link", map.get("type"));
        Assert.assertEquals("Age 1", map.get("destination"));
        Assert.assertEquals(7, map.get("dimension"));
        Assert.assertEquals(90.0f, map.get("spawnYaw"));

        final Map<Integer, Integer> spawn = (Map<Integer, Integer>) map.get("spawn");
        Assert.assertEquals(Integer.valueOf(1), spawn.get(Integer.valueOf(1)));
        Assert.assertEquals(Integer.valueOf(2), spawn.get(Integer.valueOf(2)));
        Assert.assertEquals(Integer.valueOf(3), spawn.get(Integer.valueOf(3)));

        final ItemStack ageStack = new ItemStack(AGE_BOOK);
        final NBTTagCompound ageTag = new NBTTagCompound();
        ageTag.setString("agename", "Age 2");
        ageStack.setTagCompound(ageTag);
        Assert.assertEquals(
            "age",
            ((Map<String, Object>) new openperipheral.integration.mystcraft.v1.BookMetaProvider()
                .getMeta(AGE_BOOK, ageStack)).get("type"));
    }

    @Test
    public void v1PageMetadata() {
        final openperipheral.integration.mystcraft.v1.PageMetaProvider provider = new openperipheral.integration.mystcraft.v1.PageMetaProvider();
        final ItemStack stack = new ItemStack(PAGE);
        final NBTTagCompound tag = new NBTTagCompound();
        tag.setString("symbol", "symbol");
        stack.setTagCompound(tag);

        Assert.assertEquals("symbol", provider.getMeta(PAGE, stack));
        Assert.assertNull(provider.getMeta(new Item(), stack));
    }

    @Test
    public void v2ProvidersAreInertWithoutTheApi() {
        final openperipheral.integration.mystcraft.v2.BookMetaProvider book = new openperipheral.integration.mystcraft.v2.BookMetaProvider();
        final openperipheral.integration.mystcraft.v2.PageMetaProvider page = new openperipheral.integration.mystcraft.v2.PageMetaProvider();
        final LinkingPanelMetaProvider panel = new LinkingPanelMetaProvider();
        final ItemStack stack = new ItemStack(PAGE);

        Assert.assertFalse(book.canApply(null, stack));
        Assert.assertNull(book.getMeta(null, stack));
        Assert.assertFalse(page.canApply(PAGE, stack));
        Assert.assertNull(page.getMeta(PAGE, stack));
        Assert.assertFalse(panel.canApply(PAGE, stack));
        Assert.assertNull(panel.getMeta(PAGE, stack));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void apiWiringEnablesPageMetadata() throws Exception {
        final APIInstanceProvider provider = mock(APIInstanceProvider.class);
        final PageAPI pageApi = mock(PageAPI.class);
        when(provider.getAPIInstance("page-1")).thenReturn(pageApi);

        MystcraftAccess.init(provider);
        Assert.assertSame(pageApi, MystcraftAccess.pageApi);

        final ItemStack stack = new ItemStack(PAGE);
        when(pageApi.getPageSymbol(stack)).thenReturn("sym");

        final openperipheral.integration.mystcraft.v2.PageMetaProvider page = new openperipheral.integration.mystcraft.v2.PageMetaProvider();
        Assert.assertTrue(page.canApply(PAGE, stack));
        final Map<String, Object> map = (Map<String, Object>) page.getMeta(PAGE, stack);
        Assert.assertEquals("sym", map.get("id"));
    }

    @Test
    public void deskNotebookCount() {
        final TileEntityDesk desk = mock(TileEntityDesk.class);
        when(desk.getMaxSurfaceTabCount()).thenReturn(6);
        Assert.assertEquals(6, new AdapterWritingDesk().getMaxNotebookCount(desk));
    }

    @Test
    public void deskNotebookWrapper() {
        final TileEntityDesk desk = mock(TileEntityDesk.class);
        when(desk.getWorldObj()).thenReturn(mock(WorldServer.class));

        final Item pageItem = new PageCollectionItem();
        when(desk.getTabItem((byte) 0)).thenReturn(new ItemStack(pageItem));

        final AdapterWritingDesk adapter = new AdapterWritingDesk();
        Assert.assertNotNull(adapter.getNotebook(desk, Index.fromJava(0, 0)));

        try {
            adapter.getNotebook(desk, Index.fromJava(1, 0));
            Assert.fail("Expected IllegalArgumentException for an empty slot");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void v2BookMetadata() {
        final LinkingAPI linking = mock(LinkingAPI.class);
        final LinkPropertyAPI properties = mock(LinkPropertyAPI.class);
        MystcraftAccess.linkingApi = linking;
        MystcraftAccess.linkPropertiesApi = properties;

        final ILinkInfo info = mock(ILinkInfo.class);
        when(info.getDisplayName()).thenReturn("Age 1");
        when(info.getDimensionUID()).thenReturn(7);
        when(info.getSpawn()).thenReturn(new ChunkCoordinates(1, 2, 3));
        when(info.getSpawnYaw()).thenReturn(90.0f);
        when(info.getFlag("flag1")).thenReturn(true);
        when(info.getFlag("flag2")).thenReturn(false);
        when(properties.getLinkProperties()).thenReturn(Arrays.asList("flag1", "flag2"));
        when(linking.createLinkInfo(org.mockito.ArgumentMatchers.any(NBTTagCompound.class))).thenReturn(info);

        final ItemStack stack = new ItemStack(LINK_BOOK);
        stack.setTagCompound(new NBTTagCompound());

        final openperipheral.integration.mystcraft.v2.BookMetaProvider book = new openperipheral.integration.mystcraft.v2.BookMetaProvider();
        Assert.assertTrue(book.canApply(null, stack));

        final Map<String, Object> map = (Map<String, Object>) book.getMeta(null, stack);
        Assert.assertEquals("link", map.get("type"));
        Assert.assertEquals("Age 1", map.get("destination"));
        Assert.assertEquals(7, map.get("dimension"));
        Assert.assertEquals(java.util.Arrays.asList(1, 2, 3), map.get("spawn"));
        Assert.assertEquals(90.0f, map.get("spawnYaw"));

        final Set<String> flags = (Set<String>) map.get("flags");
        Assert.assertTrue(flags.contains("flag1"));
        Assert.assertFalse(flags.contains("flag2"));

        Assert.assertFalse(book.canApply(null, new ItemStack(LINK_BOOK)));
    }

    @Test
    public void mystcraftApiErrorsAreHandled() throws Exception {
        final APIInstanceProvider provider = mock(APIInstanceProvider.class);
        when(provider.getAPIInstance("undefined")).thenThrow(new APIUndefined("x"));
        when(provider.getAPIInstance("version")).thenThrow(new APIVersionUndefined("x"));
        when(provider.getAPIInstance("removed")).thenThrow(new APIVersionRemoved("x"));
        when(provider.getAPIInstance("boom")).thenThrow(new RuntimeException("boom"));
        when(provider.getAPIInstance("ok")).thenReturn("value");

        Assert.assertNull(MystcraftAccess.getApi(provider, "undefined"));
        Assert.assertNull(MystcraftAccess.getApi(provider, "version"));
        Assert.assertNull(MystcraftAccess.getApi(provider, "removed"));
        Assert.assertNull(MystcraftAccess.getApi(provider, "boom"));
        Assert.assertEquals("value", MystcraftAccess.getApi(provider, "ok"));
    }

    @Test
    public void notebookWrapper() {
        final IItemPageCollection item = mock(IItemPageCollection.class);
        final ItemStack notebook = new ItemStack(new PageCollectionItem());
        final ItemStack first = new ItemStack(new Item());
        final ItemStack second = new ItemStack(new Item());
        when(item.getDisplayName(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
            .thenReturn("Notebook");
        when(item.getItems(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
            .thenReturn(Arrays.asList(first, second));
        when(
            item.remove(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any())).thenReturn(first);
        when(
            item.addPage(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any())).thenReturn(null);

        final NotebookWrapper wrapper = new NotebookWrapper(worldServer, item, notebook);
        Assert.assertEquals("Notebook", wrapper.getName());
        Assert.assertEquals(
            2,
            wrapper.getPages()
                .size());
        Assert.assertEquals(2, wrapper.getSlotCount());
        Assert.assertSame(first, wrapper.getPageFromSlot(Index.fromJava(0, 0)));
        Assert.assertSame(first, wrapper.removePage(first));
        Assert.assertNull(wrapper.addPage(first));
    }

    @Test
    public void deskPullAndWriteSymbol() {
        final TileEntityDesk desk = mock(TileEntityDesk.class);
        when(desk.getWorldObj()).thenReturn(worldServer);
        desk.xCoord = 0;
        desk.yCoord = 0;
        desk.zCoord = 0;

        final ItemStack page = new ItemStack(new Item());
        final PageCollectionItem pageItem = new PageCollectionItem();
        pageItem.page = page;
        final ItemStack notebook = new ItemStack(pageItem);
        when(desk.getTabItem((byte) 0)).thenReturn(notebook);

        final TileEntity neighbour = mock(
            TileEntity.class,
            org.mockito.Mockito.withSettings()
                .extraInterfaces(IInventory.class));
        when(worldServer.getTileEntity(1, 0, 0)).thenReturn(neighbour);
        final IInventory inventory = (IInventory) neighbour;
        when(inventory.getSizeInventory()).thenReturn(1);
        when(inventory.getStackInSlot(0)).thenReturn(page);

        final AdapterWritingDesk adapter = new AdapterWritingDesk();
        Assert.assertTrue(
            adapter.pullNotebookPage(desk, Index.fromJava(0, 0), ForgeDirection.EAST, Index.fromJava(0, 0)));
        verify(inventory).setInventorySlotContents(0, null);

        MystcraftAccess.pageApi = mock(PageAPI.class);
        when(MystcraftAccess.pageApi.getPageSymbol(page)).thenReturn("sym");
        adapter.writeSymbol(desk, Index.fromJava(0, 0), Index.fromJava(0, 0));
        verify(desk).writeSymbol(
            org.mockito.ArgumentMatchers.isNull(EntityPlayer.class),
            org.mockito.ArgumentMatchers.eq("sym"));
    }
}
