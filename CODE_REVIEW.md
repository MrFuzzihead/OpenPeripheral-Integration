# OpenPeripheral-Integration — code review & backlog

Review of the `feature-Refactors` tree after the 2026-11 dependency/build updates and the new test
infrastructure. Everything below was verified against the code in this repository; where a finding is
an inference from a third-party contract (AE2, Thaumcraft, Forestry, …) it is marked **verify**.

Test/build facts at the time of writing:

| Item | Value |
|------|-------|
| `./gradlew test` | **100 tests / 11 classes, all green** |
| JaCoCo | **25.7 % line** (632/2457), **24.4 % branch** (164/672) |
| Coverage floor enforced by `check` | 24 % line / 23 % branch |
| `./gradlew check` | green (compile, tests, JaCoCo verification, Spotless, Checkstyle) |
| `./gradlew gameTestServer` | green — `GAMETEST_INT_ADAPTERS_OK`, `GAMETEST_INT_LUA_OK`, `OPENPERIPHERAL_INTEGRATION_GAMETEST_OK` |
| Unit-test JVM | Java 8 toolchain (same bytecode target the mod ships) |
| Game-test JVM | Java 25 via `runServer25` |

---

## 1. Dependency audit

`./gradlew updateDependencies` was used as a first pass, then corrected by hand. **It is not safe to
trust blindly** — it compares version strings lexicographically and it does not honour the OpenModsLib
pin.

| Dependency | Was | Now | Notes |
|---|---|---|---|
| NotEnoughItems | 2.9.1-GTNH | **2.9.3-GTNH** | dev-run only (`runtimeOnlyNonPublishable`) |
| Applied Energistics 2 Unofficial | rv3-beta-1097-GTNH | **rv3-beta-1099-GTNH** | `updateDependencies` “updated” to **999**, a downgrade caused by lexical sorting; reverted to the real latest |
| BuildCraft | 7.2.0 | 7.2.0 | already latest on GTNH Maven |
| ForestryMC | 4.12.0 | 4.12.0 | already latest |
| Tinkers Mechworks | 0.5.0 | 0.5.0 | already latest |
| Railcraft | 9.18.0 | 9.18.0 | already latest |
| IndustrialCraft 2 | 2.2.828-experimental | 2.2.828-experimental | last 1.7.10 experimental build; GTNH’s buildscript substitutes `net.industrial-craft:industrialcraft-2` with `curse.maven:ic2-242638:2353971` (verified with `dependencyInsight`). Effectively a Curse dependency, left untouched as requested |
| Thaumcraft | 4.2.3.5 | 4.2.3.5 | final TC4 release for 1.7.10 |
| OpenModsLib | 0.10.15 | **0.10.15 (pinned)** | the updater wanted 0.11.0. 0.10.15 and 0.11.0 are the same commit, but 0.11.0 makes FML demand `[0.11.0,0.12.0)`, which refuses to load in packs shipping 0.10.15. A comment now documents the pin |
| ComputerCraft-Legacy | 3.3.1 | 3.3.1 | already updated by you |
| OpenPeripheral | 2.1.1 | 2.1.1 | already updated by you |
| CurseMaven (CoFH core, MFR, Mystcraft) | unchanged | unchanged | excluded per request |
| Buildscript (`gtnhsettingsconvention`) | 2.0.33 | 2.0.34 | already updated by you |

Recommendations:

- Re-run `updateDependencies` only with a diff review, and keep the OpenModsLib comment; otherwise a
  future run silently breaks pack compatibility.
- `dependencies.gradle` still declares IC2 by its (dead) Maven coordinates; the substitution rule makes
  it work, but a comment explaining that it resolves to Curse would save the next person an hour.
- AE2/NEI bump compiled cleanly against this code (no API breakage).

---

## 2. Test & verification infrastructure added

New/changed files:

| Path | Purpose |
|---|---|
| `dependencies.gradle` | `testImplementation` JUnit 4.13.2, Mockito 4.11.0 + `mockito-inline` |
| `addon.gradle.kts` | JaCoCo, Java 8 test toolchain, `gameTest` source set, `gameTestServer`, coverage floor |
| `src/test/java/openperipheral/integration/**` | 100 headless tests across 11 classes |
| `src/gameTest/java/openperipheral/integration/gametest/IntegrationGameTestMod.java` | non-shipped test mod: real provider, fake CC access/context, real ComputerCraft computer |
| `src/gameTest/resources/openperipheralintegration/lua/peripheral_tests.lua` | Lua suite shared by the headless test and the in-game test |
| `.github/workflows/game-test.yml` | CI job that boots a dedicated server and greps the markers |

Commands:

