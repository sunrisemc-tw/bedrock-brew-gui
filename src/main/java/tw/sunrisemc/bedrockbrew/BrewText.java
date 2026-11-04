package tw.sunrisemc.bedrockbrew;

import java.util.Map;
import java.util.stream.Collectors;
import tw.sunrisemc.bedrockbrew.core.BottleKind;

final class BrewText {
  private BrewText() {}

  static String cost(Map<String, Integer> cost) {
    return cost.entrySet().stream()
        .map(entry -> "• " + material(entry.getKey()) + " × " + entry.getValue())
        .collect(Collectors.joining("\n"));
  }

  static String material(String key) {
    return switch (key) {
      case "WATER_BOTTLE" -> "水瓶";
      case "NETHER_WART" -> "地獄疙瘩";
      case "REDSTONE" -> "紅石粉";
      case "GLOWSTONE_DUST" -> "螢石粉";
      case "GUNPOWDER" -> "火藥";
      case "DRAGON_BREATH" -> "龍之吐息";
      case "GOLDEN_CARROT" -> "金胡蘿蔔";
      case "FERMENTED_SPIDER_EYE" -> "發酵蜘蛛眼";
      case "RABBIT_FOOT" -> "兔子腳";
      case "MAGMA_CREAM" -> "岩漿膏";
      case "SUGAR" -> "糖";
      case "TURTLE_HELMET" -> "海龜殼";
      case "PUFFERFISH" -> "河豚";
      case "GLISTERING_MELON_SLICE" -> "閃爍西瓜片";
      case "SPIDER_EYE" -> "蜘蛛眼";
      case "GHAST_TEAR" -> "惡魂之淚";
      case "BLAZE_POWDER" -> "烈焰粉";
      case "PHANTOM_MEMBRANE" -> "幻翼膜";
      case "BREEZE_ROD" -> "旋風棒";
      case "COBWEB" -> "蜘蛛網";
      case "SLIME_BLOCK" -> "史萊姆方塊";
      case "STONE" -> "石頭";
      case "GLASS_BOTTLE" -> "玻璃瓶";
      default -> throw new IllegalArgumentException("Untranslated brewing material: " + key);
    };
  }

  static String kind(BottleKind kind) {
    return switch (kind) {
      case NORMAL -> "普通藥水";
      case SPLASH -> "飛濺藥水";
      case LINGERING -> "滯留藥水";
    };
  }
}
