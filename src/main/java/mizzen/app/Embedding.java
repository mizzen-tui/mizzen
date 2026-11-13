package mizzen.app;

import mizzen.components.composite.Composite;
import mizzen.geom.Dimension;
import mizzen.geom.Rectangle;

/**
 * A TUIApp running inside another TUIApp (the host), started by TUIApp.runEmbedded. Its main panel
 * sits in a panel of the host's, and it shares the host's event bus and screen. close() takes it out
 * again, with everything it put on the host's bus.
 * <pre>
 *     Embedding demo = TUIApp.runEmbedded(host, rightPane, () -> ButtonBarTest.main(new String[0]));
 *     ...
 *     demo.resize();               // when the pane changes size
 *     demo.close();                // gone: its panel, its key handlers, its timers
 * </pre>
 */
public final class Embedding {

    /** the code that makes and starts the TUIApp - usually an example's main */
    @FunctionalInterface
    public interface Launch {
        void run() throws Exception;
    }

    final TUIApp host;
    final Composite slot;
    private EmbeddedApp app;
    private TUIApp tuiApp;
    private boolean closed;

    Embedding(TUIApp host, Composite slot) {
        this.host = host;
        this.slot = slot;
    }

    // the TUIApp the launch code makes calls this from its constructor
    EmbeddedApp embed(TUIApp owner) {
        if (app != null) {
            throw new IllegalStateException("The code given to runEmbedded made a second TUIApp - one runEmbedded embeds one TUIApp");
        }
        tuiApp = owner;
        app = new EmbeddedApp(owner, this);
        return app;
    }

    // after the launch code ran: it must have made a TUIApp and started it
    void checkStarted() {
        if (app == null) {
            throw new IllegalStateException("The code given to runEmbedded made no TUIApp - nothing to embed");
        }
        if (!app.started) {
            close();
            throw new IllegalStateException("The code given to runEmbedded made a TUIApp but did not start() it");
        }
    }

    /** the embedded app (what its own code calls tApp) */
    public TUIApp tuiApp() {
        return tuiApp;
    }

    /**
     * the panel it runs in has a new size: fit to its inside (inside its border, if it has one). A bordered panel folded
     * to one row (a FlipSplitter's SECOND_ONLY) has an inside -1 high: the app gets no rows, and runs on out of sight.
     */
    public void resize() {
        if (app != null && !closed) {
            Rectangle inside = slot.getInnerRectangle();
            app.resize(new Dimension.Simple(Math.max(0, inside.getHeight()), Math.max(0, inside.getWidth())));
        }
    }

    /** take it out of the host: its panel, and every handler it put on the host's bus. Safe to call again. */
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        if (app != null) {
            app.close();
        }
    }

    public boolean isClosed() {
        return closed;
    }

    /** how many handlers it has on the host's bus - what close() takes off (0 once closed) */
    public int handlersOnHostBus() {
        return app == null ? 0 : app.handlersOnHostBus();
    }
}
