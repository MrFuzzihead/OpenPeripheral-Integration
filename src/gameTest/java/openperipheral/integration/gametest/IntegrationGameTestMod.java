package openperipheral.integration.gametest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.world.World;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLLoadCompleteEvent;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import dan200.computercraft.api.filesystem.IMount;
import dan200.computercraft.api.filesystem.IWritableMount;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.ILuaTask;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.MethodResult;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.ServerComputer;
import openmods.Log;
import openperipheral.integration.OpcAccess;
import openperipheral.interfaces.cc.providers.PeripheralProvider;

/**
 * In-game counterpart of the unit tests. It is compiled into a throw-away source set and only runs when
 * the game is started with {@code -PgameTest}; it never ships in the mod jar.
 *
 * <p>
 * Two layers are covered:
 * <ul>
 * <li>the Java side of the adapters, through the real OpenPeripheral peripheral provider and a fake
 * ComputerCraft access/context, so asynchronous method results are drained exactly as CC would drain
 * them;</li>
 * <li>the Lua side, by booting a real ComputerCraft computer, mounting the peripheral on its back side
 * and executing {@code peripheral_tests.lua} (the same file the headless {@code LuaProgramTest} runs).
 * </ul>
 */
@Mod(
    modid = IntegrationGameTestMod.MOD_ID,
    name = "OpenPeripheral-Integration Game Tests",
    version = "1.0",
    acceptableRemoteVersions = "*")
public class IntegrationGameTestMod {

    public static final String MOD_ID = "openperipheral_integration_gametest";

    private static final String LUA_RESOURCE = "/openperipheralintegration/lua/peripheral_tests.lua";

    private static final String COMPUTER_LUA = "local ok, err = pcall(function()\n"
        + "  local tests = assert(loadfile('peripheral_tests.lua'))()\n"
        + "  local p = peripheral.wrap('back')\n"
        + "  if not p then print('GAMETEST_INT_NO_PERIPHERAL') return end\n"
        + "  tests.run(p, print)\n"
        + "end)\n"
        + "if not ok then print('GAMETEST_INT_ERROR=' .. tostring(err)) end\n";

    private static final boolean ENABLED = Boolean.getBoolean("openperipheral.integration.gameTest");

    private static IPeripheral inventoryPeripheral;

    @Mod.EventHandler
    public void init(FMLInitializationEvent evt) {
        if (!ENABLED) return;
        Log.info("OpenPeripheral-Integration game tests enabled");
    }

    @Mod.EventHandler
    public void clientLoad(FMLLoadCompleteEvent evt) {
        if (!ENABLED) return;
        if (!FMLCommonHandler.instance()
            .getSide()
            .isClient()) return;

        ClientGameTests.run();
    }

    @Mod.EventHandler
    public void serverStarted(FMLServerStartedEvent evt) {
        if (!ENABLED) return;

        final MinecraftServer server = FMLCommonHandler.instance()
            .getMinecraftServerInstance();
        try {
            runAdapterTests(server.worldServerForDimension(0));
        } catch (Throwable t) {
            failure(t);
            return;
        }

        // A real computer queues peripheral work on the server thread; driving it from the server
        // thread itself would deadlock. Run the client-style loop on its own daemon thread.
        final Thread thread = new Thread(() -> {
            try {
                runComputerLuaTest();
                success();
            } catch (Throwable t) {
                failure(t);
            }
        }, "OpenPeripheral-Integration-GameTest");
        thread.setDaemon(true);

        thread.start();
    }

    private static void success() {
        Log.info("OPENPERIPHERAL_INTEGRATION_GAMETEST_OK");
        FMLCommonHandler.instance()
            .exitJava(0, false);
    }

    private static void failure(Throwable t) {
        t.printStackTrace();
        Log.severe(t, "OPENPERIPHERAL_INTEGRATION_GAMETEST_FAILED");
        FMLCommonHandler.instance()
            .exitJava(1, false);
    }

