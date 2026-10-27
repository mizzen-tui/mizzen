package mizzen.events;

/**
 * Handles (processes) {@link TerminalEvent}'s
 * 
 * @author david
 *
 */
@FunctionalInterface
public interface EventHandler {
    
    void handle(TerminalEvent<?> te);

    public static final EventHandler NULL_HANDLER = new EventHandler() {
        @Override
        public void handle(TerminalEvent<?> te) {
        }
    };
 
    
}
