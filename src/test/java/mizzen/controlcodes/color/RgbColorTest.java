package mizzen.controlcodes.color;

import java.util.Arrays;

import mizzen.controlcodes.color.Colors.FGBGColors;
import mizzen.controlcodes.color.Colors.RGBColorFGBG;
import junit.framework.TestCase;

/**
 * RGB colours: a pair gives back its own foreground and background; merging puts the first colour's
 * foreground over the second's background, named or RGB alike; a merge changes neither colour it was given.
 */
public class RgbColorTest extends TestCase {

    private static final Color PAIR = RGBColorFGBG.instance(200, 201, 202, 30, 31, 32);

    // one escape sequence carrying both colours' codes, foreground first
    private static String codes(Color foreground, Color background) {
        int[] f = foreground.getControlCodes(), b = background.getControlCodes();
        int[] both = Arrays.copyOf(f, f.length + b.length);
        System.arraycopy(b, 0, both, f.length, b.length);
        return Arrays.toString(both);
    }

    public void testAPairGivesItsOwnParts() {
        assertEquals(Colors.fromRGBFG(200, 201, 202).asString(), PAIR.getForeGroundColor().asString());
        assertEquals(Colors.fromRGBBG(30, 31, 32).asString(), PAIR.getBackGroundColor().asString());
    }

    public void testANamedForegroundOverAnRgbBackground() {
        Color merged = Colors.BYel.mergeWith(PAIR);
        assertEquals(codes(Colors.BYel, Colors.fromRGBBG(30, 31, 32)), Arrays.toString(merged.getControlCodes()));
    }

    public void testAnRgbPairOverANamedBackground() {
        Color merged = PAIR.mergeWith(new FGBGColors(Colors.Whi, Colors.On_Blu));
        assertEquals(codes(Colors.fromRGBFG(200, 201, 202), Colors.On_Blu), Arrays.toString(merged.getControlCodes()));
    }

    public void testTwoRgbPairsMakeAnRgbPair() {
        Color merged = PAIR.mergeWith(RGBColorFGBG.instance(1, 2, 3, 40, 41, 42));
        assertEquals(RGBColorFGBG.instance(200, 201, 202, 40, 41, 42).asString(), merged.asString());
    }

    /** a foreground over a pair: the foreground's colour on the pair's background - and the pair itself unchanged */
    public void testAForegroundOverANamedPairKeepsTheForegroundAndChangesNeither() {
        FGBGColors pair = new FGBGColors(Colors.Gre, Colors.On_Blu);
        Color merged = Colors.BYel.mergeWith(pair);
        assertEquals(new FGBGColors(Colors.BYel, Colors.On_Blu).asString(), merged.asString());
        assertEquals("the pair given is not changed", new FGBGColors(Colors.Gre, Colors.On_Blu).asString(), pair.asString());
    }
}
