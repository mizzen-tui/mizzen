package mizzen.components.composite;

import mizzen.components.BorderedComponent;
import mizzen.components.Component;
import mizzen.components.TextComponent;
import mizzen.components.frameset.FlipSplitter;
import mizzen.components.frameset.HorizontalSplitter;
import mizzen.components.frameset.VerticalSplitter;
import mizzen.core.TextSnapshot;
import mizzen.geom.Dimension;
import mizzen.geom.position.Position;
import junit.framework.TestCase;

/**
 * Shared borders: with collapseBorders(true), bordered children next to a line - the panel's border, a neighbour's
 * edge, a one-cell gap between two - share it: one line where there were two. Off (the default): nothing changes.
 */
public class SharedBordersTest extends TestCase {

    @Override
    protected void setUp() {
        Component.UTIL.COMPONENT_SEGMENTS_INTEGRITY_CHECK_FLAG = true;    // nothing drawn outside its bounds
    }

    @Override
    protected void tearDown() {
        Component.UTIL.COMPONENT_SEGMENTS_INTEGRITY_CHECK_FLAG = false;
    }

    private static BorderedComponent boxed(String title, String text) {
        BorderedComponent b = new BorderedComponent(new TextComponent(text));
        if (title != null) {
            b.setTitle(title);
        }
        return b;
    }

    private static String picture(Component<?> c) {
        return new TextSnapshot(c.getHeight(), c.getWidth()).render(c);
    }

    // a titled panel holding two titled boxes, one cell apart
    private static Composite sideBySide() {
        Composite panel = new Composite();
        panel.setTitle(" Panel ");
        BorderedComponent a = boxed(" Example ", "left one\nsecond");
        panel.add(new Position.Simple(0, 0), a);
        panel.add(new Position.Simple(0, a.getWidth() + 1), boxed(" Docs ", "right one\nsecond"));
        return panel;
    }

    public void testOffByDefaultNothingChanges() {
        Composite panel = sideBySide();
        assertFalse(panel.isCollapseBorders());
        assertEquals("┌ Panel ───────────────┐\n"
                   + "│┌ Example┐ ┌ Docs ───┐│\n"
                   + "││left one│ │right one││\n"
                   + "││second  │ │second   ││\n"
                   + "│└────────┘ └─────────┘│\n"
                   + "└──────────────────────┘", picture(panel));
    }

    /** the gap becomes their line; their sides join the panel's; two titled tops stay two lines (both were asked for) */
    public void testSideBySideShareTheGapAndThePanelsSides() {
        Composite panel = sideBySide().collapseBorders(true);
        assertEquals("┌ Panel ───────────────┐\n"
                   + "├─ Example─┬─ Docs ────┤\n"
                   + "│ left one │ right one │\n"
                   + "│ second   │ second    │\n"
                   + "│          │           │\n"
                   + "└──────────┴───────────┘", picture(panel));
    }

    /** one box filling an untitled panel: one box, the child's title on it */
    public void testAChildFillingAPanelIsOneBox() {
        Composite panel = new Composite();
        panel.setBorder(true);
        panel.add(boxed(" Inner ", "fills it"));
        panel.collapseBorders(true);
        assertEquals("┌─ Inner ──┐\n"
                   + "│          │\n"
                   + "│ fills it │\n"
                   + "│          │\n"
                   + "└──────────┘", picture(panel));
    }

    /** two boxes touching: the right one's edge onto the left one's */
    public void testTouchingBoxesShareAnEdge() {
        Composite panel = new Composite();
        BorderedComponent one = boxed(null, "one");
        panel.add(new Position.Simple(0, 0), one);
        panel.add(new Position.Simple(0, one.getWidth()), boxed(null, "two"));
        panel.collapseBorders(true);
        assertEquals("┌───┬────┐\n"
                   + "│one│ two│\n"
                   + "└───┴────┘", picture(panel));
    }

