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

    public void testALeftFooterStartsOneInSoTheCornerShows() {
        BorderedComponent box = new BorderedComponent(Colors.Yel, new TextComponent(new String(new char[30]).replace('\0', 'x')));
        box.setFooter(new TextComponent("status"), Align.LEFT);
        String bottom = bottomRow(box);
        assertTrue(bottom, bottom.startsWith("└status"));
    }
}
