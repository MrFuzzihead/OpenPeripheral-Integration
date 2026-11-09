package openperipheral.integration.railcraft;

import java.lang.reflect.Field;
import java.util.Map;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TicketMetaProviderTest {

    private Item originalTicket;
    private Item originalGold;

    @Before
    public void installTickets() throws Exception {
        final Field ticket = TicketItemHolder.class.getDeclaredField("ticket");
        final Field gold = TicketItemHolder.class.getDeclaredField("ticketGold");
        ticket.setAccessible(true);
        gold.setAccessible(true);
        originalTicket = (Item) ticket.get(null);
        originalGold = (Item) gold.get(null);

        ticket.set(null, new Item());
        gold.set(null, new Item());
    }

    @After
    public void restoreTickets() throws Exception {
        final Field ticket = TicketItemHolder.class.getDeclaredField("ticket");
        final Field gold = TicketItemHolder.class.getDeclaredField("ticketGold");
        ticket.setAccessible(true);
        gold.setAccessible(true);
        ticket.set(null, originalTicket);
        gold.set(null, originalGold);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void ticketMetadata() throws Exception {
        final Item regular = TicketItemHolder.ticket;
        final Item golden = TicketItemHolder.ticketGold;
        final TicketMetaProvider provider = new TicketMetaProvider();

        final ItemStack stack = new ItemStack(regular);
        final NBTTagCompound tag = new NBTTagCompound();
        tag.setString("owner", "Steve");
        tag.setString("dest", "station");
        stack.setTagCompound(tag);

        Assert.assertTrue(provider.canApply(regular, stack));
        final Map<String, Object> map = (Map<String, Object>) provider.getMeta(regular, stack);
        Assert.assertEquals(Boolean.FALSE, map.get("golden"));
        Assert.assertEquals("Steve", map.get("owner"));
        Assert.assertEquals("station", map.get("dest"));

        final ItemStack goldStack = new ItemStack(golden);
        Assert.assertTrue(provider.canApply(golden, goldStack));
        final Map<String, Object> goldMap = (Map<String, Object>) provider.getMeta(golden, goldStack);
        Assert.assertEquals(Boolean.TRUE, goldMap.get("golden"));
        Assert.assertFalse(goldMap.containsKey("owner"));

        Assert.assertFalse(provider.canApply(new Item(), new ItemStack(new Item())));
    }
}
