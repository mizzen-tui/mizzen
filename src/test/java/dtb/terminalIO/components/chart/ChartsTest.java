package dtb.terminalIO.components.chart;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.CompositeWithFocusManager;
import dtb.terminalIO.components.plot.PlotArea;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.events.focus.RequestFocusEvent;
import dtb.terminalIO.events.keyboard.KeyEvent;
import dtb.terminalIO.events.keyboard.KeyStroke;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.supportfortesting.CanvasForTesting;
import dtb.terminalIO.supportfortesting.SyncEventLoop;
import junit.framework.TestCase;

/**
 * The charts as a user meets them: keys reach the focused chart (through the focus tree, as in an
 * app), select an entry, and onChange / getValue report it. (Clicks are hit tests on the drawn
 * dots - PlotAreaTest covers those.)
 */
public class ChartsTest extends TestCase {

    private final SyncEventLoop bus = new SyncEventLoop();
    private final CompositeWithFocusManager root = new CompositeWithFocusManager();   // as TUIApp builds it

    /** the chart on screen, with focus */
    private void focusOn(Component<?> chart, PlotArea plot) throws Exception {
        root.add(chart);
        CanvasForTesting.buildCanvasForTestingNoControlCodesInOutput().addComponentSegments(root);
        bus.setRoot(root);
        bus.deliver(new RequestFocusEvent(plot.asFocusable()));
    }

    private void press(KeyStroke key) {
        bus.deliver(new KeyEvent("test", key));
    }

    static class Disk {
        final String mount;
        final double gb;

        Disk(String mount, double gb) {
            this.mount = mount;
            this.gb = gb;
        }
    }

    private final List<Disk> disks = Arrays.asList(new Disk("/", 100), new Disk("/home", 300), new Disk("/var", 100));

    private DonutChart<Disk> donut() throws Exception {
        DonutChart<Disk> donut = new DonutChart<Disk>(Colors.Gre, new Dimension.Simple(16, 50))
                .sliceSize((d) -> d.gb)
                .sliceLabel((d) -> d.mount)
                .setItems(disks);
        focusOn(donut, donut.getWrappedComponent());
        return donut;
    }

    public void testNothingIsSelectedAtFirst() throws Exception {
        assertNull(donut().getValue());
    }

    public void testArrowsSelectAndWrapAround() throws Exception {
        DonutChart<Disk> donut = donut();
        press(Keys.RIGHT_KEYSTROKE);
        assertEquals("/", donut.getValue().label());
        press(Keys.RIGHT_KEYSTROKE);
        press(Keys.RIGHT_KEYSTROKE);
        assertEquals("/var", donut.getValue().label());
        press(Keys.RIGHT_KEYSTROKE);
        assertEquals("wraps to the first", "/", donut.getValue().label());
        press(Keys.LEFT_KEYSTROKE);
        assertEquals("and back to the last", "/var", donut.getValue().label());
    }

    public void testTheValueIsYourObjectWithItsShare() throws Exception {
        DonutChart<Disk> donut = donut();
        press(Keys.RIGHT_KEYSTROKE);
        press(Keys.RIGHT_KEYSTROKE);
        ChartItem<Disk> slice = donut.getValue();
        assertSame(disks.get(1), slice.item());
        assertEquals(1, slice.index());
        assertEquals(300.0, slice.value(), 1e-9);
        assertEquals(60.0, slice.percent(), 1e-9);
    }

    public void testOnChangeTellsYouOnEachMove() throws Exception {
        DonutChart<Disk> donut = donut();
        List<String> seen = new ArrayList<String>();
        donut.onChange((te) -> seen.add(donut.getValue().label()));
        press(Keys.RIGHT_KEYSTROKE);
        press(Keys.DOWN_KEYSTROKE);
        assertEquals(Arrays.asList("/", "/home"), seen);
    }

