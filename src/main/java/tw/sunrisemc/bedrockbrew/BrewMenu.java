package tw.sunrisemc.bedrockbrew;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Level;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.IllegalPluginAccessException;
import org.bukkit.potion.PotionType;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.cumulus.form.Form;
import org.geysermc.cumulus.form.ModalForm;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.cumulus.form.util.FormBuilder;
import org.geysermc.cumulus.response.FormResponse;
import org.geysermc.floodgate.api.FloodgateApi;
import tw.sunrisemc.bedrockbrew.FormSessions.Session;
import tw.sunrisemc.bedrockbrew.FormSessions.Ticket;
import tw.sunrisemc.bedrockbrew.core.BottleKind;
import tw.sunrisemc.bedrockbrew.core.BrewCatalog;
import tw.sunrisemc.bedrockbrew.core.BrewCategory;
import tw.sunrisemc.bedrockbrew.core.BrewRecipe;
import tw.sunrisemc.bedrockbrew.core.BrewTransaction;

final class BrewMenu {
  private final BedrockBrewPlugin plugin;
  private final FloodgateApi floodgate;
  private final FormSessions sessions = new FormSessions();
  private final NamespacedKey fuelKey;

  BrewMenu(BedrockBrewPlugin plugin, FloodgateApi floodgate) {
    this.plugin = plugin;
    this.floodgate = floodgate;
    fuelKey = new NamespacedKey(plugin, "fuel_operations");
  }

  void openHome(Player player) {
    try {
      player
          .getScheduler()
          .run(
              plugin,
              task -> {
                if (!player.isOnline()
                    || !player.hasPermission("bedrockbrew.use")
                    || !floodgate.isFloodgatePlayer(player.getUniqueId())) {
                  return;
                }
                Session session = sessions.begin(player.getUniqueId());
                if (session != null) {
                  safely(player, session, () -> home(player, session));
                }
              },
              null);
    } catch (IllegalPluginAccessException disabled) {
      // Command or interaction delivery can race plugin shutdown.
    }
  }

  void forget(UUID playerId) {
    sessions.end(playerId);
  }

  void close() {
    sessions.clear();
  }

  private void home(Player player, Session session) {
    var form =
        SimpleForm.builder()
            .title("§l快速釀造")
            .content("原版材料，一次 1～3 瓶。\n每份材料可釀三瓶；烈焰粉供應 20 道工序。")
            .button("§a增益\n迅捷、抗火、跳躍、龜仙人")
            .button("§b探索\n夜視、隱形、水下呼吸、緩降")
            .button("§c戰鬥\n治療、回復、力量、蓄風")
            .button("§5負面\n傷害、劇毒、緩慢、虛弱等");
    send(
        player,
        session,
        form,
        response -> category(player, session, BrewCategory.values()[response.clickedButtonId()]),
        () -> sessions.end(session));
  }

  private void category(Player player, Session session, BrewCategory category) {
    List<BrewRecipe> recipes = BrewCatalog.byCategory(category);
    // Four effects per page, including the seven negative effects.
    categoryPage(player, session, category, recipes, 0);
  }

  private void categoryPage(
      Player player, Session session, BrewCategory category, List<BrewRecipe> recipes, int page) {
    int start = page * 4;
    int end = Math.min(start + 4, recipes.size());
    var form = SimpleForm.builder().title("§l選擇效果").content("選擇效果，再選普通／延長／強化。");
    for (BrewRecipe recipe : recipes.subList(start, end)) {
      form.button(recipe.name());
    }
    boolean hasNext = end < recipes.size();
    if (hasNext) {
      form.button("下一頁 ›");
    }
    form.button(page == 0 ? "‹ 返回分類" : "‹ 上一頁");
    Runnable back =
        () -> {
          if (page == 0) {
            home(player, session);
          } else {
            categoryPage(player, session, category, recipes, page - 1);
          }
        };
    send(
        player,
        session,
        form,
        response -> {
          int index = response.clickedButtonId();
          if (index < end - start) {
            variant(player, session, recipes.get(start + index));
          } else if (hasNext && index == end - start) {
            categoryPage(player, session, category, recipes, page + 1);
          } else {
            back.run();
          }
        },
        back);
  }

