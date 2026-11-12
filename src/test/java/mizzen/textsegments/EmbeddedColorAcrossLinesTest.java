package mizzen.textsegments;

import mizzen.components.TextComponent;
import mizzen.controlcodes.color.ColorParsingUtils;
import mizzen.controlcodes.color.Colors;
import junit.framework.TestCase;

/**
 * A colour code at the very end of a line - nothing after it - still sets the colour: the next line starts in it. The
 * Java highlighter ends a // comment with a reset; the line after it used to start in the comment's colour.
 */
public class EmbeddedColorAcrossLinesTest extends TestCase {

    public void testACodeEndingTheStringSetsTheColour() {
        String reset = Colors.RCol.asString();
        assertEquals(reset, ColorParsingUtils.parseColorsAndBuildResult(Colors.IBla.asString() + "// a comment" + reset,
                (color, text) -> true).asString());
    }

    public void testTheLineAfterATrailingResetStartsReset() {
        String text = Colors.IBla.asString() + "// a comment" + Colors.RCol.asString() + "\nplain";
        TextComponent component = new TextComponent(text);
        for (ScreenLineSegment segment : component.getSegmentsBuffer().getSegments()) {
            if (segment.getText().startsWith("plain")) {
                assertFalse("'plain' in the comment's colour", segment.getColors().asString().equals(Colors.IBla.asString()));
                return;
            }
        }
        fail("no segment for 'plain'");
    }

    // a // comment wider than its component: cut at the edge, its reset is past the cut, yet the next line starts reset
    public void testTheLineAfterACutCommentStartsReset() {
        String text = Colors.IBla.asString() + "// a long comment" + Colors.RCol.asString() + "\nplain";
        for (ScreenLineSegment segment : ScreenLineSegment.UTIL.segmentsOf(text, Colors.FGBGColors.NO_COLOR_SPECIFIED,
                new mizzen.geom.Rectangle.Simple(0, 0, 2, 8))) {
            if (segment.getText().startsWith("plain")) {
                assertFalse("'plain' in the comment's colour", segment.getColors().asString().equals(Colors.IBla.asString()));
                return;
            }
            assertEquals("cut at the width", "// a lon", segment.getText());
        }
        fail("no segment for 'plain'");
    }
}