```sh
./gradlew test                        # 100 JUnit tests (Java 8)
./gradlew jacocoTestReport            # build/reports/jacoco/test/html
./gradlew jacocoTestCoverageVerification
./gradlew check                       # tests + coverage floor + Spotless + Checkstyle
mkdir -p run/server && echo "eula=true" > run/server/eula.txt
./gradlew gameTestServer              # real server + real ComputerCraft Lua
```

Design notes:

- Tests run on a **Java 8 toolchain** because Mockito 4/ByteBuddy is not guaranteed on Java 25; the
  production bytecode is Java 8 anyway (Jabel).
- `src/gameTest/resources` is also added as a test resource root so the Lua suite has exactly one copy
  and can be compiled/executed headlessly by `LuaProgramTest` with ComputerCraft’s Cobalt interpreter.
- Minecraft’s global registries (`Items.*`, `Blocks.*`, `GameData`, `ForgeHooks`, `FluidRegistry`,
  `OreDictionary`, `ColorUtils`) are **not** usable without FML’s launch class loader. Tests construct
  items directly (`new ItemSword(...)`, `new ItemArmor(...)`) and avoid those statics; that limitation
  is the main reason coverage is 24 % rather than much higher.
- The in-game Lua test uses `ServerComputer#setPeripheral(2, …)`, so it exercises **this mod’s**
  peripheral directly instead of being shadowed by ComputerCraft’s native `inventory` provider (see
  the README note about CC 3.3 generic peripherals).

Coverage gaps are listed in §8.

---

## 0. Batch 1 — fixes landed

- **3.1** duplicate `ModuleThaumcraft` registration removed.
- **3.2** CoFH `AdapterInventoryHandler` methods are public and annotated with `@ScriptCallable`.
- **3.3** AE2 missing-items payload now carries the actual missing amount; the requested stack is no
  longer double-converted.
- **3.4** `getRequestedJobs()` returns an empty set instead of `null` (`injectCraftedItems` was completed in
  batch 2).
- **3.5** out-of-range/negative enchantment ids are skipped instead of throwing `AIOOBE`.
- **3.6** `AdapterSign.setText` accepts exactly four lines; **§4.1** `setLines` now truncates to 15 chars.
- **3.7** note-block pitch is wrapped with `Math.floorMod` and only written after the Forge hook approves.
- Tests: **80** (was 75), coverage **21.3 % line / 19.3 % branch**.

---

## 0b. Batch 2 — robustness & structure

- **§6.1** module registration now lives in one table (`IntegrationModules`), with a startup duplicate-key
  check and a regression test that pins all 27 config keys.
- **3.4** fully fixed: `injectCraftedItems` now injects into the AE network via the storage grid monitor
  and returns the true remainder.
- **§4.2** inventory adapters consistently use a validated `requireInventory` helper.
- **§4.3** corrected the duplicate `toDirection` argument name.
- **§4.5** `getAllStacks` now returns a `LinkedHashMap`.
- **§4.6** neighbour lookups fail with a descriptive error when the world is missing (anonymous-class
  allocation still open).
- **§4.11** `ItemFingerprint` validates ids and registry lookups; **§4.12** rejects non-string `nbt_hash`.
- **§4.13** AE2 grid access fails with `IllegalStateException` instead of NPE when unformed/unpowered.
- **§4.14** CPU lookup is null-name safe and crafting quantities must be positive.
- **§4.15** cell slots are validated against `getCellCount`; **§4.16** converter output is type-checked.
- **§4.17** facade block/meta arrays are length-checked; **§4.18** arcane bore checks for a missing pick.
- **§4.21** `SecurityUtils` fails closed on a null access mode; **§4.22** null potion entries are handled.
- **§4.23** beacon effects no longer allocate a throw-away `PotionEffect`.
- Tests: **89** (was 80), coverage **23.8 % line / 21.3 % branch**.

---

## 0c. Batch 3 — cleanup, Mystcraft contracts, async trimming

- **Mystcraft v1:** `decrStackSize` clears the slot with `null` instead of a zero-size stack;
  `getStackInSlotOnClosing` now clears and returns the stack; `pullNotebookPage` validates the 1-based
  page slot.
- **Mystcraft v2:** `pushNotebookPage` only re-adds a non-empty remainder; `createInventoryWrapper`
  throws `IllegalArgumentException` (Lua-friendly) instead of `IllegalStateException`; `getSlotCount`
  no longer takes an ignored argument (Lua callers passing one still work — extra Lua args are ignored).
- **MystcraftAccess:** log messages name the API, are spelled correctly, and pass the `api` argument
  they format.
- **Dead code removed:** unregistered `VanillaMinecraftMetaProvider` and the three unused
  `DUMMY_VANILLA_MODID` constants.
- **Async trimming:** pure getters no longer round-trip through the server thread (IC2 energy/reactor
  adapters, note-block `getNote`, inventory `getInventoryName`/`getInventorySize`/`expandStack`).
