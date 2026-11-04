package tw.sunrisemc.bedrockbrew;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;

class FormSessionsTest {
  @Test
  void onlyOneSessionPerPlayerAndOldSessionCannotRemoveNewOne() {
    var sessions = new FormSessions();
    UUID playerId = UUID.randomUUID();
    var first = sessions.begin(playerId);
    assertNotNull(first);
    assertNull(sessions.begin(playerId));
    sessions.end(first);
    var next = sessions.begin(playerId);
    sessions.end(first);
    assertTrue(sessions.isActive(next));
    sessions.end(playerId);
    assertFalse(sessions.isActive(next));
  }

  @Test
  void ticketCanOnlyBeUsedOnceEvenAfterAnotherPageOpens() {
    var session = new FormSessions.Session(UUID.randomUUID());
    var ticket = session.nextPage();
    assertTrue(session.isCurrent(ticket));
    assertTrue(ticket.claim());
    session.nextPage();
    assertFalse(ticket.claim());
    assertFalse(session.isCurrent(ticket));
  }

  @Test
  void simultaneousForeignThreadCallbacksHaveExactlyOneWinner() throws Exception {
    var ticket = new FormSessions.Ticket();
    try (var executor = Executors.newFixedThreadPool(8)) {
      var attempts = new ArrayList<Callable<Boolean>>();
      for (int index = 0; index < 1000; index++) {
        attempts.add(ticket::claim);
      }
      long winners = 0;
      for (var future : executor.invokeAll(attempts)) {
        if (future.get()) {
          winners++;
        }
      }
      assertEquals(1, winners);
    }
    assertFalse(ticket.claim());
  }

  @Test
  void clearingSessionsInvalidatesQueuedWork() {
    var sessions = new FormSessions();
    var first = sessions.begin(UUID.randomUUID());
    var second = sessions.begin(UUID.randomUUID());
    sessions.clear();
    assertFalse(sessions.isActive(first));
    assertFalse(sessions.isActive(second));
  }
}
