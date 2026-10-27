package mizzen.components.modal;

import java.io.UnsupportedEncodingException;
import java.util.StringJoiner;

import mizzen.components.CompositeWithFocusManager;
import mizzen.components.HasName;
import mizzen.components.input.TextInput;
import mizzen.components.modal.DialogComponent.ButtonSpec;
import mizzen.components.modal.DialogComponent.IconStyle;
import mizzen.controlcodes.color.Colors;
import mizzen.events.focus.FocusManager;
import mizzen.events.focus.Focusable;
import mizzen.events.focus.RequestFocusEvent;
import mizzen.events.focus.ToFocusable;
import mizzen.events.keyboard.KeyEvent;
import mizzen.events.keyboard.KeyStroke;
import mizzen.events.keyboard.Keys;
import mizzen.supportfortesting.CanvasForTesting;
import mizzen.supportfortesting.SyncEventLoop;
import junit.framework.TestCase;

/**
 * Keyboard focus inside a real MessageBox (a modal dialog): it gets keys before the application's
 * root, so TAB / SHIFT-TAB must work inside it on their own, and never reach the rest of the screen.
 * Layout: root[outside, dialog[Yes, No]]. A failure means "behaviour changed".
 *
 * TODAY "outside" (focused before the dialog opened) keeps its own focus flag while the dialog is
 * open, so two components show focus ("outside+Yes"). Parked with the modal work: the dialog should
 * take focus from it, and give it back when it closes.
 */
public class MessageBoxFocusCharacterisationTest extends TestCase {

    private final SyncEventLoop bus = new SyncEventLoop();
    private CompositeWithFocusManager root;
    private TextInput outside;

    @Override
    protected void setUp() throws UnsupportedEncodingException {
        root = new CompositeWithFocusManager();
        CanvasForTesting.buildCanvasForTestingNoControlCodesInOutput().addComponentSegments(root);
        outside = new TextInput(Colors.IYel, 10);
        outside.setName("outside");
        root.add(1, 1, outside);
        bus.setRoot(root);
        bus.deliver(new RequestFocusEvent(ToFocusable.UTIL.toFocusable(outside)));

        ButtonSpec[] buttons = {
            new ButtonSpec("Yes", Keys.F1_KEYSTROKE, (close, te) -> { }, false),
            new ButtonSpec("No",  Keys.F2_KEYSTROKE, (close, te) -> { }, false) };
        MessageBox.simpleDialog(IconStyle.QUESTION, "Question", buttons, bus, root, "Sure?", null);
        bus.drain();
    }

    private void press(KeyStroke key) {
        bus.deliver(new KeyEvent("test", key));
    }

    /**
     * Which components show focus (their own hasFocus flag - what the user sees hilited), e.g.
     * "Yes", or "Yes+outside" if two claim it. Buttons by label, others by name.
     */
    private String focused() {
        StringJoiner names = new StringJoiner("+");
        collectFocused(root, names);
        return names.length() == 0 ? "-" : names.toString();
    }

    private static void collectFocused(Focusable node, StringJoiner names) {
        if (node instanceof FocusManager && !((FocusManager) node).getChildFocusables().isEmpty()) {
            for (Focusable child : ((FocusManager) node).getChildFocusables()) {
                collectFocused(child, names);
            }
        } else if (node.hasFocus()) {
            String name = String.valueOf(HasName.UTIL.getName(node));
            names.add(name.startsWith("Button[") ? name.substring("Button[".length(), name.indexOf(']')).trim() : name.replaceAll("^TextInput\\[|\\]$", ""));
        }
    }

    public void testDialogOpensWithFocusOnItsFirstButton() {
        assertEquals("outside+Yes", focused());
    }

    /**
     * TAB / SHIFT-TAB move between the dialog's buttons, wrapping around - focus never leaves the
     * dialog (TRAP). (Before: TAB stuck on the last button, SHIFT-TAB on the first.)
     */
    public void testTabWrapsAroundInsideTheDialog() {
        StringBuilder seq = new StringBuilder(focused());
        for (KeyStroke key : new KeyStroke[] { Keys.TAB_KEYSTROKE, Keys.TAB_KEYSTROKE, Keys.SHIFT_TAB_KEYSTROKE, Keys.SHIFT_TAB_KEYSTROKE }) {
            press(key);
            seq.append(' ').append(focused());
        }

        assertEquals("outside+Yes outside+No outside+Yes outside+No outside+Yes", seq.toString());
    }
}