    private static void runAdapterTests(World world) throws Exception {
        check(world != null, "Overworld is missing");
        OpcAccess.checkApiPresent();
        testInventoryPeripheral(world);
        testFurnacePeripheral(world);
        Log.info("GAMETEST_INT_ADAPTERS_OK");
    }

    private static void testInventoryPeripheral(World world) throws Exception {
        final int x = 0;
        final int y = 100;
        final int z = 0;

        world.setBlock(x, y, z, Blocks.chest, 0, 3);
        final TileEntityChest chest = (TileEntityChest) world.getTileEntity(x, y, z);
        check(chest != null, "Chest TE was not created");
        chest.setInventorySlotContents(0, new ItemStack(Items.diamond, 3));
        chest.setInventorySlotContents(2, new ItemStack(Items.diamond, 5));

        final IPeripheral peripheral = new PeripheralProvider().getPeripheral(world, x, y, z, 2);
        check(peripheral != null, "PeripheralProvider did not wrap the chest");
        check(
            peripheral.getType() != null && !peripheral.getType()
                .isEmpty(),
            "Peripheral has no type: " + peripheral.getType());

        for (String method : new String[] { "getInventoryName", "getInventorySize", "condenseItems", "swapStacks",
            "getStackInSlot", "getAllStacks", "destroyStack", "expandStack" })
            check(indexOf(peripheral.getMethodNames(), method) >= 0, "Missing method: " + method);

        final FakeComputerAccess access = new FakeComputerAccess();
        final FakeLuaContext context = new FakeLuaContext();
        peripheral.attach(access);
        check(access.mounted.contains("openp"), "Peripheral docs mount was not created");

        final Object[] size = call(peripheral, access, context, "getInventorySize");
        check(((Number) size[0]).intValue() == 27, "Unexpected chest size: " + size[0]);

        // ComputerCraft indices are 1-based; slot 1 is Java slot 0.
        final Object[] stack = call(peripheral, access, context, "getStackInSlot", 1.0);
        check(stack[0] instanceof Map, "getStackInSlot did not return a table: " + stack[0]);
        final Map<?, ?> table = (Map<?, ?>) stack[0];
        check("minecraft:diamond".equals(table.get("id")), "Unexpected item id: " + table.get("id"));
        check(((Number) table.get("qty")).intValue() == 3, "Unexpected item qty: " + table.get("qty"));

        call(peripheral, access, context, "condenseItems");
        check(chest.getStackInSlot(0).stackSize == 8, "condenseItems did not merge the stacks");
        check(chest.getStackInSlot(2) == null, "condenseItems left an empty slot populated");

        call(peripheral, access, context, "swapStacks", 1.0, 2.0);
        check(chest.getStackInSlot(0) == null, "swapStacks did not clear the source slot");
        check(chest.getStackInSlot(1).stackSize == 8, "swapStacks did not move the stack");

        call(peripheral, access, context, "destroyStack", 2.0);
        check(chest.getStackInSlot(1) == null, "destroyStack did not clear the slot");

        final Map<String, Object> requested = new HashMap<String, Object>();
        requested.put("id", "minecraft:diamond");
        requested.put("qty", 2.0);
        final Object[] expanded = call(peripheral, access, context, "expandStack", requested);
        check(expanded[0] instanceof Map, "expandStack did not return a table");
        check(((Number) ((Map<?, ?>) expanded[0]).get("qty")).intValue() == 2, "expandStack lost the stack size");

        // Restore the fixture for the Lua/computer leg of the test.
        chest.setInventorySlotContents(0, new ItemStack(Items.diamond, 3));
        chest.setInventorySlotContents(1, null);
        chest.setInventorySlotContents(2, new ItemStack(Items.diamond, 5));

        peripheral.detach(access);
        inventoryPeripheral = peripheral;
        check(access.unmounted.contains("openp"), "Peripheral docs mount was not removed");
        Log.info("GAMETEST_INT_INVENTORY_OK");
    }

