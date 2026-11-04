package tw.sunrisemc.bedrockbrew;

import org.bukkit.Material;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.geysermc.floodgate.api.FloodgateApi;

final class BrewingStandListener implements Listener {
  private final BrewMenu menu;
  private final FloodgateApi floodgate;

  BrewingStandListener(BrewMenu menu, FloodgateApi floodgate) {
    this.menu = menu;
    this.floodgate = floodgate;
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onUse(PlayerInteractEvent event) {
    if (event.getAction() != Action.RIGHT_CLICK_BLOCK
        || event.getClickedBlock() == null
        || event.getClickedBlock().getType() != Material.BREWING_STAND
        || !floodgate.isFloodgatePlayer(event.getPlayer().getUniqueId())
        || !event.getPlayer().hasPermission("bedrockbrew.use")) {
      return;
    }
    // Deny both hands' container interactions; only the main hand may create a session.
    event.setUseInteractedBlock(Event.Result.DENY);
    event.setUseItemInHand(Event.Result.DENY);
    if (event.getHand() == EquipmentSlot.HAND) {
      menu.openHome(event.getPlayer());
    }
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    menu.forget(event.getPlayer().getUniqueId());
  }
}
