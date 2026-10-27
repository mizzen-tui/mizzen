package mizzen.components.chart;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import mizzen.components.AbstractComponentWrappper;
import mizzen.components.input.HasValue;
import mizzen.components.plot.Pen;
import mizzen.components.plot.PlotArea;
import mizzen.components.plot.shape.ConnectedPoints;
import mizzen.components.plot.shape.Polygon;
import mizzen.components.plot.shape.Shape;
import mizzen.controlcodes.color.Color;
import mizzen.controlcodes.color.Colors;
import mizzen.events.EventBus;
import mizzen.events.EventHandler;
import mizzen.events.RequestUpdateCanvasEvent;
import mizzen.events.keyboard.KeyEvent;
import mizzen.events.keyboard.KeyboardHandler;
import mizzen.events.keyboard.Keys;
import mizzen.geom.Dimension;
import mizzen.geom.position.Position;
import mizzen.geom.position.Vec2;

/**
 * A line chart: named series of points (x, y), each in its colour, optionally with the area under
 * it filled; axes and a legend. The window fits all the shown points. Pick a point with a click
 * (on or near its line) or a series with a click on its legend line; LEFT / RIGHT then move along
 * the series and UP / DOWN to the series above / below - like a list: onChange tells you, and
 * getValue() gives the point back (its series, x and y). Each legend line has a check box to hide
 * / show its series (also visible(name, shown)).
 * <pre>
 *     LineChart chart = new LineChart(Colors.Gre, new Dimension.Simple(18, 70))
 *             .series("cpu", Colors.BCya)
 *             .series("memory", Colors.BYel)
 *             .fillUnder("memory", true);
 *     chart.add("cpu", t, 37.5).add("memory", t, 62);      // as data comes in
 *     chart.redraw();
 *     chart.onChange((te) -&gt; status.setText(chart.getValue().series() + ": " + chart.getValue().y()));
 * </pre>
 * Points are kept in the order added; for a time series add them in x order.
 */
public class LineChart extends AbstractComponentWrappper<PlotArea> implements HasValue<LineChart.Point>, KeyboardHandler {

    /** a point of a series: what getValue() returns */
    public static class Point {
        private final String series;
        private final int index;
        private final double x;
        private final double y;

        Point(String series, int index, double x, double y) {
            this.series = series;
            this.index = index;
            this.x = x;
            this.y = y;
        }

        public String series() {
            return series;
        }

        /** its place in the series, from 0 */
        public int index() {
            return index;
        }

        public double x() {
            return x;
        }

        public double y() {
            return y;
        }
    }

    private static class Series {
        final String name;
        Color color;
        boolean fillUnder;
        boolean visible = true;
        final List<double[]> points = new ArrayList<double[]>();
        ConnectedPoints<?> line;                                   // as drawn last (for clicks)
        final Polygon legendLine = new Polygon();                  // what a click on its legend name reports
        final Polygon legendBox = new Polygon();                   // ... and on its [x] (hide / show)

        Series(String name, Color color) {
            this.name = name;
            this.color = color;
            this.legendLine.setName(name);
            this.legendBox.setName(name + " [x]");
        }
    }

    private final Map<String, Series> series = new LinkedHashMap<String, Series>();
    private Series selectedSeries;
    private int selectedIndex = -1;
    private EventHandler onChange;

    public LineChart(Color color, Dimension size) {
        super(color, new PlotArea(color, size));
        plot().axes(true)
              .onDraw(this::draw)
              .onClick((click) -> {
                  Series line = seriesDrawnAs(click.shape());
                  if (line != null) {
                      select(line, nearestPoint(line, click.at()), click.eventBus());
                      return;
                  }
                  Series box = seriesWithLegendBox(click.shape());
                  if (box != null) {
                      visible(box.name, !box.visible);                      // [x]: hide / show
                      click.eventBus().raiseEvent(new RequestUpdateCanvasEvent(this));
                      return;
                  }
                  Series legend = seriesWithLegendLine(click.shape());
                  if (legend != null && legend.visible && !legend.points.isEmpty()) {
                      select(legend, nearestX(legend), click.eventBus());     // the series becomes the current one
                  }
              });
    }

    private PlotArea plot() {
        return getWrappedComponent();
    }

    /** a series (its points are added with add); the same name again changes its colour */
    public LineChart series(String name, Color color) {
        Series s = series.get(name);
        if (s == null) {
            series.put(name, new Series(name, color));
        } else {
            s.color = color;
        }
        return this;
    }

    /** a series with its points (any Vec2: x, y) */
    public LineChart series(String name, Color color, Iterable<? extends Vec2<?>> points) {
        series(name, color);
        Series s = series.get(name);
        s.points.clear();
        for (Vec2<?> p : points) {
            s.points.add(new double[] { p.getX(), p.getY() });
        }
        return refit();
    }

