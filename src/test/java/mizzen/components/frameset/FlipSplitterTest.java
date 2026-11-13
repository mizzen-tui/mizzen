package mizzen.components.frameset;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import mizzen.components.Component;
import mizzen.components.CompositeWithFocusManager;
import mizzen.components.HasName;
import mizzen.components.TextComponent;
import mizzen.components.composite.Composite;
import mizzen.components.frameset.FlipSplitter.Placement;
import mizzen.components.input.ChipGroup;
import mizzen.components.input.DisplayableValue.SimpleDisplayValue;
import mizzen.core.TextSnapshot;
import mizzen.events.focus.FocusManager;
import mizzen.events.focus.Focusable;
import mizzen.events.keyboard.KeyEvent;
import mizzen.events.keyboard.KeyStroke;
import mizzen.events.keyboard.Keys;
import mizzen.geom.Dimension;
import mizzen.geom.position.Position;
import mizzen.supportfortesting.CanvasForTesting;
import mizzen.supportfortesting.SyncEventLoop;
import junit.framework.TestCase;

/**
 * Two panes flipping between stacked and beside; the control - any FormInput of Placement - floats at the top right.
 * Layout: the splitter, titled " Stats " (so bordered), holding chart and figures - panes with no border of their own.
 */
public class FlipSplitterTest extends TestCase {

    private final Composite chart = pane("a line");
    private final Composite figures = pane("412 sessions");
    private FlipSplitter split;

    private static Composite pane(String text) {
        Composite pane = new Composite();
        pane.add(new TextComponent(text));
        return pane;
    }

    @Override
    protected void setUp() {
        Component.UTIL.COMPONENT_SEGMENTS_INTEGRITY_CHECK_FLAG = true;    // segments outside a component's bounds throw
        chart.setName("chart");
        figures.setName("figures");
        split = new FlipSplitter(chart, figures).setTitle(" Stats ");
        split.resize(new Dimension.Simple(12, 60));
    }

    @Override
    protected void tearDown() {
        Component.UTIL.COMPONENT_SEGMENTS_INTEGRITY_CHECK_FLAG = false;
    }

    private String[] rows() {
        return new TextSnapshot(split.getHeight(), split.getWidth()).render(split).split("\n");
    }

    private String picture() {
        return String.join("\n", rows());
    }

    private String top() {
        return rows()[0];
    }

    /** by default radio buttons where there's room, at the top right - ending two in, the corner and a line past it */
    public void testRadioByDefault() {
        assertTrue(top(), top().startsWith("┌ Stats ─"));
        assertTrue(top(), top().endsWith("(•) Below  ( ) Right─┐"));
        assertSame(Placement.BELOW, split.getPlacement());
    }

    /** too narrow for the radio (more than half the width): the chips, a picture each; wider again, the radio */
    public void testNarrowGivesTheChips() {
        split.resize(new Dimension.Simple(12, 30));
        assertTrue(top(), top().endsWith(" [═] [║] ─┐"));
        split.resize(new Dimension.Simple(12, 60));
        assertTrue(top(), top().endsWith("( ) Right─┐"));
    }

    /** the default pictures: box drawing, in every terminal font (PuTTY had no ⊟ ◫) */
    public void testDefaultPictures() {
        assertEquals("═", FlipSplitter.pictures(Placement.BELOW));
        assertEquals("║", FlipSplitter.pictures(Placement.RIGHT));
        assertEquals("Below", FlipSplitter.words(Placement.BELOW));
    }

    /** text of your own, for either kind */
    public void testTextOfYourOwn() {
        split.control(split.chips((p) -> p == Placement.BELOW ? "Stacked" : "Side by side"));
        assertTrue(top(), top().endsWith(" [Stacked] [Side by side] ─┐"));
        split.control(split.radio((p) -> p == Placement.BELOW ? "Over" : "Beside"));
        assertTrue(top(), top().endsWith("(•) Over  ( ) Beside─┐"));
    }