- **ItemFingerprint** is now a value object (`equals`/`hashCode` by id/damage/NBT hash) with a corrected
  class Javadoc.
- **AdapterSecureTile.getOwner** advertises `OBJECT` instead of `STRING`.
- **Electric machine** progress returns 0 (not 100 %) when the energy buffer is zero.
- Tests: **92**, coverage **24.3 % line / 22.8 % branch**.

---

## 0d. Batch 4 — performance and duplicated adapter code

- **Forestry** (`AdapterBeeHousing`): mutations, individual templates and derived offspring species are
  cached per species root instead of being rebuilt on every call; the `parentYpe` argument-name typo was
  corrected (positional Lua calls are unaffected).
- **AE2** (`AdapterNetwork`): `getAvailableItems` accepts an optional `limit` and the new
  `getAvailableItemCount()` returns the type count without converting the whole network.
- **Railcraft**: the four boolean trigger adapters share `BooleanReflectionAdapter`, removing four copies
  of the same reflection wiring.
- **Inventory**: `AdapterWorldInventory` uses a named `TileEntityPosProvider` instead of an anonymous
  class per call.
- **StorageCellMetaProvider** was intentionally *not* cached: cell contents (free bytes, stored types)
  change live, so a cache would serve stale metadata.
- Tests: **95**, coverage **25.2 % line / 23.7 % branch**; floor raised to 24 % / 21 %.

---

## 0e. Batch 5 — reflection table and config migration

- All 30 reflective class names now live in `ReflectionTargets` (nested per mod) instead of string
  literals in 30 files; `ReflectionTargetsTest` asserts they are unique and plausible FQCNs.
- The `buildcraft-api-tilenentity` config key was renamed to `buildcraft-api-tileentity` with a
  `ConfigMigrations` copy step, so existing configs keep their value. `ConfigMigrationsTest` covers
  migration, existing-value precedence and no-op paths.
- **Declined with rationale:** extracting the CoFH energy adapters (the two interfaces have no shared
  storage supertype, so a base would save almost nothing) and the Ender-attuned adapters (a common
  `IEnderAttuned` base would move `@ScriptCallable` methods into inherited generics, which cannot be
  verified here because CoFH is absent from the test runtime). Both are documented rather than risked.
- Tests: **100**, coverage **25.7 % line / 24.4 % branch**; floor raised to 24 % / 23 %.

---

## 3. Critical / high-severity bugs

### 3.1 Thaumcraft integration module is registered twice — P0 ✅

`OpenPeripheralIntegration.preInit` contains the `thaumcraft-mod` check twice:

- `OpenPeripheralIntegration.java:122`
- `OpenPeripheralIntegration.java:147`

`Integration.addModule` from OpenModsLib is a plain `List#add` (verified against 0.10.15), so
`ModuleThaumcraft` is added **and loaded twice**. The second load registers `AdapterJar`,
`AdapterBrainJar`, `AdapterArcaneBore`, `AdapterArcaneEar` and `AdapterDeconstructor` a second time.
Depending on how the OpenPeripheral registry deduplicates, this produces duplicate peripheral methods,
duplicate documentation entries, or registry warnings. Delete the second block.

### 3.2 CoFH inventory adapter exposes no methods — P0 ✅

`cofh/inventory/AdapterInventoryHandler.java`: all four methods are package-private and none carries
`@ScriptCallable`:

```java
List<ItemStack> getInventoryContents(IInventoryHandler target, @Arg(name = "from") ForgeDirection from)
int getSizeInventory(IInventoryHandler target, ...)
boolean isEmpty(IInventoryHandler target, ...)
boolean isFull(IInventoryHandler target, ...)
```

OpenPeripheral builds the peripheral method list from `@ScriptCallable`-annotated methods; **none** of these
methods is annotated (they are also package-private), so the entire `CoFHAPI|inventory` module is
silently a no-op. Make the methods public and annotate them
(`getInventoryContents` → `ReturnType.TABLE`, `getSizeInventory` → `NUMBER`, `isEmpty`/`isFull` →
`BOOLEAN`), or delete the module if CoFH’s inventory API is considered obsolete.

### 3.3 AE2 crafting reports full amounts as “missing” — P1 ✅

`appeng/CraftingCallback.java:95`: `missingStack` is copied from `needed` but its size is never set to
`missing`:

```java
long missing = needed.getStackSize();
if (extracted != null) missing -= extracted.getStackSize();

if (missing > 0) {
    IAEItemStack missingStack = needed.copy();
    missingItems.add(missingStack);   // stack size = full required amount, not `missing`
}
```

The `"missing_items"` event therefore over-reports. **Fixed:** `missingStack.setStackSize(missing);` is
applied before adding.