    /** one more point at the end of a series (made with series(name, color) first) */
    public LineChart add(String name, double x, double y) {
        Series s = series.get(name);
        if (s == null) {
            throw new IllegalArgumentException("LineChart.add: no series '" + name + "' - make it with series(name, color) first");
        }
        s.points.add(new double[] { x, y });
        return this;
    }

    /** drops a series's points (to fill it again - e.g. live data); it stays the current series if it was */
    public LineChart clear(String name) {
        Series s = series.get(name);
        if (s != null) {
            s.points.clear();
        }
        return this;
    }

    /** fills between the series's line and y 0 */
    public LineChart fillUnder(String name, boolean fill) {
        Series s = series.get(name);
        if (s != null) {
            s.fillUnder = fill;
        }
        return redraw();
    }

    /** shows or hides a series (a hidden one keeps its legend line, greyed; the window fits the shown ones) */
    public LineChart visible(String name, boolean visible) {
        Series s = series.get(name);
        if (s != null) {
            s.visible = visible;
            if (!visible && s == selectedSeries) {
                selectedSeries = null;
                selectedIndex = -1;
            }
        }
        return refit();
    }

    public boolean isVisible(String name) {
        Series s = series.get(name);
        return s != null && s.visible;
    }

    public LineChart setTitle(String title) {
        plot().setTitle(title);
        return this;
    }

    /** the selection changed (a click, an arrow key): getValue() is the point now selected */
    public void onChange(EventHandler handler) {
        this.onChange = handler;
    }

    /** the selected point; null: none yet */
    @Override
    public Point getValue() {
        if (selectedSeries == null || selectedIndex < 0 || selectedIndex >= selectedSeries.points.size()) {
            return null;
        }
        double[] p = selectedSeries.points.get(selectedIndex);
        return new Point(selectedSeries.name, selectedIndex, p[0], p[1]);
    }

    /** draw again, the window fitted to all the points - after adding points */
    public LineChart redraw() {
        return refit();
    }

    @Override
    public void resize(Dimension d) {
        super.resize(d);
        refit();
    }

    private LineChart refit() {
        if (plot() == null) {
            return this;                                           // still being built
        }
        List<Vec2<?>> all = new ArrayList<Vec2<?>>();
        for (Series s : series.values()) {
            if (!s.visible) {
                continue;
            }
            for (double[] p : s.points) {
                all.add(new Vec2.BaseVec2((float) p[0], (float) p[1]));
            }
        }
        if (all.isEmpty()) {
            plot().redraw();
        } else {
            plot().fitTo(all);
        }
        return this;
    }

    /**
     * LEFT / RIGHT: along the current series (the first shown one if none yet); UP / DOWN: the
     * series above / below in the legend, at about the same x
     */
    @Override
    public boolean onKey(KeyEvent ke) {
        boolean along = ke.isKeyStroke(Keys.RIGHT_KEYSTROKE) || ke.isKeyStroke(Keys.LEFT_KEYSTROKE);
        boolean across = ke.isKeyStroke(Keys.UP_KEYSTROKE) || ke.isKeyStroke(Keys.DOWN_KEYSTROKE);
        if (!along && !across) {
            return false;
        }
        List<Series> shown = shownWithPoints();
        if (shown.isEmpty()) {
            return false;
        }
        Series s = selectedSeries;
        if (s == null) {
            s = shown.get(0);
            select(s, ke.isKeyStroke(Keys.LEFT_KEYSTROKE) ? s.points.size() - 1 : 0, ke.getEventBus());
            return true;
        }
        if (along) {
            int direction = ke.isKeyStroke(Keys.RIGHT_KEYSTROKE) ? 1 : -1;
            select(s, Math.max(0, Math.min(s.points.size() - 1, selectedIndex + direction)), ke.getEventBus());
        } else {
            int direction = ke.isKeyStroke(Keys.DOWN_KEYSTROKE) ? 1 : -1;
            Series next = shown.get(Math.floorMod(shown.indexOf(s) + direction, shown.size()));
            select(next, nearestX(next), ke.getEventBus());
        }
        return true;
    }

    @Override
    public boolean wantsKey(KeyEvent ke) {
        return !shownWithPoints().isEmpty() && (ke.isKeyStroke(Keys.RIGHT_KEYSTROKE) || ke.isKeyStroke(Keys.LEFT_KEYSTROKE)
                                                || ke.isKeyStroke(Keys.UP_KEYSTROKE) || ke.isKeyStroke(Keys.DOWN_KEYSTROKE));
    }

    /** the shown series that have points, in legend order */
    private List<Series> shownWithPoints() {
        List<Series> shown = new ArrayList<Series>();
        for (Series s : series.values()) {
            if (s.visible && !s.points.isEmpty()) {
                shown.add(s);
            }
        }
        return shown;
    }

