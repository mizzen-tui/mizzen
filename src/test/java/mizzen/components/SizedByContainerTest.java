package mizzen.components;

import java.util.Arrays;

import mizzen.components.frameset.HorizontalSplitter;
import mizzen.components.frameset.VerticalSplitter;
import mizzen.components.table.ObjectTableModel;
import mizzen.components.table.TableComponent;
import mizzen.controlcodes.color.Colors;
import mizzen.geom.Dimension;
import junit.framework.TestCase;

/**
 * Components made without a size are sized by what holds them: a splitter gives a first pane with no
 * size half, a share given replaces that, and a first pane made with a size keeps it.
 */
public class SizedByContainerTest extends TestCase {

    public static class Row {
        public String name;

        Row(String name) {
            this.name = name;
        }
    }

    private static TableComponent table() {
        return new TableComponent(Colors.Cya, new ObjectTableModel<Row>().column("Name", (r) -> r.name).setRows(Arrays.asList(new Row("a"))));
    }

    private static void draw(Component<?> c) {
        c.getSegmentsBuffer().getSegments();
    }

    public void testSideBySideNoSizesGetHalfEach() {
        TableComponent left = table(), right = table();
        HorizontalSplitter split = new HorizontalSplitter(Colors.BBlu).addComponent(left).addComponent(right);
        split.setBorder(false);
        split.resize(new Dimension.Simple(10, 81));
        draw(split);
        assertEquals(40, left.getWidth());
        assertEquals("the rest, less the divider", 40, right.getWidth());
        assertEquals(10, left.getHeight());
    }

    public void testAShareGivenReplacesTheHalf() {
        TableComponent left = table(), right = table();
        HorizontalSplitter split = new HorizontalSplitter(Colors.BBlu).addComponent(left).addComponent(right);
        split.setBorder(false);
        split.onResizeExpandRight(left, 70);
        split.resize(new Dimension.Simple(10, 101));
        draw(split);
        assertEquals(70, left.getWidth());
        assertEquals(30, right.getWidth());
    }

    public void testAboveBelowNoSizesGetHalfEach() {
        TableComponent above = table(), below = table();
        VerticalSplitter split = new VerticalSplitter(Colors.BBlu).addComponent(above).addComponent(below);
        split.resize(new Dimension.Simple(41, 50));
        draw(split);
        assertEquals(20, above.getHeight());
        assertEquals(20, below.getHeight());
        assertEquals(50, above.getWidth());
    }

    public void testAFirstPaneMadeWithASizeKeepsIt() {
        TableComponent left = new TableComponent(Colors.Cya, new Dimension.Simple(5, 30), new ObjectTableModel<Row>().column("Name", (r) -> r.name));
        TableComponent right = table();
        HorizontalSplitter split = new HorizontalSplitter(Colors.BBlu).addComponent(left).addComponent(right);
        split.setBorder(false);
        split.resize(new Dimension.Simple(10, 81));
        draw(split);
        assertEquals(30, left.getWidth());
    }
}
