package openperipheral.integration.railcraft;

import net.minecraft.item.Item;

import cpw.mods.fml.common.registry.GameRegistry.ObjectHolder;
import openmods.Mods;

@ObjectHolder(Mods.RAILCRAFT)
public class TicketItemHolder {

    @ObjectHolder("routing.ticket")
    public static Item ticket = null;

    @ObjectHolder("routing.ticket.gold")
    public static Item ticketGold = null;
}
