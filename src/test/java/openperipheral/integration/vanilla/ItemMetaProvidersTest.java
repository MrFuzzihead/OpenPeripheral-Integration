package openperipheral.integration.vanilla;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.Item;
import net.minecraft.item.Item.ToolMaterial;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemEnchantedBook;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidContainerItem;

import org.junit.Assert;
import org.junit.Test;

/**
 * Covers the item metadata providers that can be exercised without the global item registry. Items are
 * instantiated directly; {@code net.minecraft.init.Items} only resolves once FML has bootstrapped.
 */
public class ItemMetaProvidersTest {

    /** {@link ItemPickaxe}'s constructor is protected, so tests need a tiny subclass. */
    static class TestPickaxe extends ItemPickaxe {

        TestPickaxe(ToolMaterial material) {
            super(material);
        }
    }

    @Test
    public void burnTimeUsesTheFurnaceFuelValue() {
        final BurnTimeMetaProvider provider = new BurnTimeMetaProvider();
        final ItemStack woodenPick = new ItemStack(new TestPickaxe(ToolMaterial.WOOD));
        final ItemStack unknown = new ItemStack(new Item());

        Assert.assertTrue(provider.canApply(woodenPick.getItem(), woodenPick));
        Assert.assertEquals(200, provider.getMeta(woodenPick.getItem(), woodenPick));

        Assert.assertFalse(provider.canApply(unknown.getItem(), unknown));
        Assert.assertNull(provider.getMeta(unknown.getItem(), unknown));
    }

    @Test
    public void enchantmentsAreListedWithLevels() {
        final ItemStack sword = new ItemStack(new ItemSword(ToolMaterial.EMERALD));
        sword.addEnchantment(Enchantment.sharpness, 3);

        final EnchantmentMetaProvider provider = new EnchantmentMetaProvider();
        Assert.assertTrue(provider.canApply(sword.getItem(), sword));

        @SuppressWarnings("unchecked")
        final List<Map<String, Object>> ench = (List<Map<String, Object>>) provider.getMeta(sword.getItem(), sword);
        Assert.assertEquals(1, ench.size());
        Assert.assertEquals(
            3,
            ((Number) ench.get(0)
                .get("level")).intValue());
        Assert.assertEquals(
            Enchantment.sharpness.getName(),
            ench.get(0)
                .get("name"));
        Assert.assertNotNull(
            ench.get(0)
                .get("fullName"));

        final ItemStack plain = new ItemStack(new ItemSword(ToolMaterial.EMERALD));
        Assert.assertFalse(provider.canApply(plain.getItem(), plain));
    }

    @Test
    public void fingerprintHashesNbt() {
        final FingerprintMetaProvider provider = new FingerprintMetaProvider();
        final ItemStack stack = new ItemStack(new Item());
        Assert.assertFalse(provider.canApply(stack.getItem(), stack));

        stack.setTagCompound(new NBTTagCompound());
        stack.getTagCompound()
            .setInteger("value", 5);

        Assert.assertTrue(provider.canApply(stack.getItem(), stack));
        final Object hash = provider.getMeta(stack.getItem(), stack);
        Assert.assertTrue(hash instanceof String);
        Assert.assertFalse(((String) hash).isEmpty());
    }

    @Test
    public void armorMaterialAndSlotAreReported() {
        final ItemArmor helmet = new ItemArmor(ItemArmor.ArmorMaterial.DIAMOND, 0, 0);
        final ItemStack stack = new ItemStack(helmet);

        @SuppressWarnings("unchecked")
        final Map<String, Object> meta = (Map<String, Object>) new ItemArmorMetaProvider().getMeta(helmet, stack);

        Assert.assertEquals(ItemArmor.ArmorMaterial.DIAMOND, meta.get("material"));
        Assert.assertEquals("helmet", meta.get("type"));
        Assert.assertNull(meta.get("color"));
    }

    @Test
    public void leatherArmorReportsItsColour() {
        final ItemArmor boots = new ItemArmor(ItemArmor.ArmorMaterial.CLOTH, 0, 3);
        final ItemStack stack = new ItemStack(boots);
        boots.func_82813_b(stack, 0x123456);

        @SuppressWarnings("unchecked")
        final Map<String, Object> meta = (Map<String, Object>) new ItemArmorMetaProvider().getMeta(boots, stack);
        Assert.assertEquals(0x123456, meta.get("color"));
        Assert.assertEquals("boots", meta.get("type"));
    }

