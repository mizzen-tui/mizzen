package dtb.terminalIO.components.progressmeter;

import java.util.Arrays;
import java.util.Objects;

import dtb.terminalIO.components.AbstractComponentWithContainer;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.composite.Composite;
import dtb.terminalIO.components.composite.HorizontalPanel;
import dtb.terminalIO.components.composite.VerticalPanel;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.geom.Dimension;

/**
 * Shows how far a long job has got: a bar with its percentage, and a message - the step it is on,
 * what it is doing, and how it ended. On two rows (the default), or on one, message first as
 * terminal tools do (pip, rich, tqdm):
 * <pre>
 *     TWO_ROWS:  ██████████░░░░░░░░░░  50%
 *                Step 2/5 - Backing up database
 *
 *     ONE_ROW:   Step 2/5 - Backing up data  ██████████░░░░░░░░░░  50%
 *
 *     ProgressMeter meter = new ProgressMeter(40);             // a 40 wide bar
 *     meter.setStep(2, 5);
 *     meter.setProgress(50, 100);
 *     meter.setMessage("Backing up database");
 *     ...
 *     meter.setStatus(Status.SUCCESS);       // "OK - ..."; FAILED turns the bar red, CANCELLED yellow
 * </pre>
 * The bar is drawn with characters (filled █, empty ░) and coloured, so it still shows without
 * colour - in a log, or text output. It keeps its size as it fills, so nothing around it moves;
 * resize it and the bar and message take the new width (e.g. on a WindowFrame's border, it follows
 * the window). To drive it from a script's output, see ProgressMeterParser.drive(meter).
 * <p/>
 * Call it on the event thread, like any component: a thread reading a job's output hands each line
 * over with eventBus.asynch(...) rather than calling the meter itself.
 */
public class ProgressMeter extends AbstractComponentWithContainer<Composite> {

    public enum Status {
        IDLE, RUNNING, SUCCESS, FAILED, CANCELLED
    }

    /** TWO_ROWS: the message under the bar (the default); ONE_ROW: the message before it, cut to fit */
    public enum Layout {
        TWO_ROWS, ONE_ROW
    }

    private static final char FILLED = '█';
    private static final char EMPTY = '░';
    private static final int PERCENT_WIDTH = 6;         // " 100% "
    private static final int MIN_WIDTH = PERCENT_WIDTH + 6;

    private TextComponent bar;
    private TextComponent percentage;
    private TextComponent message;

    private Layout layout = Layout.TWO_ROWS;
    private int width;                                   // the whole meter's

    private Color filledColor = Colors.Gre;
    private Color emptyColor = Colors.IBla;
    private Color failedColor = Colors.Red;
    private Color cancelledColor = Colors.Yel;

    private long done = 0;
    private long total = 0;
    private int step = 0;
    private int steps = 0;
    private String currentMessage = "";
    private Status status = Status.IDLE;

    /** barWidth: the bar's width to start with; the meter is that plus its percentage, and can be resized */
    public ProgressMeter(Color color, int barWidth) {
        super(color, new VerticalPanel(color));

        if (barWidth < 1) {
            throw new IllegalArgumentException("barWidth must be positive");
        }
        this.width = Math.max(MIN_WIDTH, barWidth + PERCENT_WIDTH);
        layOut();
    }

    public ProgressMeter(int barWidth) {
        this(Colors.DEFAULT_COLOR, barWidth);
    }

    public Layout getLayout() {
        return layout;
    }

    /** two rows (message under the bar) or one (message after it); the width stays the same */
    public ProgressMeter setLayout(Layout layout) {
        if (this.layout != Objects.requireNonNull(layout)) {
            this.layout = layout;
            layOut();
        }
        return this;
    }

    /** the width is what counts: the bar and the message share it; the height is the layout's rows */
    @Override
    public void resize(Dimension d) {
        int newWidth = Math.max(MIN_WIDTH, d.getWidth());
        if (newWidth != width) {
            width = newWidth;
            render();
        }
        super.resize(new Dimension.Simple(rows(), width));
    }

    private int rows() {
        return layout == Layout.TWO_ROWS ? 2 : 1;
    }

    /** done out of total (done past total shows as full); the first progress marks it RUNNING */
    public void setProgress(long done, long total) {
        if (total <= 0 || done < 0) {
            throw new IllegalArgumentException("Invalid progress: " + done + " of " + total);
        }

        this.done = Math.min(done, total);
        this.total = total;

        if (status == Status.IDLE) {
            status = Status.RUNNING;
        }

        render();
    }

    /** progress as a fraction, 0 to 1 */
    public void setProgress(double fraction) {
        if (!Double.isFinite(fraction) || fraction < 0 || fraction > 1) {
            throw new IllegalArgumentException("Invalid fraction: " + fraction);
        }

        setProgress(Math.round(fraction * 10000), 10000);
    }

