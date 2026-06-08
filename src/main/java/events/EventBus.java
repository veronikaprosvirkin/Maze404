package events;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class EventBus {
    private static final EventBus INSTANCE = new EventBus();
    private final Map<GameEvent.Type, List<Consumer<GameEvent>>> listeners = new EnumMap<>(GameEvent.Type.class);

    private EventBus() {}

    public static EventBus getInstance() { return INSTANCE; }

    public void publish(GameEvent gameEvent) {
        GameEvent.Type type = gameEvent.getType();
        List<Consumer<GameEvent>> eventListeners = listeners.get(type);

        if (eventListeners != null) {
            for (Consumer<GameEvent> listener : eventListeners) {
                listener.accept(gameEvent);
            }
        }
    }

    public void subscribe(GameEvent.Type type, Consumer<GameEvent> listener) {
        listeners.computeIfAbsent(type, k -> new ArrayList<>());
        listeners.get(type).add(listener);
    }

    public void unsubscribe(GameEvent.Type type, Consumer<GameEvent> listener) {
        List<Consumer<GameEvent>> eventListeners = listeners.get(type);
        if (eventListeners != null) {
            eventListeners.remove(listener);
        }
    }
}
