package mizzen.components.chart;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;

import mizzen.components.Component;
import mizzen.components.input.HasValue;
import mizzen.controlcodes.color.Color;
import mizzen.controlcodes.color.Colors;
import mizzen.events.EventBus;
import mizzen.events.EventHandler;
import mizzen.events.RequestUpdateCanvasEvent;

/**
 * What a chart of your objects holds - the objects, how big each is and what it is called, their
 * colours, which one is selected, and who to tell when that changes. Held by DonutChart and
 * BarChart (each draws them its own way).
 */
class ChartItems<T> {

    static final Color[] PALETTE = { Colors.BCya, Colors.BYel, Colors.BPur, Colors.BGre, Colors.BRed, Colors.BBlu,
                                     Colors.Cya, Colors.Yel, Colors.Pur, Colors.Blu, Colors.Red };

    private List<T> items = Collections.emptyList();
    private ToDoubleFunction<? super T> size = (t) -> 1;
    private Function<? super T, String> label = String::valueOf;
    private Color[] colors = PALETTE;
    private int selected = -1;                     // -1: none
    private EventHandler onChange;

    void setItems(List<T> items) {
        this.items = items == null ? Collections.<T>emptyList() : items;
        if (selected >= this.items.size()) {
            selected = -1;
        }
    }

    void size(ToDoubleFunction<? super T> size) {
        this.size = size;
    }

    void label(Function<? super T, String> label) {
        this.label = label;
    }

    void colors(Color... colors) {
        this.colors = colors == null || colors.length == 0 ? PALETTE : colors;
    }

    void onChange(EventHandler onChange) {
        this.onChange = onChange;
    }

    int count() {
        return items.size();
    }

    /** a size: negative and not-a-number sizes count as 0 */
    double value(int i) {
        double v = size.applyAsDouble(items.get(i));
        return Double.isNaN(v) || v < 0 ? 0 : v;
    }

    double total() {
        double total = 0;
        for (int i = 0; i < items.size(); i++) {
            total += value(i);
        }
        return total;
    }

    double max() {
        double max = 0;
        for (int i = 0; i < items.size(); i++) {
            max = Math.max(max, value(i));
        }
        return max;
    }

    String label(int i) {
        return label.apply(items.get(i));
    }

    Color color(int i) {
        return colors[i % colors.length];
    }

    int selected() {
        return selected;
    }

    ChartItem<T> item(int i) {
        if (i < 0 || i >= items.size()) {
            return null;
        }
        double total = total();
        return new ChartItem<T>(items.get(i), i, value(i), total == 0 ? 0 : value(i) * 100 / total, label(i));
    }

    /** select entry i (-1: none); if it changed, tell onChange and repaint chart */
    <C extends Component<?> & HasValue<ChartItem<T>>> void select(int i, C chart, Runnable redraw, EventBus bus) {
        if (i == selected || i < -1 || i >= items.size()) {
            return;
        }
        selected = i;
        redraw.run();
        if (bus == null) {
            return;
        }
        if (onChange != null) {
            HasValue.ValueChangedEvent<ChartItem<T>> changed = new HasValue.ValueChangedEvent<ChartItem<T>>(chart);
            changed.setEventBus(bus);
            onChange.handle(changed);
        }
        bus.raiseEvent(new RequestUpdateCanvasEvent(chart));
    }

    /** a value to show: whole numbers without decimals, others with up to two */
    static String format(double value) {
        if (value == Math.rint(value) && Math.abs(value) < 1e15) {
            return String.valueOf((long) value);
        }
        return String.format("%.2f", value);
    }

    /** the next / previous entry (wrapping), from the selected one (or the first / last) */
    int step(int direction) {
        if (items.isEmpty()) {
            return -1;
        }
        if (selected < 0) {
            return direction > 0 ? 0 : items.size() - 1;
        }
        return Math.floorMod(selected + direction, items.size());
    }
}
