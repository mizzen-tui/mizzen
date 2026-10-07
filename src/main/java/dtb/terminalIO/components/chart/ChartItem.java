package dtb.terminalIO.components.chart;

/**
 * One entry of a chart (a slice, a bar): your object back, and what the chart worked out for it.
 * What a chart's getValue() returns - the entry selected (clicked, or moved to with the arrows).
 */
public class ChartItem<T> {

    private final T item;
    private final int index;
    private final double value;
    private final double percent;
    private final String label;

    ChartItem(T item, int index, double value, double percent, String label) {
        this.item = item;
        this.index = index;
        this.value = value;
        this.percent = percent;
        this.label = label;
    }

    /** your object */
    public T item() {
        return item;
    }

    /** its place in the items, from 0 */
    public int index() {
        return index;
    }

    /** its size (your sizing function's answer) */
    public double value() {
        return value;
    }

    /** its share of all the items' sizes, 0..100 */
    public double percent() {
        return percent;
    }

    /** its name (your labelling function's answer) */
    public String label() {
        return label;
    }
}
