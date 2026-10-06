package dtb.terminalIO.components.composite;

import java.util.TreeSet;

import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.geom.Align;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.VAlign;
import dtb.terminalIO.geom.position.PositionRelativeFactory;
import dtb.terminalIO.textsegments.ScreenLineSegment;
import junit.framework.TestCase;

/**
 * A bordered Composite keeps its children inside the border - whether its size is fixed or it
 * grows with its children. A child that would reach the border is clipped; it never lies on top of
 * the border (overlapping segments make a row longer than it is wide on screen, which wraps in the
 * terminal and misaligns everything after it).
 * <p/>
 * Seen with MovingPicturesViewer: a growing, bordered Composite holding an image and an Options
 * button placed top-right relative to the Composite itself - the button landed on the right border.
 */
public class CompositeBorderClipTest extends TestCase {

    /** rows x cols of '#' */
    private static TextComponent block(int rows, int cols) {
        StringBuilder text = new StringBuilder();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                text.append('#');
            }
            if (r < rows - 1) {
                text.append('\n');
            }
        }
        return new TextComponent(text.toString());
    }

    /** a growing Composite with an image and a button placed top-right relative to the Composite */
    private static Composite withTopRightButton(boolean borderFirst) {
        Composite composite = new Composite(Colors.DEFAULT_COLOR);
        if (borderFirst) {
            composite.setBorder(true);
        }
        composite.add(block(4, 10));
        Component<?> button = new TextComponent(" Opt ");
        composite.add(PositionRelativeFactory.fromAlign(VAlign.TOP, Align.RIGHT, composite, button), button);
        if (!borderFirst) {
            composite.setBorder(true);
        }
        return composite;
    }

    private static void assertNoOverlapsAndBorderIntact(Composite composite) {
        TreeSet<ScreenLineSegment> segments = composite.getSegmentsBuffer().getSegments();
        ScreenLineSegment previous = null;
        for (ScreenLineSegment segment : segments) {
            if (previous != null && previous.getRow() == segment.getRow()) {
                assertTrue("overlapping segments on row " + segment.getRow() + ": " + previous + " / " + segment,
                           segment.getCol() > previous.getRightMostCol());
            }
            previous = segment;
        }
        int right = composite.getWidth() - 1;
        for (int row = 1; row < composite.getHeight() - 1; row++) {
            assertEquals("right border on row " + row, "│", ScreenLineSegment.UTIL.getCharAt(segments, row, right));
            assertEquals("left border on row " + row, "│", ScreenLineSegment.UTIL.getCharAt(segments, row, 0));
        }
    }

    public void testGrowingCompositeKeepsARelativelyPlacedChildInsideItsBorder() {
        assertNoOverlapsAndBorderIntact(withTopRightButton(false));
    }

    public void testGrowingCompositeWithTheBorderOnFirst() {
        assertNoOverlapsAndBorderIntact(withTopRightButton(true));
    }

    /** the fixed-size mode already clipped to inside the border - keep it so */
    public void testFixedSizeCompositeClipsAChildInsideItsBorder() {
        Composite composite = new Composite(new Dimension.Simple(5, 20));
        composite.setBorder(true);
        composite.add(block(2, 25));                 // wider than the composite
        assertNoOverlapsAndBorderIntact(composite);
    }
}