    @Test
    public void enchantmentListIgnoresOutOfRangeIds() {
        final net.minecraft.nbt.NBTTagList list = new net.minecraft.nbt.NBTTagList();
        final NBTTagCompound valid = new NBTTagCompound();
        valid.setShort("id", (short) Enchantment.sharpness.effectId);
        valid.setShort("lvl", (short) 1);
        list.appendTag(valid);

        final NBTTagCompound tooLarge = new NBTTagCompound();
        tooLarge.setShort("id", (short) 5000);
        tooLarge.setShort("lvl", (short) 1);
        list.appendTag(tooLarge);

        final NBTTagCompound negative = new NBTTagCompound();
        negative.setShort("id", (short) -3);
        negative.setShort("lvl", (short) 1);
        list.appendTag(negative);

        final List<?> entries = (List<?>) ModuleVanilla.listEnchantments(list);
        Assert.assertEquals(1, entries.size());
    }

    @Test
    public void enchantmentListSkipsUnusedIds() {
        int unusedId = -1;
        for (int i = 0; i < Enchantment.enchantmentsList.length; i++) {
            if (Enchantment.enchantmentsList[i] == null) {
                unusedId = i;
                break;
            }
        }
        Assert.assertTrue("No unused enchantment id in the vanilla table", unusedId >= 0);

        final net.minecraft.nbt.NBTTagList list = new net.minecraft.nbt.NBTTagList();
        final NBTTagCompound valid = new NBTTagCompound();
        valid.setShort("id", (short) Enchantment.sharpness.effectId);
        valid.setShort("lvl", (short) 2);
        list.appendTag(valid);

        final NBTTagCompound unused = new NBTTagCompound();
        unused.setShort("id", (short) unusedId);
        unused.setShort("lvl", (short) 1);
        list.appendTag(unused);

        final List<?> entries = (List<?>) ModuleVanilla.listEnchantments(list);
        Assert.assertEquals(1, entries.size());
        @SuppressWarnings("unchecked")
        final Map<String, Object> entry = (Map<String, Object>) entries.get(0);
        Assert.assertEquals(2, ((Number) entry.get("level")).intValue());
    }

    @Test
    public void toolClassAndHarvestLevelAreReported() {
        final TestPickaxe pick = new TestPickaxe(ToolMaterial.EMERALD);
        final ItemStack stack = new ItemStack(pick);

        final ItemToolClassMetaProvider classProvider = new ItemToolClassMetaProvider();
        Assert.assertTrue(classProvider.canApply(pick, stack));
        @SuppressWarnings("unchecked")
        final Map<String, Object> classes = (Map<String, Object>) classProvider.getMeta(pick, stack);
        Assert.assertEquals(3, classes.get("pickaxe"));

        @SuppressWarnings("unchecked")
        final Map<String, Object> tool = (Map<String, Object>) new ItemToolMetaProvider().getMeta(pick, stack);
        Assert.assertEquals(ToolMaterial.EMERALD.name(), tool.get("material"));
    }

    @Test
    public void swordMaterialIsReported() {
        final ItemSword sword = new ItemSword(ToolMaterial.GOLD);
        @SuppressWarnings("unchecked")
        final Map<String, Object> meta = (Map<String, Object>) new ItemSwordMetaProvider()
            .getMeta(sword, new ItemStack(sword));
        Assert.assertEquals(ToolMaterial.GOLD.name(), meta.get("material"));
    }

