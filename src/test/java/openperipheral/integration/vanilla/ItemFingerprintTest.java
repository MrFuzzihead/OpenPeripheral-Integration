package openperipheral.integration.vanilla;

import net.minecraft.item.ItemStack;

import org.junit.Assert;
import org.junit.Test;

public class ItemFingerprintTest {

    @Test
    public void acceptsWellFormedId() {
        final ItemFingerprint fingerprint = new ItemFingerprint("minecraft:stone", 3, "hash");
        Assert.assertEquals(3, fingerprint.damage);
        Assert.assertEquals("hash", fingerprint.nbtHash);
        Assert.assertTrue(
            fingerprint.toString()
                .contains("minecraft:stone"));
    }

    @Test
    public void equalityUsesIdDamageAndNbtHash() {
        final ItemFingerprint fingerprint = new ItemFingerprint("minecraft:stone", 3, "hash");

        Assert.assertEquals(fingerprint, fingerprint);
        Assert.assertEquals(fingerprint, new ItemFingerprint("minecraft:stone", 3, "hash"));
        Assert.assertEquals(fingerprint.hashCode(), new ItemFingerprint("minecraft:stone", 3, "hash").hashCode());
        Assert.assertNotEquals(fingerprint, new ItemFingerprint("minecraft:stone", 3, null));
        Assert.assertNotEquals(fingerprint, "minecraft:stone");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsIdWithoutNamespace() {
        new ItemFingerprint("stone", 0, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsEmptyNamespace() {
        new ItemFingerprint(":stone", 0, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNullId() {
        new ItemFingerprint((String) null, 0, null);
    }

    @Test(expected = NullPointerException.class)
    public void rejectsNullStack() {
        new ItemFingerprint((ItemStack) null);
    }
}
