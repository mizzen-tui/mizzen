package dtb.terminalIO.controlcodes.color;

import dtb.terminalIO.controlcodes.ControlCode;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.textsegments.CleanString;
import junit.framework.TestCase;

public class ColorParsingUtilsTests extends TestCase {

    private String someSizeText() {
        Dimension someSize = new Dimension.Simple(10, 10);
        return String.valueOf(someSize);
    }

    private String problemColoredStr1() {
        String coloredTextThePrevioislyCauseAProblem = Colors.coloredText(Colors.BGre, "Loading image ..  Size: ")
                + Colors.coloredText(Colors.Yel, someSizeText());
        return coloredTextThePrevioislyCauseAProblem;
    }

    public void testProblemStr1OnColor() {

        ColorParsingUtils.parseColorsAndBuildResult(problemColoredStr1(),
                new ColorParsingUtils.ColorColorSegmentBuilder() {

                    int counter = 0;

                    @Override
                    public boolean newColorSegment(Color color, CleanString simpleText) {
                        boolean continueParsing = true;

                        assertNotNull("Color null on pass: " + counter, color);

                        if (counter == 0) {
                            assertEquals("First Segment does not equal BGre instead was: " + color.toString(), 0,
                                    Color.COLOR_COMPARE.compare(Colors.BGre, color));
                        }

                        if (counter == 1) {
                            assertEquals("2nd Segment does not equal RCol instead was: " + color.toString(), 0,
                                    Color.COLOR_COMPARE.compare(Colors.RCol, color));
                        }
                        if (counter == 2) {
                            assertEquals("3rd Segment does not equal Yel instead was: " + color.toString(), 0,
                                    Color.COLOR_COMPARE.compare(Colors.Yel, color));
                        }
                        if (counter > 2) {
                            fail("Too many parsed segments!!");
                        }

                        counter++;

                        return continueParsing;
                    }
                });

    }

    public void testProblemStr1OnText() {

        ColorParsingUtils.parseColorsAndBuildResult(problemColoredStr1(),
                new ColorParsingUtils.ColorColorSegmentBuilder() {

                    int counter = 0;

                    @Override
                    public boolean newColorSegment(Color color, CleanString simpleText) {
                        boolean continueParsing = true;

                        assertNotNull("Color null on pass: " + counter, color);

                        if (counter == 0) {
                            assertEquals("Loading image ..  Size: ", simpleText.getText());
                        }

                        if (counter == 1) {
                            assertEquals("", simpleText.getText());
                        }
                        if (counter == 2) {
                            assertEquals(someSizeText(), simpleText.getText());
                        }
                        if (counter > 2) {
                            fail("Too many parsed segments!!");
                        }

                        counter++;

                        return continueParsing;
                    }
                });
    }

    public void testRCOlWithNoColorCode() {

        ColorParsingUtils.parseColorsAndBuildResult(ControlCode.escape_PREFIX+"mMy Name is David",
                new ColorParsingUtils.ColorColorSegmentBuilder() {

                    int counter = 0;

                    @Override
                    public boolean newColorSegment(Color color, CleanString simpleText) {
                        boolean continueParsing = true;

                        assertNotNull("Color null on pass: " + counter, color);

                        if (counter == 0) {
                            assertEquals("Loading image ..  Size: ", simpleText.getText());
                        }

                        if (counter == 1) {
                            assertEquals("", simpleText.getText());
                        }
                        if (counter == 2) {
                            assertEquals(someSizeText(), simpleText.getText());
                        }
                        if (counter > 2) {
                            fail("Too many parsed segments!!");
                        }

                        counter++;

                        return continueParsing;
                    }
                });

    }

}
