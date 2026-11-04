package tw.sunrisemc.bedrockbrew;

import java.util.Objects;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.geysermc.floodgate.api.FloodgateApi;

public final class BedrockBrewPlugin extends JavaPlugin implements CommandExecutor {
  private BrewMenu menu;
  private FloodgateApi floodgate;

  @Override
  public void onEnable() {
    floodgate = FloodgateApi.getInstance();
    menu = new BrewMenu(this, floodgate);
    Objects.requireNonNull(getCommand("brew"), "Missing brew command in plugin.yml")
        .setExecutor(this);
    getServer().getPluginManager().registerEvents(new BrewingStandListener(menu, floodgate), this);
    getLogger().info("BedrockBrewGUI 已啟用：/brew 與基岩版釀造台攔截已註冊");
  }

  @Override
  public void onDisable() {
    if (menu != null) {
      menu.close();
    }
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!(sender instanceof Player player)) {
      sender.sendMessage("此指令只能由玩家使用。");
      return true;
    }
    if (!player.hasPermission("bedrockbrew.use")) {
      player.sendMessage("§c你沒有使用此介面的權限。");
      return true;
    }
    if (!floodgate.isFloodgatePlayer(player.getUniqueId())) {
      player.sendMessage("§e這個介面只提供給基岩版玩家；Java 版請使用原版釀造台。");
      return true;
    }
    menu.openHome(player);
    return true;
  }
}
