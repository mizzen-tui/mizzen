package mizzen.components.card;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import mizzen.components.Badge;
import mizzen.components.Component;
import mizzen.components.TextComponent;
import mizzen.components.composite.VerticalPanel;
import mizzen.components.theme.Theme;
import mizzen.textsegments.ScreenLineSegment;
import junit.framework.TestCase;

/** a card frames its rows on the theme's surface with a column of room each side; a badge is a dot in a theme colour and text */
public class CardTest extends TestCase {

    private static List<String> lines(Component<?> component) {
        TreeMap<Integer, StringBuilder> rows = new TreeMap<Integer, StringBuilder>();
        for (ScreenLineSegment s : component.getSegmentsBuffer().getSegments()) {
            StringBuilder line = rows.computeIfAbsent(s.getRow(), (r) -> new StringBuilder());
            while (line.length() < s.getCol() + s.getText().length()) {
                line.append(' ');
            }
            line.replace(s.getCol(), s.getCol() + s.getText().length(), s.getText());
        }
        List<String> out = new ArrayList<String>();
        for (StringBuilder line : rows.values()) {
            out.add(line.toString());
        }
        return out;
    }

    public void testRowsFramedWithRoom() {
        VerticalPanel screen = new VerticalPanel();
        Card card = new Card("Site").add(new TextComponent("HTTP 200")).add(new TextComponent("up"));
        screen.add(card);
        screen.setTheme(Theme.ROUNDED);
        List<String> shown = lines(card);
        assertEquals(shown.toString(), "╭ Site ────╮", shown.get(0).trim());
        assertEquals("│ HTTP 200 │", shown.get(1));
        assertEquals("│ up       │", shown.get(2));
        assertEquals("╰──────────╯", shown.get(3));
    }

    public void testWhatItHoldsIsOnTheSurface() {
        VerticalPanel screen = new VerticalPanel();
        TextComponent text = new TextComponent("HTTP 200");
        screen.add(new Card("Site").add(text));
        screen.setTheme(Theme.ROUNDED);
        assertSame(Theme.ROUNDED.surfaceTheme().textColor(), text.getColor());
        assertTrue(text.getColor().asString().endsWith("48;2;43;56;70m"));
    }

    public void testLegacyIsABorderedPanel() {
        VerticalPanel screen = new VerticalPanel();
        TextComponent text = new TextComponent("HTTP 200");
        Card card = new Card().add(text);
        screen.add(card);
        assertSame(Theme.LEGACY.textColor(), text.getColor());
        assertTrue(lines(card).get(0).startsWith("┌"));
    }

    public void testABadgesDotIsItsThemeColour() {
        VerticalPanel screen = new VerticalPanel();
        Badge ok = new Badge(Theme::successColor, "OK");
        screen.add(ok);
        screen.setTheme(Theme.DARK);
        assertEquals("● OK", lines(ok).get(0));
        boolean dot = false;
        for (ScreenLineSegment s : ok.getSegmentsBuffer().getSegments()) {
            dot |= s.getText().equals("●") && s.getColors() == Theme.DARK.successColor();
        }
        assertTrue(dot);
    }
}
