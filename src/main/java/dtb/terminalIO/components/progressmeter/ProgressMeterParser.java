package dtb.terminalIO.components.progressmeter;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.regex.Matcher;

import dtb.terminalIO.components.progressmeter.ProgressMeter.Status;
import dtb.terminalIO.util.RegexLineParser;

/**
 * Turns marked lines in a job's output into progress, step and status - you say what the marks
 * look like, with named groups:
 * <pre>
 *     ProgressMeterParser parser = new ProgressMeterParser()
 *             .addProgressPattern("##progress done=(?&lt;done&gt;\\d+) total=(?&lt;total&gt;\\d+) msg=\"(?&lt;msg&gt;[^\"]*)\"")
 *             .addStepPattern("##step (?&lt;step&gt;\\d+) of (?&lt;total&gt;\\d+) msg=\"(?&lt;msg&gt;[^\"]*)\"")
 *             .addStatusPattern("##status (?&lt;status&gt;\\w+) msg=\"(?&lt;msg&gt;[^\"]*)\"")
 *             .drive(meter)                       // progress, step and status go to the meter
 *             .onText(line -&gt; ...);              // every other line: ordinary output
 *     ... for each line of output:  parser.accept(line);
 * </pre>
 * drive(meter) is the usual wiring; onProgress / onStep / onStatus set your own handlers instead.
 * A marked line whose numbers don't fit (too long) is passed on as ordinary text.
 * <p/>
 * Parsing is RegexLineParser's: rules are tried in the order added, the first that matches the
 * whole line takes it.
 */
public class ProgressMeterParser implements Consumer<String> {

    @FunctionalInterface
    public interface ProgressHandler {
        void accept(long done, long total, String message);
    }

    @FunctionalInterface
    public interface StepHandler {
        void accept(int step, int total, String message);
    }

    @FunctionalInterface
    public interface StatusHandler {
        void accept(String status, String message);
    }

    private final RegexLineParser parser = new RegexLineParser();

    private ProgressHandler progressHandler = (done, total, msg) -> {};
    private StepHandler stepHandler = (step, total, msg) -> {};
    private StatusHandler statusHandler = (status, msg) -> {};
    private Consumer<String> textHandler = line -> {};

    public ProgressMeterParser() {
        parser.onText(line -> textHandler.accept(line));
    }

    /**
     * Sends what the marks say to meter: progress, step, and status (ok / success / done mark it
     * SUCCESS, failed / fail / error FAILED, cancelled / canceled CANCELLED; others only show their
     * message); each message, if any, becomes the meter's message.
     */
    public ProgressMeterParser drive(ProgressMeter meter) {
        Objects.requireNonNull(meter);
        onProgress((done, total, msg) -> {
            meter.setProgress(done, total);
            showMessage(meter, msg);
        });
        onStep((step, total, msg) -> {
            meter.setStep(step, total);
            showMessage(meter, msg);
        });
        onStatus((status, msg) -> {
            Status meterStatus = statusFor(status);
            if (meterStatus != null) {
                meter.setStatus(meterStatus);
            }
            showMessage(meter, msg);
        });
        return this;
    }

    /** a status word as the meter's Status: SUCCESS, FAILED, CANCELLED, or null for any other word */
    public static Status statusFor(String status) {
        if (status == null) {
            return null;
        }
        switch (status.toLowerCase()) {
            case "ok":
            case "success":
            case "done":
                return Status.SUCCESS;
            case "failed":
            case "fail":
            case "error":
                return Status.FAILED;
            case "cancelled":
            case "canceled":
                return Status.CANCELLED;
            default:
                return null;
        }
    }

    private static void showMessage(ProgressMeter meter, String msg) {
        if (msg != null) {
            meter.setMessage(msg);
        }
    }

    public ProgressMeterParser onProgress(ProgressHandler handler) {
        progressHandler = Objects.requireNonNull(handler);
        return this;
    }

    public ProgressMeterParser onStep(StepHandler handler) {
        stepHandler = Objects.requireNonNull(handler);
        return this;
    }

    public ProgressMeterParser onStatus(StatusHandler handler) {
        statusHandler = Objects.requireNonNull(handler);
        return this;
    }

    /** lines no pattern matches: ordinary output */
    public ProgressMeterParser onText(Consumer<String> handler) {
        textHandler = Objects.requireNonNull(handler);
        return this;
    }

    /**
     * Required named groups: done, total
     * Optional named group: msg
     */
    public ProgressMeterParser addProgressPattern(String regex) {
        parser.addRule(regex, matcher -> {
            try {
                long done = Long.parseLong(matcher.group("done"));
                long total = Long.parseLong(matcher.group("total"));
                progressHandler.accept(done, total, optionalGroup(matcher, "msg"));
            } catch (NumberFormatException tooLong) {
                textHandler.accept(matcher.group());
            }
        });
        return this;
    }

    /**
     * Required named groups: step, total
     * Optional named group: msg
     */
    public ProgressMeterParser addStepPattern(String regex) {
        parser.addRule(regex, matcher -> {
            try {
                int step = Integer.parseInt(matcher.group("step"));
                int total = Integer.parseInt(matcher.group("total"));
                stepHandler.accept(step, total, optionalGroup(matcher, "msg"));
            } catch (NumberFormatException tooLong) {
                textHandler.accept(matcher.group());
            }
        });
        return this;
    }

    /**
     * Required named group: status
     * Optional named group: msg
     */
    public ProgressMeterParser addStatusPattern(String regex) {
        parser.addRule(regex, matcher -> {
            String status = matcher.group("status");
            statusHandler.accept(status, optionalGroup(matcher, "msg"));
        });
        return this;
    }

    /**
     * Escape hatch for application-specific parsing.
     */
    public ProgressMeterParser addRule(String regex, Consumer<Matcher> action) {
        parser.addRule(regex, action);
        return this;
    }

    /**
     * Whether this line is one of the marks (a pattern matches it) - without acting on it. A
     * reading thread can route with it: marks to the event thread for the meter, the rest to an
     * output pane.
     */
    public boolean matches(String line) {
        return parser.matches(line);
    }

    @Override
    public void accept(String line) {
        parser.accept(line);
    }

    /**
     * Returns null if the named group is absent or unmatched.
     */
    private static String optionalGroup(Matcher matcher, String name) {
        try {
            return matcher.group(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
