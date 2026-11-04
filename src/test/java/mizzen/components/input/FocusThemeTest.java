package mizzen.components.input;

import java.io.UnsupportedEncodingException;

import mizzen.components.CompositeWithFocusManager;
import mizzen.components.theme.Theme;
import mizzen.controlcodes.color.Colors;
import mizzen.events.focus.RequestFocusEvent;
import mizzen.events.focus.ToFocusable;
import mizzen.supportfortesting.CanvasForTesting;
import mizzen.supportfortesting.SyncEventLoop;
import junit.framework.TestCase;

/**
 * Focus shows itself without fixing a colour: a component with no colour of its own draws its theme colour in bold while
 * focused, worked out as it is drawn - a theme set later still reaches it - and has no colour of its own after. One given
 * a colour shows that colour in bold, as before.
 */
public class FocusThemeTest extends TestCase {

    private final SyncEventLoop bus = new SyncEventLoop();
    private CompositeWithFocusManager root;

    private void onScreen(Object... components) throws UnsupportedEncodingException {
        root = new CompositeWithFocusManager();
        CanvasForTesting.buildCanvasForTestingNoControlCodesInOutput().addComponentSegments(root);
        int row = 1;
        for (Object component : components) {
            root.add(row, 1, (mizzen.components.Component<?>) component);
            row += 3;
        }
        bus.setRoot(root);
    }

    private void focus(Object component) {
        bus.deliver(new RequestFocusEvent(ToFocusable.UTIL.toFocusable(component)));
    }

    public void testFocusedWithNoColourItFollowsATheme() throws UnsupportedEncodingException {
        TextInput first = new TextInput(10);
        TextInput second = new TextInput(10);
        onScreen(first, second);
        root.setTheme(Theme.DARK);
        focus(first);
        assertNull("no colour of its own while focused", first.getOwnColor());
        assertEquals("Dark's input colour, bold", Theme.DARK.inputColor().bold().asString(), first.getColor().asString());
        root.setTheme(Theme.LIGHT);
        assertEquals("Light's, bold: the theme set later reaches it", Theme.LIGHT.inputColor().bold().asString(), first.getColor().asString());
        focus(second);
        assertNull("none after it loses focus", first.getOwnColor());
        assertSame("Light's input colour, not bold", Theme.LIGHT.inputColor(), first.getColor());
    }

    public void testGivenAColourFocusShowsItBoldAsBefore() throws UnsupportedEncodingException {
        TextInput first = new TextInput(Colors.IYel, 10);
        TextInput second = new TextInput(10);
        onScreen(first, second);
        focus(first);
        assertEquals(Colors.IYel.bold().asString(), first.getColor().asString());
        focus(second);
        assertSame(Colors.IYel, first.getColor());
    }

    public void testAnOpenDropdownListFollowsATheme() throws UnsupportedEncodingException {
        DropdownListBox<String> dropdown = DropdownListBox.fromStrings(new String[] { "A", "B", "C" });
        onScreen(dropdown);
        root.setTheme(Theme.LIGHT);
        focus(dropdown);
        assertTrue("open", dropdown.listBoxVisible());
        root.setTheme(Theme.DARK);
        for (mizzen.textsegments.ScreenLineSegment segment : root.getSegmentsBuffer().getSegments()) {
            String codes = segment.getColors() == null ? "" : segment.getColors().asString();
            assertFalse("drawn in Light's field colour after the switch to Dark: '" + segment.getText() + "'",
                    codes.contains("48;2;234;238;242"));
        }
    }
    // what a button shows, characters only
    private static String shown(Button button) {
        StringBuilder b = new StringBuilder();
        for (mizzen.textsegments.ScreenLineSegment segment : button.getSegmentsBuffer().getSegments()) {
            b.append(segment.getText());
        }
        return b.toString();
    }

    public void testRoundedShowsAFocusedButtonByColourNotBrackets() throws UnsupportedEncodingException {
        Button ok = new Button("OK");
        Button other = new Button("Other");
        onScreen(ok, other);
        root.setTheme(Theme.ROUNDED);
        focus(ok);
        assertFalse("no brackets: " + shown(ok), shown(ok).contains("["));
        assertSame(Theme.ROUNDED.buttonFocusedColor(), ok.getColor());
        focus(other);
        assertSame("its own theme colour again", Theme.ROUNDED.buttonColor(), ok.getColor());
    }

    public void testAnOutlinedButtonTurnsTheAccent() throws UnsupportedEncodingException {
        Button outlined = new Button("Details").setThemeColor(Theme::textColor);
        onScreen(outlined, new Button("Other"));
        root.setTheme(Theme.ROUNDED);
        focus(outlined);
        assertSame(Theme.ROUNDED.accentColor(), outlined.getColor());
    }

    public void testDarkStillShowsBrackets() throws UnsupportedEncodingException {
        Button ok = new Button("OK");
        onScreen(ok, new Button("Other"));
        root.setTheme(Theme.DARK);
        focus(ok);
        assertTrue(shown(ok), shown(ok).contains("[OK]"));
    }
}