Also: `converter.fromJava(this.requestedStack)` was called on an object that is **already converted** in
the constructor (`requestedStack = converter.fromJava(requestedStack)`), so the signal payload was
double-converted. **Fixed:** send `requestedStack` directly.

### 3.4 AE2 crafting request contract is violated — P1 ✅

`appeng/CraftingRequester.java`:

- `getRequestedJobs()` returned `null`; AE2 iterates this set when a link is cancelled/invalidated.
  **Fixed:** returns `ImmutableSet.of()`.
- `injectCraftedItems(...)` returned `items`, i.e. “none of the crafted items were accepted”. The class
  comment claimed crafted items go straight back into the AE network, but the contract for the return
  value is *the remainder that could not be injected*. **Fixed:** the items are now injected through
  `IStorageGrid#getItemInventory()` with the machine action source, and the true remainder is returned.

### 3.5 `ModuleVanilla.listEnchantments` throws on out-of-range enchantment ids — P1 ✅

`ModuleVanilla.java:97` indexes `Enchantment.enchantmentsList[id]` without a bounds check. A corrupted
or modded NBT tag with a negative/large id crashes the metadata call with `AIOOBE` (reproduced in the
test suite while writing this review). Guard with `id >= 0 && id < Enchantment.enchantmentsList.length`.
The same array can contain `null` entries; that case is already handled.

### 3.6 `AdapterSign.setText` rejects exactly four lines — P1 ✅

`AdapterSign.java:80`:

```java
Preconditions.checkArgument(newLength >= 0 && newLength < maxLength, "Value must be in range [1,%s]", maxLength);
```

`maxLength` is `signText.length == 4`, so `setText("a\nb\nc\nd")` — the most natural use of a sign — is
rejected. The lower bound is also wrong (`>= 0` allows 0 lines). Use
`newLength >= 1 && newLength <= maxLength`.

### 3.7 `AdapterNoteBlock.setPitch` mutates before the Forge hook can veto — P1 ✅

`AdapterNoteBlock.java:40` writes the note **before** `ForgeHooks.onNoteChange(...)` and returns `false`
without restoring the old value. A mod vetoing the change leaves the tile entity desynchronised from
the client (the hook exists precisely to allow this). Also:

- the Javadoc says “From 0 to 25”, but 25 wraps to 0; the usable range is 0–24;
- negative input is accepted and stored as a negative byte (`%` keeps the sign).
  Clamp with `Math.floorMod(newNote, 25)` and only assign after the hook approves.

---

## 4. Correctness & robustness issues (P2)

