package dtb.terminalIO.components;

import dtb.terminalIO.components.progressmeter.ProgressMeter;
import dtb.terminalIO.components.progressmeter.ProgressMeter.Layout;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.textsegments.ScreenLineSegment;
import junit.framework.TestCase;

/**
 * WindowFrame.setFooterOnBorder: one row on the bottom border line, from one in on the left (the
 * corner shows) to just before the resize corner, kept that wide as the window is resized.
 */
public class WindowFrameFooterOnBorderTest extends TestCase {

    private final ProgressMeter meter = new ProgressMeter(10).setLayout(Layout.ONE_ROW);
    private final WindowFrame window = new WindowFrame(Colors.Cya, " Output ", new TextComponent(new String(new char[50]).replace('\0', '.')));

    private String bottomRow() {
        int bottom = window.getHeight() - 1;
        StringBuilder row = new StringBuilder();
        for (ScreenLineSegment s : window.getSegmentsBuffer().getSegments()) {
            if (s.getRow() == bottom) {
                row.append(s.getText());
            }
        }
        return row.toString();
    }

    public void testItSitsOnTheBorderLineBetweenTheCornerAndTheResizeCorner() {
        int height = window.getHeight();
        meter.setMessage("Copying");
        window.setFooterOnBorder(meter);
        assertEquals("the window doesn't grow", height, window.getHeight());
        assertEquals(window.getWidth() - 2 - 3, meter.getWidth());
        String bottom = bottomRow();
        assertTrue(bottom, bottom.substring(1).startsWith("Copying"));
    }

    public void testItFollowsTheWindowsWidth() {
        window.setFooterOnBorder(meter);
        window.resize(new Dimension.Simple(window.getHeight(), 80));
        assertEquals(80 - 2 - 3, meter.getWidth());
        window.resize(new Dimension.Simple(window.getHeight(), 40));
        assertEquals(40 - 2 - 3, meter.getWidth());
    }

    public void testATwoRowComponentIsRefused() {
        try {
            window.setFooterOnBorder(new ProgressMeter(10));        // two rows by default
            fail();
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("one row"));
        }
    }
}
