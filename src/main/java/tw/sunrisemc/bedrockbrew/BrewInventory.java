package tw.sunrisemc.bedrockbrew;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

/** Only used on the player's entity scheduler. Preparation never touches live slot contents. */
final class BrewInventory {
  private BrewInventory() {}

  static boolean isWater(ItemStack item) {
    return item != null
        && item.getType() == Material.POTION
        && item.getItemMeta() instanceof PotionMeta meta
        && meta.getBasePotionType() == PotionType.WATER
        && !meta.hasCustomEffects();
  }

  static Predicate<ItemStack> plainWater(ItemStack canonicalWater) {
    // Exact metadata comparison also protects named, dyed, enchanted and plugin-tagged bottles.
    return item -> isWater(item) && item.isSimilar(canonicalWater);
  }

  static Map<String, Integer> count(ItemStack[] contents, Predicate<ItemStack> water) {
    Map<String, Integer> counts = new LinkedHashMap<>();
    for (ItemStack item : contents) {
      if (isEmpty(item)) {
        continue;
      }
      String key = water.test(item) ? "WATER_BOTTLE" : item.getType().name();
      counts.merge(key, item.getAmount(), Integer::sum);
    }
    return counts;
  }

  static ItemStack[] copy(ItemStack[] contents) {
    return Arrays.stream(contents)
        .map(item -> item == null ? null : item.clone())
        .toArray(ItemStack[]::new);
  }

  static ItemStack[] prepare(
      ItemStack[] snapshot,
      Map<String, Integer> cost,
      List<ItemStack> outputs,
      Predicate<ItemStack> water,
      int inventoryStackLimit) {
    Map<String, Integer> counts = count(snapshot, water);
    if (cost.entrySet().stream()
        .anyMatch(entry -> counts.getOrDefault(entry.getKey(), 0) < entry.getValue())) {
      return null;
    }
    ItemStack[] result = copy(snapshot);
    for (var entry : cost.entrySet()) {
      int left = entry.getValue();
      for (int slot = 0; slot < result.length && left > 0; slot++) {
        ItemStack item = result[slot];
        if (item == null) {
          continue;
        }
        boolean matches =
            entry.getKey().equals("WATER_BOTTLE")
                ? water.test(item)
                : item.getType().name().equals(entry.getKey());
        if (!matches) {
          continue;
        }
        int take = Math.min(left, item.getAmount());
        left -= take;
        if (take == item.getAmount()) {
          result[slot] = null;
        } else {
          item.setAmount(item.getAmount() - take);
        }
      }
    }
    for (ItemStack output : outputs) {
      int left = output.getAmount();
      int limit = Math.min(output.getMaxStackSize(), inventoryStackLimit);
      for (ItemStack item : result) {
        if (item == null || !item.isSimilar(output)) {
          continue;
        }
        int add = Math.min(left, Math.max(0, limit - item.getAmount()));
        item.setAmount(item.getAmount() + add);
        left -= add;
      }
      for (int slot = 0; slot < result.length && left > 0; slot++) {
        if (!isEmpty(result[slot])) {
          continue;
        }
        ItemStack item = output.clone();
        int add = Math.min(left, limit);
        item.setAmount(add);
        result[slot] = item;
        left -= add;
      }
      if (left > 0) {
        // No world drops: even a cancelled ItemSpawnEvent must never lose an output.
        return null;
      }
    }
    return result;
  }

  private static boolean isEmpty(ItemStack item) {
    return item == null
        || item.getAmount() <= 0
        || item.getType() == Material.AIR
        || item.getType() == Material.CAVE_AIR
        || item.getType() == Material.VOID_AIR;
  }

  static boolean commit(PlayerInventory inventory, ItemStack[] snapshot, ItemStack[] replacement) {
    if (!Arrays.equals(snapshot, inventory.getStorageContents())) {
      return false;
    }
    inventory.setStorageContents(replacement);
    return true;
  }
}