    private static void testFurnacePeripheral(World world) throws Exception {
        final int x = 2;
        final int y = 100;
        final int z = 0;

        world.setBlock(x, y, z, Blocks.furnace, 0, 3);
        final TileEntityFurnace furnace = (TileEntityFurnace) world.getTileEntity(x, y, z);
        check(furnace != null, "Furnace TE was not created");
        furnace.furnaceBurnTime = 120;
        furnace.furnaceCookTime = 30;

        final IPeripheral peripheral = new PeripheralProvider().getPeripheral(world, x, y, z, 2);
        check(peripheral != null, "PeripheralProvider did not wrap the furnace");

        final FakeComputerAccess access = new FakeComputerAccess();
        final FakeLuaContext context = new FakeLuaContext();
        peripheral.attach(access);

        check(((Number) call(peripheral, access, context, "getBurnTime")[0]).intValue() == 120, "Wrong burn time");
        check(((Number) call(peripheral, access, context, "getCookTime")[0]).intValue() == 30, "Wrong cook time");
        check(Boolean.TRUE.equals(call(peripheral, access, context, "isBurning")[0]), "Furnace should be burning");

        peripheral.detach(access);
        Log.info("GAMETEST_INT_FURNACE_OK");
    }

    private static void runComputerLuaTest() throws Exception {
        final MinecraftServer server = FMLCommonHandler.instance()
            .getMinecraftServerInstance();
        final World world = server.worldServerForDimension(0);
        final ServerComputer computer = new ServerComputer(
            world,
            0,
            "integrationtest",
            0,
            ComputerFamily.Normal,
            51,
            19);
        computer.setPosition(0, 100, 0);
        computer.setPeripheral(2, inventoryPeripheral);

        final IWritableMount root = computer.getRootMount();
        check(root != null, "Computer root mount is null");
        write(root, "peripheral_tests.lua", readResource(LUA_RESOURCE));
        write(root, "startup", COMPUTER_LUA);
        write(root, "startup.lua", COMPUTER_LUA);

        computer.turnOn();
        final long deadline = System.currentTimeMillis() + 30000;
        String terminal = "";
        while (System.currentTimeMillis() < deadline) {
            computer.update();
            Thread.sleep(50);
            terminal = readTerminal(computer);
            if (terminal.contains("GAMETEST_INT_OK") || terminal.contains("GAMETEST_INT_ERROR=")
                || terminal.contains("GAMETEST_INT_NO_PERIPHERAL")) break;
        }

        check(terminal.contains("GAMETEST_INT_SIZE=27"), "Lua did not read the inventory size. Terminal:\n" + terminal);
        check(
            terminal.contains("GAMETEST_INT_NAME=true"),
            "Lua did not read the inventory name. Terminal:\n" + terminal);
        check(
            terminal.contains("GAMETEST_INT_STACK=minecraft:diamond:3"),
            "Lua did not read the first stack. Terminal:\n" + terminal);
        check(
            terminal.contains("GAMETEST_INT_CONDENSED=minecraft:diamond:8"),
            "Lua condenseItems failed. Terminal:\n" + terminal);
        check(
            terminal.contains("GAMETEST_INT_SWAPPED=nil/minecraft:diamond:8"),
            "Lua swapStacks failed. Terminal:\n" + terminal);
        check(terminal.contains("GAMETEST_INT_CLEARED=true"), "Lua destroyStack failed. Terminal:\n" + terminal);
        check(terminal.contains("GAMETEST_INT_ALL=1"), "Lua getAllStacks failed. Terminal:\n" + terminal);
        check(
            terminal.contains("GAMETEST_INT_EXPANDED=minecraft:diamond:2"),
            "Lua expandStack failed. Terminal:\n" + terminal);
        check(terminal.contains("GAMETEST_INT_OK"), "Lua suite did not finish. Terminal:\n" + terminal);
        check(!terminal.contains("GAMETEST_INT_ERROR="), "Lua suite raised an error. Terminal:\n" + terminal);

        computer.shutdown();
        Log.info("GAMETEST_INT_LUA_OK");
    }

