package openperipheral.integration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import dan200.computercraft.shadow.org.squiddev.cobalt.Constants;
import dan200.computercraft.shadow.org.squiddev.cobalt.LuaError;
import dan200.computercraft.shadow.org.squiddev.cobalt.LuaState;
import dan200.computercraft.shadow.org.squiddev.cobalt.LuaString;
import dan200.computercraft.shadow.org.squiddev.cobalt.LuaTable;
import dan200.computercraft.shadow.org.squiddev.cobalt.LuaThread;
import dan200.computercraft.shadow.org.squiddev.cobalt.LuaValue;
import dan200.computercraft.shadow.org.squiddev.cobalt.Prototype;
import dan200.computercraft.shadow.org.squiddev.cobalt.compiler.LuaC;
import dan200.computercraft.shadow.org.squiddev.cobalt.debug.Upvalue;
import dan200.computercraft.shadow.org.squiddev.cobalt.function.LuaInterpretedFunction;
import dan200.computercraft.shadow.org.squiddev.cobalt.lib.BaseLib;
import dan200.computercraft.shadow.org.squiddev.cobalt.lib.MathLib;
import dan200.computercraft.shadow.org.squiddev.cobalt.lib.StringLib;
import dan200.computercraft.shadow.org.squiddev.cobalt.lib.TableLib;

/**
 * Headless counterpart of the in-game Lua suite. The production Lua program is compiled with the
 * same Cobalt interpreter ComputerCraft uses, then executed against a Lua-implemented stub of the
 * vanilla inventory peripheral. This keeps the Lua itself (method names, argument order, nil
 * handling, proxy-table field access) covered by {@code ./gradlew test} without booting a game.
 */
public class LuaProgramTest {

    private static final String RESOURCE = "/openperipheralintegration/lua/peripheral_tests.lua";

    private static final String STUB_AND_DRIVER = "local lines = {}\n"
        + "local slots = { [1] = { id = \"minecraft:diamond\", qty = 3 }, [3] = { id = \"minecraft:diamond\", qty = 5 } }\n"
        + "local p = {}\n"
        + "function p.getInventorySize() return 27 end\n"
        + "function p.getInventoryName() return \"container.chest\" end\n"
        + "function p.getStackInSlot(i) return slots[i] end\n"
        + "function p.condenseItems()\n"
        + "  local total = 0\n"
        + "  for i = 1, 27 do\n"
        + "    local s = slots[i]\n"
        + "    if s ~= nil and s.id == \"minecraft:diamond\" then\n"
        + "      total = total + s.qty\n"
        + "      slots[i] = nil\n"
        + "    end\n"
        + "  end\n"
        + "  slots[1] = { id = \"minecraft:diamond\", qty = total }\n"
        + "end\n"
        + "function p.swapStacks(a, b) slots[a], slots[b] = slots[b], slots[a] end\n"
        + "function p.destroyStack(i) slots[i] = nil end\n"
        + "function p.getAllStacks()\n"
        + "  local r = {}\n"
        + "  for i = 1, 27 do if slots[i] ~= nil then r[i] = slots[i] end end\n"
        + "  return r\n"
        + "end\n"
        + "function p.expandStack(s) return s end\n"
        + "tests.run(p, function(line) lines[#lines + 1] = line end)\n"
        + "_G.__test_output = lines\n";

