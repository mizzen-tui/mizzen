package mizzen.components.scroll;

import mizzen.components.composite.Composite;
import mizzen.components.input.MultiLineTextInput;
import mizzen.controlcodes.color.Colors;
import mizzen.geom.Dimension;
import mizzen.geom.position.Position;
import mizzen.supportfortesting.CanvasForTesting;
import mizzen.supportfortesting.SyncEventLoop;
import junit.framework.TestCase;

import static mizzen.events.mouse.MouseEventsForTesting.click;

/**
 * A click in a scrollable's viewing area that misses the text shown - past a short line, below the
 * last one - still puts the caret there (as near as the text allows), scrolling allowed for.
 * Layout: the scrollable at 0, 0 - its border, then the view from 1, 1.
 */
public class ScrollableClickTest extends TestCase {

    private final SyncEventLoop bus = new SyncEventLoop();
    private final MultiLineTextInput text = new MultiLineTextInput(Colors.IYel);
    private ScrollableComponent scrollable;

    @Override
    protected void setUp() throws Exception {
        StringBuilder lines = new StringBuilder("short\nlonger line");
        for (int i = 3; i <= 20; i++) {
            lines.append("\nline ").append(i);
        }
        text.setText(lines.toString());
        scrollable = new ScrollableComponent(Colors.Yel, new Dimension.Simple(8, 40), text);   // wide enough for its footer buttons
        Composite root = new Composite();
        root.add(scrollable);
        CanvasForTesting.buildCanvasForTestingNoControlCodesInOutput().addComponentSegments(root);
    }

    /** a click at row, col of the view (1, 1 into the scrollable: its border) */
    private Position clickView(int row, int col) {
        scrollable.onClick(click(row + 1, col + 1, bus));
        return text.getCaretPosition();
    }

    public void testPastTheEndOfAShortLineGoesToItsEnd() {
        Position caret = clickView(0, 20);
        assertEquals(0, caret.getRow());
        assertEquals("short".length(), caret.getCol());
    }

    public void testOnTheTextGoesThere() {
        Position caret = clickView(1, 3);
        assertEquals(1, caret.getRow());
        assertEquals(3, caret.getCol());
    }

    public void testScrolledTheClickIsOnTheLineShownThere() {
        scrollable.setViewOffset(10, 0);                     // line 11 at the top of the view
        Position caret = clickView(2, 25);
        assertEquals("the third line shown", 12, caret.getRow());
        assertEquals("line 13".length(), caret.getCol());
    }

    public void testOnTheBorderNothingMoves() {
        text.setCaretPosition(new Position.Simple(1, 2));
        scrollable.onClick(click(0, 5, bus));                // the top border
        assertEquals(1, text.getCaretPosition().getRow());
        assertEquals(2, text.getCaretPosition().getCol());
    }
}