    @Test
    public void potionsReportSplashFlagAndEffects() {
        final ItemPotion potion = new ItemPotion();
        @SuppressWarnings("unchecked")
        final Map<String, Object> meta = (Map<String, Object>) new ItemPotionMetaProvider()
            .getMeta(potion, new ItemStack(potion));

        Assert.assertEquals(Boolean.FALSE, meta.get("splash"));
        Assert.assertNotNull(meta.get("effects"));

        final Map<String, Object> info = ItemPotionMetaProvider.getPotionInfo(1);
        Assert.assertNotNull(info);
        Assert.assertNotNull(info.get("name"));
        Assert.assertNull(ItemPotionMetaProvider.getPotionInfo(-1));
        Assert.assertNull(ItemPotionMetaProvider.getPotionInfo(9999));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void potionEffectsAreListed() {
        final ItemPotion potion = new ItemPotion();
        final ItemStack stack = new ItemStack(potion, 1, 8193);
        final Map<String, Object> meta = (Map<String, Object>) new ItemPotionMetaProvider().getMeta(potion, stack);
        final List<?> effects = (List<?>) meta.get("effects");
        Assert.assertNotNull(effects);
        Assert.assertFalse("Expected at least one potion effect for meta 8193", effects.isEmpty());
    }

    @Test
    public void potionInfoHandlesNullEntries() {
        int nullIndex = -1;
        for (int i = 0; i < net.minecraft.potion.Potion.potionTypes.length; i++) {
            if (net.minecraft.potion.Potion.potionTypes[i] == null) {
                nullIndex = i;
                break;
            }
        }
        if (nullIndex >= 0) Assert.assertNull(ItemPotionMetaProvider.getPotionInfo(nullIndex));
    }

    @Test
    public void enchantedBookWithoutDataHasNoMetadata() {
        final ItemEnchantedBook book = new ItemEnchantedBook();
        final Object meta = new EnchantedBookMetaProvider().getMeta(book, new ItemStack(book));
        Assert.assertTrue(meta instanceof List);
        Assert.assertTrue(((List<?>) meta).isEmpty());
    }

    @Test
    public void fluidContainerReportsContentsAndCapacity() {
        final IFluidContainerItem container = mock(IFluidContainerItem.class);
        final ItemStack stack = new ItemStack(new Item());
        final FluidStack fluid = mock(FluidStack.class);
        when(container.getFluid(stack)).thenReturn(fluid);
        when(container.getCapacity(stack)).thenReturn(1000);

        @SuppressWarnings("unchecked")
        final Map<String, Object> meta = (Map<String, Object>) new FluidContainerMetaProvider()
            .getMeta(container, stack);

        Assert.assertSame(fluid, meta.get("contents"));
        Assert.assertEquals(1000, meta.get("capacity"));
    }

    @Test
    public void itemFingerprintConversionIsBidirectional() {
        final ItemFingerprint fingerprint = new ItemFingerprint("minecraft:stone", 3, "abc");
        final ConverterItemFingerprint converter = new ConverterItemFingerprint();

        @SuppressWarnings("unchecked")
        final Map<String, Object> out = (Map<String, Object>) converter.fromJava(null, fingerprint);
        Assert.assertEquals(fingerprint.id.toString(), out.get("id"));
        Assert.assertEquals(3, out.get("dmg"));
        Assert.assertEquals("abc", out.get("nbt_hash"));

        Assert.assertNull(converter.fromJava(null, new Object()));

        final ItemFingerprint in = (ItemFingerprint) converter.toJava(null, out, ItemFingerprint.class);
        Assert.assertEquals(fingerprint.id.toString(), in.id.toString());
        Assert.assertEquals(3, in.damage);
        Assert.assertEquals("abc", in.nbtHash);

        Assert.assertNull(converter.toJava(null, out, String.class));
    }

    @Test
    public void itemFingerprintRejectsMalformedTables() {
        final ConverterItemFingerprint converter = new ConverterItemFingerprint();
        final Map<String, Object> missingId = new java.util.HashMap<String, Object>();
        try {
            converter.toJava(null, missingId, ItemFingerprint.class);
            Assert.fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // expected
        }

        final Map<String, Object> wrongId = new java.util.HashMap<String, Object>();
        wrongId.put("id", 5);
        try {
            converter.toJava(null, wrongId, ItemFingerprint.class);
            Assert.fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    @Test
    public void itemFingerprintRejectsNonStringNbtHash() {
        final java.util.Map<String, Object> table = new java.util.HashMap<String, Object>();
        table.put("id", "minecraft:stone");
        table.put("nbt_hash", 5);

        try {
            new ConverterItemFingerprint().toJava(null, table, ItemFingerprint.class);
            Assert.fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    @Test
    public void itemFingerprintToStringAndEqualitySemantics() {
        final ItemFingerprint fingerprint = new ItemFingerprint("minecraft:stone", 3, "abc");
        Assert.assertTrue(
            fingerprint.toString()
                .contains("minecraft:stone"));
        Assert.assertTrue(
            fingerprint.toString()
                .contains("3"));
        Assert.assertTrue(
            fingerprint.toString()
                .contains("abc"));

        Assert.assertEquals(fingerprint, new ItemFingerprint("minecraft:stone", 3, "abc"));
        Assert.assertEquals(fingerprint.hashCode(), new ItemFingerprint("minecraft:stone", 3, "abc").hashCode());
        Assert.assertNotEquals(fingerprint, new ItemFingerprint("minecraft:stone", 4, "abc"));
        Assert.assertNotEquals(fingerprint, new ItemFingerprint("minecraft:stone", 3, "other"));
        Assert.assertNotEquals(fingerprint, new ItemFingerprint("minecraft:diamond", 3, "abc"));
    }

    @Test
    public void fingerprintOnlyRemembersTheHashWhenPresent() {
        final ItemFingerprint fingerprint = new ItemFingerprint("minecraft:stone", -1, null);
        @SuppressWarnings("unchecked")
        final Map<String, Object> out = (Map<String, Object>) new ConverterItemFingerprint()
            .fromJava(null, fingerprint);
        Assert.assertFalse(out.containsKey("nbt_hash"));
    }

    @Test
    public void mockitoCanMockFinalMinecraftClasses() {
        // mockito-inline is on the test classpath specifically for this.
        final ItemStack mockStack = mock(ItemStack.class);
        when(mockStack.getItem()).thenReturn(new Item());
        Assert.assertNotNull(mockStack.getItem());
    }
}
