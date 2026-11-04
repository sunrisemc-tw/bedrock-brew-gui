package tw.sunrisemc.bedrockbrew.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.bukkit.Material;
import org.bukkit.potion.PotionType;
import org.junit.jupiter.api.Test;

class BrewCatalogTest {
  @Test
  void listsEveryVanillaSurvivalEffectVariantWithoutDuplicates() {
    var recipes = BrewCatalog.all();
    Set<PotionType> nonEffects =
        Set.of(
            PotionType.WATER,
            PotionType.AWKWARD,
            PotionType.MUNDANE,
            PotionType.THICK,
            PotionType.LUCK);
    Set<PotionType> expected =
        Arrays.stream(PotionType.values())
            .filter(type -> !nonEffects.contains(type))
            .collect(Collectors.toSet());
    assertEquals(41, recipes.size());
    assertEquals(41, recipes.stream().map(BrewRecipe::id).distinct().count());
    assertEquals(41, recipes.stream().map(BrewRecipe::potionType).distinct().count());
    assertEquals(
        expected, recipes.stream().map(BrewRecipe::potionType).collect(Collectors.toSet()));
    recipes.forEach(recipe -> recipe.ingredients().forEach(Material::valueOf));
  }

  @Test
  void categoryTaxonomyHasNineteenBaseEffects() {
    assertEquals(
        List.of("leaping", "fire_resistance", "swiftness", "turtle_master"),
        ids(BrewCategory.BUFF));
    assertEquals(
        List.of("night_vision", "invisibility", "water_breathing", "slow_falling"),
        ids(BrewCategory.UTILITY));
    assertEquals(
        List.of("healing", "regeneration", "strength", "wind_charged"), ids(BrewCategory.COMBAT));
    assertEquals(
        List.of("slowness", "harming", "poison", "weakness", "weaving", "oozing", "infested"),
        ids(BrewCategory.NEGATIVE));
  }

  @Test
  void variantsStayWithinTheirBaseEffect() {
    for (BrewCategory category : BrewCategory.values()) {
      for (BrewRecipe base : BrewCatalog.byCategory(category)) {
        var variants = BrewCatalog.variants(base);
        assertTrue(variants.size() >= 1 && variants.size() <= 3);
        assertTrue(variants.stream().allMatch(recipe -> recipe.baseId().equals(base.id())));
        assertTrue(variants.contains(base));
      }
    }
  }

  @Test
  void weaknessDoesNotUseWartAndAllOtherPathsDo() {
    for (BrewRecipe recipe : BrewCatalog.all()) {
      assertEquals(
          !recipe.baseId().equals("weakness"), recipe.ingredients().contains("NETHER_WART"));
    }
    assertEquals(
        List.of("FERMENTED_SPIDER_EYE"), BrewCatalog.byId("weakness").orElseThrow().ingredients());
    assertEquals(
        List.of("FERMENTED_SPIDER_EYE", "REDSTONE"),
        BrewCatalog.byId("long_weakness").orElseThrow().ingredients());
  }

  @Test
  void specialRecipesUseActualMaterialsAndCorruptionPaths() {
    assertPath("wind_charged", "NETHER_WART", "BREEZE_ROD");
    assertPath("weaving", "NETHER_WART", "COBWEB");
    assertPath("oozing", "NETHER_WART", "SLIME_BLOCK");
    assertPath("infested", "NETHER_WART", "STONE");
    assertPath("turtle_master", "NETHER_WART", "TURTLE_HELMET");
    assertPath("strong_slowness", "NETHER_WART", "SUGAR", "FERMENTED_SPIDER_EYE", "GLOWSTONE_DUST");
    assertPath(
        "strong_harming",
        "NETHER_WART",
        "GLISTERING_MELON_SLICE",
        "FERMENTED_SPIDER_EYE",
        "GLOWSTONE_DUST");
  }

  @Test
  void allKindsUseOneIngredientPerBatchAndWaterPerBottle() {
    for (BrewRecipe recipe : BrewCatalog.all()) {
      for (BottleKind kind : BottleKind.values()) {
        var single = recipe.cost(1, kind);
        for (int amount = 1; amount <= 3; amount++) {
          var cost = recipe.cost(amount, kind);
          assertEquals(amount, cost.get("WATER_BOTTLE"));
          single.forEach(
              (key, count) -> {
                if (!key.equals("WATER_BOTTLE")) {
                  assertEquals(count, cost.get(key));
                }
              });
        }
        assertEquals(kind != BottleKind.NORMAL, single.containsKey("GUNPOWDER"));
        assertEquals(kind == BottleKind.LINGERING, single.containsKey("DRAGON_BREATH"));
      }
    }
  }

  @Test
  void recipeCollectionsAreImmutableAndInputsAreValidated() {
    var recipe = BrewCatalog.byId("strength").orElseThrow();
    assertThrows(UnsupportedOperationException.class, () -> BrewCatalog.all().clear());
    assertThrows(UnsupportedOperationException.class, () -> recipe.ingredients().clear());
    assertThrows(UnsupportedOperationException.class, () -> recipe.cost(1).clear());
    assertThrows(IllegalArgumentException.class, () -> recipe.cost(0));
    assertThrows(IllegalArgumentException.class, () -> recipe.cost(4));
    assertThrows(NullPointerException.class, () -> recipe.cost(1, null));
    assertThrows(IllegalArgumentException.class, () -> recipe.cost(1, BottleKind.NORMAL, -1));
    assertThrows(IllegalArgumentException.class, () -> recipe.cost(1, BottleKind.NORMAL, 20));
  }

  private static List<String> ids(BrewCategory category) {
    return BrewCatalog.byCategory(category).stream().map(BrewRecipe::id).toList();
  }

  private static void assertPath(String id, String... path) {
    assertEquals(List.of(path), BrewCatalog.byId(id).orElseThrow().ingredients());
  }
}