  private void variant(Player player, Session session, BrewRecipe selected) {
    BrewRecipe base = BrewCatalog.byId(selected.baseId()).orElseThrow();
    List<BrewRecipe> variants = BrewCatalog.variants(base);
    var form = SimpleForm.builder().title("§l" + base.name()).content("選擇藥水版本");
    variants.forEach(recipe -> form.button(recipe.name()));
    form.button("‹ 返回效果");
    Runnable back = () -> category(player, session, base.category());
    send(
        player,
        session,
        form,
        response -> {
          int index = response.clickedButtonId();
          if (index == variants.size()) {
            back.run();
          } else {
            options(player, session, variants.get(index), BottleKind.NORMAL, 1);
          }
        },
        back);
  }

  private void options(
      Player player, Session session, BrewRecipe recipe, BottleKind kind, int amount) {
    var form =
        CustomForm.builder()
            .title("§l" + recipe.name())
            .dropdown("瓶子類型", kind.ordinal(), "普通藥水", "飛濺藥水", "滯留藥水")
            .stepSlider("數量", amount - 1, "1 瓶", "2 瓶", "3 瓶")
            .dropdown("操作", "下一步", "‹ 返回版本")
            .label("下一頁確認完整材料。\n只使用背包內未改造的普通水瓶。");
    Runnable back = () -> variant(player, session, recipe);
    send(
        player,
        session,
        form,
        response -> {
          int kindIndex = response.asDropdown(0);
          int countIndex = response.asStepSlider(1);
          int action = response.asDropdown(2);
          if (kindIndex < 0
              || kindIndex >= BottleKind.values().length
              || countIndex < 0
              || countIndex > 2
              || action < 0
              || action > 1) {
            throw new IllegalArgumentException("Invalid brewing options");
          }
          if (action == 1) {
            back.run();
          } else {
            confirm(player, session, recipe, BottleKind.values()[kindIndex], countIndex + 1);
          }
        },
        back);
  }

  private void confirm(
      Player player, Session session, BrewRecipe recipe, BottleKind kind, int amount) {
    String content =
        recipe.name()
            + " × "
            + amount
            + "\n"
            + BrewText.kind(kind)
            + "\n\n將消耗：\n"
            + BrewText.cost(recipe.cost(amount, kind, fuel(player)))
            + (kind == BottleKind.LINGERING ? "\n返還：玻璃瓶 × 1" : "");
    var form =
        ModalForm.builder().title("§l確認釀造").content(content).button1("§a確認釀造").button2("‹ 返回修改");
    Runnable back = () -> options(player, session, recipe, kind, amount);
    send(
        player,
        session,
        form,
        response -> {
          if (response.clickedButtonId() == 0) {
            execute(player, session, recipe, kind, amount);
          } else {
            back.run();
          }
        },
        back);
  }

  private void execute(
      Player player, Session session, BrewRecipe recipe, BottleKind kind, int amount) {
    // This method is only reached by dispatch() on the player's entity scheduler.
    var inventory = player.getInventory();
    ItemStack[] snapshot = BrewInventory.copy(inventory.getStorageContents());
    var water = BrewInventory.plainWater(potion(PotionType.WATER, BottleKind.NORMAL));
    var plan =
        BrewTransaction.plan(
            recipe, kind, amount, BrewInventory.count(snapshot, water), fuel(player));
    if (!plan.success()) {
      var form =
          ModalForm.builder()
              .title("§c材料不足")
              .content("沒有扣除物品。\n還缺：\n" + BrewText.cost(plan.missing()))
              .button1("重新檢查")
              .button2("‹ 返回修改");
      send(
          player,
          session,
          form,
          response -> {
            if (response.clickedButtonId() == 0) {
              confirm(player, session, recipe, kind, amount);
            } else {
              options(player, session, recipe, kind, amount);
            }
          },
          () -> options(player, session, recipe, kind, amount));
      return;
    }
    List<ItemStack> outputs = new ArrayList<>();
    for (int bottle = 0; bottle < plan.outputs(); bottle++) {
      outputs.add(potion(recipe.potionType(), kind));
    }
    if (kind == BottleKind.LINGERING) {
      outputs.add(new ItemStack(Material.GLASS_BOTTLE));
    }
    ItemStack[] replacement =
        BrewInventory.prepare(snapshot, plan.remove(), outputs, water, inventory.getMaxStackSize());
    if (replacement == null) {
      notice(
          player,
          session,
          "§c背包空間不足",
          "沒有扣除物品。\n請騰出空位（滯留藥水會返還玻璃瓶）。",
          () -> confirm(player, session, recipe, kind, amount));
      return;
    }
    if (!BrewInventory.commit(inventory, snapshot, replacement)) {
      notice(
          player,
          session,
          "§e材料已變動",
          "沒有扣除物品，請重新確認。",
          () -> confirm(player, session, recipe, kind, amount));
      return;
    }
    player
        .getPersistentDataContainer()
        .set(fuelKey, PersistentDataType.INTEGER, plan.fuelRemaining());
    player.playSound(player, Sound.BLOCK_BREWING_STAND_BREW, 1f, 1f);
    plugin
        .getLogger()
        .info("[BREW] " + player.getName() + " -> " + recipe.id() + " " + kind + " x" + amount);
    notice(
        player,
        session,
        "§a釀造完成",
        recipe.name() + " × " + amount + "\n" + BrewText.kind(kind),
        () -> home(player, session));
  }

