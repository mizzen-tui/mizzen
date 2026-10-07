package dtb.terminalIO.components.chart;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;

import dtb.terminalIO.components.AbstractComponentWrappper;
import dtb.terminalIO.components.input.HasValue;
import dtb.terminalIO.components.plot.Pen;
import dtb.terminalIO.components.plot.PlotArea;
import dtb.terminalIO.components.plot.shape.Polygon;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.keyboard.KeyEvent;
import dtb.terminalIO.events.keyboard.KeyboardHandler;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.position.Vec2;

/**
 * A donut chart of your objects: one slice each, sized and named by the functions you give, with a
 * legend. Pick a slice with a click (on it, or on its legend line), or the arrow keys - like a list: onChange tells you, and
 * getValue() gives the slice back (your object, its size, its percent, its name).
 * <pre>
 *     DonutChart&lt;Disk&gt; donut = new DonutChart&lt;Disk&gt;(Colors.Gre, new Dimension.Simple(16, 50))
 *             .sliceSize(Disk::getUsedGb)
 *             .sliceLabel(Disk::getMount)
 *             .setItems(disks);
 *     donut.onChange((te) -&gt; {
 *         ChartItem&lt;Disk&gt; slice = donut.getValue();
 *         status.setText(slice.label() + ": " + slice.percent() + "%");
 *         te.getEventBus().raiseEvent(new RequestUpdateCanvasEvent(status));
 *     });
 * </pre>
 * The selected slice is pulled out a little and named in the hole. Drawn on a PlotArea (proportions
 * kept, so it stays round at any size).
 */
public class DonutChart<T> extends AbstractComponentWrappper<PlotArea> implements HasValue<ChartItem<T>>, KeyboardHandler {

    private static final double LEGEND_WIDTH = 2.6;                 // in donut radii, right of the donut

    private final ChartItems<T> items = new ChartItems<T>();
    private final List<Polygon> slices = new ArrayList<Polygon>();
    private double hole = 0.55;                                     // the hole's radius, as a part of the donut's

    public DonutChart(Color color, Dimension size) {
        super(color, new PlotArea(color, size));
        plot().scaling(PlotArea.Scaling.EQUAL)
              .showing(-1.15, 1.15 + LEGEND_WIDTH, -1.15, 1.15)
              .onDraw(this::draw)
              .onClick((click) -> {
                  int i = slices.indexOf(click.shape());
                  if (i >= 0) {
                      items.select(i, this, this::redraw, click.eventBus());
                  }
              });
    }

    private PlotArea plot() {
        return getWrappedComponent();
    }

    /** how big each object's slice is */
    public DonutChart<T> sliceSize(ToDoubleFunction<? super T> size) {
        items.size(size);
        return redraw();
    }

    /** each slice's name (legend, getValue().label()) */
    public DonutChart<T> sliceLabel(Function<? super T, String> label) {
        items.label(label);
        return redraw();
    }

    /** one slice per object, in this order, clockwise from the top */
    public DonutChart<T> setItems(List<T> items) {
        this.items.setItems(items);
        return redraw();
    }

    /** the slices' colours, in turn (default: a palette of bright colours) */
    public DonutChart<T> colors(Color... colors) {
        items.colors(colors);
        return redraw();
    }

    /** the hole's radius as a part of the donut's: 0 is a pie, 0.55 the default */
    public DonutChart<T> hole(double hole) {
        this.hole = Math.max(0, Math.min(0.9, hole));
        return redraw();
    }

    public DonutChart<T> setTitle(String title) {
        plot().setTitle(title);
        return this;
    }

    /** the selection changed (a click, an arrow key): getValue() is the slice now selected */
    public void onChange(EventHandler handler) {
        items.onChange(handler);
    }

    /** the selected slice; null: none yet */
    @Override
    public ChartItem<T> getValue() {
        return items.item(items.selected());
    }

    /** draw again - e.g. after your objects' sizes changed */
    public DonutChart<T> redraw() {
        plot().redraw();
        return this;
    }

