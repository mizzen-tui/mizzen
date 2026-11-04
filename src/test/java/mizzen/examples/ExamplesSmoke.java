package mizzen.examples;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import mizzen.app.BusOnlyHost;
import mizzen.app.Embedding;
import mizzen.app.TUIApp;
import mizzen.components.composite.Composite;
import mizzen.components.input.tree.DefaultTreeNode;
import mizzen.events.ConditionalEventHandler;
import mizzen.events.EventBus;
import mizzen.events.EventHandlerContainer;
import mizzen.events.OnIdleEventEventHandler;
import mizzen.events.OnTerminateEventHandler;
import mizzen.events.TerminalEvent;
import mizzen.geom.Dimension;
import junit.framework.TestCase;

/**
 * Starts every example in the desktop's list (EmbeddedAppTest.examples) headless, as the desktop runs them, at a
 * normal, a small, a tiny and an empty size: draws, lets its timers tick, resizes, draws again, closes. Fails with
 * every example and size that threw.
 * <p/>
 * Not part of the usual build (its name doesn't end in Test): a few seconds, and heavy. Run it on its own:
 * <pre>
 *     python3 local/tools/sterile-test.py -Dtest=ExamplesSmoke
 * </pre>
 */
public class ExamplesSmoke extends TestCase {

    private static final int[][] SIZES = { { 30, 110 }, { 12, 40 }, { 6, 20 }, { 0, 0 } };
    private static final int TICKS = 3;

    /** the host's bus: keeps what is on it; idle() runs its idle handlers once, as the real bus does when idle */
    static class HostBus extends EventBus.STUBBED {
        final List<ConditionalEventHandler> handlers = new ArrayList<>();

        @Override
        public EventHandlerContainer prependEventHandler(ConditionalEventHandler eventHandler) {
            handlers.add(0, eventHandler);
            return this;
        }

        @Override
        public EventHandlerContainer appendEventHandler(ConditionalEventHandler eventHandler) {
            handlers.add(eventHandler);
            return this;
        }

        @Override
        public EventBus appendIdleEventHandler(OnIdleEventEventHandler onIdleEventHandler) {
            handlers.add(onIdleEventHandler);
            return this;
        }

        @Override
        public EventBus appendTerminateEventHandler(OnTerminateEventHandler terminateHandler) {
            handlers.add(terminateHandler);
            return this;
        }

        @Override
        public EventHandlerContainer removeEventHandler(ConditionalEventHandler eventHandler) {
            handlers.remove(eventHandler);
            return this;
        }

        @Override
        public EventBus raiseEvent(TerminalEvent<?> te) {
            return this;
        }

        void idle() {
            TerminalEvent<Void> timer = new TerminalEvent<Void>(this, TerminalEvent.TYPE.TIMER, null, this);
            for (ConditionalEventHandler handler : new ArrayList<>(handlers)) {
                if (handler instanceof OnIdleEventEventHandler && handler.canHandle(timer) != null) {
                    handler.handle(timer);
                }
            }
        }
    }

    private static void collect(DefaultTreeNode<EmbeddedAppTest.Demo> node, List<EmbeddedAppTest.Demo> demos) {
        EmbeddedAppTest.Demo demo = node.getValue();
        if (demo != null && demo.type != null) {
            demos.add(demo);
        }
        for (DefaultTreeNode<EmbeddedAppTest.Demo> child : node.getChildren()) {
            collect(child, demos);
        }
    }

    public void testEveryExampleAtEverySize() {
        List<EmbeddedAppTest.Demo> demos = new ArrayList<>();
        collect(EmbeddedAppTest.examples(), demos);
        List<String> failed = new ArrayList<>();
        for (EmbeddedAppTest.Demo demo : demos) {
            for (int[] size : SIZES) {
                String failure = run(demo, size[0], size[1]);
                if (failure != null) {
                    failed.add(demo.type.getSimpleName() + " at " + size[0] + "x" + size[1] + ": " + failure);
                }
            }
        }
        assertTrue(demos.size() + " examples, " + failed.size() + " failed:\n  " + String.join("\n  ", failed), failed.isEmpty());
    }

    // one example at rows x cols: null if it ran, else what it threw and where (our first frame)
    private static String run(EmbeddedAppTest.Demo demo, int rows, int cols) {
        HostBus bus = new HostBus();
        TUIApp host = BusOnlyHost.of(bus);
        Composite slot = new Composite(new Dimension.Simple(rows, cols));
        Embedding embedding = null;
        try {
            embedding = TUIApp.runEmbedded(host, slot, demo.launch());
            slot.getSegmentsBuffer();
            for (int i = 0; i < TICKS; i++) {
                bus.idle();
                slot.getSegmentsBuffer();
            }
            slot.resize(new Dimension.Simple(rows + 5, cols + 10));      // a resize, as a terminal does
            embedding.resize();
            slot.getSegmentsBuffer();
            return null;
        } catch (Throwable t) {
            return describe(t);
        } finally {
            if (embedding != null && !embedding.isClosed()) {
                try {
                    embedding.close();
                } catch (Throwable ignored) {
                    // the failure above is the one to report
                }
            }
        }
    }

    private static String describe(Throwable t) {
        while ((t instanceof InvocationTargetException || t.getCause() != null && t.getMessage() == null) && t.getCause() != null) {
            t = t.getCause();
        }
        String where = "";
        for (StackTraceElement frame : t.getStackTrace()) {
            if (frame.getClassName().startsWith("mizzen.") && !frame.getClassName().startsWith("mizzen.geom.")) {   // past the size checks: who worked it out
                where = " at " + frame.getClassName().replaceAll(".*\\.", "") + "." + frame.getMethodName() + ":" + frame.getLineNumber();
                break;
            }
        }
        return t.getClass().getSimpleName() + ": " + t.getMessage() + where;
    }
}
