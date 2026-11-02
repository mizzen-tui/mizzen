package mizzen.components.modal;

import java.io.UnsupportedEncodingException;

import mizzen.components.CompositeWithFocusManager;
import mizzen.components.input.TextInput;
import mizzen.controlcodes.color.Colors;
import mizzen.events.focus.ToFocusable;
import mizzen.events.keyboard.KeyEvent;
import mizzen.events.keyboard.KeyStroke;
import mizzen.events.keyboard.Keys;
import mizzen.geom.Dimension;
import mizzen.supportfortesting.CanvasForTesting;
import mizzen.supportfortesting.SyncEventLoop;
import junit.framework.TestCase;

/**
 * Dialogs as a user meets them: a button closes it once its action has run; it stays open when the
 * action shows an error, a submit finds the form invalid, or the button stays open; a disabled button
 * can't be pressed. Enter in a field presses the first button; Esc presses cancel. Focus starts in the
 * first field.
 */
public class DialogTest extends TestCase {

    private final SyncEventLoop bus = new SyncEventLoop();
    private CompositeWithFocusManager screen;
    private final StringBuilder heard = new StringBuilder();

    public static class Login {
        public String user = "";
        public String password = "";
    }

    @Override
    protected void setUp() throws UnsupportedEncodingException {
        screen = new CompositeWithFocusManager();
        CanvasForTesting.buildCanvasForTestingNoControlCodesInOutput().addComponentSegments(screen);
        screen.resize(new Dimension.Simple(30, 100));
        bus.setRoot(screen);
    }

    private Dialog show(Dialog dialog) {
        dialog.onClose(() -> heard.append("closed ")).show(bus, screen);
        bus.drain();
        return dialog;
    }

    private void press(KeyStroke key) {
        bus.deliver(new KeyEvent("test", key));
        bus.drain();
    }

    private void type(String text) {
        for (char c : text.toCharArray()) {
            press(new KeyStroke(c));
        }
    }

    public void testOkCloses() {
        Dialog dialog = show(Dialog.info("Saved 3 files.").ok());
        press(Keys.ENTER_KEYSTROKE);                                     // focus is on OK: no fields
        assertFalse(dialog.isOpen());
        assertEquals("closed ", heard.toString());
    }

    public void testAButtonsActionRunsThenItCloses() {
        Dialog dialog = show(Dialog.question("Delete 3 backups?")
                .button("delete", "Delete", (d) -> heard.append("deleted ")).key(KeyStroke.caseInsensitive('d'))
                .cancel());
        press(new KeyStroke('d'));
        assertFalse(dialog.isOpen());
        assertEquals("deleted closed ", heard.toString());
    }

    public void testEscPressesCancel() {
        Dialog dialog = show(Dialog.question("Delete 3 backups?")
                .button("delete", "Delete", (d) -> heard.append("deleted "))
                .cancel((d) -> heard.append("cancelled ")));
        press(Keys.ESC_KEYSTROKE);
        assertFalse(dialog.isOpen());
        assertEquals("cancelled closed ", heard.toString());
    }

    public void testEscWithNoCancelJustCloses() {
        Dialog dialog = show(Dialog.info("Saved.").ok());
        press(Keys.ESC_KEYSTROKE);
        assertFalse(dialog.isOpen());
    }

    public void testEnterInAFieldPressesTheFirstButton() {
        TextInput name = new TextInput(Colors.IYel, 20);
        Dialog dialog = show(Dialog.question("What's your name?")
                .add("Name: ", name)
                .button("ok", "OK", (d) -> heard.append("hello " + name.getText() + " "))
                .cancel());
        assertTrue("focus starts in the field", ToFocusable.UTIL.toFocusable(name).hasFocus());
        type("Dav");
        press(Keys.ENTER_KEYSTROKE);
        assertFalse(dialog.isOpen());
        assertEquals("hello Dav closed ", heard.toString());
    }

    public void testADisabledButtonCantBePressed() {
        TextInput word = new TextInput(Colors.IYel, 10);
        Dialog dialog = show(Dialog.warning("Runs on the live server.").title("Back up production")
                .add("Type BACKUP to continue: ", word)
                .button("run", "Run", (d) -> heard.append("run "))
                    .enabledWhen(() -> "BACKUP".equals(word.getText()))
                .cancel());
        assertFalse(dialog.button("run").isEnabled());
        type("BACK");
        press(Keys.ENTER_KEYSTROKE);
        assertTrue("Enter does nothing yet", dialog.isOpen());
        type("UP");
        assertTrue(dialog.button("run").isEnabled());
        press(Keys.ENTER_KEYSTROKE);
        assertFalse(dialog.isOpen());
        assertEquals("run closed ", heard.toString());
    }

    public void testShowErrorKeepsItOpen() {
        Dialog dialog = show(Dialog.question("Connect?")
                .button("connect", "Connect", (d) -> d.showError("No answer from the server"))
                .cancel());
        press(Keys.ENTER_KEYSTROKE);
        assertTrue(dialog.isOpen());
        assertEquals("", heard.toString());
    }

    public void testStaysOpen() {
        TextInput name = new TextInput(Colors.IYel, 10);
        Dialog dialog = show(Dialog.question("Name?").add(name)
                .ok()
                .button("reset", "Reset", (d) -> name.setText("")).key(Keys.F2_KEYSTROKE).staysOpen());
        type("x");
        press(Keys.F2_KEYSTROKE);
        assertTrue(dialog.isOpen());
        assertEquals("", name.getText());
    }

    public void testSubmitChecksTheFormFirst() {
        Login login = new Login();
        Dialog dialog = show(Dialog.question("Login to the server").title("Sign in")
                .form(login, (f) -> f.text("User", "user").required().password("Password", "password").required())
                .submit("login", "Login", (d) -> {
                    if (!"secret".equals(login.password)) {
                        d.showError("Login failed");
                        return;
                    }
                    heard.append("in as " + login.user + " ");
                })
                .cancel());
        press(Keys.ENTER_KEYSTROKE);                                     // nothing typed: the form says so
        assertTrue(dialog.isOpen());
        type("david");
        press(Keys.TAB_KEYSTROKE);
        type("wrong");
        press(Keys.ENTER_KEYSTROKE);
        assertTrue("the server said no: still open", dialog.isOpen());
        for (int i = 0; i < 5; i++) {
            press(Keys.BACKSPACE_KEYSTROKE);
        }
        type("secret");
        press(Keys.ENTER_KEYSTROKE);
        assertFalse(dialog.isOpen());
        assertEquals("in as david closed ", heard.toString());
    }

    public void testADialogNeedsAButton() {
        try {
            Dialog.info("Nothing to close me").show(bus, screen);
            fail("no button");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("ok()"));
        }
    }

    public void testANameThatIsNotThere() {
        Dialog dialog = show(Dialog.info("Saved.").ok());
        try {
            dialog.button("run");
            fail("no such button");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("[ok]"));
        }
    }
}
