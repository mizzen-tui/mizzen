package dtb.terminalIO.components.composite;

import dtb.terminalIO.components.BoundedComponent;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.geom.Dimension;
import junit.framework.TestCase;

/**
 * Resize rules: a rule belongs to the parent and sizes one child from the parent's size. A child
 * that grows leaves room for the siblings after it, a rule can have a minimum, a height rule and a
 * width rule can sit together, and the rules run before the first draw.
 */
public class ResizeRulesTest extends TestCase {

    private static BoundedComponent.DONOTHING box(int height, int width) {
        return new BoundedComponent.DONOTHING(height, width);
    }

    public void testAFillLeavesRoomForThePanesAfterIt() {
        HorizontalPanel row = new HorizontalPanel(Colors.DEFAULT_COLOR, 1);
        BoundedComponent.DONOTHING table = box(5, 10), actions = box(5, 26);
        row.add(table);
        row.add(actions);
        row.onResizeExpandRight(table);
        row.resize(new Dimension.Simple(5, 100));
        assertEquals("100 less the gap and the actions", 73, table.getWidth());
        assertEquals("the actions keep their width", 26, actions.getWidth());
    }

    public void testTheMiddleOfAStackFillsDownToTheRowsBelowIt() {
        VerticalPanel stack = new VerticalPanel();
        BoundedComponent.DONOTHING heading = box(1, 40), list = box(3, 40), buttons = box(1, 40);
        stack.add(heading);
        stack.add(list);
        stack.add(buttons);
        stack.onResizeExpandDown(list);
        stack.resize(new Dimension.Simple(20, 40));
        assertEquals("20 less the heading and the buttons", 18, list.getHeight());
    }

    public void testAFillStopsAtASiblingThatStaysPut() {
        Composite panel = new Composite(new Dimension.Simple(5, 80));
        BoundedComponent.DONOTHING left = box(5, 10), fixed = box(5, 10);
        panel.add(0, 0, left);
        panel.add(0, 50, fixed);
        panel.onResizeExpandRight(left);
        panel.resize(new Dimension.Simple(5, 100));
        assertEquals("up to the sibling at column 50", 50, left.getWidth());
    }

    public void testTheLastChildFillsAsBefore() {
        VerticalPanel stack = new VerticalPanel();
        BoundedComponent.DONOTHING heading = box(1, 40), rest = box(3, 40);
        stack.add(heading);
        stack.add(rest);
        stack.onResizeExpandDownAndRight(rest);
        stack.resize(new Dimension.Simple(20, 60));
        assertEquals(19, rest.getHeight());
        assertEquals(60, rest.getWidth());
    }

    public void testAtLeast() {
        VerticalPanel stack = new VerticalPanel();
        BoundedComponent.DONOTHING top = box(3, 40);
        stack.add(top);
        stack.onResizeExpandDown(top, 40).atLeast(9);
        stack.resize(new Dimension.Simple(50, 40));
        assertEquals("40% of 50", 20, top.getHeight());
        stack.resize(new Dimension.Simple(12, 40));
        assertEquals("40% of 12 is 4: at least 9", 9, top.getHeight());
    }

    public void testAHeightRuleAndAWidthRuleSitTogether() {
        Composite panel = new Composite(new Dimension.Simple(10, 10));
        BoundedComponent.DONOTHING pane = box(3, 3);
        panel.add(pane);
        panel.onResizeExpandRight(pane);
        panel.onResizeExpandDown(pane, 50);
        panel.resize(new Dimension.Simple(40, 80));
        assertEquals("the width rule", 80, pane.getWidth());
        assertEquals("the height rule", 20, pane.getHeight());

        panel.onResizeExpandDown(pane, 25);                   // the same direction: replaces the height rule
        panel.resize(new Dimension.Simple(40, 60));
        assertEquals(10, pane.getHeight());
        assertEquals(60, pane.getWidth());
    }

    public void testTheRulesRunBeforeTheFirstDraw() {
        Composite panel = new Composite(new Dimension.Simple(10, 30));
        BoundedComponent.DONOTHING pane = box(1, 1);
        panel.add(pane);
        panel.onResizeExpandDownAndRight(pane);
        assertEquals("not yet", 1, pane.getWidth());
        panel.getSegmentsBuffer();                             // draws
        assertEquals(10, pane.getHeight());
        assertEquals(30, pane.getWidth());
    }
}