    /** a splitter's panes share its divider: the grip on one line, joined to its border */
    public void testASplittersPanesShareItsDivider() {
        HorizontalSplitter split = new HorizontalSplitter().addComponent(boxed(" Left ", "one")).addComponent(boxed(" Right ", "two"));
        split.collapseBorders(true);
        split.resize(new Dimension.Simple(6, 30));
        String[] rows = picture(split).split("\n");
        assertTrue(rows[0], rows[0].startsWith("┌─") && rows[0].contains("┬─ Right") && rows[0].endsWith("┐"));
        assertTrue(rows[2], rows[2].contains(" one ⣿ two"));
        assertTrue(rows[5], rows[5].matches("└─+┴─+┘"));

        VerticalSplitter stacked = new VerticalSplitter().addComponent(boxed(" Top ", "one")).addComponent(boxed(" Bottom ", "two"));
        stacked.collapseBorders(true);
        stacked.resize(new Dimension.Simple(10, 24));
        assertTrue("the lower pane's title on the divider: " + picture(stacked), picture(stacked).contains("├ Bottom ──⣿⣿──────────┤"));
    }

    /** a divider is a line even when the pane across it has no box there (a tab panel's top is its tabs) */
    public void testAnEdgeSharesADividerWithNoBoxAcrossIt() {
        Composite tabsLike = new Composite();
        tabsLike.add(new TextComponent("[Docs][Code]\nsome text"));
        VerticalSplitter stacked = new VerticalSplitter().addComponent(boxed(" Example ", "one")).addComponent(tabsLike);
        stacked.collapseBorders(true);
        stacked.resize(new Dimension.Simple(10, 24));
        String text = picture(stacked);
        assertTrue(text, text.contains("└──────────⣿⣿──────────┘"));
        assertTrue("the tabs untouched:\n" + text, text.contains("[Docs][Code]"));
    }

    /** the desktop's shape: a plain holder around a bordered slot counts - its corners are the slot's */
    public void testAHolderAroundABorderedSlotTakesPart() {
        Composite slot = new Composite();
        slot.setBorder(true);
        slot.setTitle(" Example ");
        Composite holder = new Composite();
        holder.add(slot);
        holder.onResizeExpandDownAndRight(slot);
        FlipSplitter split = new FlipSplitter(holder, boxed(" Docs ", "readme")).collapseBorders(true);
        split.placement(FlipSplitter.Placement.RIGHT);
        split.resize(new Dimension.Simple(8, 50));
        String[] rows = picture(split).split("\n");
        assertTrue(rows[0], rows[0].startsWith("┌ Example ") && rows[0].contains("┬"));
        assertTrue(rows[3], rows[3].contains("⣿") && !rows[3].contains("│⣿") && !rows[3].contains("⣿│"));
        assertTrue(rows[7], rows[7].matches("└─+┴─+┘"));
    }

    // ---- scroll panes and tab panels -----------------------------------------------------------------------------

