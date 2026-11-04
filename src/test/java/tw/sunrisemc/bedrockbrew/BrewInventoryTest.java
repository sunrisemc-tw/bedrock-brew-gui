package tw.sunrisemc.bedrockbrew;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;
import org.junit.jupiter.api.Test;

class BrewInventoryTest {
  @Test
  void customEffectsAndOtherPotionsAreNeverWater() {
    var customWater = mock(ItemStack.class);
    var meta = mock(PotionMeta.class);
    when(customWater.getType()).thenReturn(Material.POTION);
    when(customWater.getItemMeta()).thenReturn(meta);
    when(meta.getBasePotionType()).thenReturn(PotionType.WATER);
    when(meta.hasCustomEffects()).thenReturn(true);
    assertFalse(BrewInventory.isWater(customWater));
    when(meta.hasCustomEffects()).thenReturn(false);
    assertTrue(BrewInventory.isWater(customWater));
    // Exact metadata comparison must additionally reject renamed/plugin-tagged water.
    assertFalse(BrewInventory.plainWater(mock(ItemStack.class)).test(customWater));
    when(meta.getBasePotionType()).thenReturn(PotionType.HEALING);
    assertFalse(BrewInventory.isWater(customWater));
    when(customWater.getType()).thenReturn(Material.SPLASH_POTION);
    when(meta.getBasePotionType()).thenReturn(PotionType.WATER);
    assertFalse(BrewInventory.isWater(customWater));
    assertFalse(BrewInventory.isWater(null));
  }

  @Test
  void countIgnoresNullAirAndNonWaterPotions() {
    ItemStack[] contents = {
      null,
      item(Material.AIR, 1),
      item(Material.POTION, 2),
      item(Material.SPLASH_POTION, 1),
      item(Material.SUGAR, 4)
    };
    var counts = BrewInventory.count(contents, BrewInventory::isWater);
    assertFalse(counts.containsKey("WATER_BOTTLE"));
    assertEquals(2, counts.get("POTION"));
    assertEquals(4, counts.get("SUGAR"));
  }

  @Test
  void preparationNeverMutatesLiveStacksAndRemovesAcrossSlots() {
    ItemStack sugar = item(Material.SUGAR, 2);
    ItemStack[] snapshot = {sugar, item(Material.SUGAR, 2), null};
    ItemStack output = item(Material.POTION, 1);
    var result =
        BrewInventory.prepare(snapshot, Map.of("SUGAR", 3), List.of(output), ignored -> false, 64);
    assertNotNull(result);
    assertEquals(2, sugar.getAmount());
    assertEquals(2, snapshot[1].getAmount());
    assertEquals(Material.POTION, result[0].getType());
    assertEquals(1, result[1].getAmount());
    assertNotSame(output, result[0]);
  }

  @Test
  void fullInventoryWorksWhenConsumedBottlesFreeSlots() {
    ItemStack[] full = {
      item(Material.POTION, 1),
      item(Material.POTION, 1),
      item(Material.POTION, 1),
      item(Material.SUGAR, 64)
    };
    var result =
        BrewInventory.prepare(
            full,
            Map.of("WATER_BOTTLE", 3, "SUGAR", 1),
            List.of(
                item(Material.SPLASH_POTION, 1),
                item(Material.SPLASH_POTION, 1),
                item(Material.SPLASH_POTION, 1)),
            stack -> stack.getType() == Material.POTION,
            64);
    assertNotNull(result);
    assertEquals(
        3,
        java.util.Arrays.stream(result)
            .filter(stack -> stack.getType() == Material.SPLASH_POTION)
            .count());
    assertEquals(63, result[3].getAmount());
    assertEquals(64, full[3].getAmount());
  }

  @Test
  void insufficientRoomForReturnedBottleAbortsWithoutConsumption() {
    ItemStack[] full = {item(Material.POTION, 1), item(Material.DRAGON_BREATH, 2)};
    var result =
        BrewInventory.prepare(
            full,
            Map.of("WATER_BOTTLE", 1, "DRAGON_BREATH", 1),
            List.of(item(Material.LINGERING_POTION, 1), item(Material.GLASS_BOTTLE, 1)),
            stack -> stack.getType() == Material.POTION,
            64);
    assertNull(result);
    assertEquals(1, full[0].getAmount());
    assertEquals(2, full[1].getAmount());
  }

  @Test
  void returnedBottleStacksWithoutIncreasingPotionStackSize() {
    ItemStack[] before = {item(Material.POTION, 3), item(Material.GLASS_BOTTLE, 63), null, null};
    var result =
        BrewInventory.prepare(
            before,
            Map.of("WATER_BOTTLE", 3),
            List.of(item(Material.LINGERING_POTION, 3), item(Material.GLASS_BOTTLE, 1)),
            stack -> stack.getType() == Material.POTION,
            64);
    assertNotNull(result);
    assertEquals(64, result[1].getAmount());
    for (ItemStack stack : result) {
      if (stack.getType() == Material.LINGERING_POTION) {
        assertEquals(1, stack.getAmount());
      }
    }
  }

  @Test
  void missingIngredientAbortsBeforeAnyRemoval() {
    ItemStack[] before = {item(Material.SUGAR, 2)};
    assertNull(
        BrewInventory.prepare(
            before, Map.of("SUGAR", 1, "NETHER_WART", 1), List.of(), ignored -> false, 64));
    assertEquals(2, before[0].getAmount());
  }

  @Test
  void commitRechecksExactSnapshotBeforeSingleWrite() {
    var inventory = mock(PlayerInventory.class);
    ItemStack[] snapshot = {item(Material.SUGAR, 1)};
    ItemStack[] replacement = {item(Material.POTION, 1)};
    ItemStack[] changed = {item(Material.STONE, 1)};
    when(inventory.getStorageContents()).thenReturn(changed);
    assertFalse(BrewInventory.commit(inventory, snapshot, replacement));
    verify(inventory, never()).setStorageContents(any());
    when(inventory.getStorageContents()).thenReturn(snapshot);
    assertTrue(BrewInventory.commit(inventory, snapshot, replacement));
    verify(inventory).setStorageContents(replacement);
  }

  private static ItemStack item(Material type, int amount) {
    var item = mock(ItemStack.class);
    var quantity = new AtomicInteger(amount);
    when(item.getType()).thenReturn(type);
    when(item.getAmount()).thenAnswer(invocation -> quantity.get());
    doAnswer(
            invocation -> {
              quantity.set(invocation.getArgument(0));
              return null;
            })
        .when(item)
        .setAmount(anyInt());
    when(item.getMaxStackSize())
        .thenReturn(
            type == Material.POTION
                    || type == Material.SPLASH_POTION
                    || type == Material.LINGERING_POTION
                ? 1
                : 64);
    when(item.clone()).thenAnswer(invocation -> item(type, quantity.get()));
    when(item.isSimilar(any()))
        .thenAnswer(
            invocation -> {
              ItemStack other = invocation.getArgument(0);
              return other != null && other.getType() == type;
            });
    return item;
  }
}