| # | Location | Issue |
|---|---|---|
| 1 | `AdapterSign.setLines` ✅ | ~~Does not truncate lines to 15 chars~~ (fixed: now routes through `setLine`); it did not need `ForgeHooks` because it does not change the note. |
| 2 ✅ | `AdapterInventory.java:105,118,131`, `condenseItems` | `InventoryUtils.getInventory(target)` is dereferenced without a null check in `getStackInSlot`, `getAllStacks`, `destroyStack` and `condenseItems`, while `getInventoryName`/`getInventorySize` do check. `InventoryUtils.getInventory(IInventory)` currently returns the input for non-chests (and a large-chest wrapper for chests), so this is a defensive-consistency issue rather than a proven crash — but the split invites a future NPE. |
| 3 ✅ | `AdapterInventory.swapStacks` | The second `@Arg` is named `fromDirection` (copy/paste); generated Lua docs are wrong for `toDirection`. |
| 4 | `AdapterInventory.getStackInSlot` vs `getAllStacks` | Defaults are inconsistent: `getStackInSlot` returns a raw `ItemStack` unless `proxy == TRUE`, `getAllStacks` returns proxies unless `proxy == FALSE`. Document or align. |
| 5 ✅ | `AdapterInventory.getAllStacks` | Uses `HashMap`, so slot order in the returned table is arbitrary; `LinkedHashMap` (or `Index`-sorted) is friendlier to Lua consumers. |
| 6 ✅ | `AdapterWorldInventory.getProvider` | Allocates an anonymous `IWorldPosProvider` on every call; hoist a private static class (or a cached wrapper). `getNeighborTarget` also does not check that `getWorld()` is non-null. |
| 7 | `AdapterWorldInventory.pushItem` | Unlike `pullItem`, it has no `otherTarget == target` guard; a self-referential world/TE can duplicate or eat items. |
| 8 | `BurnTimeMetaProvider` | `canApply` and `getMeta` each call `TileEntityFurnace.getItemBurnTime(stack)`, which fires Forge’s fuel event twice per metadata lookup. Compute once (e.g. `getMeta` returning `null` for 0 instead of a separate `canApply`). |
| 9 | `EnergyMetaProvider.getKey` | Returns `"energy_te"` for an **item** stack metadata provider; the `_te` suffix is misleading and differs from every other key. Renaming is a Lua API break; deprecate/alias instead. |
| 10 | `AdapterTileSteamTurbine.getTurbineRotorStatus` | Divides by `item.getMaxDamage()`, which is 0 for unbreakable items → `Infinity` → `(int)` overflow → nonsensical negative percentage. Guard `maxDamage > 0`. |
| 11 ✅ | `ItemFingerprint` | No `equals`/`hashCode` (each instance is unique), and `new UniqueIdentifier(id)` splits on `:` with no validation: a Lua table with `id = "stone"` throws `AIOOBE`; a failed registry lookup in the `Item`/`ItemStack` constructors produces `new UniqueIdentifier(null)` → NPE. Validate the id and use the two-argument `UniqueIdentifier(mod, name)` or a proper key class. The Javadoc still calls it “SearchNeedle”. |
| 12 ✅ | `ConverterItemFingerprint` | `(String) nbtHash` is an unchecked cast; a non-string `nbt_hash` gives `ClassCastException` instead of a `LuaException`-friendly `IllegalArgumentException`. |
| 13 ✅ | `AdapterGridBase` | `host.getGridNode(...).getGrid().getCache(...)` NPEs for unformed/unpowered AE2 blocks (grid node or grid is null). Every public AE2 method should throw a descriptive `IllegalStateException` instead. |
| 14 ✅ | `AdapterInterface.findCpu` | `cpu.getName().equals(cpuName)` NPEs if a CPU has no name; use `cpuName.equals(cpu.getName())`. `requestCrafting` also accepts `qty <= 0` and passing `quantity = Long.MAX_VALUE` overflows the AE stack. |
| 15 ✅ | `AdapterStorage.getCellStatus` | Does not validate `slot` against `getCellCount()`; relies on the AE2 implementation throwing `IndexOutOfBoundsException` (it may throw `AIOOBE`, which is caught, or nothing). Validate explicitly. |
| 16 ✅ | `AdapterNetwork.getAvailableItems` | Unchecked `(Map<String,Object>) converter.fromJava(stack)`; if a converter returns anything else this is a `CCE` inside Lua. It also materialises the whole network into a list on every call — a large network can stall the computer thread. |
| 17 ✅ | `FacadeMetaProvider` | Assumes `getBlocksForFacade(stack).length == getMetaValuesForFacade(stack).length`; a mismatched third-party facade gives `AIOOBE`. |
| 18 ✅ | `AdapterArcaneBore` | `isWorking` NPEs if `hasPickaxe` is true but `getPick` returns null; `getPick` hard-codes slot 1 (Thaumcraft internal); a renamed/remapped field makes the `FieldAccess` initialisers fail at adapter construction. |
| 19 ✅ | `AdapterElecticMachine` (typo) | `getProgress` divides by `MAX_ENERGY`; 0 gives `Infinity` → clamps to 100 %. Also, `CLASS` is `null` if IC2 renames the internal class, and the adapter instance is created during module load — a hard crash instead of a skipped adapter. |
| 20 ✅ | `AdapterSecureTile` | `getOwner` is declared `ReturnType.STRING` but returns `GameProfile`; `getOwnerName`/`getOwner` descriptions are identical. |
| 21 ✅ | `SecurityUtils.canModify` | NPEs if `ISecurable#getAccess()` returns null; CoFH tiles normally don’t, but a one-line null guard is cheap. |
| 22 ✅ | `ItemPotionMetaProvider.getPotionInfo` | Range-checks the id but not `Potion.potionTypes[id] != null`; unused-but-in-range ids NPE on `potion.getName()`. |
| 23 ✅ | `AdapterBeacon.getEffectName` | Constructs a throw-away `PotionEffect` just to get the name; `Potion.potionTypes[id].getName()` is enough and cannot throw on valid beacon effects. |
| 24 | `ModuleVanilla` / `ModuleVanillaInventory` / `ModuleVanillaInventoryManipulation` | All three declare an unused, mutable `public static String DUMMY_VANILLA_MODID = "vanilla"`. Remove (it is not a mod id). |
| 25 | `InventoryMetaProvider.wrapToProxyTable` | Hard-codes `slot + 1` for both architectures; correct for CC and OC (both are 1-based today), but the 1-based assumption is undocumented. |
| 26 | `EntityLivingMetaProvider` | Exposes the same information twice: `potionEffects` (map `1..n` → effect name) and `potion_effects` (list of tables). **Kept intentionally** for Lua compatibility (both shapes are in use); document the preferred key instead of removing one. |
| 27 | `EntityLivingMetaProvider` (perf) | Performs a 10-block `rayTraceBlocks` on every metadata read; metadata is often requested in bulk (e.g. `getEntityMetadata` loops). Consider making `lookingAt` opt-in or throttled. |
| 28 ✅ | `Mystcraft v2 NotebookWrapper.getSlotCount(int slot)` | Has an unused `slot` parameter; Lua callers must pass a slot for a value that ignores it. Change to `getSlotCount()` (breaking Lua signature — document it). |
| 29 ✅ | `Mystcraft v1 NotebookIInventoryWrapper.decrStackSize` | When the stack reaches size 0 it calls `setInventorySlotContents(slot, stack)` with a zero-size stack instead of `null`; `getStackInSlotOnClosing` throws `UnsupportedOperationException`, and `markDirty` is a no-op. This wrapper violates the `IInventory` contract and can confuse hoppers/pipes. |
| 30 ✅ | `Mystcraft v1 AdapterWritingDesk.pullNotebookPage` | Converts a 1-based `notebookSlot` with `notebookSlot - 1` while every other method takes an `Index`; a Lua `0` becomes `-1`. Validate/clamp. |
| 31 ✅ | `Mystcraft v2 AdapterWritingDesk.pushNotebookPage` | `if (remains >= 0)` is always true; a fully inserted page calls `addPage` with a 0-size copy. Use `> 0`. |
| 32 ✅ | `Mystcraft v2 createInventoryWrapper` | Throws `IllegalStateException` (`Preconditions.checkState`) for an empty/invalid slot; every other adapter reports `IllegalArgumentException`, which OpenPeripheral turns into a Lua error. Use `checkArgument`. |
| 33 ✅ | `MystcraftAccess` | Log messages say “Mystraft” and “unavalable”; the catch-all `Exception` hides API bugs. |
| 34 | `AdapterBeeHousing` | `parentYpe` argument name and the repeated `getMutations(false)` calls are fixed ✅ (batch 4); `alleleCompatator` is still misspelled, and `getBeeBreedingData` still aborts the whole list with a `RuntimeException` if one mutation is malformed (skip + log instead). |
| 35 | `ConverterIIndividual` | `conventers` is misspelled; `convertAllele` returns the literal `"missing"` for a null allele, which is indistinguishable from a legitimate species name; `describeIndividual` calls `hasEffect()` on every `IIndividual` (verify that all Forestry implementations support it). |
| 36 | `OpenPeripheralIntegration` config key `buildcraft-api-tilenentity` ✅ | Renamed to `buildcraft-api-tileentity`; `ConfigMigrations` copies the old value and removes the legacy key (batch 5). |
| 37 | `ApiIntegrationModule.name()` / `ModIntegrationModule.name()` | Both produce “… (API) CC integration module” / “(mod) CC integration module” even for AE2/IC2/Railcraft — “CC” is wrong for non-ComputerCraft integrations and makes logs confusing. |
| 38 | `AdapterInventoryHandler` (3.2) | All four methods are package-private and carry no `@ScriptCallable`; the module registers an adapter that exposes zero methods. |
| 39 | `OpenPeripheralIntegration` `@Mod(name = MOD_ID)` | The display name is the internal id (`OpenPeripheralIntegration`); `mcmod.info` has the human-readable one. Consider a proper name. |
| 40 | `AdapterFrequencyOwner` | `@MultipleReturn` on `int[] getColors` / `String[] getColorNames`: verify OpenPeripheral turns the array into three return values (it may emit one table). If it emits a table, the `returnTypes = {NUMBER, NUMBER, NUMBER}` docs are wrong. |

