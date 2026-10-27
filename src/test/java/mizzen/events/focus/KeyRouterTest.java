package mizzen.events.focus;

import mizzen.components.CompositeWithFocusManager;
import mizzen.components.composite.VerticalPanel;
import mizzen.components.input.CheckBoxInput;
import mizzen.components.input.TextInput;
import mizzen.controlcodes.color.Colors;
import mizzen.events.keyboard.KeyboardHandler;
import mizzen.events.keyboard.KeyEvent;
import mizzen.events.keyboard.KeyStroke;
import mizzen.events.keyboard.Keys;
import mizzen.supportfortesting.CanvasForTesting;
import mizzen.supportfortesting.SyncEventLoop;
import junit.framework.TestCase;

/**
 * Keys go to the focused component first, then up through the components around it until one uses
 * it (KeyboardHandler.onKey returns true); only then does focus move (TAB). Layout:
 * root[card[checkbox, name], after] - card is a plain panel that handles 'x' itself.
 */
public class KeyRouterTest extends TestCase {

    /** a panel that uses the 'x' key itself: counts it */
    static class Card extends VerticalPanel implements KeyboardHandler {
        int xPressed;

        @Override
        public boolean onKey(KeyEvent ke) {
            if (ke.isKeyStroke(new KeyStroke('x'))) {
                xPressed++;
                return true;
            }
            return false;
        }
    }

    private final SyncEventLoop bus = new SyncEventLoop();
    private final Card card = new Card();
    private final CheckBoxInput checkbox = new CheckBoxInput(Colors.IYel);
    private final TextInput name = new TextInput(Colors.IYel, 10);
    private final TextInput after = new TextInput(Colors.IYel, 10);
    private final CompositeWithFocusManager root = new CompositeWithFocusManager();   // as TUIApp builds it

    @Override
    protected void setUp() throws Exception {
        checkbox.setName("checkbox");
        name.setName("name");
        after.setName("after");
        card.add(checkbox);
        card.add(name);
        root.add(card);
        root.add(after);
        CanvasForTesting.buildCanvasForTestingNoControlCodesInOutput().addComponentSegments(root);   // a focused checkbox shows the cursor
        bus.setRoot(root);
        bus.deliver(new RequestFocusEvent(checkbox.asFocusable()));
    }

    private void press(KeyStroke key) {
        bus.deliver(new KeyEvent("test", key));
    }

    private String focusedName() {
        Focusable focused = FocusManager.UTIL.findCurrentFocus(root);
        return focused == null ? "-" : mizzen.components.HasName.UTIL.getName(focused.getFocusComponent());
    }

    /** the focused component's own onKey: SPACE toggles the checkbox */
    public void testFocusedComponentGetsTheKeyFirst() {
        press(Keys.SPACE_KEYSTROKE);

        assertTrue(checkbox.getValue());
        assertEquals("checkbox", focusedName());
    }

    /** a key the focused component doesn't use goes on to the panel around it */
    public void testUnusedKeyBubblesToTheParent() {
        press(new KeyStroke('x'));

        assertEquals(1, card.xPressed);
        assertFalse(checkbox.getValue());
        assertEquals("checkbox", focusedName());
    }

    /** a key nobody on the way up uses: nothing happens */
    public void testKeyNobodyUsesDoesNothing() {
        press(new KeyStroke('z'));

        assertEquals(0, card.xPressed);
        assertFalse(checkbox.getValue());
        assertEquals("checkbox", focusedName());
    }

    /**
     * The root takes only keys someone on the way up wants. A checkbox's key handler is an adapted
     * event handler, so it can tell without acting: SPACE yes, 'z' no. (The card above is a plain
     * onKey, which can't tell - so this uses its own root: root2[agree].)
     */
    public void testRootTakesOnlyKeysSomeoneWants() throws Exception {
        CheckBoxInput agree = new CheckBoxInput(Colors.IYel);
        CompositeWithFocusManager root2 = new CompositeWithFocusManager();
        root2.add(agree);
        CanvasForTesting.buildCanvasForTestingNoControlCodesInOutput().addComponentSegments(root2);
        SyncEventLoop bus2 = new SyncEventLoop();
        bus2.setRoot(root2);
        bus2.deliver(new RequestFocusEvent(agree.asFocusable()));

        assertNotNull("SPACE: the checkbox wants it", root2.canHandle(new KeyEvent("test", Keys.SPACE_KEYSTROKE)));
        assertNull("'z': nobody wants it", root2.canHandle(new KeyEvent("test", new KeyStroke('z'))));
    }

    /** TAB: nobody uses it, so focus moves - inside the card, then out of it */
    public void testTabMovesFocusWhenNobodyUsesIt() {
        press(Keys.TAB_KEYSTROKE);
        assertEquals("name", focusedName());

        press(Keys.TAB_KEYSTROKE);
        assertEquals("after", focusedName());
    }

    /** the focused component that uses a key keeps it from its parents: 'x' typed into the name field */
    public void testFocusedComponentKeepsKeysItUses() {
        press(Keys.TAB_KEYSTROKE);

        press(new KeyStroke('x'));

        assertEquals("x", name.getValue());
        assertEquals(0, card.xPressed);
    }
}
