package mizzen.components.table;

import java.util.Arrays;

import mizzen.components.composite.VerticalPanel;
import mizzen.components.theme.Theme;
import mizzen.geom.Dimension;
import mizzen.textsegments.ScreenLineSegment;
import junit.framework.TestCase;

/** a light table: a rule under the heading and nothing else; striped rows on the theme's stripe colour */
public class LightTableTest extends TestCase {

    private static TableComponent<String> table(Theme theme) {
        TableComponent<String> table = new TableComponent<String>(new ObjectTableModel<String>()
                .column("Name", (s) -> s).column("Size", (s) -> "" + s.length())
                .setRows(Arrays.asList("alpha", "beta", "gamma", "delta")));
        table.setLinesStyle(TableComponent.TableLinesStyle.HEADER_RULE);
        table.setSpacedRows(false);
        VerticalPanel screen = new VerticalPanel();
        screen.add(table);
        table.resize(new Dimension.Simple(6, 20));
        screen.setTheme(theme);
        return table;
    }

    public void testOnlyARuleUnderTheHeading() {
        StringBuilder all = new StringBuilder();
        for (ScreenLineSegment segment : table(Theme.DARK).getSegmentsBuffer().getSegments()) {
            all.append(segment.getText());
        }
        assertTrue(all.toString(), all.toString().contains("─"));
        assertFalse("no lines between columns: " + all, all.toString().contains("│") || all.toString().contains("┼"));
    }

    public void testStripedRowsOnTheThemesStripe() {
        TableComponent<String> table = table(Theme.DARK).setStripedRows(true);
        String stripe = "48;2;44;51;61";                                  // Dark's stripe, 0x2C333D
        boolean beta = false, alpha = false;
        for (ScreenLineSegment segment : table.getSegmentsBuffer().getSegments()) {
            String codes = segment.getColors().asString();
            beta |= segment.getText().contains("beta") && codes.contains(stripe);
            alpha |= segment.getText().contains("alpha") && codes.contains(stripe);
        }
        assertTrue("the second row on the stripe", beta);
        assertFalse("the first not", alpha);
    }

    public void testLegacyHasNoStripe() {
        TableComponent<String> table = table(Theme.LEGACY).setStripedRows(true);
        assertNull(Theme.LEGACY.stripeColor());
        assertTrue(table.getSegmentsBuffer().getSegments().size() > 0);
    }
    public void testCellsAreInTheTablesThemeOnACard() {
        TableComponent<String> table = new TableComponent<String>(new ObjectTableModel<String>()
                .column("Name", (st) -> st).setRows(Arrays.asList("alpha")));
        table.resize(new Dimension.Simple(7, 12));                        // drawn once before it is on the card
        table.getSegmentsBuffer();
        VerticalPanel screen = new VerticalPanel();
        screen.add(new mizzen.components.card.Card().add(table));
        screen.setTheme(Theme.ROUNDED);
        boolean onSurface = false;
        for (ScreenLineSegment segment : table.getSegmentsBuffer().getSegments()) {
            onSurface |= segment.getText().contains("alpha") && segment.getColors().asString().contains("48;2;43;56;70");
        }
        StringBuilder seen = new StringBuilder();
        for (ScreenLineSegment segment : table.getSegmentsBuffer().getSegments()) {
            seen.append("[").append(segment.getText()).append("|").append(segment.getColors().asString().replace("\u001b", "E")).append("] ");
        }
        assertTrue("the cell on the card's surface, 0x2B3846: " + seen, onSurface);
    }
}