    public void testABarChartSelectsTheSameWay() throws Exception {
        BarChart<Disk> bars = new BarChart<Disk>(Colors.Cya, new Dimension.Simple(16, 50))
                .barSize((d) -> d.gb)
                .barLabel((d) -> d.mount)
                .setItems(disks);
        focusOn(bars, bars.getWrappedComponent());
        press(Keys.LEFT_KEYSTROKE);
        assertEquals("LEFT from nothing: the last bar", "/var", bars.getValue().label());
        assertEquals(20.0, bars.getValue().percent(), 1e-9);
    }

    // ---- the line chart -------------------------------------------------------------------------

    private LineChart lines() throws Exception {
        LineChart chart = new LineChart(Colors.Gre, new Dimension.Simple(16, 60))
                .series("a", Colors.BCya)
                .series("b", Colors.BYel)
                .series("c", Colors.BRed);
        for (int x = 0; x <= 10; x++) {
            chart.add("a", x, x).add("b", x, 10 - x).add("c", x, 5);
        }
        chart.redraw();
        focusOn(chart, chart.getWrappedComponent());
        return chart;
    }

    public void testRightStartsOnTheFirstSeriesAndMovesAlongIt() throws Exception {
        LineChart chart = lines();
        press(Keys.RIGHT_KEYSTROKE);
        assertEquals("a", chart.getValue().series());
        assertEquals(0, chart.getValue().index());
        press(Keys.RIGHT_KEYSTROKE);
        press(Keys.RIGHT_KEYSTROKE);
        assertEquals(2.0, chart.getValue().x(), 1e-9);
        assertEquals(2.0, chart.getValue().y(), 1e-9);
    }

    public void testDownGoesToTheNextSeriesAtTheSameX() throws Exception {
        LineChart chart = lines();
        press(Keys.RIGHT_KEYSTROKE);
        press(Keys.RIGHT_KEYSTROKE);
        press(Keys.RIGHT_KEYSTROKE);                                   // a, x 2
        press(Keys.DOWN_KEYSTROKE);
        assertEquals("b", chart.getValue().series());
        assertEquals(2.0, chart.getValue().x(), 1e-9);
        assertEquals(8.0, chart.getValue().y(), 1e-9);
        press(Keys.UP_KEYSTROKE);
        assertEquals("a", chart.getValue().series());
    }

    public void testAHiddenSeriesIsSkipped() throws Exception {
        LineChart chart = lines();
        chart.visible("b", false);
        assertFalse(chart.isVisible("b"));
        press(Keys.RIGHT_KEYSTROKE);                                   // a
        press(Keys.DOWN_KEYSTROKE);
        assertEquals("c", chart.getValue().series());
    }

    public void testHidingTheCurrentSeriesClearsTheSelection() throws Exception {
        LineChart chart = lines();
        press(Keys.RIGHT_KEYSTROKE);                                   // a
        chart.visible("a", false);
        assertNull(chart.getValue());
    }

    public void testRefillingASeriesKeepsItCurrent() throws Exception {
        LineChart chart = lines();
        press(Keys.RIGHT_KEYSTROKE);
        press(Keys.RIGHT_KEYSTROKE);                                   // a, point 1
        chart.clear("a");
        for (int x = 0; x <= 10; x++) {
            chart.add("a", x, x * 2);                                  // live data: the series refilled
        }
        chart.redraw();
        assertEquals("a", chart.getValue().series());
        assertEquals(2.0, chart.getValue().y(), 1e-9);                 // point 1 of the new data
    }

    public void testAddingToAMissingSeriesFailsClearly() throws Exception {
        try {
            new LineChart(Colors.Gre, new Dimension.Simple(10, 30)).add("nope", 1, 1);
            fail("expected an error naming the series");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("nope"));
        }
    }

    public void testValuesAreShownWithoutNeedlessDecimals() throws Exception {
        assertEquals("3", ChartItems.format(3.0));
        assertEquals("2.50", ChartItems.format(2.5));
        assertEquals("-4", ChartItems.format(-4));
    }
}
