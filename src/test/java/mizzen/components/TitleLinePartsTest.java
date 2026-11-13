package mizzen.components;

import mizzen.components.composite.VerticalPanel;
import mizzen.components.input.Button;
import mizzen.components.input.TextInput;
import mizzen.controlcodes.color.Colors;
import mizzen.core.TextSnapshot;
import mizzen.events.focus.FocusManager;
import mizzen.events.focus.Focusable;
import mizzen.events.keyboard.KeyEvent;
import mizzen.events.keyboard.Keys;
import mizzen.geom.Dimension;
import mizzen.geom.position.Position;
import mizzen.supportfortesting.CanvasForTesting;
import mizzen.supportfortesting.SyncEventLoop;
import junit.framework.TestCase;

/**
 * The top line of a border holds the title at the left, a part in the middle and one at the right: the right one
 * keeps its width and the title is cut, at least one ─ between them; a part that doesn't fit isn't drawn, takes no
 * clicks and is no TAB stop. A panel's border is made again as it resizes: narrowed and widened again it must look as
 * a new one would. Layout: a bordered panel holding a text field, " Visitor stats " left, a button right.
 */
public class TitleLinePartsTest extends TestCase {

    private static final String TITLE = " Visitor stats, last 30 days - r refresh ";

    private final Button flip = new Button(Colors.Cya, "Below");
    private final TextInput field = new TextInput(Colors.IYel, 10);
    private final VerticalPanel panel = new VerticalPanel();

    @Override
    protected void setUp() {
        Component.UTIL.COMPONENT_SEGMENTS_INTEGRITY_CHECK_FLAG = true;    // segments outside a component's bounds throw
        flip.setName("flip");
        field.setName("field");
        panel.add(field);
        panel.setTitle(TITLE);
        panel.setTitleRight(flip);
        panel.resize(new Dimension.Simple(4, 60));
    }

    @Override
    protected void tearDown() {
        Component.UTIL.COMPONENT_SEGMENTS_INTEGRITY_CHECK_FLAG = false;
    }

    private static String topRow(Component<?> c) {
        return new TextSnapshot(c.getHeight(), c.getWidth()).render(c).split("\n")[0];
    }

    private String topRowAt(int width) {
        panel.resize(new Dimension.Simple(4, width));
        return topRow(panel);
    }

    // the same panel, made new at this width
    private static String freshTopRowAt(int width) {
        VerticalPanel fresh = new VerticalPanel();
        fresh.add(new TextInput(Colors.IYel, 10));
        fresh.setTitle(TITLE);
        fresh.setTitleRight(new Button(Colors.Cya, "Below"));
        fresh.resize(new Dimension.Simple(4, width));
        return topRow(fresh);
    }

    private String flipText() {
        return topRow(flip);
    }

    public void testBothShowTheTitleLeftTheButtonRight() {
        String top = topRowAt(60);
        assertTrue(top, top.startsWith("┌" + TITLE));
        assertTrue(top, top.endsWith(flipText() + "┐"));
        assertTrue("lines between: " + top, top.contains("─"));
    }

    /** narrower: the button keeps its width; the title is cut, one ─ at least before the button */
    public void testNarrowerCutsTheTitleNotTheButton() {
        int width = TITLE.length() + flip.getWidth();                  // the title can't be whole
        String top = topRowAt(width);
        assertEquals(top, width, top.length());
        assertTrue(top, top.endsWith("─" + flipText() + "┐"));
        assertTrue(top, top.startsWith("┌" + TITLE.substring(0, width - flip.getWidth() - 3)));
        assertFalse("cut: " + top, top.contains(TITLE));
    }

    /** too narrow for the button: not drawn, and the title has the line to itself */
    public void testTooNarrowForTheButtonLeavesItOut() {
        int width = flip.getWidth() + 1;
        String top = topRowAt(width);
        assertEquals(top, width, top.length());
        assertTrue(top, top.startsWith("┌") && top.endsWith("┐"));
        assertEquals(top, "┌" + TITLE.substring(0, width - 2) + "┐", top);
        assertNotSame("no clicks", flip, partAt(0, width - 2 - 1));
        assertFalse("no TAB stop", panel.getTitleLineParts().contains(flip));
    }