---

## 5. Optimizations

1. **Cache adapter-derived objects** ✅ (batch 4)
   - `AdapterWorldInventory` uses a named `TileEntityPosProvider`.
   - `AdapterBeeHousing` caches mutations, templates and offspring species per species root. The filtered
     species list is still rebuilt per call so the on-line config toggles keep working.
2. **Avoid duplicate work**
   - `BurnTimeMetaProvider` (see §4.8): the provider API calls `canApply` and `getMeta` separately, so
     each lookup legitimately computes the fuel value twice; merging them would require an API change.
   - `AdapterTileSteamTurbine` calls `getMaxDamage()` once per item; cache in a local.
   - `AdapterBeeHousing.getOffspringSpecies` calls `templateAsGenome` for every mutation; compute once
     per mutation and reuse across the three public methods.
3. **Lazy/limited results**
   - `AdapterNetwork.getAvailableItems` accepts an optional `limit`, and `getAvailableItemCount()`
     reports the size without converting everything. ✅ (batch 4)
   - `StorageCellMetaProvider` performs a full AE2 cell inventory scan on every item metadata lookup.
     **Deliberately not cached** (batch 4): a cell's free bytes/stored types change as the network does,
     so a cache would be stale; the scan is the price of accurate metadata.
4. **Async annotations** ✅ (batch 3)
   - `ModuleIC2Api` adapters (`AdapterEnergyConductor/Sink/Source/Storage`) were annotated
     `@Asynchronous` at class level although they are pure getters. Every Lua call allocates a main-thread
     task and resumes on the next tick. Remove class-level `@Asynchronous` and keep it only where a call
     can block (none of these do).
   - `AdapterNoteBlock.getNote` was `@Asynchronous` although it reads a byte (now synchronous).
   - `AdapterInventory` keeps class-level `@Asynchronous` for the heavy calls; `getInventoryName`,
     `getInventorySize` and `expandStack` are now `@Asynchronous(false)`.