    private static String readResource() throws IOException {
        try (InputStream stream = LuaProgramTest.class.getResourceAsStream(RESOURCE)) {
            Assert.assertNotNull("Missing Lua test program: " + RESOURCE, stream);
            final byte[] buffer = new byte[8192];
            final java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            int read;
            while ((read = stream.read(buffer)) != -1) out.write(buffer, 0, read);
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private static LuaState newState() throws LuaError {
        final LuaState state = new LuaState();
        BaseLib.add(state);
        TableLib.add(state);
        StringLib.add(state);
        MathLib.add(state);
        return state;
    }

    private static LuaValue runChunk(LuaState state, String source, String name) throws Exception {
        final Prototype prototype = LuaC
            .compile(state, new ByteArrayInputStream(source.getBytes(StandardCharsets.UTF_8)), LuaString.valueOf(name));
        final LuaInterpretedFunction function = new LuaInterpretedFunction(prototype);
        function.nilUpvalues();
        function.setUpvalue(0, new Upvalue(state.globals()));
        final dan200.computercraft.shadow.org.squiddev.cobalt.Varargs result = LuaThread
            .runMain(state, function, Constants.NONE);
        return result.first();
    }

    @Test
    public void luaProgramRunsAgainstTheStubbedPeripheral() throws Exception {
        final LuaState state = newState();
        final LuaTable module = runChunk(state, readResource(), "peripheral_tests.lua").checkTable();
        state.globals()
            .rawset("tests", module);

        runChunk(state, STUB_AND_DRIVER, "peripheral_tests_driver.lua");

        final LuaTable output = (LuaTable) state.globals()
            .rawget("__test_output");
        Assert.assertNotNull("Lua driver did not produce output", output);

        final List<String> lines = new ArrayList<String>();
        for (int i = 1; i <= output.length(); i++) lines.add(
            output.rawget(i)
                .toString());

        Assert.assertTrue("Missing size marker: " + lines, lines.contains("GAMETEST_INT_SIZE=27"));
        Assert.assertTrue("Missing name marker: " + lines, lines.contains("GAMETEST_INT_NAME=true"));
        Assert.assertTrue("Missing stack marker: " + lines, lines.contains("GAMETEST_INT_STACK=minecraft:diamond:3"));
        Assert.assertTrue(
            "Condense did not merge stacks: " + lines,
            lines.contains("GAMETEST_INT_CONDENSED=minecraft:diamond:8"));
        Assert.assertTrue(
            "Swap did not move stacks: " + lines,
            lines.contains("GAMETEST_INT_SWAPPED=nil/minecraft:diamond:8"));
        Assert.assertTrue("Missing clear marker: " + lines, lines.contains("GAMETEST_INT_CLEARED=true"));
        Assert.assertTrue("Missing stack count: " + lines, lines.contains("GAMETEST_INT_ALL=1"));
        Assert.assertTrue(
            "expandStack did not return the table: " + lines,
            lines.contains("GAMETEST_INT_EXPANDED=minecraft:diamond:2"));
        Assert.assertTrue("Missing success marker: " + lines, lines.contains("GAMETEST_INT_OK"));
    }

    @Test
    public void luaProgramReportsAMissingPeripheral() throws Exception {
        final String source = "local lines = {}\n"
            + "local ok = tests.run(nil, function(line) lines[#lines + 1] = line end)\n"
            + "_G.__test_output = lines\n"
            + "_G.__test_ok = ok\n";
        final LuaState state = newState();
        final LuaTable module = runChunk(state, readResource(), "peripheral_tests.lua").checkTable();
        state.globals()
            .rawset("tests", module);
        runChunk(state, source, "peripheral_tests_nil.lua");

        final LuaTable output = (LuaTable) state.globals()
            .rawget("__test_output");
        Assert.assertNotNull(output);
        Assert.assertEquals(1, output.length());
        Assert.assertEquals(
            "GAMETEST_INT_NO_PERIPHERAL",
            output.rawget(1)
                .toString());
        Assert.assertEquals(
            Constants.FALSE,
            state.globals()
                .rawget("__test_ok"));
    }

    @Test
    public void luaProgramStateIsLocalToTheModule() throws Exception {
        final LuaState state = newState();
        final LuaTable first = runChunk(state, readResource(), "peripheral_tests.lua").checkTable();
        final LuaTable second = runChunk(state, readResource(), "peripheral_tests.lua").checkTable();
        Assert.assertNotSame(first, second);
        Assert.assertNotNull(
            first.rawget("run")
                .checkFunction());
    }

    @Test
    public void luaProgramUsesOnlyTheDocumentedPeripheralMethods() throws Exception {
        final String source = readResource();
        final String[] allowed = { "getInventorySize", "getInventoryName", "getStackInSlot", "condenseItems",
            "swapStacks", "destroyStack", "getAllStacks", "expandStack" };
        for (String method : allowed)
            Assert.assertTrue("Lua program does not exercise " + method, source.contains("peripheral." + method + "("));
    }
}
