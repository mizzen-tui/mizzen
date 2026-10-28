package mizzen.components.composite;

import mizzen.components.TextComponent;
import mizzen.controlcodes.color.Colors;
import mizzen.geom.Dimension;
import mizzen.textsegments.ScreenLineSegment;
import junit.framework.TestCase;

/**
 * A child kept on top stays above its siblings: one added after it, one brought to the front (as focus
 * does), one moved up. Three texts on the same spot - the drawn character says which is on top.
 */
public class KeepOnTopTest extends TestCase {

    private final Composite area = new Composite(new Dimension.Simple(1, 10));
    private final TextComponent a = new TextComponent(Colors.Whi, "AAAA");
    private final TextComponent bar = new TextComponent(Colors.Whi, "BB");

    @Override
    protected void setUp() throws Exception {
        area.add(0, 0, a);
        area.add(0, 0, bar);
        area.keepOnTop(bar);
    }

    // the character drawn at column 0
    private char onTop() {
        for (ScreenLineSegment segment : area.getSegmentsBuffer().getSegments()) {
            if (segment.getRow() == 0 && segment.getCol() <= 0 && segment.getRightMostCol() >= 0 && segment.getText().trim().length() > 0) {
                return segment.getText().charAt(-segment.getCol());
            }
        }
        return ' ';
    }

    public void testAChildAddedAfterItStaysUnder() {
        area.add(0, 0, new TextComponent(Colors.Whi, "CCCC"));
        assertEquals('B', onTop());
    }

    public void testBringingAnotherToTheFrontLeavesItUnder() {
        TextComponent c = new TextComponent(Colors.Whi, "CCCC");
        area.add(0, 0, c);
        area.bringToFront(c);                           // as focus does
        assertEquals('B', onTop());
        area.bringToFront(a);
        assertEquals('B', onTop());
    }

    public void testMovingItDownDoesNotBuryIt() {
        area.bringToBack(bar);
        assertEquals('B', onTop());
    }

    /** it floats: a sibling that fills its parent fills under it, leaving it no room */
    public void testAFillingSiblingFillsUnderIt() {
        Composite panel = new Composite(new Dimension.Simple(5, 30));
        TextComponent floating = new TextComponent(Colors.Whi, "bar");
        panel.add(0, 20, floating);
        panel.keepOnTop(floating);
        TextComponent filler = new TextComponent(Colors.Whi, new Dimension.Simple(1, 1), "x");
        panel.add(0, 0, filler);
        panel.onResizeExpandDownAndRight(filler);
        panel.applyResizeRules();
        assertEquals("the whole width, under the bar", 30, filler.getWidth());
    }

    /** without keepOnTop: the last raised is on top - the behaviour keepOnTop changes */
    public void testWithoutItTheRaisedOneIsOnTop() {
        Composite plain = new Composite(new Dimension.Simple(1, 10));
        TextComponent x = new TextComponent(Colors.Whi, "XX"), y = new TextComponent(Colors.Whi, "YYYY");
        plain.add(0, 0, x);
        plain.add(0, 0, y);
        plain.bringToFront(y);
        char shown = ' ';
        for (ScreenLineSegment segment : plain.getSegmentsBuffer().getSegments()) {
            if (segment.getRow() == 0 && segment.getCol() == 0) {
                shown = segment.getText().charAt(0);
            }
        }
        assertEquals('Y', shown);
    }
}
