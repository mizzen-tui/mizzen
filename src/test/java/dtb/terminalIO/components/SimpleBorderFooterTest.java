package dtb.terminalIO.components;

import dtb.terminalIO.components.input.MultiLineTextInput;
import dtb.terminalIO.components.scroll.ScrollableComponent;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.geom.Align;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.textsegments.ScreenLineSegment;
import junit.framework.TestCase;

/**
 * A component on a border's bottom line: drawn between the corners, and not at all when it can't
 * fit - a scrollable narrower than its footer buttons used to draw them off its left edge.
 */
public class SimpleBorderFooterTest extends TestCase {

    @Override
    protected void setUp() {
        Component.UTIL.COMPONENT_SEGMENTS_INTEGRITY_CHECK_FLAG = true;    // segments outside a component's bounds throw
    }

    @Override
    protected void tearDown() {
        Component.UTIL.COMPONENT_SEGMENTS_INTEGRITY_CHECK_FLAG = false;
    }

    private static String bottomRow(Component<?> c) {
        int bottom = c.getHeight() - 1;
        StringBuilder row = new StringBuilder();
        for (ScreenLineSegment s : c.getSegmentsBuffer().getSegments()) {
            if (s.getRow() == bottom) {
                row.append(s.getText());
            }
        }
        return row.toString();
    }

    public void testANarrowScrollableDropsFooterButtonsThatDontFit() {
        ScrollableComponent narrow = new ScrollableComponent(Colors.Yel, new Dimension.Simple(8, 20), new MultiLineTextInput(Colors.IYel));
        narrow.getSegmentsBuffer();                                         // threw before: footer at a negative column
        assertFalse(bottomRow(narrow), bottomRow(narrow).contains("PgUp"));
    }

    public void testAScrollableWideEnoughKeepsItsFooterButtons() {
        ScrollableComponent wide = new ScrollableComponent(Colors.Yel, new Dimension.Simple(8, 50), new MultiLineTextInput(Colors.IYel));
        assertTrue(bottomRow(wide), bottomRow(wide).contains("PgUp"));
    }

    private static String row(Component<?> c, int r) {
        StringBuilder row = new StringBuilder();
        for (ScreenLineSegment s : c.getSegmentsBuffer().getSegments()) {
            if (s.getRow() == r) {
                row.append(s.getText());
            }
        }
        return row.toString();
    }

    /** squeezed to one row there is room for one line: with no footer, the top one with its title */
    public void testOneRowWithoutAFooterShowsTheTitleLine() {
        SimpleBorder border = new SimpleBorder(Colors.Yel, new Dimension.Simple(1, 30));
        border.setTitle("Output");
        String line = row(border, 0);                                    // overlaps would throw (strict check)
        assertTrue(line, line.startsWith("┌") && line.endsWith("┐") && line.contains("Output"));
    }

    /** ... and with a footer, the bottom one with the footer - it carries live things */
    public void testOneRowWithAFooterShowsTheFooterLine() {
        SimpleBorder border = new SimpleBorder(Colors.Yel, new Dimension.Simple(1, 30));
        border.setTitle("Output");
        border.setFooter(new TextComponent("42%"), Align.LEFT);
        String line = row(border, 0);
        assertTrue(line, line.startsWith("└42%") && line.endsWith("┘") && !line.contains("Output"));
    }

    public void testAScrollableSqueezedToOneRowShowsItsFooterLine() {
        ScrollableComponent pane = new ScrollableComponent(Colors.Yel, new Dimension.Simple(10, 60), new MultiLineTextInput(Colors.IYel));
        pane.setTitle(" Output ");
        pane.getSegmentsBuffer();
        pane.resize(new Dimension.Simple(1, 60));
        String line = row(pane, 0);
        assertTrue(line, line.startsWith("└") && line.endsWith("┘") && line.contains("PgUp"));
    }

    public void testAScrollableWithoutAFooterSqueezedToOneRowShowsItsTitleLine() {
        ScrollableComponent pane = new ScrollableComponent(Colors.Yel, new Dimension.Simple(10, 60), new MultiLineTextInput(Colors.IYel));
        pane.setTitle(" Output ");
        pane.removeFooter();
        pane.getSegmentsBuffer();
        pane.resize(new Dimension.Simple(1, 60));
        String line = row(pane, 0);
        assertTrue(line, line.startsWith("┌") && line.contains("Output"));
    }

    public void testALeftFooterStartsOneInSoTheCornerShows() {
        BorderedComponent box = new BorderedComponent(Colors.Yel, new TextComponent(new String(new char[30]).replace('\0', 'x')));
        box.setFooter(new TextComponent("status"), Align.LEFT);
        String bottom = bottomRow(box);
        assertTrue(bottom, bottom.startsWith("└status"));
    }
}
