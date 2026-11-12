package mizzen.textsegments;

import mizzen.controlcodes.color.Colors;
import mizzen.geom.Rectangle;
import junit.framework.TestCase;

/**
 * Coloured runs back to back - a reset then straight away the next colour, with no text between - each show once. A
 * code highlighter writes them so: "$USER" in a bash string is green ", reset, yellow $USER. The text before such a
 * pair used to show twice: green "B" then yellow "C" read "BBC".
 */
public class EmbeddedColorBackToBackTest extends TestCase {

    public void testAResetThenAColourShowsTheRunBeforeOnce() {
        String g = Colors.Gre.asString(), y = Colors.Yel.asString(), r = Colors.RCol.asString();
        assertEquals("[0:a][1:B][2:C][3:d]", segments("a" + g + "B" + r + y + "C" + r + "d"));
        assertEquals("[0:\"][1:$V][3: x\"]", segments(g + "\"" + r + y + "$V" + r + g + " x\"" + r));
    }

    // each segment as [column:text], in order
    private static String segments(String text) {
        StringBuilder out = new StringBuilder();
        for (ScreenLineSegment segment : ScreenLineSegment.UTIL.segmentsOf(text, Colors.FGBGColors.NO_COLOR_SPECIFIED,
                new Rectangle.Simple(0, 0, 1, 40))) {
            out.append('[').append(segment.getCol()).append(':').append(segment.getText()).append(']');
        }
        return out.toString();
    }
}