5. **Collections**
   - `AdapterInventory.getAllStacks` → `LinkedHashMap` or `TreeMap<Index, Object>`.
   - `EntityLivingMetaProvider` uses two maps for potion data; keep one.
   - `ItemFingerprint` NBT hashing is repeated for the same tag in AE2 loops; cache the hash on the
     `NBTTagCompound` identity if profiling shows it.
6. **Reflection**
   - `FieldAccess<Integer>`/`FieldAccess<Double>` box on every read (`AdapterArcaneBore`,
     `AdapterElecticMachine`). OpenModsLib likely has primitive accessors; if not, cache the `Field` and
     read primitives.
   - Build the `FieldAccess`/`MethodAccess` handles in `static final` fields, not per adapter instance,
     where the target class is version-stable (most MFR/Railcraft/Thaumcraft adapters).
7. **Startup**
   - `ModuleVanilla.load()` performs ~40 `register` calls on the OpenPeripheral registry; each one
     triggers reflection/annotation scanning. It only runs once, but a table-driven list would make
     ordering and duplicate keys reviewable at a glance.

---

## 6. Refactors / code quality

1. **De-duplicate the `preInit` if-chain** ✅ (`IntegrationModules`). The 25-entry table is ordered, has a
   startup duplicate-key check, and `IntegrationModulesTest` pins the config-key set.
2. **Reflection target constants** ✅ (batch 5): all class names moved to `ReflectionTargets`, with a
   uniqueness/FQCN test.
3. **Adapter base classes**: the four Railcraft `AdapterHas*/Needs*` adapters share
   `BooleanReflectionAdapter` ✅ (batch 4). The CoFH energy and Ender-attuned adapters were reviewed in
   batch 5 and deliberately left alone: the energy interfaces have no shared storage supertype, and the
   Ender base would move `@ScriptCallable` methods into inherited generics that cannot be validated
   without CoFH on the test classpath.
4. **MFR/Railcraft reflection adapters** are ~15 classes of the same getter/setter pattern. A tiny
   `PropertyReflection` helper (`get`/`set` method name + type) would make them data-driven.
5. **Fingerprint type** ✅ (batch 3): `ItemFingerprint` now has validated ids and `equals`/`hashCode`.
   Using it directly in AE2/Railcraft comparisons (instead of ad-hoc `UniqueIdentifier` construction) is
   still open.
6. **Consistent validation**: decide once whether adapters throw `IllegalArgumentException` /
   `IllegalStateException` / `LuaException` for bad input and apply it. Today `AdapterInventory` uses
   `Index#checkElementIndex` (IOOBE), Mystcraft uses `IllegalStateException`, AE2 uses
   `Preconditions.checkArgument`, and several adapters NPE.
7. **Naming typos** (class renames are safe; config keys/Lua names are not):
   `AdapterElecticMachine` → `AdapterElectricMachine`, `AugumenedItemMetaProvider` →
   `AugmentedItemMetaProvider` (key `augumented` must stay), `RefuealableCartMetaProvider` →
   `RefuelableCartMetaProvider`, `alleleCompatator` → `alleleComparator`, `conventers` → `converters`,
   `parentYpe` → `parentType` ✅ (batch 4), `tilenentity` → migrated (batch 5).
8. **`AdapterBeacon`**: make `getEffectName` take `Potion`/`int` and avoid constructing `PotionEffect`.
9. **`ItemFingerprint` Javadoc** still says “SearchNeedle”; align the docs with the class name and its
   Lua table shape (`{id, dmg?, nbt_hash?}`).
10. **`README.md`** is two lines. OpenPeripheral’s README documents the test commands, the coverage gate,
    the game-test workflow and the dependency pins; mirroring that would make the new CI/verification
    discoverable.
11. **`mcmod.info`**: the author list uses a tab before `"boq"`; harmless but Spotless/code review noise.
12. **`modId` casing**: `modId = OpenPeripheralIntegration` (mixed case). Forge tolerates it, but mod ids
    are conventionally lowercase; changing it now is a breaking change, so at least note it.

---

## 7. Dead / unused code

