package mizzen.components.input;

import mizzen.core.TextSnapshot;
import junit.framework.TestCase;

/** A number input as wide as its numbers: 1500 in 3 columns shows as 150; given 6 columns it shows whole. */
public class NumberInputWidthTest extends TestCase {

    public void testThreeColumnsByDefault() {
        IntegerInputWithIncrDecrButtons number = new IntegerInputWithIncrDecrButtons(null, "", "-", "+");
        number.setValue(1500);
        String drawn = TextSnapshot.of(number);
        assertTrue(drawn, drawn.contains("150") && !drawn.contains("1500"));
    }

    public void testAWidthGivenShowsTheWholeNumber() {
        IntegerInputWithIncrDecrButtons number = new IntegerInputWithIncrDecrButtons(null, "", "-", "+", 6);
        number.setValue(1500);
        assertTrue(TextSnapshot.of(number), TextSnapshot.of(number).contains("1500"));
    }

    public void testFloatTakesAWidthToo() {
        FloatInputWithIncrDecrButtons number = new FloatInputWithIncrDecrButtons(null, "", "-", "+", 8);
        number.setValue(1234.5f);
        assertTrue(TextSnapshot.of(number), TextSnapshot.of(number).contains("1234.5"));
    }
}
