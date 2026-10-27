package mizzen.app;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import mizzen.components.Component;
import mizzen.components.composite.Composite;
import mizzen.events.ConditionalEventHandler;
import mizzen.events.EventBus;
import mizzen.events.EventHandlerContainer;
import mizzen.events.OnIdleEventEventHandler;
import mizzen.events.OnTerminateEventHandler;
import mizzen.events.TerminalEvent;
import mizzen.events.keyboard.KeyInputEventHandler;
import mizzen.events.keyboard.Keys;
import mizzen.geom.Dimension;
import junit.framework.TestCase;

/**
 * A TUIApp made inside TUIApp.runEmbedded runs inside the host: its main panel goes into the host's
 * panel, its handlers onto the host's bus - and close() takes all of that out again. Outside
 * runEmbedded a TUIApp is the usual terminal one. Bad launch code fails without leaving anything behind.
 */
public class EmbeddedAppTest extends TestCase {

    /** the host's bus: keeps what is on it; idle() runs the idle handlers once, as the real bus does when idle */
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

    private final HostBus hostBus = new HostBus();
    private final Composite slot = new Composite(new Dimension.Simple(10, 40));
    private TUIApp host;

    @Override
    protected void setUp() {
        // a host that is only a bus: the embedded app needs nothing else of it here
        host = new TUIApp((TUIAppInterface) Proxy.newProxyInstance(TUIAppInterface.class.getClassLoader(),
                new Class<?>[] { TUIAppInterface.class },
                (proxy, method, args) -> method.getName().equals("getEventBus") ? hostBus : null));
    }

    public void testTheAppRunsInTheSlotAndCloseTakesEverythingOut() {
        AtomicReference<TUIApp> setupGot = new AtomicReference<>();
        Embedding embedding = TUIApp.runEmbedded(host, slot, () -> {
            new TUIApp(new String[0])
                .setDefaultMainApplicationPanel()
                .setOnReadyStart((tApp) -> {
                    setupGot.set(tApp);
                    tApp.getEventBus().appendEventHandler(new KeyInputEventHandler(Keys.ESC_KEYSTROKE, (te) -> {}));
                    tApp.getEventBus().appendIdleEventHandler(new OnIdleEventEventHandler.RepeatingInterval(100, (te) -> {}));
                })
                .start();
        });
        Component<?> panel = (Component<?>) embedding.tuiApp().getMainApplicationPanel();
        assertTrue("its main panel is in the host's panel", slot.containsChild(panel));
        assertEquals("its main panel is the host panel's size", 40, panel.getWidth());

        assertEquals("before the setup ran: the setup task is on the bus", 1, embedding.handlersOnHostBus());
        hostBus.idle();
        assertSame("the setup code got the embedded TUIApp", embedding.tuiApp(), setupGot.get());
        assertEquals("after: its key handler and timer (the setup task has gone)", 2, embedding.handlersOnHostBus());
        assertEquals(2, hostBus.handlers.size());

        embedding.close();
        assertTrue("nothing of it left on the host's bus", hostBus.handlers.isEmpty());
        assertEquals(0, embedding.handlersOnHostBus());
        assertFalse("its panel is out of the host's panel", slot.containsChild(panel));
        embedding.close();                          // again: nothing happens
    }

    public void testHandlersItRemovesItselfAreForgotten() {
        Embedding embedding = TUIApp.runEmbedded(host, slot, () -> new TUIApp().setDefaultMainApplicationPanel().start());
        hostBus.idle();
        ConditionalEventHandler key = new KeyInputEventHandler(Keys.ESC_KEYSTROKE, (te) -> {});
        EventBus bus = embedding.tuiApp().getEventBus();
        bus.appendEventHandler(key);
        assertEquals(1, embedding.handlersOnHostBus());
        bus.removeEventHandler(key);
        assertEquals(0, embedding.handlersOnHostBus());
        assertTrue(hostBus.handlers.isEmpty());

        // a one-shot task: forgotten once it ran, though it takes itself off the host's bus (te.getEventBus())
        AtomicInteger ran = new AtomicInteger();
        bus.asynch((te) -> ran.incrementAndGet());
        assertEquals(1, embedding.handlersOnHostBus());
        hostBus.idle();
        assertEquals(1, ran.get());
        assertEquals(0, embedding.handlersOnHostBus());
        assertTrue(hostBus.handlers.isEmpty());
    }