| Item | Notes |
|---|---|
| `cofh/inventory/AdapterInventoryHandler` methods | Unreachable from Lua (no `@ScriptCallable`, package-private) — see 3.2. |
| `NotebookIInventoryWrapper.getStackInSlotOnClosing` | Always throws; part of `IInventory` but never useful. |
| `Mystcraft v1` package | Only loaded for Mystcraft `< 0.11.1.00`; kept for compatibility, but the code is effectively maintenance-only. Consider documenting that in the package-info. |

---

## 8. Test-coverage gaps & recommended next tests

Current coverage is deliberately concentrated on the vanilla layer, because all other integrations are
`compileOnly` and therefore absent from the test runtime classpath.

Highest-value additions:

1. **Regression test for the duplicate module key** (would have caught 3.1). Extract the
   `key → module` table from `preInit` and unit-test that keys are unique and cover every module class.
2. **`/op_dump`-style config test**: construct `OpenPeripheralIntegration` config in a temp dir and assert
   every module key exists (no FML bus needed if the key list is separated from registration).
3. **Reflection-target tests**: add a test-only `Class#forName` smoke test for each hard-coded target
   string, asserting the class exists when the corresponding dependency is on the test classpath. Adding
   `testImplementation` for IC2/MFR/Railcraft/Thaumcraft (as OpenPeripheral does for OpenComputers) would
   let these run in CI and catch silent integration rot.
4. **Pure-helper extraction tests**: `AdapterGridBase.compareToAEStack`, `ConverterIIndividual.describeIndividual`,
   `ConverterAspectList.aspectsToMap`, `SecurityUtils.canModify`, `TicketMetaProvider.canApply` — these are
   static/stateless and testable without the full mod if the helpers are moved out of the adapter classes
   (or if the compile-only mods are added to the test classpath).
5. **Additional in-game coverage**: a second test mod adapter for `AdapterWorldInventory` that places two
   chests, calls `pullItem`/`pushItem` through a real peripheral, and verifies item counts — that path is
   currently only covered by mocked headless tests.
6. **Config GUI**: a client-side game test (Xvfb) that opens the config screen and toggles a module would
   catch `ConfigGuiFactory` regressions; the current game test is server-only.
7. **Error paths**: `ItemFingerprint` with a malformed id, `AdapterSign.setText` with 5 lines,
   `AdapterInventory` with a null inventory, AE2 `AdapterGridBase` with a null grid — each currently either
   NPEs or throws an internal exception; tests would pin the intended error type once fixed.

---

## 9. Threading / lifecycle notes

- `CraftingCallback.calculationComplete` is invoked by AE2 on its own calculation thread and calls
  `access.signal(...)` (OpenPeripheral’s `IArchitectureAccess`). Verify that `signal` is thread-safe for
  the ComputerCraft architecture; if not, marshal the signal onto the server thread. The same applies to
  `CraftingRequester.jobStateChange`.
- The game test correctly runs the computer-driving loop off the server thread; keep that pattern if more
  in-game tests are added (CC’s `issueMainThreadTask` waits for the server thread, so driving a computer
  from the server thread deadlocks).
- `Integration.addModule` logs a warning and silently does nothing if modules are added after loading;
  `preInit` is correct today, but a late `OpenPeripheralIntegration.instance` access elsewhere would not
  be caught.

---

## 10. Suggested priority order

1. Fix 3.1 (duplicate Thaumcraft module) and 3.2 (CoFH inventory adapter) — both are one-file fixes with
   immediate player-visible impact.
2. Fix 3.3/3.4 (AE2 crafting) and 3.5 (enchantment bounds) — data corruption/over-reporting and a crash.
3. Fix 3.6/3.7 (sign + note block) — small, well-contained.
4. Apply the null-safety sweep from §4 (inventory, AE2 grid, potion, turbine, fingerprint).
5. Do the refactors in §6.1–6.4 (config table, reflection constants, adapter bases); they reduce the
   surface for the next round of bugs.
6. Add the tests from §8.1–8.4 and grow the coverage floor as the reflection layer becomes testable.

---

## Appendix — commands run for this review

```text
./gradlew updateDependencies        # then manually corrected (AE2 1099, OpenModsLib 0.10.15)
./gradlew spotlessApply
./gradlew test                      # 100 tests, green
./gradlew jacocoTestReport          # 21.3 % line, 18.6 % branch
./gradlew check                     # green
./gradlew gameTestServer            # green (adapter + real Lua markers)
```

Note: `updateDependencies` reported `Applied-Energistics-2-Unofficial: rv3-beta-1097-GTNH -> rv3-beta-999-GTNH`
and `OpenModsLib: 0.10.15 -> 0.11.0`; both were reverted by hand. Future runs need the same review until
the task’s version comparison is fixed upstream.