    /** narrowed step by step past both, then wider again: at each width as a new one would look */
    public void testNarrowedAndWidenedAgainLeavesNothingBehind() {
        int[] widths = { 60, 45, 30, 20, 12, 8, 5, 3, 2, 3, 8, 12, 20, 30, 45, 60 };
        for (int width : widths) {
            assertEquals("at " + width, freshTopRowAt(width), topRowAt(width));
        }
    }

    // what a click on the panel's top line finds: the button, if it is the button or inside it (its text)
    private Component<?> partAt(int row, int col) {
        Component<?> found = panel.getComponentAtOffset(new Position.Simple(row, col));
        for (Object c = found; c instanceof Component; c = ((Component<?>) c).getParentContainer()) {
            if (c == flip) {
                return flip;
            }
        }
        return found;
    }

    public void testClicksLandOnTheButtonWhereItIsDrawn() {
        topRowAt(60);
        int start = 60 - 1 - flip.getWidth();
        assertSame(flip, partAt(0, start));
        assertSame(flip, partAt(0, 60 - 2));
        assertNotSame(flip, partAt(0, start - 1));
        assertSame("the title is the panel's, as before", panel, partAt(0, 3));
        topRowAt(30);
        assertSame("it follows the width", flip, partAt(0, 30 - 2));
    }

    /** a part in the middle: centred; short of room it goes before the right one, and before the title is cut to nothing */
    public void testAMiddlePartGoesFirst() {
        TextComponent status = new TextComponent(Colors.Gre, " 3 new ");
        panel.setTitle(" Stats ");
        panel.setTitleMiddle(status);
        String wide = topRowAt(60);
        assertTrue(wide, wide.contains("┌ Stats ─"));
        int at = wide.indexOf(" 3 new ");
        assertEquals("centred: " + wide, (60 - 7) / 2, at);
        assertTrue(wide, wide.endsWith(flipText() + "┐"));
        String narrow = topRowAt(7 + flip.getWidth() + 6);
        assertFalse(narrow, narrow.contains("3 new"));
        assertTrue(narrow, narrow.endsWith(flipText() + "┐"));
    }

    /** the title alone, as before: cut at the right corner, and whole again when wider */
    public void testATitleAloneIsCutAndComesBack() {
        panel.setTitleRight(null);
        String narrow = topRowAt(12);
        assertEquals(narrow, "┌" + TITLE.substring(0, 10) + "┐", narrow);
        assertTrue(topRowAt(60), topRowAt(60).startsWith("┌" + TITLE + "─"));
    }

    /** squeezed to one row, the border leaves nothing inside: its contents aren't drawn below it (they were) */
    public void testOneRowHighItDrawsNothingBelowItself() {
        panel.resize(new Dimension.Simple(1, 60));                    // segments outside its bounds would throw
        String top = topRow(panel);
        assertTrue(top, top.startsWith("┌" + TITLE));
    }

    // ---- TAB ---------------------------------------------------------------------------------------------------

    private final SyncEventLoop bus = new SyncEventLoop();
    private final CompositeWithFocusManager root = new CompositeWithFocusManager();

    private String focused() {
        Focusable focusable = FocusManager.UTIL.findCurrentFocus(root);
        return focusable == null ? null : HasName.UTIL.getName(focusable.getFocusComponent());
    }

    private void tab() {
        bus.deliver(new KeyEvent("test", Keys.TAB_KEYSTROKE));
    }

    private void inAnApp() throws Exception {
        root.add(panel);
        CanvasForTesting.buildCanvasForTestingNoControlCodesInOutput().addComponentSegments(root);
        bus.setRoot(root);
    }

    /** a button on the top line is a TAB stop, before what is inside the border; ENTER on it acts */
    public void testTabReachesTheButtonFirst() throws Exception {
        int[] pressed = { 0 };
        flip.onChange((te) -> pressed[0]++);
        inAnApp();
        tab();
        assertEquals("flip", focused());
        bus.deliver(new KeyEvent("test", Keys.ENTER_KEYSTROKE));
        assertEquals(1, pressed[0]);
        tab();
        assertEquals("field", focused());
        tab();
        assertEquals("round again", "flip", focused());
    }

    /** too narrow for it: TAB passes it by */
    public void testTabSkipsAButtonNotDrawn() throws Exception {
        panel.resize(new Dimension.Simple(4, flip.getWidth() + 1));
        inAnApp();
        tab();
        assertEquals("field", focused());
        tab();
        assertEquals("field", focused());
    }
}