    /** RIGHT / DOWN: the next slice, LEFT / UP: the previous one */
    @Override
    public boolean onKey(KeyEvent ke) {
        int direction = ke.isKeyStroke(Keys.RIGHT_KEYSTROKE) || ke.isKeyStroke(Keys.DOWN_KEYSTROKE) ? 1
                      : ke.isKeyStroke(Keys.LEFT_KEYSTROKE) || ke.isKeyStroke(Keys.UP_KEYSTROKE) ? -1 : 0;
        if (direction == 0 || items.count() == 0) {
            return false;
        }
        items.select(items.step(direction), this, this::redraw, ke.getEventBus());
        return true;
    }

    @Override
    public boolean wantsKey(KeyEvent ke) {
        return items.count() > 0 && (ke.isKeyStroke(Keys.RIGHT_KEYSTROKE) || ke.isKeyStroke(Keys.DOWN_KEYSTROKE)
                                     || ke.isKeyStroke(Keys.LEFT_KEYSTROKE) || ke.isKeyStroke(Keys.UP_KEYSTROKE));
    }

    // ---- drawing --------------------------------------------------------------------------------

    private void draw(Pen pen) {
        slices.clear();
        double total = items.total();
        if (items.count() == 0 || total <= 0) {
            pen.text(-0.4, 0, "no data");
            return;
        }
        double start = 90;                                          // degrees: the top, going clockwise
        for (int i = 0; i < items.count(); i++) {
            double sweep = items.value(i) * 360 / total;
            Polygon slice = slice(start, start - sweep, i == items.selected() ? 0.08 : 0);
            slice.setFilled(true);
            slice.setName(items.label(i));
            slices.add(slice);
            pen.color(items.color(i)).draw(slice);
            start -= sweep;
        }
        legend(pen);
        ChartItem<T> selected = getValue();
        if (selected != null && hole > 0.3) {
            centred(pen, 0, plot().characterHeight() / 2, selected.label());
            centred(pen, 0, -plot().characterHeight() / 2, String.format("%.1f%%", selected.percent()));
        }
    }

    /** a ring piece from angle a0 to a1 (degrees, a1 < a0), pulled out by pull along its middle */
    private Polygon slice(double a0, double a1, double pull) {
        double middle = Math.toRadians((a0 + a1) / 2);
        double dx = Math.cos(middle) * pull, dy = Math.sin(middle) * pull;
        List<Vec2<?>> points = new ArrayList<Vec2<?>>();
        int steps = Math.max(2, (int) Math.ceil((a0 - a1) / 3));      // a point every 3 degrees or so
        for (int k = 0; k <= steps; k++) {
            double a = Math.toRadians(a0 - (a0 - a1) * k / steps);
            points.add(new Vec2.BaseVec2((float) (dx + Math.cos(a)), (float) (dy + Math.sin(a))));
        }
        for (int k = steps; k >= 0; k--) {
            double a = Math.toRadians(a0 - (a0 - a1) * k / steps);
            points.add(new Vec2.BaseVec2((float) (dx + hole * Math.cos(a)), (float) (dy + hole * Math.sin(a))));
        }
        return new Polygon(points);
    }

    /** a line per slice, right of the donut: a colour block, its name, its percent; ">" marks the selected one - clicking a line picks its slice */
    private void legend(Pen pen) {
        double row = plot().characterHeight();
        double x = 1.35;
        double y = Math.min(1.1, (items.count() - 1) * row / 2);       // centred down the side, as far as it fits
        double total = items.total();
        for (int i = 0; i < items.count(); i++) {
            String mark = i == items.selected() ? ">" : " ";
            pen.color(items.color(i)).text(x, y - i * row, mark + "█ " + items.label(i) + String.format("  %.1f%%", items.value(i) * 100 / total),
                                           slices.get(i));         // a click on the line picks its slice
        }
    }

    private void centred(Pen pen, double x, double y, String text) {
        pen.color(getColor()).text(x - text.length() * plot().characterWidth() / 2, y, text);
    }
}
