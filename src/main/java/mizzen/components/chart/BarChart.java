package mizzen.components.chart;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;

import mizzen.components.AbstractComponentWrappper;
import mizzen.components.input.HasValue;
import mizzen.components.plot.Pen;
import mizzen.components.plot.PlotArea;
import mizzen.components.plot.shape.Polygon;
import mizzen.controlcodes.color.Color;
import mizzen.controlcodes.color.Colors;
import mizzen.events.EventHandler;
import mizzen.events.Registration;
import mizzen.events.keyboard.KeyEvent;
import mizzen.events.keyboard.KeyboardHandler;
import mizzen.events.keyboard.Keys;
import mizzen.geom.Dimension;
import mizzen.geom.position.Vec2;

/**
 * A bar chart of your objects: one bar each, its height and name from the functions you give,
 * with a y axis and the names under the bars. Pick a bar with a click (on it, or its name), or the arrow keys - like a
 * list: onChange tells you, and getValue() gives the bar back (your object, its value, its percent
 * of the total, its name).
 * <pre>
 *     BarChart&lt;Sale&gt; bars = new BarChart&lt;Sale&gt;(Colors.Cya, new Dimension.Simple(16, 60))
 *             .barSize(Sale::getAmount)
 *             .barLabel(Sale::getRegion)
 *             .setItems(sales);
 *     bars.onChange((te) -&gt; status.setText(bars.getValue().label() + ": " + bars.getValue().value()));
 * </pre>
 * The bars are the chart's colour (or colors(...), one each in turn); the selected one is
 * highlighted, with its value above it.
 */
public class BarChart<T> extends AbstractComponentWrappper<PlotArea> implements HasValue<ChartItem<T>>, KeyboardHandler {

    private static final double BAR_WIDTH = 0.7;                  // of each bar's slot

    private final ChartItems<T> items = new ChartItems<T>();
    private final List<Polygon> bars = new ArrayList<Polygon>();
    private Color selectedColor = Colors.BYel;

    /** sized by what holds it (a splitter's rules, a resize): it starts at no size */
    public BarChart(Color color) {
        this(color, Dimension.ZERO);
    }

    public BarChart(Color color, Dimension size) {
        super(color, new PlotArea(color, size));
        items.colors(color);
        plot().axes(true)
              .xTickLabels(false)
              .yAxisAt(-Double.MAX_VALUE)                       // the left edge, not through the first bar
              .onDraw(this::draw)
              .onClick((click) -> {
                  int i = bars.indexOf(click.shape());
                  if (i >= 0) {
                      items.select(i, this, this::redraw, click.eventBus());
                  }
              });
        refit();
    }

    private PlotArea plot() {
        return getWrappedComponent();
    }

    /** each object's bar height */
    public BarChart<T> barSize(ToDoubleFunction<? super T> size) {
        items.size(size);
        return refit();
    }

    /** each bar's name (under it, and getValue().label()) */
    public BarChart<T> barLabel(Function<? super T, String> label) {
        items.label(label);
        return redraw();
    }

    /** one bar per object, left to right */
    public BarChart<T> setItems(List<T> items) {
        this.items.setItems(items);
        return refit();
    }

    /** the bars' colours, one each in turn (default: all the chart's colour) */
    public BarChart<T> colors(Color... colors) {
        items.colors(colors);
        return redraw();
    }

    public BarChart<T> selectedColor(Color selectedColor) {
        this.selectedColor = selectedColor;
        return redraw();
    }

    public BarChart<T> setTitle(String title) {
        plot().setTitle(title);
        return this;
    }

    /** the selection changed (a click, an arrow key): getValue() is the bar now selected */
    public Registration onChange(EventHandler handler) {
        return items.onChange(handler);
    }

    /** the selected bar; null: none yet */
    @Override
    public ChartItem<T> getValue() {
        return items.item(items.selected());
    }

    /** draw again - e.g. after your objects' values changed (the y axis is fitted to them again) */
    public BarChart<T> redraw() {
        return refit();
    }

    @Override
    public void resize(Dimension d) {
        super.resize(d);
        refit();
    }

    /** the window: a slot per bar across, 0 to just above the tallest bar up - plus a row below 0 for the names */
    private BarChart<T> refit() {
        if (plot() == null) {
            return this;                                          // still being built
        }
        int n = Math.max(1, items.count());
        double top = items.max() > 0 ? items.max() * 1.15 : 1;
        plot().showing(-0.6, n - 0.4, 0, top);
        double nameRow = plot().characterHeight() * 1.5;       // with this window: one and a half rows of names
        plot().showing(-0.6, n - 0.4, -nameRow, top);
        return this;
    }

    /** LEFT / UP: the previous bar, RIGHT / DOWN: the next one */
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
        bars.clear();
        double charWidth = plot().characterWidth();
        double charHeight = plot().characterHeight();
        int nameChars = Math.max(1, (int) Math.floor(1 / charWidth) - 1);        // a slot's width, in characters
        for (int i = 0; i < items.count(); i++) {
            double value = items.value(i);
            Polygon bar = new Polygon(listOf(new Vec2.BaseVec2((float) (i - BAR_WIDTH / 2), 0f),
                                             new Vec2.BaseVec2((float) (i + BAR_WIDTH / 2), 0f),
                                             new Vec2.BaseVec2((float) (i + BAR_WIDTH / 2), (float) value),
                                             new Vec2.BaseVec2((float) (i - BAR_WIDTH / 2), (float) value)));
            bar.setFilled(true);
            bar.setName(items.label(i));
            bars.add(bar);
            boolean selected = i == items.selected();
            pen.color(selected ? selectedColor : items.color(i)).draw(bar);

            String name = items.label(i);
            if (name.length() > nameChars) {
                name = name.substring(0, nameChars);
            }
            pen.color(selected ? selectedColor : getColor()).text(i - name.length() * charWidth / 2, -charHeight * 0.75, name, bar);   // its name clicks as the bar
            if (selected) {
                String shown = ChartItems.format(value);
                pen.text(i - shown.length() * charWidth / 2, value + charHeight, shown);
            }
        }
    }

    private static List<Vec2<?>> listOf(Vec2<?>... points) {
        List<Vec2<?>> list = new ArrayList<Vec2<?>>();
        for (Vec2<?> p : points) {
            list.add(p);
        }
        return list;
    }
}
