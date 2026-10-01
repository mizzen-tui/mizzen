package dtb.terminalIO.components.modal;

import dtb.terminalIO.events.EventHandlerContainer;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.keyboard.KeyEvent;
import dtb.terminalIO.events.keyboard.KeyInputEventHandler;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.supportfortesting.SyncEventLoop;
import junit.framework.TestCase;

/**
 * Characterisation test for the existing modal trap (ModalEventHandler, used by MessageBox):
 * it prepends itself to its parent container, lets its own handlers take their keys, and
 * swallows every other key (keyboard sinkhole). It is separate from the focus managers and must
 * keep working through the focus redesign (docs/ARCHITECTURE.md §4.4).
 */
public class ModalEventHandlerCharacterisationTest extends TestCase {

    private final SyncEventLoop bus = new SyncEventLoop();

    private KeyEvent key(dtb.terminalIO.events.keyboard.KeyStroke k) {
        KeyEvent ke = new KeyEvent("test", k);
        ke.setEventBus(bus);
        return ke;
    }

    public void testModalSwallowsKeysItDoesNotHandle() {
        EventHandlerContainer.SimpleList parent = new EventHandlerContainer.SimpleList();
        ModalEventHandler modal = new ModalEventHandler(parent);
        int[] enter = { 0 };
        modal.appendEventHandler(new KeyInputEventHandler(Keys.ENTER_KEYSTROKE, te -> enter[0]++));

        // registers itself with its parent (prepended, so it sees events first)
        assertTrue(parent.getEventHandlers().contains(modal));

        // its own key reaches its handler
        KeyEvent enterKey = key(Keys.ENTER_KEYSTROKE);
        assertNotNull(modal.canHandle(enterKey));
        modal.handle(enterKey);
        assertEquals(1, enter[0]);

        // any other key is claimed too (swallowed) and goes nowhere
        KeyEvent tab = key(Keys.TAB_KEYSTROKE);
        assertNotNull("TAB must be swallowed by the modal", modal.canHandle(tab));
        modal.handle(tab);
        assertEquals(1, enter[0]);
    }

    public void testModalDoesNotClaimNonKeyEvents() {
        ModalEventHandler modal = new ModalEventHandler(new EventHandlerContainer.SimpleList());

        TerminalEvent<Object> custom = new TerminalEvent<Object>("test", TerminalEvent.TYPE.CUSTOM_APPLICATION_EVENT, null);
        custom.setEventBus(bus);

        assertNull(modal.canHandle(custom));
    }
}
