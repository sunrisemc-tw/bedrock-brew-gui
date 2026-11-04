package tw.sunrisemc.bedrockbrew.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.bukkit.potion.PotionType;

public record BrewRecipe(
    String id,
    String name,
    BrewCategory category,
    PotionType potionType,
    List<String> ingredients) {
  public BrewRecipe {
    Objects.requireNonNull(id);
    Objects.requireNonNull(name);
    Objects.requireNonNull(category);
    Objects.requireNonNull(potionType);
    ingredients = List.copyOf(ingredients);
    if (ingredients.isEmpty()) {
      throw new IllegalArgumentException("A brewing path cannot be empty");
    }
  }

  public String baseId() {
    return id.replaceFirst("^(long_|strong_)", "");
  }

  public boolean isBase() {
    return id.equals(baseId());
  }

  public int operations(BottleKind kind) {
    Objects.requireNonNull(kind);
    return ingredients.size()
        + switch (kind) {
          case NORMAL -> 0;
          case SPLASH -> 1;
          case LINGERING -> 2;
        };
  }

  public int fuelPowder(BottleKind kind, int fuelCredits) {
    if (fuelCredits < 0 || fuelCredits > 19) {
      throw new IllegalArgumentException("fuel credits must be 0..19");
    }
    return Math.max(0, (operations(kind) - fuelCredits + 19) / 20);
  }

  public Map<String, Integer> cost(int bottles) {
    return cost(bottles, BottleKind.NORMAL);
  }

  public Map<String, Integer> cost(int bottles, BottleKind kind) {
    return cost(bottles, kind, 0);
  }

  public Map<String, Integer> cost(int bottles, BottleKind kind, int fuelCredits) {
    if (bottles < 1 || bottles > 3) {
      throw new IllegalArgumentException("bottles must be 1..3");
    }
    Map<String, Integer> cost = new LinkedHashMap<>();
    cost.put("WATER_BOTTLE", bottles);
    for (String ingredient : ingredients) {
      cost.merge(ingredient, 1, Integer::sum);
    }
    if (kind != BottleKind.NORMAL) {
      cost.put("GUNPOWDER", 1);
    }
    if (kind == BottleKind.LINGERING) {
      cost.put("DRAGON_BREATH", 1);
    }
    int powder = fuelPowder(kind, fuelCredits);
    if (powder > 0) {
      cost.merge("BLAZE_POWDER", powder, Integer::sum);
    }
    return Collections.unmodifiableMap(cost);
  }
}
