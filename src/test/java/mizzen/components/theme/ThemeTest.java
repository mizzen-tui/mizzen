package mizzen.components.theme;

import mizzen.components.BorderedComponent;
import mizzen.components.BlockChars;
import mizzen.components.TextComponent;
import mizzen.components.plot.PlotArea;
import mizzen.components.input.Button;
import mizzen.components.input.TextInput;
import mizzen.components.composite.Composite;
import mizzen.components.composite.VerticalPanel;
import mizzen.controlcodes.color.Color;
import mizzen.controlcodes.color.Colors;
import mizzen.components.modal.ModalPanel;
import mizzen.events.EventBus;
import mizzen.geom.Dimension;
import mizzen.textsegments.IScreenLineSegmentsBuffer;
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

    public void testDisabledFollowsATheme() {
        EventBus bus = new EventBus.STUBBED();
        Composite panel = new Composite(new Dimension.Simple(2, 20));
        panel.setTheme(YELLOW_TEXT.withDisabledColor(Colors.IBla));
        TextComponent own = new TextComponent(Colors.BRed, "own");
        panel.add(own);
        own.setEnabled(false, bus);
        panel.setTheme(CYAN_TEXT.withDisabledColor(Colors.Bla));
        assertSame("the new theme's disabled colour", Colors.Bla, own.getColor());
        assertSame("its own colour kept while disabled", Colors.BRed, own.getOwnColor());
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
        Color ownColor = Theme.rgb(0xFFFF55, 0x0000AA);                   // a foreground and a background
        TextInput input = new TextInput(ownColor, 6);
        panel.add(input);
        panel.setTheme(Theme.CLASSIC);
        assertEquals(ownColor.asString(), firstColor(input).asString());
    }

    /**
     * given only a foreground (Colors.IYel), it keeps that and shows on the background it would have had - the
     * theme's field - as a terminal keeps the background when only the foreground changes
     */
    public void testAnInputGivenOnlyAForegroundShowsOnItsFieldsBackground() {
        Composite panel = new VerticalPanel();
        TextInput input = new TextInput(Colors.IYel, 6);
        panel.add(input);
        panel.setTheme(Theme.CLASSIC);
        assertEquals(Color.onBackgroundOf(Colors.IYel, Theme.CLASSIC.inputColor()).asString(), firstColor(input).asString());
        assertTrue("yellow on the field's background", Color.setsBackground(firstColor(input)));
    }
    /**
     * shown on a background, a foreground-only colour still behaves as itself: bold green stays bold (a focused frame),
     * and its inverse is its own inverse (a selected item) - each on that background again
     */
    public void testAColourOnABackgroundKeepsItsBoldAndInverse() {
        Color under = Theme.CLASSIC.inputColor();
        Color green = Color.onBackgroundOf(Colors.Gre, under);
        assertEquals(Color.onBackgroundOf(Colors.Gre.bold(), under).asString(), green.bold().asString());
        assertFalse("bold changes it", green.asString().equals(green.bold().asString()));
        assertEquals(Color.onBackgroundOf(Colors.Gre.invert(), under).asString(), green.invert().asString());
        assertSame("a colour with its own background is left as it is", under, Color.onBackgroundOf(under, Theme.CLASSIC.textColor()));
    }

    /**
     * a theme with no drawing colours of its own (an app's, say - every standard theme has its own) makes them from its
     * other colours - once, not on every call; a copy with another colour makes its own, from its colours
     */
    public void testDrawingColorsMadeFromItsColoursOnceAndAfreshInACopy() {
        Theme plain = Theme.rgbTheme("Plain", 0x000000, 0xCCCCCC, 0xFFFFFF, 0xCCCCCC, 0x222222, 0xFFFFFF, 0x333333,
                0x000000, 0xFFFF00, 0x888888, 0x555555, 0xFF0000, 0x00FF00, 0x000000, 0x00FFFF);
        assertSame("made once", plain.drawingColors(), plain.drawingColors());
        Theme redText = plain.withTextColor(Colors.IRed);
        assertNotSame("a copy makes its own", plain.drawingColors(), redText.drawingColors());
        assertSame("from its own text colour, the last of the six", Colors.IRed, redText.drawingColor(5));
        assertSame(plain.textColor(), plain.drawingColor(5));
    }

    public void testAPanelsChildrenLookAgainWhenThePanelMoves() {
        Composite yellow = new VerticalPanel();
        yellow.setTheme(YELLOW_TEXT);
        Composite middle = new VerticalPanel();          // never looks a theme up itself
        TextComponent text = new TextComponent((Color) null, "hello");
        middle.add(text);
        assertSame(Theme.LEGACY.textColor(), text.getColor());   // looked up before middle is anywhere
        yellow.add(middle);
        assertSame(Colors.BYel, text.getColor());
    }

    public void testABoxGivenNoColourFollowsWhatItHolds() {
        Composite panel = new VerticalPanel();
        BorderedComponent box = new BorderedComponent(new TextComponent((Color) null, "i"));   // made off screen
        panel.add(box);
        panel.setTheme(YELLOW_TEXT);
        assertSame("not the colour it had when made", Colors.BYel, box.getColor());
    }

    public void testInADialogTextIsOnTheDialogsColour() {
        Theme dialog = Theme.DARK.dialogTheme();
        assertSame(Theme.DARK.dialogColor(), dialog.backgroundColor());
        assertEquals("text, on the dialog's colour", "\u001b[0;38;2;173;186;199;48;2;45;51;59m", dialog.textColor().asString());
        assertSame("a field keeps its own colour", Theme.LIGHT.inputColor(), Theme.LIGHT.dialogTheme().inputColor());
        assertSame("Legacy: text with no colour takes the dialog's", Theme.LEGACY.dialogColor(), Theme.LEGACY.dialogTheme().textColor());
    }

    public void testADialogsInsideFollowsTheThemeOfTheScreen() {
        Composite screen = new VerticalPanel();
        ModalPanel panel = new ModalPanel();
        TextComponent text = new TextComponent((Color) null, "Saved.");
        panel.add(text);
        screen.add(panel);
        screen.setTheme(Theme.DARK);
        assertSame(Theme.DARK.dialogTheme().textColor(), text.getColor());
        screen.setTheme(Theme.LIGHT);
        assertSame("a theme set later reaches it", Theme.LIGHT.dialogTheme().textColor(), text.getColor());
    }
    // whether any segment is drawn in a colour whose codes contain these (e.g. an RGB foreground "38;2;r;g;b")
    private static boolean drawsIn(IScreenLineSegmentsBuffer buffer, String codes) {
        for (ScreenLineSegment segment : buffer.getSegments()) {
            if (segment.getColors() != null && segment.getColors().asString().contains(codes) && !segment.getText().trim().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public void testDrawingColoursCountRound() {
        assertSame(Theme.DARK.drawingColor(1), Theme.DARK.drawingColor(1 + Theme.DARK.drawingColors().size()));
    }

    public void testAPlotDrawsAgainInANewThemesDrawingColours() {
        Composite panel = new VerticalPanel();
        PlotArea plot = new PlotArea(new Dimension.Simple(4, 10)).showing(0, 1, 0, 1)
                .onDraw((pen) -> pen.drawingColor(0).line(0, 0.5, 1, 0.5));
        panel.add(plot);
        panel.setTheme(Theme.DARK);
        assertTrue("Dark's cyan", drawsIn(plot.getSegmentsBuffer(), "38;2;118;227;234"));
        panel.setTheme(Theme.LIGHT);
        assertTrue("Light's cyan, with no redraw() called", drawsIn(plot.getSegmentsBuffer(), "38;2;8;145;178"));
    }

    public void testAPlotsDotsSitOnItsBackground() {
        Composite panel = new VerticalPanel();
        PlotArea plot = new PlotArea(new Dimension.Simple(4, 10)).showing(0, 1, 0, 1)
                .onDraw((pen) -> pen.drawingColor(0).line(0, 0.5, 1, 0.5));
        panel.add(plot);
        panel.setTheme(Theme.DARK.dialogTheme());
        assertTrue("the dialog's background behind the dots, not none", drawsIn(plot.getSegmentsBuffer(), "38;2;118;227;234;48;2;45;51;59"));
    }

    public void testSeriesGivenNoColourTakeDrawingColoursInTurn() {
        Composite panel = new VerticalPanel();
        mizzen.components.chart.LineChart chart = new mizzen.components.chart.LineChart(new Dimension.Simple(10, 40))
                .series("a").series("b").legend(true);
        chart.add("a", 0, 0).add("a", 1, 1).add("b", 0, 1).add("b", 1, 0);
        panel.add(chart);
        panel.setTheme(Theme.DARK);
        chart.redraw();
        assertTrue("the first series in drawing colour 0", drawsIn(chart.getSegmentsBuffer(), "38;2;118;227;234"));
        assertTrue("the second in drawing colour 1", drawsIn(chart.getSegmentsBuffer(), "38;2;218;170;63"));
    }
    // every character a component draws
    private static String drawn(mizzen.components.Component<?> component) {
        StringBuilder b = new StringBuilder();
        for (ScreenLineSegment segment : component.getSegmentsBuffer().getSegments()) {
            b.append(segment.getText());
        }
        return b.toString();
    }

    public void testABorderTakesItsThemesBlockChars() {
        Composite outer = new VerticalPanel();
        Composite boxed = new Composite(new Dimension.Simple(4, 10));
        boxed.setBorder(true);
        outer.add(boxed);
        outer.setTheme(Theme.DARK.withBlockChars(mizzen.components.BlockChars.ROUNDED));
        assertTrue(drawn(boxed), drawn(boxed).contains("╭") && drawn(boxed).contains("╯"));
        outer.setTheme(Theme.DARK);
        assertTrue("a switch reaches it: " + drawn(boxed), drawn(boxed).contains("┌") && !drawn(boxed).contains("╭"));
    }

    public void testItsOwnBlockCharsWin() {
        Composite outer = new VerticalPanel();
        Composite boxed = new Composite(new Dimension.Simple(4, 10));
        boxed.setBorder(true);
        outer.add(boxed);
        outer.setTheme(Theme.DARK.withBlockChars(mizzen.components.BlockChars.ROUNDED));
        boxed.setBlockChars(mizzen.components.BlockChars.BASIC);
        assertSame(mizzen.components.BlockChars.BASIC, boxed.getBlockChars());
    }

    public void testAFocusedBoxUsesTheFocusedSet() {
        Composite outer = new VerticalPanel();
        mizzen.components.RectangleComponent box = new mizzen.components.RectangleComponent(3, 6);
        outer.add(box);
        outer.setTheme(Theme.DARK.withBlockChars(mizzen.components.BlockChars.ROUNDED).withFocusedBlockChars(mizzen.components.BlockChars.NICE_DOUBLE));
        assertTrue(drawn(box), drawn(box).contains("╭"));
        box.hilite();
        assertTrue("focused: " + drawn(box), drawn(box).contains("╔"));
        box.unHilite();
        assertTrue(drawn(box), drawn(box).contains("╭"));
    }
    public void testARegisteredThemeIsFoundByNameAndListed() {
        Theme harbour = Theme.ROUNDED.withName("Harbour").withBackgroundColor(Theme.rgb(0xE6EEF2, 0x1D4F4A));
        Theme.register(harbour);
        assertSame(harbour, Theme.named("harbour"));
        assertTrue(Theme.all().contains(harbour));
        assertFalse("the standard list stays the library's", Theme.standard().contains(harbour));
        Theme again = harbour.withAccentColor(Theme.rgb(0xF2C94C, 0x1D4F4A));
        Theme.register(again);
        assertSame("the same name again replaces it", again, Theme.named("Harbour"));
    }

    public void testSurfaceAndAccentFallBack() {
        Theme plain = Theme.DARK.withName("plain");
        assertSame("no accent: the heading colour", plain.headingColor(), plain.accentColor());
        assertSame("Dark's dialogs keep their own colour", Theme.DARK.dialogColor(), plain.dialogColor());
        Theme surfaced = Theme.ROUNDED;
        assertSame("Rounded's dialogs are on its surface", surfaced.surfaceColor(), surfaced.dialogColor());
        assertSame(BlockChars.ROUNDED, Theme.ROUNDED.blockChars());
    }

    public void testRgbIsTheThemesOwnKindOfColour() {
        assertEquals("\u001b[0;38;2;230;238;242;48;2;29;79;74m", Theme.rgb(0xE6EEF2, 0x1D4F4A).asString());
        assertTrue(Theme.rgbBold(0xF2C94C, 0x1D4F4A).asString().startsWith("\u001b[0;1;38;2;242;201;76"));
    }

    /** a theme on another background: what was on the old one moves to the new, what had its own background keeps it */
    public void testWithBackgroundColorMovesWhatWasOnTheOldBackground() {
        Theme base = Theme.ROUNDED;
        mizzen.controlcodes.color.Color teal = Theme.rgb(0xE6EEF2, 0x1D4F4A);
        Theme guardian = base.withBackgroundColor(teal);
        mizzen.controlcodes.color.Color[][] roles = {
                { base.textColor(), guardian.textColor() },
                { base.drawingColor(0), guardian.drawingColor(0) },
                { base.subheadingColor(), guardian.subheadingColor() },
                { base.strongColor(), guardian.strongColor() },
                { base.linkColor(), guardian.linkColor() },
                { base.statusColor(), guardian.statusColor() },
                { base.disabledColor(), guardian.disabledColor() } };
        for (mizzen.controlcodes.color.Color[] role : roles) {
            assertTrue("on Rounded's background to start with: " + role[0], mizzen.controlcodes.color.Color.sameBackground(role[0], base.backgroundColor()));
            assertTrue("now on the teal: " + role[1], mizzen.controlcodes.color.Color.sameBackground(role[1], teal));
        }
        assertFalse("its own background: kept", mizzen.controlcodes.color.Color.sameBackground(base.surfaceColor(), base.backgroundColor()));
        assertSame(base.surfaceColor(), guardian.surfaceColor());
        mizzen.controlcodes.color.Color white = Theme.rgb(0xFFFFFF, 0x1D4F4A);
        assertSame("given after: as given", white, guardian.withTextColor(white).textColor());
    }
}