    /** what a handler adds through its event (te.getEventBus()), as a dialog does, is the app's too */
    public void testWhatAHandlerAddsThroughItsEventIsTakenOutOnClose() {
        Embedding embedding = TUIApp.runEmbedded(host, slot, () -> new TUIApp().setDefaultMainApplicationPanel().start());
        hostBus.idle();
        ConditionalEventHandler dialog = new KeyInputEventHandler(Keys.ESC_KEYSTROKE, (te) -> {});
        embedding.tuiApp().getEventBus().appendIdleEventHandler(new OnIdleEventEventHandler.OneTime(
                (te) -> te.getEventBus().prependEventHandler(dialog)));
        hostBus.idle();
        assertEquals("the dialog's handler, through the event's bus", 1, embedding.handlersOnHostBus());
        assertEquals(1, hostBus.handlers.size());

        embedding.close();
        assertTrue("closing took the dialog's handler off too", hostBus.handlers.isEmpty());
    }

    public void testATUIAppOutsideRunEmbeddedIsTheTerminalOne() {
        TUIApp.runEmbedded(host, slot, () -> new TUIApp().setDefaultMainApplicationPanel().start()).close();
        assertNotNull(new TUIApp().terminal());
    }

    public void testLaunchCodeThatThrowsLeavesNothingBehind() {
        RuntimeException failure = new RuntimeException("setup failed");
        try {
            TUIApp.runEmbedded(host, slot, () -> {
                TUIApp app = new TUIApp().setDefaultMainApplicationPanel();
                app.getEventBus().appendEventHandler(new KeyInputEventHandler(Keys.ESC_KEYSTROKE, (te) -> {}));
                app.start();
                throw failure;
            });
            fail("expected the launch code's exception");
        } catch (RuntimeException e) {
            assertSame(failure, e);
        }
        assertTrue(hostBus.handlers.isEmpty());
        assertTrue(slot.getChildren().isEmpty());
        assertNotNull("not embedding any more", new TUIApp().terminal());
    }

    public void testLaunchCodeMustMakeAndStartOneTUIApp() {
        assertFails("made no TUIApp", () -> {});
        assertFails("did not start", () -> new TUIApp().setDefaultMainApplicationPanel());
        assertFails("second TUIApp", () -> {
            new TUIApp().setDefaultMainApplicationPanel().start();
            new TUIApp();
        });
        assertTrue(hostBus.handlers.isEmpty());
        assertTrue(slot.getChildren().isEmpty());
    }

    public void testTheTerminalIsTheHosts() {
        Embedding embedding = TUIApp.runEmbedded(host, slot, () -> new TUIApp().setDefaultMainApplicationPanel().start());
        try {
            embedding.tuiApp().getScreen();
            fail("an embedded app has no screen");
        } catch (UnsupportedOperationException expected) {
        }
        embedding.close();
    }

    /** one terminal: a second TUIApp starting on it fails (pointing at runEmbedded) until the first terminates */
    public void testOnlyOneTUIAppAtATimeHasTheTerminal() {
        TerminalApp first = new TUIApp().terminal();
        TerminalApp second = new TUIApp().terminal();
        TerminalApp.claimTheTerminal(first);
        try {
            TerminalApp.claimTheTerminal(first);          // the same one again: fine
            try {
                TerminalApp.claimTheTerminal(second);
                fail("a second TUIApp got the terminal");
            } catch (IllegalStateException expected) {
                assertTrue(expected.getMessage().contains("runEmbedded"));
            }
            TerminalApp.releaseTheTerminal(second);       // not its to release: no effect
            try {
                TerminalApp.claimTheTerminal(second);
                fail("released by the wrong TUIApp");
            } catch (IllegalStateException expected) {
            }
            // an embedded TUIApp is not a terminal one: fine while the first has the terminal
            TUIApp.runEmbedded(host, slot, () -> new TUIApp().setDefaultMainApplicationPanel().start()).close();
        } finally {
            TerminalApp.releaseTheTerminal(first);
        }
        TerminalApp.claimTheTerminal(second);              // free again
        TerminalApp.releaseTheTerminal(second);
    }

    private void assertFails(String messagePart, Embedding.Launch launch) {
        try {
            TUIApp.runEmbedded(host, slot, launch);
            fail("expected a failure: " + messagePart);
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage(), e.getMessage().contains(messagePart));
        }
    }
}
