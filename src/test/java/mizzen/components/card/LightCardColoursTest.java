package mizzen.components.card;

import mizzen.components.composite.HorizontalPanel;
import mizzen.components.theme.Theme;
import mizzen.controlcodes.color.Color;
import mizzen.textsegments.ScreenLineSegment;
import junit.framework.TestCase;

/**
 * Light cards on a dark screen: laid on the card, text keeps the screen's light foreground and can't be read. The
 * theme gives the card's own text colours with cards(..).
 */
public class LightCardColoursTest extends TestCase {

    private static final Color CARD = Theme.rgb(0x22313A, 0xF4F7F8);          // dark text on near-white
    private static final Theme SCREEN = Theme.ROUNDED
            .withTextColor(Theme.rgb(0xE6EEF2, 0x1D4F4A))                      // light text on teal
            .withStrongColor(Theme.rgb(0xF0F4F8, 0x1D4F4A))
            .withSurfaceColor(CARD);

    public void testWithoutCardsTheTextStaysLight() {
        assertTrue(colourOf("HTTP 200", SCREEN), colourOf("HTTP 200", SCREEN).contains("38;2;240;244;248"));
    }

    public void testCardsGivesTheCardsTextColours() {
        Theme theme = SCREEN.cards(c -> c.withTextColor(CARD).withStrongColor(Theme.rgb(0x0B1A21, 0xF4F7F8))
                                          .withHeadingColor(Theme.rgb(0x1D4F4A, 0xF4F7F8)));
        String value = colourOf("HTTP 200", theme);
        assertTrue(value, value.contains("38;2;11;26;33") && value.contains("48;2;244;247;248"));
        String title = colourOf("Site", theme);                               // a card frames and titles in text
        assertTrue(title, title.contains("38;2;34;49;58") && title.contains("48;2;244;247;248"));
        assertTrue("the screen keeps its own text", theme.textColor().asString().contains("38;2;230;238;242"));
    }

    public void testCardsWithoutASurfaceChangeTheCardsToo() {
        Theme theme = Theme.ROUNDED.cards(c -> c.withHeadingColor(CARD));
        assertSame(CARD, theme.surfaceTheme().headingColor());
    }

    // the colour codes of the segment drawing text, in a tile on a screen in theme
    private static String colourOf(String text, Theme theme) {
        HorizontalPanel screen = new HorizontalPanel();
        Tile site = new Tile("Site", 16).value("HTTP 200");
        screen.add(site);
        screen.setTheme(theme);
        for (ScreenLineSegment s : site.getSegmentsBuffer().getSegments()) {
            if (s.getText().contains(text)) {
                return s.getColors().asString();
            }
        }
        throw new AssertionError("no '" + text + "' drawn");
    }
}
