package tw.sunrisemc.bedrockbrew.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class BrewTransactionTest {
  @Test
  void rejectsWithoutChangingItemsWhenAnyIngredientIsMissing() {
    var inventory = new HashMap<>(Map.of("WATER_BOTTLE", 1, "NETHER_WART", 1));
    var before = new HashMap<>(inventory);
    var result = BrewTransaction.plan(recipe("strength"), BottleKind.NORMAL, 1, inventory);
    assertFalse(result.success());
    assertEquals(before, inventory);
    assertEquals(Map.of("BLAZE_POWDER", 2), result.missing());
    assertEquals(Map.of(), result.remove());
    assertEquals(0, result.outputs());
    assertEquals(0, result.fuelRemaining());
  }

  @Test
  void successfulPlanReturnsExactRemovalAndOutputsWithoutMutatingSnapshot() {
    var inventory =
        new HashMap<>(
            Map.of("WATER_BOTTLE", 2, "NETHER_WART", 1, "BLAZE_POWDER", 2, "GLOWSTONE_DUST", 1));
    var before = new HashMap<>(inventory);
    var result = BrewTransaction.plan(recipe("strong_strength"), BottleKind.NORMAL, 2, inventory);
    assertTrue(result.success());
    assertEquals(before, result.remove());
    assertEquals(2, result.outputs());
    assertEquals(17, result.fuelRemaining());
    assertEquals(before, inventory);
  }

  @Test
  void missingQuantitiesAreExactIncludingContainersAndFuel() {
    var inventory = Map.of("WATER_BOTTLE", 1, "NETHER_WART", 1, "BLAZE_POWDER", 1);
    var result =
        BrewTransaction.plan(recipe("strong_strength"), BottleKind.LINGERING, 3, inventory, 0);
    assertEquals(
        Map.of(
            "WATER_BOTTLE",
            2,
            "BLAZE_POWDER",
            1,
            "GLOWSTONE_DUST",
            1,
            "GUNPOWDER",
            1,
            "DRAGON_BREATH",
            1),
        result.missing());
  }

  @Test
  void everyCatalogPlanPreservesBatchCostsAndFuelBoundaries() {
    for (BrewRecipe recipe : BrewCatalog.all()) {
      for (BottleKind kind : BottleKind.values()) {
        for (int credits = 0; credits < 20; credits++) {
          for (int amount = 1; amount <= 3; amount++) {
            var available = recipe.cost(amount, kind, credits);
            var result = BrewTransaction.plan(recipe, kind, amount, available, credits);
            assertTrue(result.success());
            assertEquals(available, result.remove());
            int powder = recipe.fuelPowder(kind, credits);
            assertEquals(credits + 20 * powder - recipe.operations(kind), result.fuelRemaining());
            assertTrue(result.fuelRemaining() >= 0 && result.fuelRemaining() < 20);
          }
        }
      }
    }
  }

  @Test
  void fuelCarriesAcrossBatchesAndStrengthIngredientIsNotFuel() {
    var recipe = recipe("weakness");
    int credits = 0;
    int powderUsed = 0;
    for (int batch = 0; batch < 21; batch++) {
      var available = recipe.cost(3, BottleKind.NORMAL, credits);
      powderUsed += available.getOrDefault("BLAZE_POWDER", 0);
      credits =
          BrewTransaction.plan(recipe, BottleKind.NORMAL, 3, available, credits).fuelRemaining();
    }
    assertEquals(2, powderUsed);
    assertEquals(19, credits);
    assertEquals(1, recipe("strength").cost(3, BottleKind.NORMAL, 2).get("BLAZE_POWDER"));
  }

  @Test
  void failurePreservesCreditsAndDefensivelyCopiesPlans() {
    var result = BrewTransaction.plan(recipe("healing"), BottleKind.NORMAL, 3, Map.of(), 19);
    assertEquals(19, result.fuelRemaining());
    assertThrows(UnsupportedOperationException.class, () -> result.missing().clear());
    var mutable = new HashMap<>(Map.of("SUGAR", 1));
    var plan = new BrewPlan(true, mutable, Map.of(), 1, 0);
    mutable.clear();
    assertEquals(Map.of("SUGAR", 1), plan.remove());
  }

  @Test
  void amountMustBeOneToThree() {
    assertThrows(
        IllegalArgumentException.class,
        () -> BrewTransaction.plan(recipe("strength"), BottleKind.NORMAL, 0, Map.of()));
    assertThrows(
        IllegalArgumentException.class,
        () -> BrewTransaction.plan(recipe("strength"), BottleKind.NORMAL, 4, Map.of()));
  }

  private static BrewRecipe recipe(String id) {
    return BrewCatalog.byId(id).orElseThrow();
  }
}
