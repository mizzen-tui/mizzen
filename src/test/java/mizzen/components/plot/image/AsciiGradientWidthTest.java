package mizzen.components.plot.image;

import junit.framework.TestCase;
import mizzen.components.input.DisplayableValue;

/**
 * Every character of every ASCII-picture gradient takes exactly one column, as Mizzen assumes of all text: none that a
 * terminal draws as nothing (a soft hyphen, a combining mark, a control) and none drawn two columns wide (CJK, full
 * width). One such character in a gradient shifts the rest of each picture row it lands in.
 * <p/>
 * Characters Unicode calls "ambiguous" width (box lines, shade blocks, Latin-1 letters) are allowed: they are one
 * column outside CJK terminals, and Mizzen's borders already rely on that.
 */
public class AsciiGradientWidthTest extends TestCase {

    public void testEveryGradientCharacterIsOneColumn() {
        for (DisplayableValue<String> gradient : ImageViewerFactory.standardAsciiGradients()) {
            String chars = gradient.getValue();
            for (int i = 0; i < chars.length(); i++) {
                char ch = chars.charAt(i);
                assertTrue(String.format("U+%04X at %d of gradient \"%s\" is not one column", (int) ch, i, chars),
                        isOneColumn(ch));
            }
        }
    }

    private static boolean isOneColumn(char ch) {
        switch (Character.getType(ch)) {
            case Character.FORMAT:                       // soft hyphen, zero-width space and joiners, direction marks
            case Character.CONTROL:
            case Character.NON_SPACING_MARK:             // combining accents: drawn on the character before
            case Character.ENCLOSING_MARK:
            case Character.COMBINING_SPACING_MARK:
            case Character.SURROGATE:                    // half of a character outside the BMP
            case Character.PRIVATE_USE:
            case Character.UNASSIGNED:
                return false;
            default:
                return !isWide(ch);
        }
    }

    // the East Asian wide and full-width blocks: two columns in every terminal
    private static boolean isWide(char ch) {
        return (ch >= 0x1100 && ch <= 0x115F)            // Hangul Jamo initials
                || (ch >= 0x2E80 && ch <= 0xA4CF)        // CJK radicals .. Yi
                || (ch >= 0xAC00 && ch <= 0xD7A3)        // Hangul syllables
                || (ch >= 0xF900 && ch <= 0xFAFF)        // CJK compatibility ideographs
                || (ch >= 0xFE30 && ch <= 0xFE4F)        // CJK compatibility forms
                || (ch >= 0xFF00 && ch <= 0xFF60)        // full-width forms
                || (ch >= 0xFFE0 && ch <= 0xFFE6);
    }
}
