package dtb.terminalIO.events;

@FunctionalInterface
public interface EventHandler {
    void handle(TerminalEvent<?> te);

    public static EventHandler NULL_HANDLER = new EventHandler() {
        @Override
        public void handle(TerminalEvent<?> te) {
        }
    };
    
}
