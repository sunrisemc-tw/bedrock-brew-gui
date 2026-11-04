package tw.sunrisemc.bedrockbrew;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.papermc.paper.threadedregions.scheduler.EntityScheduler;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.logging.Logger;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.IllegalPluginAccessException;
import org.geysermc.cumulus.component.ButtonComponent;
import org.geysermc.cumulus.component.DropdownComponent;
import org.geysermc.cumulus.component.StepSliderComponent;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.cumulus.form.Form;
import org.geysermc.cumulus.form.ModalForm;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.cumulus.form.impl.FormDefinitions;
import org.geysermc.floodgate.api.FloodgateApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BrewMenuTest {
  private final BedrockBrewPlugin plugin = mock(BedrockBrewPlugin.class);
  private final FloodgateApi floodgate = mock(FloodgateApi.class);
  private final Player player = mock(Player.class);
  private final EntityScheduler scheduler = mock(EntityScheduler.class);
  private final PersistentDataContainer data = mock(PersistentDataContainer.class);
  private final UUID playerId = UUID.randomUUID();
  private final Queue<Consumer<ScheduledTask>> queued = new ArrayDeque<>();
  private final List<Form> forms = new ArrayList<>();
  private BrewMenu menu;

  @BeforeEach
  void setUp() {
    when(plugin.getName()).thenReturn("BedrockBrewGUI");
    when(plugin.namespace()).thenReturn("bedrockbrewgui");
    when(plugin.getLogger()).thenReturn(Logger.getLogger("BrewMenuTest"));
    when(player.getUniqueId()).thenReturn(playerId);
    when(player.isOnline()).thenReturn(true);
    when(player.hasPermission("bedrockbrew.use")).thenReturn(true);
    when(player.getScheduler()).thenReturn(scheduler);
    when(player.getPersistentDataContainer()).thenReturn(data);
    when(data.getOrDefault(any(), eq(PersistentDataType.INTEGER), eq(0))).thenReturn(0);
    when(floodgate.isFloodgatePlayer(playerId)).thenReturn(true);
    when(scheduler.run(eq(plugin), any(), any()))
        .thenAnswer(
            invocation -> {
              queued.add(invocation.getArgument(1));
              return mock(ScheduledTask.class);
            });
    when(floodgate.sendForm(eq(playerId), any(Form.class)))
        .thenAnswer(
            invocation -> {
              forms.add(invocation.getArgument(1));
              return true;
            });
    menu = new BrewMenu(plugin, floodgate);
  }

  @Test
  void openingAndForeignThreadCallbacksOnlyRunPlayerWorkOnEntityScheduler() throws Exception {
    menu.openHome(player);
    assertTrue(forms.isEmpty());
    drain();
    assertEquals(1, forms.size());
    clearInvocations(player, floodgate);
    try (var worker = Executors.newSingleThreadExecutor()) {
      worker
          .submit(
              () -> {
                respond(last(), "0");
                return null;
              })
          .get();
    }
    assertEquals(1, forms.size());
    verify(player).getScheduler();
    verifyNoMoreInteractions(player);
    verifyNoInteractions(floodgate);
    drain();
    assertEquals(2, forms.size());
  }

  @Test
  void negativeEffectsArePaginatedWithWorkingBackButtons() throws Exception {
    open();
    choose("3");
    var first = assertInstanceOf(SimpleForm.class, last());
    assertEquals(6, first.buttons().size());
    assertEquals("下一頁 ›", first.buttons().get(4).text());
    choose("4");
    var second = assertInstanceOf(SimpleForm.class, last());
    assertEquals(4, second.buttons().size());
    assertEquals("織網", second.buttons().getFirst().text());
    choose("3");
    assertEquals(
        first.buttons().stream().map(ButtonComponent::text).toList(),
        assertInstanceOf(SimpleForm.class, last()).buttons().stream()
            .map(ButtonComponent::text)
            .toList());
    choose("5");
    assertEquals("§l快速釀造", last().title());
  }

  @Test
  void actualCumulusOptionsIndicesAndConfirmationBackPreserveSelection() throws Exception {
    open();
    choose("0");
    choose("0");
    choose("0");
    assertInstanceOf(CustomForm.class, last());
    choose("[2,2,0,null]");
    var confirmation = assertInstanceOf(ModalForm.class, last());
    assertTrue(confirmation.content().contains("跳躍 × 3"));
    assertTrue(confirmation.content().contains("滯留藥水"));
    assertTrue(confirmation.content().contains("龍之吐息 × 1"));
    choose("false");
    var options = assertInstanceOf(CustomForm.class, last());
    assertEquals(2, ((DropdownComponent) options.content().get(0)).defaultOption());
    assertEquals(2, ((StepSliderComponent) options.content().get(1)).defaultStep());
    choose("[2,2,1,null]");
    assertInstanceOf(SimpleForm.class, last());
    assertEquals("§l跳躍", last().title());
  }

  @Test
  void replayedFormCannotScheduleTwiceEvenAfterNextPage() throws Exception {
    open();
    Form home = last();
    respond(home, "0");
    respond(home, "0");
    assertEquals(1, queued.size());
    drain();
    int count = forms.size();
    respond(home, "0");
    assertTrue(queued.isEmpty());
    assertEquals(count, forms.size());
  }

  @Test
  void duplicateEntryIsIgnoredAndClosingHomeAllowsNewSession() throws Exception {
    open();
    menu.openHome(player);
    drain();
    assertEquals(1, forms.size());
    choose("null");
    menu.openHome(player);
    drain();
    assertEquals(2, forms.size());
  }

  @Test
  void quitInvalidatesQueuedResponseAndOldCallbacksCannotAffectNewSession() throws Exception {
    open();
    Form old = last();
    respond(old, "0");
    menu.forget(playerId);
    drain();
    assertEquals(1, forms.size());
    open();
    respond(old, "0");
    drain();
    assertEquals(2, forms.size());
  }

  @Test
  void refusedSchedulerReleasesSessionWithoutForeignThreadPlayerMutations() throws Exception {
    open();
    when(scheduler.run(eq(plugin), any(), any())).thenReturn(null);
    clearInvocations(player);
    respond(last(), "0");
    verify(player).getScheduler();
    verifyNoMoreInteractions(player);
    when(scheduler.run(eq(plugin), any(), any()))
        .thenAnswer(
            invocation -> {
              queued.add(invocation.getArgument(1));
              return mock(ScheduledTask.class);
            });
    open();
    assertEquals(2, forms.size());
  }

  @Test
  void retiredCallbackReleasesSession() throws Exception {
    open();
    when(scheduler.run(eq(plugin), any(), any()))
        .thenAnswer(
            invocation -> {
              Runnable retired = invocation.getArgument(2);
              retired.run();
              return mock(ScheduledTask.class);
            });
    respond(last(), "0");
    when(scheduler.run(eq(plugin), any(), any()))
        .thenAnswer(
            invocation -> {
              queued.add(invocation.getArgument(1));
              return mock(ScheduledTask.class);
            });
    open();
    assertEquals(2, forms.size());
  }

  @Test
  void failedSendDoesNotStrandSession() {
    when(floodgate.sendForm(eq(playerId), any(Form.class))).thenReturn(false);
    open();
    when(floodgate.sendForm(eq(playerId), any(Form.class)))
        .thenAnswer(
            invocation -> {
              forms.add(invocation.getArgument(1));
              return true;
            });
    open();
    assertEquals(1, forms.size());
  }

  @Test
  void pluginDisableRaceDoesNotThrowFromForeignCallback() throws Exception {
    open();
    when(scheduler.run(eq(plugin), any(), any()))
        .thenThrow(new IllegalPluginAccessException("Plugin is disabled"));
    assertDoesNotThrow(() -> respond(last(), "0"));
    verify(player, never()).getInventory();
  }

  @Test
  void malformedResponseEndsSessionWithoutInventoryAccess() throws Exception {
    open();
    choose("1000");
    verify(player, never()).getInventory();
    open();
    assertEquals(2, forms.size());
  }

  @Test
  void permissionAndOnlineStatusAreRecheckedBeforeCallback() throws Exception {
    open();
    respond(last(), "0");
    when(player.hasPermission("bedrockbrew.use")).thenReturn(false);
    drain();
    assertEquals(1, forms.size());
    verify(player, never()).getInventory();
    when(player.hasPermission("bedrockbrew.use")).thenReturn(true);
    when(player.isOnline()).thenReturn(false);
    open();
    assertEquals(1, forms.size());
  }

  @Test
  void missingPageShowsExactCostsAndConfirmationReplayCannotRecheckAgain() throws Exception {
    var inventory = mock(PlayerInventory.class);
    when(player.getInventory()).thenReturn(inventory);
    when(inventory.getStorageContents()).thenReturn(new ItemStack[36]);
    open();
    choose("0");
    choose("0");
    choose("0");
    choose("[0,2,0,null]");
    Form confirmation = last();
    try (var construction =
        mockConstruction(
            ItemStack.class,
            (item, context) -> when(item.getItemMeta()).thenReturn(mock(PotionMeta.class)))) {
      choose("true");
      assertEquals(1, construction.constructed().size()); // canonical water only; no outputs
    }
    var missing = assertInstanceOf(ModalForm.class, last());
    assertEquals("§c材料不足", missing.title());
    assertTrue(missing.content().contains("水瓶 × 3"));
    assertTrue(missing.content().contains("地獄疙瘩 × 1"));
    assertTrue(missing.content().contains("兔子腳 × 1"));
    assertTrue(missing.content().contains("烈焰粉 × 1"));
    respond(confirmation, "true");
    assertTrue(queued.isEmpty());
    verify(inventory, never()).setStorageContents(any());
    verify(data, never()).set(any(), any(), any());
    choose("false");
    assertInstanceOf(CustomForm.class, last());
  }

  private void open() {
    menu.openHome(player);
    drain();
  }

  private Form last() {
    return forms.getLast();
  }

  private void choose(String response) throws Exception {
    respond(last(), response);
    drain();
  }

  private static void respond(Form form, String response) throws Exception {
    FormDefinitions.instance().definitionFor(form).handleFormResponse(form, response);
  }

  private void drain() {
    while (!queued.isEmpty()) {
      queued.remove().accept(mock(ScheduledTask.class));
    }
  }
}
