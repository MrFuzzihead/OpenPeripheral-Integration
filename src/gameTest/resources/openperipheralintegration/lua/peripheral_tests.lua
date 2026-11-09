-- Integration tests for the vanilla inventory peripheral shipped by OpenPeripheral-Integration.
--
-- The same file is used twice:
--   * headless, by integration.LuaProgramTest, which executes `run` against a Lua stub;
--   * in-game, by the gametest mod, which writes it into a ComputerCraft computer and runs `run`
--     against the real peripheral returned by OpenPeripheral's provider.
--
-- Keeping the assertions in Lua (instead of Java) is intentional: it exercises the same method
-- names, argument types, proxy tables and async resumption path a player's Lua program uses.

local tests = {}

local function stackString(stack)
    if stack == nil then
        return "nil"
    end
    return tostring(stack.id) .. ":" .. tostring(stack.qty)
end

--- Runs the suite against a peripheral that implements the vanilla inventory adapter.
-- @param peripheral the wrapped peripheral
-- @param out a function receiving one line of output per assertion
-- @return true when the peripheral was usable, false otherwise
function tests.run(peripheral, out)
    out = out or print
    if peripheral == nil then
        out("GAMETEST_INT_NO_PERIPHERAL")
        return false
    end

    local size = peripheral.getInventorySize()
    out("GAMETEST_INT_SIZE=" .. tostring(size))

    local name = peripheral.getInventoryName()
    out("GAMETEST_INT_NAME=" .. tostring(name ~= nil and #tostring(name) > 0))

    out("GAMETEST_INT_STACK=" .. stackString(peripheral.getStackInSlot(1)))

    peripheral.condenseItems()
    out("GAMETEST_INT_CONDENSED=" .. stackString(peripheral.getStackInSlot(1)))

    peripheral.swapStacks(1, 2)
    out("GAMETEST_INT_SWAPPED=" .. stackString(peripheral.getStackInSlot(1)) .. "/" .. stackString(peripheral.getStackInSlot(2)))

    local all = peripheral.getAllStacks()
    local count = 0
    for _ in pairs(all) do
        count = count + 1
    end
    out("GAMETEST_INT_ALL=" .. tostring(count))

    peripheral.destroyStack(2)
    out("GAMETEST_INT_CLEARED=" .. tostring(peripheral.getStackInSlot(2) == nil))

    local expanded = peripheral.expandStack({ id = "minecraft:diamond", qty = 2, dmg = 0 })
    out("GAMETEST_INT_EXPANDED=" .. stackString(expanded))

    out("GAMETEST_INT_OK")
    return true
end

return tests