  private void notice(Player player, Session session, String title, String text, Runnable next) {
    var form = ModalForm.builder().title(title).content(text).button1("繼續").button2("關閉");
    send(
        player,
        session,
        form,
        response -> {
          if (response.clickedButtonId() == 0) {
            next.run();
          } else {
            sessions.end(session);
          }
        },
        () -> sessions.end(session));
  }

  private int fuel(Player player) {
    int credits =
        player.getPersistentDataContainer().getOrDefault(fuelKey, PersistentDataType.INTEGER, 0);
    return credits >= 0 && credits <= 19 ? credits : 0;
  }

  private static ItemStack potion(PotionType type, BottleKind kind) {
    Material material =
        switch (kind) {
          case NORMAL -> Material.POTION;
          case SPLASH -> Material.SPLASH_POTION;
          case LINGERING -> Material.LINGERING_POTION;
        };
    ItemStack item = new ItemStack(material);
    PotionMeta meta = (PotionMeta) item.getItemMeta();
    meta.setBasePotionType(type);
    item.setItemMeta(meta);
    return item;
  }

  private <B extends FormBuilder<B, F, R>, F extends Form, R extends FormResponse> void send(
      Player player,
      Session session,
      FormBuilder<B, F, R> form,
      Consumer<R> handler,
      Runnable back) {
    Ticket ticket = session.nextPage();
    form.validResultHandler(
        response -> dispatch(player, session, ticket, () -> handler.accept(response)));
    form.closedResultHandler(() -> dispatch(player, session, ticket, back));
    form.invalidResultHandler(
        () ->
            dispatch(
                player,
                session,
                ticket,
                () -> {
                  player.sendMessage("§c表單回應無效，請重新開啟 /brew。");
                  sessions.end(session);
                }));
    if (!player.isOnline() || !floodgate.sendForm(session.playerId, form.build())) {
      sessions.end(session);
    }
  }

  private void dispatch(Player player, Session session, Ticket ticket, Runnable action) {
    // Claim before queueing. Even a replay after completion cannot brew a second time.
    if (!sessions.isActive(session) || !ticket.claim()) {
      return;
    }
    try {
      var task =
          player
              .getScheduler()
              .run(
                  plugin,
                  scheduled -> {
                    if (!sessions.isActive(session) || !session.isCurrent(ticket)) {
                      return;
                    }
                    if (!player.isOnline()
                        || !player.hasPermission("bedrockbrew.use")
                        || !floodgate.isFloodgatePlayer(session.playerId)) {
                      sessions.end(session);
                      return;
                    }
                    safely(player, session, action);
                  },
                  () -> sessions.end(session));
      if (task == null) {
        sessions.end(session);
      }
    } catch (IllegalPluginAccessException disabled) {
      // A late network response may race plugin shutdown. No Bukkit calls from this thread.
      sessions.end(session);
    }
  }

  private void safely(Player player, Session session, Runnable action) {
    try {
      action.run();
    } catch (RuntimeException exception) {
      sessions.end(session);
      plugin
          .getLogger()
          .log(Level.SEVERE, "Brewing form failed for " + session.playerId, exception);
      player.sendMessage("§c釀造介面發生錯誤，請重新開啟 /brew。");
    }
  }
}
