package mizzen.components.markdown;

import static mizzen.events.mouse.MouseEventsForTesting.click;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mizzen.components.CompositeWithFocusManager;
import mizzen.components.scroll.ScrollableComponent;
import mizzen.controlcodes.color.Colors;
import mizzen.events.Registration;
import mizzen.events.focus.RequestFocusEvent;
import mizzen.geom.Dimension;
import mizzen.supportfortesting.SyncEventLoop;
import junit.framework.TestCase;

/** A click on a link: handlers first, then a .md opens here, relative to the document showing, with Back. */
public class MarkdownLinksTest extends TestCase {

    private final SyncEventLoop bus = new SyncEventLoop();
    private final List<String> heard = new ArrayList<String>();
    private final Map<String, String> files = new HashMap<String, String>();
    private MarkdownView view;

    @Override
    protected void setUp() throws Exception {
        files.put("README.md", "[arch](docs/A.md) and [code](X.java)");
        files.put("docs/A.md", "# A\n\n[b](B.md) [up](../README.md) [gone](C.md)");
        files.put("docs/B.md", "# B");
        open(new MarkdownView("README.md", files::get));
    }

    private void open(MarkdownView shown) {
        view = shown;
        CompositeWithFocusManager root = new CompositeWithFocusManager();
        root.add(view);
        bus.setRoot(root);
        view.fitTo(40);
    }

    // a click on the first place target's text shows
    private void clickLink(String target) {
        for (int row = 0; row < view.lines.size(); row++) {
            for (int col = 0; col < 40; col++) {
                if (target.equals(view.linkAt(row, col))) {
                    view.onClick(click(row, col, bus));
                    bus.drain();
                    return;
                }
            }
        }
        fail("no link to " + target + " on screen");
    }

    private String firstLine() {
        return view.lines.get(0).get(0).text;
    }

    public void testNoLinkThere() {
        assertNull(view.linkAt(0, 39));
        assertNull(view.linkAt(99, 0));
        assertEquals("docs/A.md", view.linkAt(0, 0));
    }

    public void testAHandlerHearsTheTargetAsWrittenWithTheBus() {
        view.onLink((target) -> true, (te) -> {
            heard.add(te.getTarget());
            assertNotNull(te.getEventBus());
        });
        clickLink("X.java");
        assertEquals("[X.java]", heard.toString());
    }

    public void testAHandlerGoesBeforeOpeningADocument() {
        view.onLink((target) -> true, (te) -> heard.add(te.getTarget()));
        clickLink("docs/A.md");
        assertEquals("[docs/A.md]", heard.toString());
        assertEquals("still the README", "arch", firstLine());
    }

    public void testNewestWantingItGetsItAloneOthersPassItOn() {
        view.onLink((target) -> true, (te) -> heard.add("older"));
        view.onLink((target) -> target.endsWith(".nope"), (te) -> heard.add("wants other links"));
        clickLink("X.java");
        assertEquals("[older]", heard.toString());
        view.onLink((target) -> true, (te) -> heard.add("newest"));
        clickLink("X.java");
        assertEquals("[older, newest]", heard.toString());
    }

    public void testARegistrationTakesItOut() {
        Registration handler = view.onLink((target) -> true, (te) -> heard.add("heard"));
        handler.unregister();
        clickLink("X.java");
        assertEquals("[]", heard.toString());
    }

    /** Back is a link: through the event bus, and a handler that wants it can take it */
    public void testAHandlerCanTakeBack() {
        clickLink("docs/A.md");
        view.onLink(MarkdownView.BACK::equals, (te) -> heard.add(te.getTarget()));
        clickLink(MarkdownView.BACK);
        assertEquals("[" + MarkdownView.BACK + "]", heard.toString());
        assertEquals("it stayed: the handler took it", "<< Back", firstLine());
    }

    /** a link in a table cell: found through the table's cell, wrapped or not */
    public void testALinkInATableCell() {
        open(new MarkdownView("| A | B |\n|---|---|\n| plain [one two](X.java) | [y](Y.java) |"));
        view.onLink((target) -> true, (te) -> heard.add(te.getTarget()));
        clickLink("Y.java");
        clickLink("X.java");
        assertEquals("[Y.java, X.java]", heard.toString());
        view.fitTo(14);                                                         // narrow: the cell wraps, the link goes with it
        clickLink("X.java");
        assertEquals("[Y.java, X.java, X.java]", heard.toString());
        assertNull("the heading", view.linkAt(0, 1));
    }

    public void testAnotherLinkDoesNothing() {
        clickLink("X.java");
        assertEquals("arch", firstLine());
    }

    public void testADocumentOpensAndBackReturns() {
        clickLink("docs/A.md");
        assertEquals("<< Back", firstLine());
        clickLink("B.md");                                                     // relative to docs/A.md
        assertEquals("<< Back", firstLine());
        assertEquals("B", view.lines.get(2).get(0).text);
        view.onClick(click(0, 0, bus));
        bus.drain();
        assertEquals("A", view.lines.get(2).get(0).text);
        view.onClick(click(0, 0, bus));
        bus.drain();
        assertEquals("no Back on the first", "arch", firstLine());
    }

    public void testUpAFolder() {
        clickLink("docs/A.md");
        clickLink("../README.md");
        assertEquals("<< Back", firstLine());
        assertEquals("arch", view.lines.get(2).get(0).text);
    }

    public void testADocumentThatIsNotThereSaysSo() {
        clickLink("docs/A.md");
        clickLink("C.md");
        assertEquals("Not found", view.lines.get(2).get(0).text);
        view.onClick(click(0, 0, bus));
        bus.drain();
        assertEquals("A", view.lines.get(2).get(0).text);
    }

    public void testWithNoSourceALinkToAMarkdownFileDoesNothing() {
        open(new MarkdownView("[a](A.md)"));
        clickLink("A.md");
        assertEquals("a", firstLine());
    }

    /** in a scroll pane: focus arriving leaves the view where it is; a new document starts at the top, and names itself */
    public void testInAScrollPane() {
        StringBuilder longDocument = new StringBuilder("[next](docs/B.md)\n\n");
        for (int i = 0; i < 100; i++) {
            longDocument.append("line ").append(i).append("\n\n");
        }
        files.put("README.md", longDocument.toString());
        MarkdownView shown = new MarkdownView("README.md", files::get);
        ScrollableComponent pane = MarkdownView.inScrollPane(Colors.IGre, shown);
        CompositeWithFocusManager root = new CompositeWithFocusManager();
        root.add(pane);
        bus.setRoot(root);
        pane.resize(new Dimension.Simple(20, 40));
        pane.getSegmentsBuffer();
        pane.setViewOffset(50, 0);
        assertEquals(50, pane.getOffsetIntoSource().getRow());

        bus.deliver(new RequestFocusEvent(shown.asFocusable()));
        bus.drain();
        pane.getSegmentsBuffer();
        assertEquals("focus didn't move it", 50, pane.getOffsetIntoSource().getRow());

        pane.setViewOffset(0, 0);
        shown.onClick(click(0, 0, bus));
        bus.drain();
        assertEquals(" docs/B.md ", pane.getTitle());
        assertEquals(0, pane.getOffsetIntoSource().getRow());
    }

    public void testALinkWrappedOverTwoLinesWorksOnBoth() {
        open(new MarkdownView("[one two three](X.java)"));
        view.fitTo(8);
        assertEquals("X.java", view.linkAt(0, 0));
        assertEquals("X.java", view.linkAt(1, 0));
    }
}
