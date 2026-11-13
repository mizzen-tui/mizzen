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
import mizzen.components.frameset.FlipSplitter.Style;
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
 * Two panes flipping between stacked and beside; the control floats at the top right. Layout: the splitter, titled
 * " Stats " (so bordered), holding chart and figures - panes with no border of their own.
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

    /** small by default, at the top right: ending two in, so the corner and a line show past it */
    public void testTheControlFloatsTopRightTersely() {
        String top = rows()[0];
        assertTrue(top, top.startsWith("┌ Stats ─"));
        assertTrue(top, top.endsWith("─▀ ▌─┐"));
        assertEquals(Style.TERSE, split.getControl().getShownStyle());
        assertSame(Placement.BELOW, split.getPlacement());
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
        plain.resize(new Dimension.Simple(8, 40));
        String text = new TextSnapshot(8, 40).render(plain);
        String top = text.split("\n")[0];
        assertTrue(top, top.startsWith("top"));
        assertTrue("[" + top + "]", top.endsWith("▀ ▌  "));          // two in from the edge, as on a border
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
        assertTrue(top, top.endsWith("▀ ▌─┐"));
    }

    /** flipped beside and back: as at the start, no stray pieces; the panes are the same components */
    public void testFlippedAndBackLooksTheSame() {
        String before = picture();
        split.placement(Placement.RIGHT);
        assertFalse(picture(), picture().equals(before));
        split.placement(Placement.BELOW);
        assertEquals(before, picture());
        assertNotNull("the same panes, in the new splitter", chart.getParentContainer());
        assertNotNull(figures.getParentContainer());
    }

    public void testStyleWidths() {
        split.resize(new Dimension.Simple(12, 120));
        assertEquals(3, split.getControl().getWidth());
        split.controlStyle(Style.PLAIN, null);
        assertEquals(20, split.getControl().getWidth());
        split.controlStyle(Style.WORDY, "Figures");
        assertEquals(22 + 2 * "Figures".length(), split.getControl().getWidth());
    }

    /** block elements, in every terminal font: PuTTY draws from one font only, and had no ⊟ ◫ (U+229F, U+25EB) */
    public void testTerseGlyphs() {
        assertEquals(0x2580, Placement.BELOW.glyph());
        assertEquals(0x258C, Placement.RIGHT.glyph());
        assertEquals(0x2588, Placement.SECOND_ONLY.glyph());
    }

    /** narrower: a terser style, never cut; wider again: the one asked for */
    public void testNarrowDropsAStyleWiderBringsItBack() {
        split.controlStyle(Style.WORDY, "Figures");
        split.resize(new Dimension.Simple(12, 120));
        assertEquals(Style.WORDY, split.getControl().getShownStyle());
        split.resize(new Dimension.Simple(12, 50));
        assertEquals(Style.PLAIN, split.getControl().getShownStyle());
        split.resize(new Dimension.Simple(12, 30));
        assertEquals(Style.TERSE, split.getControl().getShownStyle());
        split.resize(new Dimension.Simple(12, 120));
        assertEquals(Style.WORDY, split.getControl().getShownStyle());
        assertTrue(picture(), rows()[0].endsWith("(•) Figures below  ( ) Figures right─┐"));
    }

    /** the second alone, in the same border and title; the control stays, with a third choice */
    public void testSecondOnly() {
        split.offerSecondOnly(true);
        assertEquals("▀ ▌ █", 5, split.getControl().getWidth());
        split.placement(Placement.SECOND_ONLY);
        String text = picture();
        assertFalse(text, text.contains("a line"));
        assertTrue(text, text.contains("412 sessions"));
        assertTrue(text, rows()[0].startsWith("┌ Stats ─") && rows()[0].endsWith("▀ ▌ █─┐"));
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

    /** the second put away: the first alone, and no control - nothing to flip */
    public void testSecondPutAway() {
        split.secondShown(false);
        assertFalse(picture(), picture().contains("412 sessions"));
        assertFalse(picture(), picture().contains("▀"));
        split.secondShown(true);
        assertTrue(picture(), picture().contains("412 sessions"));
        assertTrue(picture(), picture().contains("▀"));
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

    private void press(KeyStroke key) {
        bus.deliver(new KeyEvent("test", key));
    }

    private String focused() {
        Focusable focusable = FocusManager.UTIL.findCurrentFocus(root);
        return focusable == null ? null : HasName.UTIL.getName(focusable.getFocusComponent());
    }

    /** TAB reaches the control; RIGHT flips beside, SPACE goes round; the watchers hear each; clicks land on it */
    public void testKeysFlipIt() throws Exception {
        List<Placement> heard = new ArrayList<Placement>();
        split.onFlip(heard::add);
        root.add(split);
        CanvasForTesting.buildCanvasForTestingNoControlCodesInOutput().addComponentSegments(root);
        bus.setRoot(root);
        press(Keys.TAB_KEYSTROKE);
        assertEquals("flipControl", focused());
        press(Keys.RIGHT_KEYSTROKE);
        assertSame(Placement.RIGHT, split.getPlacement());
        press(Keys.RIGHT_KEYSTROKE);
        assertSame("at the end it stays", Placement.RIGHT, split.getPlacement());
        press(Keys.SPACE_KEYSTROKE);
        assertSame("round again", Placement.BELOW, split.getPlacement());
        assertEquals(Arrays.asList(Placement.RIGHT, Placement.BELOW), heard);
        assertEquals("focus stays on it", "flipControl", focused());
        assertSame("clicks land on it", split.getControl(), split.getComponentAtOffset(new Position.Simple(0, 60 - 4)));
    }
}
