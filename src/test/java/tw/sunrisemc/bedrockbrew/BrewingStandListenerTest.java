package tw.sunrisemc.bedrockbrew;

import static org.mockito.Mockito.*;

import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.geysermc.floodgate.api.FloodgateApi;
import org.junit.jupiter.api.Test;

class BrewingStandListenerTest {
  @Test
  void onlyBedrockRightClickMainHandOpensAndOffHandIsDeniedWithoutAnotherForm() {
    var menu = mock(BrewMenu.class);
    var floodgate = mock(FloodgateApi.class);
    var event = mock(PlayerInteractEvent.class);
    var player = mock(Player.class);
    var stand = mock(Block.class);
    UUID id = UUID.randomUUID();
    when(event.getPlayer()).thenReturn(player);
    when(player.getUniqueId()).thenReturn(id);
    when(player.hasPermission("bedrockbrew.use")).thenReturn(true);
    when(event.getClickedBlock()).thenReturn(stand);
    when(stand.getType()).thenReturn(Material.BREWING_STAND);
    when(event.getAction()).thenReturn(Action.RIGHT_CLICK_BLOCK);
    when(event.getHand()).thenReturn(EquipmentSlot.HAND);
    var listener = new BrewingStandListener(menu, floodgate);
    listener.onUse(event);
    verifyNoInteractions(menu);
    verify(event, never()).setUseInteractedBlock(any());
    when(floodgate.isFloodgatePlayer(id)).thenReturn(true);
    when(event.getAction()).thenReturn(Action.LEFT_CLICK_BLOCK);
    listener.onUse(event);
    verifyNoInteractions(menu);
    when(event.getAction()).thenReturn(Action.RIGHT_CLICK_BLOCK);
    when(player.hasPermission("bedrockbrew.use")).thenReturn(false);
    listener.onUse(event);
    verifyNoInteractions(menu);
    when(player.hasPermission("bedrockbrew.use")).thenReturn(true);
    listener.onUse(event);
    verify(menu).openHome(player);
    verify(event).setUseInteractedBlock(Event.Result.DENY);
    verify(event).setUseItemInHand(Event.Result.DENY);
    when(event.getHand()).thenReturn(EquipmentSlot.OFF_HAND);
    listener.onUse(event);
    verify(menu, times(1)).openHome(player);
    verify(event, times(2)).setUseInteractedBlock(Event.Result.DENY);
  }
}