    /** the point of s nearest in x to the selected point (its first point if none selected) */
    private int nearestX(Series s) {
        Point now = getValue();
        if (now == null) {
            return 0;
        }
        int best = 0;
        for (int i = 1; i < s.points.size(); i++) {
            if (Math.abs(s.points.get(i)[0] - now.x()) < Math.abs(s.points.get(best)[0] - now.x())) {
                best = i;
            }
        }
        return best;
    }

    private void select(Series s, int index, EventBus bus) {
        if (s == selectedSeries && index == selectedIndex) {
            return;
        }
        selectedSeries = s;
        selectedIndex = index;
        plot().redraw();
        if (bus == null) {
            return;
        }
        if (onChange != null) {
            ValueChangedEvent<Point> changed = new ValueChangedEvent<Point>(this);
            changed.setEventBus(bus);
            onChange.handle(changed);
        }
        bus.raiseEvent(new RequestUpdateCanvasEvent(this));
    }

    private Series seriesWithLegendBox(Shape<?> shape) {
        for (Series s : series.values()) {
            if (s.legendBox == shape) {
                return s;
            }
        }
        return null;
    }

    private Series seriesWithLegendLine(Shape<?> shape) {
        for (Series s : series.values()) {
            if (s.legendLine == shape) {
                return s;
            }
        }
        return null;
    }

    private Series seriesDrawnAs(Shape<?> shape) {
        for (Series s : series.values()) {
            if (s.line != null && s.line == shape) {
                return s;
            }
        }
        return null;
    }

    /** the point of a series nearest a data position - measured on screen, so x and y count alike */
    private int nearestPoint(Series s, Vec2<?> at) {
        Position mouse = plot().toGrid(at.getX(), at.getY());
        int best = 0;
        double bestDistance = Double.MAX_VALUE;
        for (int i = 0; i < s.points.size(); i++) {
            Position p = plot().toGrid(s.points.get(i)[0], s.points.get(i)[1]);
            double d = Math.hypot(p.getRow() - mouse.getRow(), p.getCol() - mouse.getCol());
            if (d < bestDistance) {
                bestDistance = d;
                best = i;
            }
        }
        return best;
    }

    // ---- drawing --------------------------------------------------------------------------------

    @SuppressWarnings({ "rawtypes" })
    private void draw(Pen pen) {
        for (Series s : series.values()) {
            s.line = null;
            if (s.points.isEmpty() || !s.visible) {
                continue;
            }
            if (s.fillUnder) {
                pen.color(s.color).fillUnder((x) -> interpolate(s.points, x), s.points.get(0)[0], s.points.get(s.points.size() - 1)[0]);
            }
            ConnectedPoints line = new ConnectedPoints(false);
            for (double[] p : s.points) {
                line.addPoint((float) p[0], (float) p[1]);
            }
            line.setName(s.name);
            s.line = line;
            pen.color(s.color).draw(line);
        }
        legend(pen);
        Point selected = getValue();
        if (selected != null) {
            double w = plot().characterWidth(), h = plot().characterHeight();
            pen.color(selectedSeries.color).text(selected.x() - w / 2, selected.y(), "◆");
            String label = ChartItems.format(selected.x()) + ", " + ChartItems.format(selected.y());
            double x = Math.min(selected.x() + w, plot().getXMax() - (label.length() + 1) * w);
            pen.text(x, selected.y() + h, label);
        }
    }

    /**
     * A line per series, top right: a check box (click: hide / show the series) and its name in its
     * colour (click: make it the current series, marked ">"); a hidden one is grey and unticked.
     */
    private void legend(Pen pen) {
        double w = plot().characterWidth(), h = plot().characterHeight();
        int widest = 0;
        for (Series s : series.values()) {
            widest = Math.max(widest, 4 + nameText(s).length());
        }
        double x = plot().getXMax() - (widest + 1) * w;
        double y = plot().getYMax() - h / 2;
        for (Series s : series.values()) {
            Color color = s.visible ? s.color : Colors.BBla;
            pen.color(color).text(x, y, s.visible ? "[x]" : "[ ]", s.legendBox);
            pen.color(color).text(x + 4 * w, y, nameText(s), s.legendLine);
            y -= h;
        }
    }

    private String nameText(Series s) {
        return (s == selectedSeries ? ">" : " ") + "━ " + s.name;
    }

    /** the series's y at x, along its straight lines (outside its points: the nearest end) */
    private static double interpolate(List<double[]> points, double x) {
        if (x <= points.get(0)[0]) {
            return points.get(0)[1];
        }
        for (int i = 1; i < points.size(); i++) {
            double[] a = points.get(i - 1), b = points.get(i);
            if (x <= b[0]) {
                return b[0] == a[0] ? b[1] : a[1] + (b[1] - a[1]) * (x - a[0]) / (b[0] - a[0]);
            }
        }
        return points.get(points.size() - 1)[1];
    }
}
