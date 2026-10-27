package mizzen.events;

import mizzen.components.Component;

/**
 * Repaints what changed without being asked. Every so often, while the app is idle, it looks at one
 * flag - is the panel dirty? - and if it is, asks for a canvas update:
 * <pre>
 *   a button handler pans the plot     ->  the plot marks itself dirty, and its panels up to the top
 *   next check (at most 100 ms later)  ->  main panel dirty? yes  ->  RequestUpdateCanvasEvent
 *   the canvas update                  ->  redraws only what is dirty
 * </pre>
 * So code that changes the screen - a button handler, a timer, a thread feeding a viewer - no longer
 * raises RequestUpdateCanvasEvent itself: a component's own setters mark it dirty, and that is all
 * it takes.
 * <p/>
 * It is cheap: dirty bubbles up from a component to its parents, so one flag on the panel says
 * whether anything inside changed, and when nothing did, nothing else happens. It still only asks
 * (RequestUpdateCanvasEvent), so bursts of changes are merged into one update as before.
 * <p/>
 * The TUIApp adds one for its main panel. It runs on idle events: an app that turns them off asks
 * for canvas updates itself, as before.
 */
public class RepaintWhenDirty extends OnIdleEventEventHandler.RepeatingInterval {

    /** how often it looks, in milliseconds: about ten times a second */
    public static final long DEFAULT_INTERVAL = 100;

    public RepaintWhenDirty(Component<?> panel) {
        this(panel, DEFAULT_INTERVAL);
    }

    public RepaintWhenDirty(Component<?> panel, long intervalInMilliSeconds) {
        super(intervalInMilliSeconds, (te) -> {
            if (panel.isDirty()) {
                te.getEventBus().raiseEvent(new RequestUpdateCanvasEvent(panel));
            }
        });
    }
}
