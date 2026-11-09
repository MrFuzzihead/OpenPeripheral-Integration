package openperipheral.integration.vanilla;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.IBossDisplayData;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.monster.EntityWitch;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityBat;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.passive.EntitySheep;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.mojang.authlib.GameProfile;

import openperipheral.api.meta.IItemStackPartialMetaBuilder;
import openperipheral.api.meta.IMetaProviderProxy;
import openperipheral.integration.OpcAccess;

/**
 * Entity metadata providers, driven by Mockito mocks. Constructing real entities needs a bootstrapped
 * world, but the providers only read a handful of getters, so mocks cover every branch cheaply.
 */
public class EntityMetaProvidersTest {

    private IItemStackPartialMetaBuilder originalMetaBuilder;

    @Before
    public void installMetaBuilder() {
        originalMetaBuilder = OpcAccess.itemStackMetaBuilder;
        OpcAccess.itemStackMetaBuilder = mock(IItemStackPartialMetaBuilder.class);
        when(OpcAccess.itemStackMetaBuilder.createProxy(any(ItemStack.class)))
            .thenReturn(mock(IMetaProviderProxy.class));
    }

    @After
    public void restoreMetaBuilder() {
        OpcAccess.itemStackMetaBuilder = originalMetaBuilder;
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Class<?> type = target.getClass();
        while (type != null) {
            try {
                final Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                field.set(target, value);
                return;
            } catch (NoSuchFieldException e) {
                type = type.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }

    @Test
    public void bossMetadata() throws Exception {
        final IBossDisplayData boss = mock(IBossDisplayData.class);
        when(boss.func_145748_c_()).thenReturn(new ChatComponentText("Boss"));
        when(boss.getHealth()).thenReturn(50.0f);
        when(boss.getMaxHealth()).thenReturn(100.0f);

        final BossMetaProvider provider = new BossMetaProvider();
        @SuppressWarnings("unchecked")
        final Map<String, Object> map = (Map<String, Object>) provider.getMeta(boss, null);
        Assert.assertEquals("Boss", map.get("name"));
        Assert.assertEquals(50.0f, map.get("health"));
        Assert.assertEquals(100.0f, map.get("maxHealth"));
    }

    @Test
    public void batMetadata() {
        final EntityBat bat = mock(EntityBat.class);
        when(bat.getIsBatHanging()).thenReturn(true);
        @SuppressWarnings("unchecked")
        final Map<String, Object> map = (Map<String, Object>) new EntityBatMetaProvider().getMeta(bat, null);
        Assert.assertEquals(Boolean.TRUE, map.get("isHanging"));
    }

    @Test
    public void creeperMetadata() {
        final EntityCreeper creeper = mock(EntityCreeper.class);
        when(creeper.getPowered()).thenReturn(true);
        @SuppressWarnings("unchecked")
        final Map<String, Object> map = (Map<String, Object>) new EntityCreeperMetaProvider().getMeta(creeper, null);
        Assert.assertEquals(Boolean.TRUE, map.get("isCharged"));
    }

    @Test
    public void pigAndSheepMetadata() {
        final EntityPig pig = mock(EntityPig.class);
        when(pig.getSaddled()).thenReturn(true);
        @SuppressWarnings("unchecked")
        final Map<String, Object> pigMap = (Map<String, Object>) new EntityPigMetaProvider().getMeta(pig, null);
        Assert.assertEquals(Boolean.TRUE, pigMap.get("isSaddled"));

        final EntitySheep sheep = mock(EntitySheep.class);
        when(sheep.getFleeceColor()).thenReturn(5);
        when(sheep.getSheared()).thenReturn(true);
        @SuppressWarnings("unchecked")
        final Map<String, Object> sheepMap = (Map<String, Object>) new EntitySheepMetaProvider().getMeta(sheep, null);
        Assert.assertEquals(5, sheepMap.get("sheepColor"));
        Assert.assertEquals(Boolean.TRUE, sheepMap.get("isSheared"));
    }

    @Test
    public void zombieAndWitchMetadata() {
        final EntityZombie zombie = mock(EntityZombie.class);
        when(zombie.isVillager()).thenReturn(true);
        when(zombie.isConverting()).thenReturn(true);
        @SuppressWarnings("unchecked")
        final Map<String, Object> zombieMap = (Map<String, Object>) new EntityZombieMetaProvider()
            .getMeta(zombie, null);
        Assert.assertEquals(Boolean.TRUE, zombieMap.get("isVillagerZombie"));
        Assert.assertEquals(Boolean.TRUE, zombieMap.get("convertingToVillager"));

        final EntityWitch witch = mock(EntityWitch.class);
        when(witch.getAggressive()).thenReturn(true);
        @SuppressWarnings("unchecked")
        final Map<String, Object> witchMap = (Map<String, Object>) new EntityWitchMetaProvider().getMeta(witch, null);
        Assert.assertEquals(Boolean.TRUE, witchMap.get("isAggressive"));
    }

    @Test
    public void tameableMetadataOnlyAddsOwnerWhenTamed() {
        final EntityTameable tameable = mock(EntityTameable.class);
        when(tameable.isTamed()).thenReturn(false);
        final EntityTameableMetaProvider provider = new EntityTameableMetaProvider();

        @SuppressWarnings("unchecked")
        final Map<String, Object> untamed = (Map<String, Object>) provider.getMeta(tameable, null);
        Assert.assertEquals(Boolean.FALSE, untamed.get("isTamed"));
        Assert.assertFalse(untamed.containsKey("ownerUUID"));

        final UUID owner = UUID.randomUUID();
        when(tameable.isTamed()).thenReturn(true);
        when(tameable.isSitting()).thenReturn(true);
        when(tameable.func_152113_b()).thenReturn(owner.toString());

        @SuppressWarnings("unchecked")
        final Map<String, Object> tamed = (Map<String, Object>) provider.getMeta(tameable, null);
        Assert.assertEquals(Boolean.TRUE, tamed.get("isTamed"));
        Assert.assertEquals(Boolean.TRUE, tamed.get("isSitting"));
        Assert.assertEquals(owner.toString(), tamed.get("ownerUUID"));
    }

    @Test
    public void villagerMetadataHandlesTrading() {
        final EntityVillager villager = mock(EntityVillager.class);
        when(villager.getProfession()).thenReturn(2);
        when(villager.isMating()).thenReturn(true);
        when(villager.isPlaying()).thenReturn(false);
        when(villager.isTrading()).thenReturn(false);

        final EntityVillagerMetaProvider provider = new EntityVillagerMetaProvider();
        @SuppressWarnings("unchecked")
        final Map<String, Object> idle = (Map<String, Object>) provider.getMeta(villager, null);
        Assert.assertEquals(2, idle.get("profession"));
        Assert.assertFalse(idle.containsKey("customer"));

        final EntityPlayer customer = mock(EntityPlayer.class);
        final GameProfile profile = new GameProfile(UUID.randomUUID(), "Customer");
        when(customer.getGameProfile()).thenReturn(profile);
        when(villager.isTrading()).thenReturn(true);
        when(villager.getCustomer()).thenReturn(customer);

        @SuppressWarnings("unchecked")
        final Map<String, Object> trading = (Map<String, Object>) provider.getMeta(villager, null);
        Assert.assertEquals(profile, trading.get("customer"));
    }

    @Test
    public void horseMetadataSkipsTheChestWhenNotChested() {
        final EntityHorse horse = mock(EntityHorse.class);
        when(horse.isEatingHaystack()).thenReturn(true);
        when(horse.getHasReproduced()).thenReturn(false);
        when(horse.func_110205_ce()).thenReturn(false);
        when(horse.getHorseType()).thenReturn(0);
        when(horse.getHorseVariant()).thenReturn(1);
        when(horse.getTemper()).thenReturn(10);
        when(horse.isTame()).thenReturn(true);
        when(horse.isChested()).thenReturn(false);

        @SuppressWarnings("unchecked")
        final Map<String, Object> map = (Map<String, Object>) new EntityHorseMetaProvider().getMeta(horse, null);
        Assert.assertEquals(Boolean.TRUE, map.get("eatingHaystack"));
        Assert.assertEquals(Boolean.TRUE, map.get("horseTame"));
        Assert.assertEquals(Boolean.FALSE, map.get("chestedHorse"));
        Assert.assertFalse(map.containsKey("chest"));
    }

    @Test
    public void horseMetadataIncludesChestWhenChested() throws Exception {
        final EntityHorse horse = mock(EntityHorse.class);
        when(horse.isChested()).thenReturn(true);

        final net.minecraft.inventory.AnimalChest chest = mock(net.minecraft.inventory.AnimalChest.class);
        when(chest.getSizeInventory()).thenReturn(1);
        when(chest.getStackInSlot(0)).thenReturn(new ItemStack(new Item(), 1));
        setField(horse, "horseChest", chest);

        @SuppressWarnings("unchecked")
        final Map<String, Object> map = (Map<String, Object>) new EntityHorseMetaProvider().getMeta(horse, null);
        Assert.assertEquals(Boolean.TRUE, map.get("chestedHorse"));
        Assert.assertNotNull("Chest inventory was not wrapped", map.get("chest"));
    }

    @Test
    public void livingMetadataReportsBlockAndEntityRayHits() throws Exception {
        final EntityLivingBase living = mock(EntityLivingBase.class);
        final World world = mock(World.class);
        setField(living, "worldObj", world);
        when(living.getActivePotionEffects()).thenReturn(Collections.emptyList());
        when(living.getHealth()).thenReturn(20.0f);
        when(living.getMaxHealth()).thenReturn(20.0f);
        when(living.getLook(1.0f)).thenReturn(Vec3.createVectorHelper(0, 0, 1));

        final MovingObjectPosition blockHit = new MovingObjectPosition(4, 5, 6, 1, Vec3.createVectorHelper(4, 5, 6));
        when(world.rayTraceBlocks(any(Vec3.class), any(Vec3.class))).thenReturn(blockHit);

        final EntityLivingMetaProvider provider = new EntityLivingMetaProvider();
        @SuppressWarnings("unchecked")
        final Map<String, Object> absolute = (Map<String, Object>) provider.getMeta(living, null);
        @SuppressWarnings("unchecked")
        final Map<String, Object> absoluteHit = (Map<String, Object>) absolute.get("lookingAt");
        Assert.assertEquals("block", absoluteHit.get("type"));
        @SuppressWarnings("unchecked")
        final Map<String, Object> absolutePos = (Map<String, Object>) absoluteHit.get("position");
        Assert.assertEquals(4, absolutePos.get("x"));
        Assert.assertEquals(5, absolutePos.get("y"));
        Assert.assertEquals(6, absolutePos.get("z"));

        @SuppressWarnings("unchecked")
        final Map<String, Object> relative = (Map<String, Object>) provider
            .getMeta(living, Vec3.createVectorHelper(1, 2, 3));
        @SuppressWarnings("unchecked")
        final Map<String, Object> relativePos = (Map<String, Object>) ((Map<String, Object>) relative.get("lookingAt"))
            .get("position");
        Assert.assertEquals(3.0, ((Number) relativePos.get("x")).doubleValue(), 0.0001);
        Assert.assertEquals(3.0, ((Number) relativePos.get("y")).doubleValue(), 0.0001);
        Assert.assertEquals(3.0, ((Number) relativePos.get("z")).doubleValue(), 0.0001);

        final Entity target = mock(Entity.class);
        when(target.getEntityId()).thenReturn(42);
        when(world.rayTraceBlocks(any(Vec3.class), any(Vec3.class))).thenReturn(new MovingObjectPosition(target));

        @SuppressWarnings("unchecked")
        final Map<String, Object> entityMap = (Map<String, Object>) provider.getMeta(living, null);
        @SuppressWarnings("unchecked")
        final Map<String, Object> entityHit = (Map<String, Object>) entityMap.get("lookingAt");
        Assert.assertEquals("entity", entityHit.get("type"));
        Assert.assertEquals(42, entityHit.get("id"));
    }

    @Test
    public void itemAndItemFrameMetadata() {
        final EntityItem item = mock(EntityItem.class);
        final ItemStack stack = new ItemStack(new Item(), 2);
        when(item.getEntityItem()).thenReturn(stack);
        item.age = 40;
        item.lifespan = 6000;

        @SuppressWarnings("unchecked")
        final Map<String, Object> itemMap = (Map<String, Object>) new EntityItemMetaProvider().getMeta(item, null);
        Assert.assertSame(stack, itemMap.get("stack"));
        Assert.assertEquals(40, itemMap.get("age"));
        Assert.assertEquals(6000, itemMap.get("lifespan"));

        final EntityItemFrame frame = mock(EntityItemFrame.class);
        when(frame.getDisplayedItem()).thenReturn(stack);
        when(frame.getRotation()).thenReturn(3);

        @SuppressWarnings("unchecked")
        final Map<String, Object> frameMap = (Map<String, Object>) new ItemFrameMetaProvider().getMeta(frame, null);
        Assert.assertSame(stack, frameMap.get("item"));
        Assert.assertEquals(3, frameMap.get("rotation"));
    }

    @Test
    public void playerMetadata() throws Exception {
        final EntityPlayer player = mock(EntityPlayer.class);
        final net.minecraft.entity.player.InventoryPlayer inventory = mock(
            net.minecraft.entity.player.InventoryPlayer.class);
        when(inventory.getSizeInventory()).thenReturn(0);
        final net.minecraft.util.FoodStats food = mock(net.minecraft.util.FoodStats.class);
        when(food.getFoodLevel()).thenReturn(18);
        final GameProfile profile = new GameProfile(UUID.randomUUID(), "Player");
        when(player.getGameProfile()).thenReturn(profile);
        when(player.getFoodStats()).thenReturn(food);
        when(player.xpBarCap()).thenReturn(30);
        when(player.isBlocking()).thenReturn(true);
        player.experienceLevel = 7;
        player.experience = 0.5f;
        player.capabilities = new net.minecraft.entity.player.PlayerCapabilities();
        player.inventory = inventory;

        @SuppressWarnings("unchecked")
        final Map<String, Object> map = (Map<String, Object>) new EntityPlayerMetaProvider().getMeta(player, null);
        Assert.assertEquals(profile, map.get("profile"));
        Assert.assertEquals(18, map.get("foodLevel"));
        Assert.assertEquals(Boolean.TRUE, map.get("isBlocking"));
        Assert.assertEquals(Boolean.FALSE, map.get("isCreativeMode"));

        @SuppressWarnings("unchecked")
        final Map<String, Object> experience = (Map<String, Object>) map.get("experience");
        Assert.assertEquals(7, experience.get("level"));
        Assert.assertEquals(0.5f, experience.get("levelProgress"));
        Assert.assertEquals(30, experience.get("nextLevelXp"));
    }

    @Test
    public void livingMetadata() throws Exception {
        final EntityLivingBase living = mock(EntityLivingBase.class);
        final World world = mock(World.class);
        setField(living, "worldObj", world);
        when(living.getEquipmentInSlot(0)).thenReturn(new ItemStack(new Item(), 1));
        for (int slot = 1; slot <= 4; slot++) when(living.getEquipmentInSlot(slot)).thenReturn(null);
        when(living.getActivePotionEffects()).thenReturn(Collections.emptyList());
        when(living.getHealth()).thenReturn(20.0f);
        when(living.getMaxHealth()).thenReturn(20.0f);
        when(living.isBurning()).thenReturn(true);
        when(living.isEntityAlive()).thenReturn(true);
        when(living.getLook(1.0f)).thenReturn(Vec3.createVectorHelper(0, 0, 1));

        @SuppressWarnings("unchecked")
        final Map<String, Object> map = (Map<String, Object>) new EntityLivingMetaProvider().getMeta(living, null);
        Assert.assertEquals(20.0f, map.get("health"));
        Assert.assertEquals(20.0f, map.get("maxHealth"));
        Assert.assertEquals(Boolean.TRUE, map.get("isBurning"));
        Assert.assertEquals(Boolean.TRUE, map.get("isAlive"));
        Assert.assertTrue(map.containsKey("armor"));
        Assert.assertTrue(map.containsKey("potionEffects"));
        Assert.assertTrue(map.containsKey("potion_effects"));
        Assert.assertFalse(map.containsKey("lookingAt"));
    }

    @Test
    public void livingMetadataWithPotionEffects() throws Exception {
        final EntityLivingBase living = mock(EntityLivingBase.class);
        setField(living, "worldObj", mock(World.class));
        final net.minecraft.potion.PotionEffect effect = new net.minecraft.potion.PotionEffect(1, 200, 1, true);
        when(living.getActivePotionEffects()).thenReturn(Collections.singletonList(effect));
        when(living.getHealth()).thenReturn(10.0f);
        when(living.getMaxHealth()).thenReturn(20.0f);
        when(living.getLook(1.0f)).thenReturn(Vec3.createVectorHelper(0, 0, 1));

        @SuppressWarnings("unchecked")
        final Map<String, Object> map = (Map<String, Object>) new EntityLivingMetaProvider().getMeta(living, null);

        @SuppressWarnings("unchecked")
        final List<Map<String, Object>> effects = (List<Map<String, Object>>) map.get("potion_effects");
        Assert.assertEquals(1, effects.size());
        Assert.assertEquals(
            10,
            effects.get(0)
                .get("duration"));
        Assert.assertEquals(
            1,
            effects.get(0)
                .get("amplifier"));
        Assert.assertEquals(
            Boolean.TRUE,
            effects.get(0)
                .get("is_ambient"));

        @SuppressWarnings("unchecked")
        final Map<Object, String> legacy = (Map<Object, String>) map.get("potionEffects");
        Assert.assertEquals("potion.moveSpeed", legacy.get(1));
    }

    @Test
    public void entityMetaProvidersDecareTheirKeys() {
        Assert.assertEquals("boss", new BossMetaProvider().getKey());
        Assert.assertEquals("bat", new EntityBatMetaProvider().getKey());
        Assert.assertEquals("living", new EntityLivingMetaProvider().getKey());
        Assert.assertEquals("inventory", new InventoryMetaProvider().getKey());
        Assert.assertEquals("inventory-provider", new InventoryProviderMetaProvider().getKey());
    }

}
