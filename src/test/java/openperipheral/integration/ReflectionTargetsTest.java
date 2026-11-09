package openperipheral.integration;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

/**
 * Keeps the reflection target table honest: every entry must be a plausible fully-qualified class name and
 * no two entries may point at the same class (a duplicated literal is how the Mystcraft desk name was
 * maintained in two unrelated files before batch 5).
 */
public class ReflectionTargetsTest {

    @Test
    public void targetsAreUniqueAndLookLikeClassNames() throws Exception {
        final Set<String> seen = new HashSet<String>();
        int count = 0;

        for (Class<?> group : ReflectionTargets.class.getDeclaredClasses()) {
            for (Field field : group.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers()) || field.getType() != String.class) continue;

                field.setAccessible(true);
                final String value = (String) field.get(null);
                final String name = group.getSimpleName() + "." + field.getName();

                Assert.assertNotNull(name, value);
                Assert.assertFalse(
                    name,
                    value.trim()
                        .isEmpty());
                Assert.assertTrue(name + " is not a class name: " + value, value.indexOf('.') > 0);
                Assert.assertTrue("Duplicate reflection target " + value + " (" + name + ")", seen.add(value));
                count++;
            }
        }

        Assert.assertTrue("Reflection target table looks incomplete: " + count, count >= 25);
    }
}
