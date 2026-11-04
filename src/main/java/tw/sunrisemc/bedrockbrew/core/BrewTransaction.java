package tw.sunrisemc.bedrockbrew.core;

import java.util.LinkedHashMap;
import java.util.Map;

public final class BrewTransaction {
  private BrewTransaction() {}

  public static BrewPlan plan(
      BrewRecipe recipe, BottleKind kind, int amount, Map<String, Integer> inventory) {
    return plan(recipe, kind, amount, inventory, 0);
  }

  public static BrewPlan plan(
      BrewRecipe recipe,
      BottleKind kind,
      int amount,
      Map<String, Integer> inventory,
      int fuelCredits) {
    Map<String, Integer> cost = recipe.cost(amount, kind, fuelCredits);
    Map<String, Integer> missing = new LinkedHashMap<>();
    cost.forEach(
        (item, needed) -> {
          int lack = needed - Math.max(0, inventory.getOrDefault(item, 0));
          if (lack > 0) {
            missing.put(item, lack);
          }
        });
    if (!missing.isEmpty()) {
      return new BrewPlan(false, Map.of(), missing, 0, fuelCredits);
    }
    int remainingFuel =
        fuelCredits + recipe.fuelPowder(kind, fuelCredits) * 20 - recipe.operations(kind);
    return new BrewPlan(true, cost, Map.of(), amount, remainingFuel);
  }
}
