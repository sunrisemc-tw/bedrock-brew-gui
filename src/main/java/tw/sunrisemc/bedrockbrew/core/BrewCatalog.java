package tw.sunrisemc.bedrockbrew.core;

import static org.bukkit.Material.*;
import static org.bukkit.potion.PotionType.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.bukkit.Material;
import org.bukkit.potion.PotionType;

public final class BrewCatalog {
  private static final List<BrewRecipe> ALL =
      List.of(
          r("night_vision", "夜視", BrewCategory.UTILITY, NIGHT_VISION, GOLDEN_CARROT),
          r(
              "long_night_vision",
              "夜視（延長）",
              BrewCategory.UTILITY,
              LONG_NIGHT_VISION,
              GOLDEN_CARROT,
              REDSTONE),
          r(
              "invisibility",
              "隱形",
              BrewCategory.UTILITY,
              INVISIBILITY,
              GOLDEN_CARROT,
              FERMENTED_SPIDER_EYE),
          r(
              "long_invisibility",
              "隱形（延長）",
              BrewCategory.UTILITY,
              LONG_INVISIBILITY,
              GOLDEN_CARROT,
              FERMENTED_SPIDER_EYE,
              REDSTONE),
          r("leaping", "跳躍", BrewCategory.BUFF, LEAPING, RABBIT_FOOT),
          r("long_leaping", "跳躍（延長）", BrewCategory.BUFF, LONG_LEAPING, RABBIT_FOOT, REDSTONE),
          r(
              "strong_leaping",
              "跳躍 II",
              BrewCategory.BUFF,
              STRONG_LEAPING,
              RABBIT_FOOT,
              GLOWSTONE_DUST),
          r("fire_resistance", "抗火", BrewCategory.BUFF, FIRE_RESISTANCE, MAGMA_CREAM),
          r(
              "long_fire_resistance",
              "抗火（延長）",
              BrewCategory.BUFF,
              LONG_FIRE_RESISTANCE,
              MAGMA_CREAM,
              REDSTONE),
          r("swiftness", "迅捷", BrewCategory.BUFF, SWIFTNESS, SUGAR),
          r("long_swiftness", "迅捷（延長）", BrewCategory.BUFF, LONG_SWIFTNESS, SUGAR, REDSTONE),
          r(
              "strong_swiftness",
              "迅捷 II",
              BrewCategory.BUFF,
              STRONG_SWIFTNESS,
              SUGAR,
              GLOWSTONE_DUST),
          r("slowness", "緩慢", BrewCategory.NEGATIVE, SLOWNESS, SUGAR, FERMENTED_SPIDER_EYE),
          r(
              "long_slowness",
              "緩慢（延長）",
              BrewCategory.NEGATIVE,
              LONG_SLOWNESS,
              SUGAR,
              FERMENTED_SPIDER_EYE,
              REDSTONE),
          r(
              "strong_slowness",
              "緩慢 IV",
              BrewCategory.NEGATIVE,
              STRONG_SLOWNESS,
              SUGAR,
              FERMENTED_SPIDER_EYE,
              GLOWSTONE_DUST),
          r("turtle_master", "龜仙人", BrewCategory.BUFF, TURTLE_MASTER, TURTLE_HELMET),
          r(
              "long_turtle_master",
              "龜仙人（延長）",
              BrewCategory.BUFF,
              LONG_TURTLE_MASTER,
              TURTLE_HELMET,
              REDSTONE),
          r(
              "strong_turtle_master",
              "龜仙人 II",
              BrewCategory.BUFF,
              STRONG_TURTLE_MASTER,
              TURTLE_HELMET,
              GLOWSTONE_DUST),
          r("water_breathing", "水下呼吸", BrewCategory.UTILITY, WATER_BREATHING, PUFFERFISH),
          r(
              "long_water_breathing",
              "水下呼吸（延長）",
              BrewCategory.UTILITY,
              LONG_WATER_BREATHING,
              PUFFERFISH,
              REDSTONE),
          r("healing", "治療", BrewCategory.COMBAT, HEALING, GLISTERING_MELON_SLICE),
          r(
              "strong_healing",
              "治療 II",
              BrewCategory.COMBAT,
              STRONG_HEALING,
              GLISTERING_MELON_SLICE,
              GLOWSTONE_DUST),
          r(
              "harming",
              "傷害",
              BrewCategory.NEGATIVE,
              HARMING,
              GLISTERING_MELON_SLICE,
              FERMENTED_SPIDER_EYE),
          r(
              "strong_harming",
              "傷害 II",
              BrewCategory.NEGATIVE,
              STRONG_HARMING,
              GLISTERING_MELON_SLICE,
              FERMENTED_SPIDER_EYE,
              GLOWSTONE_DUST),
          r("poison", "劇毒", BrewCategory.NEGATIVE, POISON, SPIDER_EYE),
          r("long_poison", "劇毒（延長）", BrewCategory.NEGATIVE, LONG_POISON, SPIDER_EYE, REDSTONE),
          r(
              "strong_poison",
              "劇毒 II",
              BrewCategory.NEGATIVE,
              STRONG_POISON,
              SPIDER_EYE,
              GLOWSTONE_DUST),
          r("regeneration", "回復", BrewCategory.COMBAT, REGENERATION, GHAST_TEAR),
          r(
              "long_regeneration",
              "回復（延長）",
              BrewCategory.COMBAT,
              LONG_REGENERATION,
              GHAST_TEAR,
              REDSTONE),
          r(
              "strong_regeneration",
              "回復 II",
              BrewCategory.COMBAT,
              STRONG_REGENERATION,
              GHAST_TEAR,
              GLOWSTONE_DUST),
          r("strength", "力量", BrewCategory.COMBAT, STRENGTH, BLAZE_POWDER),
          r("long_strength", "力量（延長）", BrewCategory.COMBAT, LONG_STRENGTH, BLAZE_POWDER, REDSTONE),
          r(
              "strong_strength",
              "力量 II",
              BrewCategory.COMBAT,
              STRONG_STRENGTH,
              BLAZE_POWDER,
              GLOWSTONE_DUST),
          r("weakness", "虛弱", BrewCategory.NEGATIVE, WEAKNESS, FERMENTED_SPIDER_EYE),
          r(
              "long_weakness",
              "虛弱（延長）",
              BrewCategory.NEGATIVE,
              LONG_WEAKNESS,
              FERMENTED_SPIDER_EYE,
              REDSTONE),
          r("slow_falling", "緩降", BrewCategory.UTILITY, SLOW_FALLING, PHANTOM_MEMBRANE),
          r(
              "long_slow_falling",
              "緩降（延長）",
              BrewCategory.UTILITY,
              LONG_SLOW_FALLING,
              PHANTOM_MEMBRANE,
              REDSTONE),
          r("wind_charged", "蓄風", BrewCategory.COMBAT, WIND_CHARGED, BREEZE_ROD),
          r("weaving", "織網", BrewCategory.NEGATIVE, WEAVING, COBWEB),
          r("oozing", "滲漿", BrewCategory.NEGATIVE, OOZING, SLIME_BLOCK),
          r("infested", "寄生", BrewCategory.NEGATIVE, INFESTED, STONE));

  private BrewCatalog() {}

  private static BrewRecipe r(
      String id, String name, BrewCategory category, PotionType type, Material... ingredients) {
    List<String> path = new ArrayList<>();
    if (type != WEAKNESS && type != LONG_WEAKNESS) {
      path.add(NETHER_WART.name());
    }
    for (Material ingredient : ingredients) {
      path.add(ingredient.name());
    }
    return new BrewRecipe(id, name, category, type, path);
  }

  public static List<BrewRecipe> all() {
    return ALL;
  }

  public static Optional<BrewRecipe> byId(String id) {
    return ALL.stream().filter(recipe -> recipe.id().equals(id)).findFirst();
  }

  public static List<BrewRecipe> byCategory(BrewCategory category) {
    return ALL.stream().filter(recipe -> recipe.category() == category && recipe.isBase()).toList();
  }

  public static List<BrewRecipe> variants(BrewRecipe base) {
    return ALL.stream().filter(recipe -> recipe.baseId().equals(base.baseId())).toList();
  }
}