    /** a control of your own: any FormInput of Placement - it is told the placement */
    public void testAControlOfYourOwn() {
        ChipGroup<Placement> mine = ChipGroup.of(new SimpleDisplayValue<Placement>("top", Placement.BELOW),
                                                 new SimpleDisplayValue<Placement>("side", Placement.RIGHT));
        split.control(mine);
        assertTrue(top(), top().endsWith(" [top] [side] ─┐"));
        split.placement(Placement.RIGHT);
        assertSame(Placement.RIGHT, mine.getValue());
    }

    /** the first that fits; none fits, the last */
    public void testFallbacksInOrder() {
        split.control(split.radio((p) -> p.word() + " and a long way round"), split.radio(), split.chips());
        assertTrue(top(), top().endsWith("(•) Below  ( ) Right─┐"));
        split.resize(new Dimension.Simple(12, 12));
        assertTrue(top(), top().endsWith(" [═] [║] ─┐"));
    }

    /** one border, joined to the divider: stacked ├ ┤, beside ┬ ┴ - not a box around boxes */
    public void testTheBorderJoinsTheDivider() {
        boolean joined = false;
        for (String row : rows()) {
            joined |= row.startsWith("├") && row.endsWith("┤");
        }
        assertTrue("stacked:\n" + picture(), joined);
        split.placement(Placement.RIGHT);
        String[] beside = rows();
        assertTrue("beside:\n" + picture(), beside[0].contains("┬") && beside[beside.length - 1].contains("┴"));
    }

    /** no border: no lines of its own but the divider */
    public void testNoBorderByDefault() {
        FlipSplitter plain = new FlipSplitter(pane("top"), pane("bottom"));
        plain.resize(new Dimension.Simple(8, 30));
        String text = new TextSnapshot(8, 30).render(plain);
        String top = text.split("\n")[0];
        assertTrue(top, top.startsWith("top"));
        assertTrue("[" + top + "]", top.endsWith(" [═] [║]   "));          // two in from the edge, as on a border
        assertFalse(text, text.contains("┌"));
    }

    /** sized by the panel holding it, on its first layout - a pane wider than the room made it wider than the panel */
    public void testItTakesThePanelsWidthFirstTime() {
        FlipSplitter wide = new FlipSplitter(pane("short"), pane("0123456789012345678901234567890123456789012345678901234567890123456789"))
                .setTitle(" T ");
        Composite main = new Composite();
        main.add(wide);
        main.onResizeExpandDownAndRight(wide);
        main.resize(new Dimension.Simple(8, 40));
        assertEquals(40, wide.getWidth());
        String top = new TextSnapshot(8, 40).render(main).split("\n")[0];
        assertTrue(top, top.endsWith("(•) Below  ( ) Right─┐"));
    }

    /** flipped beside and back: as at the start, no stray pieces; the panes are the same components */
    public void testFlippedAndBackLooksTheSame() {
        String before = picture();
        split.placement(Placement.RIGHT);
        assertFalse(picture(), picture().equals(before));
        assertTrue(top(), top().endsWith("( ) Below  (•) Right─┐"));
        split.placement(Placement.BELOW);
        assertEquals(before, picture());
        assertNotNull("the same panes, in the new splitter", chart.getParentContainer());
        assertNotNull(figures.getParentContainer());
    }

    /** the second alone, in the same border and title; the control stays, with a third choice */
    public void testSecondOnly() {
        split.offerSecondOnly(true);
        assertTrue(top(), top().endsWith("( ) Only─┐"));
        split.placement(Placement.SECOND_ONLY);
        String text = picture();
        assertFalse(text, text.contains("a line"));
        assertTrue(text, text.contains("412 sessions"));
        assertTrue(text, top().startsWith("┌ Stats ─") && top().endsWith("(•) Only─┐"));
        split.placement(Placement.BELOW);
        assertTrue(picture(), picture().contains("a line"));
    }

