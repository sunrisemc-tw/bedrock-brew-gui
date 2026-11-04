package tw.sunrisemc.bedrockbrew;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import tw.sunrisemc.bedrockbrew.core.BottleKind;
import tw.sunrisemc.bedrockbrew.core.BrewCatalog;

class BrewTextTest {
  @Test
  void everyCostAndContainerHasTraditionalChineseText() {
    BrewCatalog.all()
        .forEach(
            recipe -> {
              for (BottleKind kind : BottleKind.values()) {
                recipe
                    .cost(3, kind)
                    .keySet()
                    .forEach(
                        key ->
                            assertTrue(
                                BrewText.material(key)
                                    .codePoints()
                                    .anyMatch(character -> character > 127)));
                assertFalse(BrewText.cost(recipe.cost(3, kind)).isBlank());
                assertFalse(BrewText.kind(kind).isBlank());
              }
            });
    assertEquals("玻璃瓶", BrewText.material("GLASS_BOTTLE"));
  }
}
