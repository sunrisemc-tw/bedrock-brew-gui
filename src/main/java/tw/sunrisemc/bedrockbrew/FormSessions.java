package tw.sunrisemc.bedrockbrew;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/** A session is exclusive per player; every page response is a single-use capability. */
final class FormSessions {
  private final Map<UUID, Session> active = new ConcurrentHashMap<>();

  Session begin(UUID playerId) {
    Session session = new Session(playerId);
    return active.putIfAbsent(playerId, session) == null ? session : null;
  }

  boolean isActive(Session session) {
    return active.get(session.playerId) == session;
  }

  void end(Session session) {
    active.remove(session.playerId, session);
  }

  void end(UUID playerId) {
    active.remove(playerId);
  }

  void clear() {
    active.clear();
  }

  static final class Session {
    final UUID playerId;
    private Ticket current;

    Session(UUID playerId) {
      this.playerId = playerId;
    }

    Ticket nextPage() {
      current = new Ticket();
      return current;
    }

    boolean isCurrent(Ticket ticket) {
      return current == ticket;
    }
  }

  static final class Ticket {
    private final AtomicBoolean used = new AtomicBoolean();

    boolean claim() {
      return used.compareAndSet(false, true);
    }
  }
}