    /** step (1 to steps) of steps: shown as "Step 2/5 - " before the message */
    public void setStep(int step, int steps) {
        if (steps <= 0 || step < 1 || step > steps) {
            throw new IllegalArgumentException("Invalid step: " + step + " of " + steps);
        }

        this.step = step;
        this.steps = steps;
        render();
    }

    public void setMessage(String message) {
        this.currentMessage = message == null ? "" : message;
        render();
    }

    /**
     * SUCCESS shows "OK - " before the message; FAILED shows "FAILED - " and turns the bar red;
     * CANCELLED shows "CANCELLED - " and turns it yellow
     */
    public void setStatus(Status status) {
        this.status = Objects.requireNonNull(status);
        render();
    }

    public Status getStatus() {
        return status;
    }

    /** back to empty and IDLE: no progress, step, message */
    public void reset() {
        done = total = 0;
        step = steps = 0;
        currentMessage = "";
        status = Status.IDLE;
        render();
    }

    /** the colours of the bar's filled (█) and empty (░) parts (default Colors.Gre, Colors.IBla) */
    public ProgressMeter setBarColors(Color filled, Color empty) {
        this.filledColor = Objects.requireNonNull(filled);
        this.emptyColor = Objects.requireNonNull(empty);
        render();
        return this;
    }

    /** the bar's filled part once FAILED (default Colors.Red) */
    public ProgressMeter setFailedColor(Color failed) {
        this.failedColor = Objects.requireNonNull(failed);
        render();
        return this;
    }

    /** the bar's filled part once CANCELLED (default Colors.Yel) */
    public ProgressMeter setCancelledColor(Color cancelled) {
        this.cancelledColor = Objects.requireNonNull(cancelled);
        render();
        return this;
    }

    /** the parts, arranged for the layout: the message under the bar and percentage, or before them */
    private void layOut() {
        Color color = getColor();
        bar = new TextComponent(color, "");
        percentage = new TextComponent(color, "");
        message = new TextComponent(color, "");

        Composite panel = getWrappedComponent();
        panel.clearAndRemoveAllChildren();
        HorizontalPanel row = new HorizontalPanel(color, 0);   // no gaps: the percentage brings its own spaces
        if (layout == Layout.ONE_ROW) {
            row.add(message);
            row.add(bar);
            row.add(percentage);
            panel.add(row);
        } else {
            row.add(bar);
            row.add(percentage);
            panel.add(row);
            panel.add(message);
        }
        render();
    }

    /** the bar's width now: the whole width less the percentage (two rows), or half of that (one row) */
    private int barWidth() {
        int room = width - PERCENT_WIDTH;
        return layout == Layout.TWO_ROWS ? room : Math.max(5, room / 2);
    }

    private int messageWidth() {
        return layout == Layout.TWO_ROWS ? width : width - PERCENT_WIDTH - barWidth();
    }

    private void render() {
        int barWidth = barWidth();
        int filled = total > 0 ? (int) ((double) done / total * barWidth) : 0;
        filled = Math.max(0, Math.min(barWidth, filled));

        Color filledNow = status == Status.FAILED ? failedColor : status == Status.CANCELLED ? cancelledColor : filledColor;
        bar.setTextAndResize(filledNow.asString() + repeat(FILLED, filled)
                           + emptyColor.asString() + repeat(EMPTY, barWidth - filled)
                           + Colors.RCol.asString());   // the same visible width whatever the progress

        int pct = total > 0 ? (int) Math.round(100.0 * done / total) : 0;
        percentage.setTextAndResize(String.format(" %3d%% ", pct));

        String stepPrefix = steps > 0 ? "Step " + step + "/" + steps + " - " : "";
        String statusPrefix;
        switch (status) {
            case SUCCESS:
                statusPrefix = "OK - ";
                break;
            case FAILED:
                statusPrefix = "FAILED - ";
                break;
            case CANCELLED:
                statusPrefix = "CANCELLED - ";
                break;
            default:
                statusPrefix = "";
                break;
        }

        int messageWidth = messageWidth();
        int room = layout == Layout.ONE_ROW ? messageWidth - 1 : messageWidth;   // one row: a space before the bar
        String display = statusPrefix + stepPrefix + currentMessage;
        if (display.length() > room) {
            display = display.substring(0, room);            // its own row (or its share of the one row) only
        }
        message.setTextAndResize(display + repeat(' ', messageWidth - display.length()));   // padded: erases the previous message
    }

    private static String repeat(char c, int count) {
        char[] chars = new char[Math.max(0, count)];
        Arrays.fill(chars, c);
        return new String(chars);
    }
}
