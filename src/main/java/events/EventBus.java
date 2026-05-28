package events;

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
    }
}