    private static String lines(int n) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < n; i++) {
            b.append("line ").append(i).append(" of the readme, long enough to scroll sideways\n");
        }
        return b.toString();
    }

    private static mizzen.components.scroll.ScrollableComponent scroll(String title) {
        mizzen.components.scroll.ScrollableComponent sc = new mizzen.components.scroll.ScrollableComponent((mizzen.controlcodes.color.Color) null,
                                                                                                            new TextComponent(lines(30)));
        sc.setTitle(title);
        return sc;
    }

    /** a scroll pane's scrollbars and footer go with its edges: the thumb onto the shared line, the footer whole */
    public void testAScrollPanesScrollbarsAndFooterAreCarried() {
        Composite panel = new Composite();
        panel.setTitle(" Panel ");
        panel.resize(new Dimension.Simple(10, 44));
        mizzen.components.scroll.ScrollableComponent sc = scroll(" Inner ");
        panel.add(sc);
        panel.onResizeExpandDownAndRight(sc);
        panel.collapseBorders(true);
        String[] rows = picture(panel).split("\n");
        assertTrue(rows[2], rows[2].endsWith(" ▒"));                                 // the thumb, on the panel's right line
        assertTrue(rows[9], rows[9].startsWith("└─▒▒▒") && rows[9].contains("┤ ^ │ v │ PgUp │ PgDn │ « │ » ├─┘"));
    }

    /** two scroll panes sharing their gap: the left one's thumb stays on it - a plain line never covers it */
    public void testAThumbOnASharedGapStays() {
        Composite panel = new Composite();
        panel.resize(new Dimension.Simple(8, 60));
        panel.add(new Position.Simple(0, 0), new mizzen.components.scroll.ScrollableComponent((mizzen.controlcodes.color.Color) null,
                new Dimension.Simple(8, 29), new TextComponent(lines(20))));
        panel.add(new Position.Simple(0, 30), new mizzen.components.scroll.ScrollableComponent((mizzen.controlcodes.color.Color) null,
                new Dimension.Simple(8, 30), new TextComponent(lines(20))));
        panel.collapseBorders(true);
        String[] rows = picture(panel).split("\n");
        assertEquals(rows[1], '▒', rows[1].charAt(29));
        assertEquals(rows[0], '┬', rows[0].charAt(29));
    }

    /** a tab panel's content shares the frame's sides; its titled top stays under the tab strip; focused, it is double */
    public void testATabPanelsContentSharesItsFrame() throws Exception {
        mizzen.components.tab.TabPanel tabs = new mizzen.components.tab.TabPanel(new Dimension.Simple(12, 50));
        tabs.addTab("Docs", scroll(" README.md "));
        tabs.addTab("Code", scroll(" Code "));
        tabs.collapseBorders(true);
        mizzen.supportfortesting.SyncEventLoop bus = new mizzen.supportfortesting.SyncEventLoop();
        mizzen.components.CompositeWithFocusManager root = new mizzen.components.CompositeWithFocusManager();
        root.add(tabs);
        mizzen.supportfortesting.CanvasForTesting.buildCanvasForTestingNoControlCodesInOutput().addComponentSegments(root);
        bus.setRoot(root);
        tabs.showTab("Docs", bus);
        bus.drain();
        String[] rows = picture(tabs).split("\n");
        assertTrue(rows[2], rows[2].startsWith("┌┘      └┴"));                     // the tabs untouched
        assertTrue(rows[3], rows[3].startsWith("╠═ README.md ═") && rows[3].endsWith("╣"));   // focused: double, the heavier line
        assertTrue(rows[4], rows[4].startsWith("║ line 0") && rows[4].endsWith(" ▒"));
        assertTrue(rows[10], rows[10].contains("┤ ^ │ v │ PgUp │ PgDn │ « │ » ├"));
        assertTrue("its own bottom, under the footer: " + rows[11], rows[11].contains("<-    ->"));
    }

    // ---- the console's checks (round 15): rounded corners, a holder, clicks ----------------------------------------

    private static String inTheme(mizzen.components.theme.Theme theme, java.util.function.Supplier<String> drawing) {
        mizzen.components.theme.Theme before = mizzen.components.theme.Theme.getDefault();
        mizzen.components.theme.Theme.setDefault(theme);
        try {
            return drawing.get();
        } finally {
            mizzen.components.theme.Theme.setDefault(before);
        }
    }

    /** ╭╮╰╯ boxes share as ┌┐└┘ ones: a rounded theme's panes on a divider, in both placements */
    public void testRoundedCornersShare() {
        for (FlipSplitter.Placement placement : new FlipSplitter.Placement[] { FlipSplitter.Placement.BELOW, FlipSplitter.Placement.RIGHT }) {
            String text = inTheme(mizzen.components.theme.Theme.ROUNDED, () -> {
                FlipSplitter split = new FlipSplitter(scroll(" Chart "), scroll(" Figures ")).setTitle(" Stats ").collapseBorders(true);
                split.placement(placement);
                split.resize(new Dimension.Simple(14, 40));
                return picture(split);
            });
            assertTrue(placement + ":\n" + text, text.startsWith("╭"));
            assertFalse(placement + ", a pane's own corner left:\n" + text, text.substring(1).contains("╭"));
            assertTrue(placement + ":\n" + text, text.contains("⣿"));
        }
    }

    /** a pane held in an unbordered panel (a screen's bottom half, its content swapped) still shares - its corners are its content's */
    public void testAPaneInAnUnborderedHolderShares() {
        String text = inTheme(mizzen.components.theme.Theme.ROUNDED, () -> {
            BorderedComponent top = boxed(" Top ", "top");
            VerticalPanel holder = new VerticalPanel();
            mizzen.components.scroll.ScrollableComponent report = scroll(" Report ");
            holder.add(report);
            holder.onResizeExpandDownAndRight(report);
            VerticalSplitter screen = new VerticalSplitter().addComponent(top).addComponent(holder);
            screen.setBorder(true);
            screen.collapseBorders(true);
            screen.resize(new Dimension.Simple(14, 30));
            return picture(screen);
        });
        String[] rows = text.split("\n");
        assertTrue(text, rows[4].startsWith("├─ Report ") && rows[4].contains("⣿⣿") && rows[4].endsWith("┤"));
        assertTrue(text, rows[13].startsWith("╰") && rows[13].contains("┤ ^ │ v │") && rows[13].endsWith("╯"));
    }

    /** after the edges moved, the footer's buttons and the scrollbar on the shared lines still take the clicks */
    public void testMovedFooterAndScrollbarTakeClicks() {
        Composite panel = new Composite();
        panel.setTitle(" Panel ");
        panel.resize(new Dimension.Simple(10, 44));
        mizzen.components.scroll.ScrollableComponent sc = scroll(" Inner ");
        panel.add(sc);
        panel.onResizeExpandDownAndRight(sc);
        panel.collapseBorders(true);
        String[] rows = picture(panel).split("\n");
        int pgUp = rows[9].indexOf("PgUp");
        assertTrue(rows[9], pgUp > 0);
        Component<?> button = panel.getComponentAtOffset(new Position.Simple(9, pgUp));
        assertTrue("a footer button: " + button, insideOf(button, sc) && classChainHas(button, "Button"));
        Component<?> thumb = panel.getComponentAtOffset(new Position.Simple(2, 43));
        assertTrue("the vertical scrollbar: " + thumb, insideOf(thumb, sc) && classChainHas(thumb, "ScrollBar"));
    }

    private static boolean insideOf(Object c, Object outer) {
        for (Object x = c; x instanceof Component; x = ((Component<?>) x).getParentContainer()) {
            if (x == outer) {
                return true;
            }
        }
        return false;
    }

    private static boolean classChainHas(Object c, String name) {
        for (Object x = c; x instanceof Component; x = ((Component<?>) x).getParentContainer()) {
            if (x.getClass().getSimpleName().contains(name)) {
                return true;
            }
        }
        return false;
    }

    // ---- the console's round 16: a swap, a pane beside an unbordered one, nested splitters ---------------------------

    private static Composite plain(String text) {
        Composite c = new Composite();
        c.add(new TextComponent(text));
        return c;
    }

    /** a scroll pane swapped out of a screen's bottom half leaves nothing behind on the screen's lines (its thumb was) */
    public void testASwappedOutPaneLeavesNoThumbBehind() {
        String text = inTheme(mizzen.components.theme.Theme.ROUNDED, () -> {
            VerticalPanel bottomHalf = new VerticalPanel();
            mizzen.components.scroll.ScrollableComponent status = scroll(" Status ");
            bottomHalf.add(status);
            bottomHalf.onResizeExpandDownAndRight(status);
            VerticalSplitter screen = new VerticalSplitter().addComponent(plain("menu")).addComponent(bottomHalf);
            screen.setBorder(true);
            screen.collapseBorders(true);
            screen.resize(new Dimension.Simple(16, 50));
            assertTrue(picture(screen), picture(screen).contains("▒"));      // Status's thumb, on the screen's right line
            bottomHalf.remove(status);
            FlipSplitter visitors = new FlipSplitter(plain("chart"), boxed(" Figures ", "fig")).collapseBorders(true);
            bottomHalf.add(visitors);
            bottomHalf.onResizeExpandDownAndRight(visitors);
            bottomHalf.applyResizeRules();
            String swapped = picture(screen);
            visitors.placement(FlipSplitter.Placement.RIGHT);
            return swapped + "\n" + picture(screen);
        });
        assertFalse(text, text.contains("▒"));
    }

    /** beside an unbordered pane, a pane shares the screen's divider above it and its bottom line below, as stacked */
    public void testAPaneBesideAnUnborderedOneSharesTopAndBottom() {
        String text = inTheme(mizzen.components.theme.Theme.ROUNDED, () -> {
            VerticalPanel bottomHalf = new VerticalPanel();
            FlipSplitter visitors = new FlipSplitter(plain("chart"), boxed(" Figures ", "fig")).collapseBorders(true);
            visitors.placement(FlipSplitter.Placement.RIGHT);
            bottomHalf.add(visitors);
            bottomHalf.onResizeExpandDownAndRight(visitors);
            VerticalSplitter screen = new VerticalSplitter().addComponent(plain("menu")).addComponent(bottomHalf);
            screen.setBorder(true);
            screen.collapseBorders(true);
            screen.resize(new Dimension.Simple(16, 50));
            return picture(screen);
        });
        String[] rows = text.split("\n");
        int divider = 0;
        while (!rows[divider].startsWith("├")) {
            divider++;
        }
        assertFalse("its top on the screen's divider, not a row under it:\n" + text,
                    rows[divider + 1].contains("╭") || rows[divider + 1].contains("─"));
        assertTrue("its bottom on the screen's:\n" + text, rows[rows.length - 1].contains("┴"));
    }

    /** two bordered panes in an unbordered splitter, in a bordered screen: one line between them, both on the screen's lines */
    public void testNestedSplittersShareEveryLine() {
        String text = inTheme(mizzen.components.theme.Theme.ROUNDED, () -> {
            mizzen.components.scroll.ScrollableComponent actions = scroll(" Actions ");
            HorizontalSplitter bottom = new HorizontalSplitter().addComponent(actions).addComponent(scroll(" Output "));
            bottom.onResizeExpandRight(actions, 40);
            bottom.setBorder(false);
            bottom.collapseBorders(true);
            VerticalSplitter home = new VerticalSplitter().addComponent(plain("logo")).addComponent(bottom);
            home.setBorder(true);
            home.collapseBorders(true);
            home.resize(new Dimension.Simple(18, 60));
            return picture(home);
        });
        String[] rows = text.split("\n");
        int divider = 0;
        while (!rows[divider].startsWith("├")) {
            divider++;
        }
        assertTrue(text, rows[divider].startsWith("├─ Actions ") && rows[divider].contains("┬─ Ou"));
        int junction = rows[divider].indexOf('┬');
        for (int row = divider + 1; row < rows.length - 1; row++) {
            assertTrue("one line between them:\n" + text, rows[row].charAt(junction - 1) != '│' && rows[row].charAt(junction + 1) != '│');
        }
        assertEquals(text, '┴', rows[rows.length - 1].charAt(junction));
    }

    // ---- the console's round 17: keeping things whole -------------------------------------------------------------

    // a teal page with white cards, as the console's
    private static final mizzen.components.theme.Theme CARDS_ON_TEAL = mizzen.components.theme.Theme.ROUNDED
            .withBackgroundColor(mizzen.components.theme.Theme.rgb(0xE6EEF2, 0x1D4F4A))
            .withSurfaceColor(mizzen.components.theme.Theme.rgb(0x22313A, 0xF4F7F8));

    // a screen: a bordered splitter, top the given pane, bottom a scroll pane (a report)
    private static VerticalSplitter screen(Component<?> top) {
        VerticalSplitter screen = new VerticalSplitter().addComponent(top).addComponent(scroll(" Report "));
        screen.setBorder(true);
        screen.resize(new Dimension.Simple(18, 50));
        return screen;
    }

    private static String topRows(String picture, int rows) {
        return String.join("\n", java.util.Arrays.copyOfRange(picture.split("\n"), 0, rows));
    }

    /** a card (its own surface: white on teal) keeps its frame whole; the report beside the same lines still shares */
    public void testACardOnItsOwnSurfaceStaysWhole() {
        String[] off = new String[1];
        String on = inTheme(CARDS_ON_TEAL, () -> {
            VerticalSplitter s = screen(new mizzen.components.card.Card("Site").add(new TextComponent("up 12 days")));
            off[0] = picture(s);
            s.collapseBorders(true);
            return picture(s);
        });
        int divider = 0;
        while (!on.split("\n")[divider].startsWith("├")) {
            divider++;
        }
        assertEquals("the card as it was:\n" + on, topRows(off[0], divider), topRows(on, divider));
        assertTrue("the report shares the divider:\n" + on, on.contains("─ Report ─"));
    }

    /** keepWhole: a pane and all in it as drawn (a row of frames, the Actions frame); the others share */
    public void testAPaneKeptWholeIsAsDrawn() {
        String[] off = new String[1];
        String on = inTheme(mizzen.components.theme.Theme.ROUNDED, () -> {
            HorizontalSplitter topHalf = new HorizontalSplitter().addComponent(boxed(" Actions ", "run")).addComponent(boxed(" Site ", "up"));
            topHalf.setBorder(false);
            VerticalSplitter s = screen(topHalf);
            off[0] = picture(s);
            s.collapseBorders(true).keepWhole(topHalf);
            return picture(s);
        });
        String[] offRows = off[0].split("\n"), onRows = on.split("\n");
        int divider = 0;
        while (!onRows[divider].startsWith("├")) {
            divider++;
        }
        assertEquals("the kept pane as it was:\n" + on, topRows(off[0], divider), topRows(on, divider));
        assertTrue("the report shares the divider:\n" + on, onRows[divider].contains("─ Report ─"));
        assertFalse(offRows[divider].contains("Report"));
    }

    /** a footer moved down leaves no line above its separators, even with a column line in the text above them */
    public void testAMovedFooterLeavesNoStrayLine() {
        StringBuilder columns = new StringBuilder();
        for (int i = 0; i < 20; i++) {
            columns.append("││││││││││││││││││││││││││││││││││││││││││\n");
        }
        Composite panel = new Composite();
        panel.setBorder(true);
        panel.resize(new Dimension.Simple(10, 44));
        mizzen.components.scroll.ScrollableComponent sc = new mizzen.components.scroll.ScrollableComponent((mizzen.controlcodes.color.Color) null,
                                                                                                            new TextComponent(columns.toString()));
        panel.add(sc);
        panel.onResizeExpandDownAndRight(sc);
        panel.collapseBorders(true);
        String[] rows = picture(panel).split("\n");
        String vacated = rows[8];                                          // the footer's old row
        assertTrue("[" + vacated + "]", vacated.substring(1, vacated.length() - 1).trim().isEmpty());
    }

    /** a tab panel under a divider: its frame shares, its tabs stay as drawn (a tab is a box, but not a pane) */
    public void testTabsUnderADividerStayAsDrawn() throws Exception {
        String[] pictures = new String[2];
        for (int k = 0; k < 2; k++) {
            boolean collapse = k == 1;
            pictures[k] = inTheme(mizzen.components.theme.Theme.ROUNDED, () -> {
                mizzen.components.tab.TabPanel tabs = new mizzen.components.tab.TabPanel();
                tabs.addTab("Docs", scroll(" README.md "));
                tabs.addTab("Code", scroll(" Code "));
                tabs.addTab("Components", scroll(" Parts "));
                tabs.collapseBorders(collapse);
                VerticalSplitter stacked = new VerticalSplitter().addComponent(boxed(" Example ", "running")).addComponent(tabs);
                stacked.collapseBorders(collapse);
                stacked.resize(new Dimension.Simple(20, 50));
                mizzen.supportfortesting.SyncEventLoop bus = new mizzen.supportfortesting.SyncEventLoop();
                mizzen.components.CompositeWithFocusManager root = new mizzen.components.CompositeWithFocusManager();
                root.add(stacked);
                try {
                    mizzen.supportfortesting.CanvasForTesting.buildCanvasForTestingNoControlCodesInOutput().addComponentSegments(root);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                bus.setRoot(root);
                tabs.showTab("Docs", bus);
                bus.drain();
                return picture(stacked);
            });
        }
        String[] off = pictures[0].split("\n"), on = pictures[1].split("\n");
        int strip = 0;
        while (!off[strip].contains("Code")) {
            strip++;
        }
        assertEquals("the tabs as drawn:\n" + pictures[1], off[strip - 1].trim(), on[strip - 1].trim());
        assertEquals(off[strip].trim(), on[strip].trim());
    }

    /** a click on a shared line goes to the box whose edge it is - at that edge */
    public void testClicksOnASharedLineReachTheBox() {
        Composite panel = new Composite();
        panel.setBorder(true);
        BorderedComponent inner = boxed(" Inner ", "fills it");
        panel.add(inner);
        panel.collapseBorders(true);
        picture(panel);                                                // drawn: the moves are known
        Component<?> hit = panel.getComponentAtOffset(new Position.Simple(2, 0));   // the panel's left line, row 2
        for (Object c = hit; c != null; c = c instanceof Component ? ((Component<?>) c).getParentContainer() : null) {
            if (c == inner) {
                return;
            }
            if (!(c instanceof Component)) {
                break;
            }
        }
        fail("the shared line is the inner box's edge: " + hit);
    }
}
