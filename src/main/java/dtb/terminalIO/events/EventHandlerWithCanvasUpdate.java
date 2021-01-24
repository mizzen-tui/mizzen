package dtb.terminalIO.events;

/**
 * A conditional event handler that upon handling an event, will subsequently
 * raise an event to request a canvas update.
 */
public class EventHandlerWithCanvasUpdate extends ConditionalEventHandler.Delegate {

    private Object source;

    public EventHandlerWithCanvasUpdate(Object source, ConditionalEventHandler handler) {
        super(handler);
        this.source = source;
    }

    public EventHandlerWithCanvasUpdate(ConditionalEventHandler handler) {
        this(null, handler);
        this.source = this;
    }

    public void handle(TerminalEvent<?> te) {
      super.handle(te);
      updateCanvas(te.getEventBus());
  }

   private void updateCanvas(EventBus eventBus) {
        eventBus.raiseEvent(new RequestUpdateCanvasEvent(this.source) );
  }

}