    public void testSecondOnlyNotOfferedThrows() {
        try {
            split.placement(Placement.SECOND_ONLY);
            fail();
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("offerSecondOnly"));
        }
    }

    /** the controls show the choices there were when they were made: offerSecondOnly after control(..) is a mistake */
    public void testSecondOnlyAfterTheControlThrows() {
        split.control(split.chips());
        try {
            split.offerSecondOnly(true);
            fail();
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("call it first"));
        }
    }

    /** the second put away: the first alone, and no control - nothing to flip */
    public void testSecondPutAway() {
        split.secondShown(false);
        assertFalse(picture(), picture().contains("412 sessions"));
        assertNull(split.getControl());
        assertFalse(picture(), picture().contains("Below"));
        split.secondShown(true);
        assertTrue(picture(), picture().contains("412 sessions"));
        assertTrue(picture(), picture().contains("Below"));
    }

    /** the share is the first pane's part, given before the splitter is sized or after - not the panes' own heights */
    public void testShareBeforeOrAfterSizing() {
        Composite a = pane("a\nb"), b = pane("x");
        FlipSplitter before = new FlipSplitter(a, b).share(40);
        before.resize(new Dimension.Simple(30, 100));
        assertEquals(12, a.getHeight());
        Composite c = pane("a\nb"), d = pane("x");
        FlipSplitter after = new FlipSplitter(c, d);
        after.resize(new Dimension.Simple(30, 100));
        assertEquals(15, c.getHeight());
        after.share(40);
        assertEquals(12, c.getHeight());
        after.placement(Placement.RIGHT);
        assertEquals("beside: 40 of 100 columns", 40, c.getWidth());
    }

    // ---- in an app: TAB, keys, clicks, watchers -------------------------------------------------------------------

    private final SyncEventLoop bus = new SyncEventLoop();
    private final CompositeWithFocusManager root = new CompositeWithFocusManager();
    private final List<Placement> heard = new ArrayList<Placement>();

    private void press(KeyStroke key) {
        bus.deliver(new KeyEvent("test", key));
    }

    private void inAnApp() throws Exception {
        split.onFlip(heard::add);
        root.add(split);
        CanvasForTesting.buildCanvasForTestingNoControlCodesInOutput().addComponentSegments(root);
        bus.setRoot(root);
    }

    // the control, if focus is on it or inside it (a radio's option); else the focused component's name
    private Object focusedControl() {
        Focusable focusable = FocusManager.UTIL.findCurrentFocus(root);
        for (Object c = focusable == null ? null : focusable.getFocusComponent(); c instanceof Component; c = ((Component<?>) c).getParentContainer()) {
            if (c == split.getControl()) {
                return c;
            }
        }
        return focusable == null ? null : HasName.UTIL.getName(focusable.getFocusComponent());
    }

    /** the radio: TAB reaches it; RIGHT flips beside, and round again; the watchers hear each; focus stays on it */
    public void testRadioKeysFlipIt() throws Exception {
        inAnApp();
        press(Keys.TAB_KEYSTROKE);
        assertSame(split.getControl(), focusedControl());
        press(Keys.RIGHT_KEYSTROKE);
        assertSame(Placement.RIGHT, split.getPlacement());
        press(Keys.RIGHT_KEYSTROKE);
        assertSame("round again", Placement.BELOW, split.getPlacement());
        assertEquals(Arrays.asList(Placement.RIGHT, Placement.BELOW), heard);
        assertSame("focus stays on it", split.getControl(), focusedControl());
    }

    /** the chips: SPACE and LEFT flip it; a click on a chip lands on it */
    public void testChipsKeysAndClicksFlipIt() throws Exception {
        split.control(split.chips());
        inAnApp();
        press(Keys.TAB_KEYSTROKE);
        assertSame(split.getControl(), focusedControl());
        press(Keys.SPACE_KEYSTROKE);
        assertSame(Placement.RIGHT, split.getPlacement());
        press(Keys.LEFT_KEYSTROKE);
        assertSame(Placement.BELOW, split.getPlacement());
        assertEquals(Arrays.asList(Placement.RIGHT, Placement.BELOW), heard);
        int start = 60 - 2 - " [═] [║] ".length() + 1;            // the first chip, past the gap
        assertSame("clicks land on it", split.getControl(), split.getComponentAtOffset(new Position.Simple(0, start)));
    }
}