    private static void write(IWritableMount root, String path, String contents) throws IOException {
        try (OutputStream out = root.openForWrite(path)) {
            out.write(contents.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static String readResource(String path) throws IOException {
        try (InputStream stream = IntegrationGameTestMod.class.getResourceAsStream(path)) {
            check(stream != null, "Missing resource: " + path);
            final java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            final byte[] buffer = new byte[4096];
            int read;
            while ((read = stream.read(buffer)) != -1) out.write(buffer, 0, read);
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private static String readTerminal(ServerComputer computer) {
        final StringBuilder result = new StringBuilder();
        for (int i = 0; i < computer.getTerminal()
            .getHeight(); i++) {
            final dan200.computercraft.core.terminal.TextBuffer line = computer.getTerminal()
                .getLine(i);
            if (line != null) result.append(line.toString())
                .append('\n');
        }
        return result.toString();
    }

    private static Object[] call(IPeripheral peripheral, FakeComputerAccess access, FakeLuaContext context,
        String methodName, Object... args) throws Exception {
        final int index = indexOf(peripheral.getMethodNames(), methodName);
        check(index >= 0, "Method not found: " + methodName);
        final Object result = peripheral.callMethodResult(access, context, index, args);
        return context.complete(result);
    }

    private static int indexOf(String[] names, String name) {
        for (int i = 0; i < names.length; i++) if (name.equals(names[i])) return i;
        return -1;
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static class FakeComputerAccess implements IComputerAccess {

        final List<String> mounted = new ArrayList<String>();
        final List<String> unmounted = new ArrayList<String>();

        @Override
        public String mount(String path, IMount mount) {
            mounted.add(path);
            return path;
        }

        @Override
        public String mount(String path, IMount mount, String drive) {
            mounted.add(path);
            return path;
        }

        @Override
        public String mountWritable(String path, IWritableMount mount) {
            mounted.add(path);
            return path;
        }

        @Override
        public String mountWritable(String path, IWritableMount mount, String drive) {
            mounted.add(path);
            return path;
        }

        @Override
        public void unmount(String path) {
            unmounted.add(path);
        }

        @Override
        public int getID() {
            return 1;
        }

        @Override
        public void queueEvent(String event, Object[] args) {}

        @Override
        public String getAttachmentName() {
            return "back";
        }
    }

    private static class FakeLuaContext implements ILuaContext {

        private long nextId = 1;
        private long lastId;
        private final Map<Long, Object[]> taskResults = new HashMap<Long, Object[]>();

        @Override
        public MethodResult executeMainThreadTask(ILuaTask task) throws LuaException {
            return MethodResult.of(task.execute());
        }

        @Override
        public long issueMainThreadTask(ILuaTask task) throws LuaException {
            final long id = nextId++;
            try {
                taskResults.put(id, task.execute());
            } catch (LuaException e) {
                taskResults.put(id, new Object[] { false, e.getMessage() });
            }
            lastId = id;
            return id;
        }

        Object[] complete(Object result) throws LuaException {
            if (!(result instanceof MethodResult)) return (Object[]) result;

            final MethodResult methodResult = (MethodResult) result;
            if (methodResult.isImmediate()) return methodResult.getResults();

            final Object[] taskResult = taskResults.get(lastId);
            if (taskResult == null) throw new AssertionError("No fake task result for id " + lastId);

            final Object[] event = new Object[3 + taskResult.length];
            event[0] = "task_complete";
            event[1] = lastId;
            event[2] = true;
            System.arraycopy(taskResult, 0, event, 3, taskResult.length);

            final MethodResult resumed = methodResult.resumeWith(event);
            if (resumed == null || !resumed.isImmediate()) throw new AssertionError("Task did not resume");
            return resumed.getResults();
        }
    }
}
