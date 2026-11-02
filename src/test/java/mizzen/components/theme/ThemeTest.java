package mizzen.components.theme;

import mizzen.components.TextComponent;
import mizzen.components.input.Button;
import mizzen.components.input.TextInput;
import mizzen.components.composite.Composite;
import mizzen.components.composite.VerticalPanel;
import mizzen.controlcodes.color.Color;
import mizzen.controlcodes.color.Colors;
import mizzen.events.EventBus;
import mizzen.geom.Dimension;
import mizzen.textsegments.ScreenLineSegment;
import junit.framework.TestCase;

/**
 * Which colour a component draws with: its own if given one; else the theme that applies - set on it or
 * the nearest ancestor, else the default. A theme set later, or a move to another parent, shows on the
 * next draw. Disabled: the theme's disabled colour, and its own colour back after - none, if it had none.
 */
public class ThemeTest extends TestCase {

    private static final Theme YELLOW_TEXT = Theme.LEGACY.withTextColor(Colors.BYel);
    private static final Theme CYAN_TEXT = Theme.LEGACY.withTextColor(Colors.BCya);

    @Override
    protected void tearDown() {
        Theme.setDefault(Theme.LEGACY);                   // a global: leave it as found
    }

    // the colour its text is drawn in
    private static Color drawnIn(TextComponent text) {
        for (ScreenLineSegment segment : text.getSegmentsBuffer().getSegments()) {
            if (!segment.getText().trim().isEmpty()) {
                return segment.getColors();
            }
        }
        return null;
    }

    public void testGivenNoColourItTakesTheTheme() {
        TextComponent text = new TextComponent((Color) null, "hello");
        assertSame(Theme.LEGACY.textColor(), text.getColor());
    }

    public void testItsOwnColourWinsOverAnyTheme() {
        Composite panel = new VerticalPanel();
        panel.setTheme(YELLOW_TEXT);
        TextComponent red = new TextComponent(Colors.BRed, "overdue");
        panel.add(red);
        assertSame(Colors.BRed, red.getColor());
    }

    public void testTheNearestAncestorsThemeApplies() {
        Composite outer = new VerticalPanel();
        Composite inner = new VerticalPanel();
        outer.add(inner);
        TextComponent text = new TextComponent((Color) null, "hello");
        inner.add(text);
        outer.setTheme(YELLOW_TEXT);
        assertSame(Colors.BYel, text.getColor());
        inner.setTheme(CYAN_TEXT);
        assertSame("the nearer one", Colors.BCya, text.getColor());
    }

    public void testTheDefaultAppliesWhenNoAncestorHasOne() {
        TextComponent text = new TextComponent((Color) null, "hello");
        Theme.setDefault(YELLOW_TEXT);
        assertSame(Colors.BYel, text.getColor());
    }

    public void testATextDrawsAgainInANewTheme() {
        Composite panel = new VerticalPanel();
        TextComponent text = new TextComponent((Color) null, "hello");
        panel.add(text);
        assertSame(Theme.LEGACY.textColor(), drawnIn(text));
        panel.setTheme(YELLOW_TEXT);                     // marks it dirty: drawn again
        assertSame(Colors.BYel, drawnIn(text));
    }

    public void testAMovedComponentLooksAgain() {
        Composite yellow = new VerticalPanel();
        yellow.setTheme(YELLOW_TEXT);
        Composite cyan = new VerticalPanel();
        cyan.setTheme(CYAN_TEXT);
        TextComponent text = new TextComponent((Color) null, "hello");
        yellow.add(text);
        assertSame(Colors.BYel, text.getColor());
        yellow.remove(text);
        cyan.add(text);
        assertSame(Colors.BCya, text.getColor());
    }

    public void testDisabledThenEnabledAgain() {
        EventBus bus = new EventBus.STUBBED();
        Composite panel = new Composite(new Dimension.Simple(2, 20));
        panel.setTheme(YELLOW_TEXT.withDisabledColor(Colors.IBla));
        TextComponent own = new TextComponent(Colors.BRed, "own");
        TextComponent themed = new TextComponent((Color) null, "themed");
        panel.add(own);
        panel.add(1, 0, themed);

        own.setEnabled(false, bus);
        themed.setEnabled(false, bus);
        assertSame(Colors.IBla, own.getColor());
        assertSame(Colors.IBla, drawnIn(themed));

        own.setEnabled(true, bus);
        themed.setEnabled(true, bus);
        assertSame("its own colour back", Colors.BRed, own.getColor());
        panel.setTheme(CYAN_TEXT);
        assertSame("still themed: it had no colour of its own", Colors.BCya, themed.getColor());
    }

    // the colour of the first segment with a background - what a field or button shows
    private static Color firstColor(mizzen.components.Component<?> component) {
        for (ScreenLineSegment segment : component.getSegmentsBuffer().getSegments()) {
            return segment.getColors();
        }
        return null;
    }

    /** an input and a button made before the theme changes follow it: their parts too (they bake no colour of their own) */
    public void testInputsAndButtonsFollowAThemeSetLater() {
        Composite panel = new VerticalPanel();
        TextInput input = new TextInput(6);
        Button button = new Button("Order");
        panel.add(input);
        panel.add(button);
        firstColor(input);
        firstColor(button);                              // drawn once, in Legacy

        panel.setTheme(Theme.CLASSIC);
        assertEquals(Theme.CLASSIC.inputColor().toString(), firstColor(input).toString());
        assertEquals("the escape codes it sends", Theme.CLASSIC.buttonColor().asString(), firstColor(button).asString());
    }

    /** a component given its own colour keeps it, and so do the parts it makes */
    public void testAnInputGivenAColourKeepsIt() {
        Composite panel = new VerticalPanel();
        TextInput input = new TextInput(Colors.IYel, 6);
        panel.add(input);
        panel.setTheme(Theme.CLASSIC);
        assertSame(Colors.IYel, firstColor(input));
    }
}
