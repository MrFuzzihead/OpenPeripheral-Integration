package openperipheral.integration.mystcraft.v2;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.xcompwiz.mystcraft.api.APIInstanceProvider;
import com.xcompwiz.mystcraft.api.hook.PageAPI;
import com.xcompwiz.mystcraft.tileentity.TileEntityDesk;

public class MystcraftAdaptersTest {

    private static final Item LINK_BOOK = new Item() {

        @Override
        public String getUnlocalizedName() {
            return "item.myst.linkbook";
        }
    };

    private static final Item AGE_BOOK = new Item() {

        @Override
        public String getUnlocalizedName() {
            return "item.myst.agebook";
        }
    };

    private static final Item PAGE = new Item() {

        @Override
        public String getUnlocalizedName() {
            return "item.myst.page";
        }
    };

    @Before
    public void resetApi() {
        MystcraftAccess.linkingApi = null;
        MystcraftAccess.linkPropertiesApi = null;
        MystcraftAccess.pageApi = null;
        MystcraftAccess.symbolApi = null;
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
}
