
package dtb.terminalIO.examples;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.components.ButtonBar;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.composite.Composite;
import dtb.terminalIO.components.input.Button;
import dtb.terminalIO.components.input.RadioGroup;
import dtb.terminalIO.components.progressmeter.ProgressMeter;
import dtb.terminalIO.components.progressmeter.ProgressMeter.Layout;
import dtb.terminalIO.components.progressmeter.ProgressMeter.Status;
import dtb.terminalIO.components.progressmeter.ProgressMeterParser;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.events.EventBus;

public class ProgressMeterTest {

    private static final int BAR_WIDTH = 40;
    private static final int STEPS = 5;
    private static final int TICKS = 100;
    private static final int INTERVAL_MS = 100;

    public static void main(String[] args) throws IOException {

        new TUIApp(args)
            .setCanvasSize(24, 85)
            .setEmptyChar(' ')
            .setDefaultMainApplicationPanel()
            .confirmOnExit()
            .setOnReadyStart((tApp) -> {

                Composite panel = new Composite(Colors.DEFAULT_COLOR);
                panel.setBorder(true);
                panel.setBorderColor(Colors.ICya);
                panel.setTitle(Colors.ICya, "Progress Meter Test ");

                TextComponent heading = new TextComponent(
                    Colors.IYel, "Simulated website backup");

                ProgressMeter meter = new ProgressMeter(
                    Colors.DEFAULT_COLOR, BAR_WIDTH);

                TextComponent result = new TextComponent(
                    Colors.ICya, "Waiting to start...     ");

                panel.add(1, 2, heading);
                panel.add(3, 2, meter);
                panel.add(7, 2, result);

                ButtonBar buttonBar = new ButtonBar(Colors.DEFAULT_COLOR)
                    .setLeftMostBorderChar((char) 0)
                    .setRightMostBorderChar((char) 0);

                Button directButton = new Button(
                    Colors.IGre, "Direct API");

                Button parserButton = new Button(
                    Colors.ICya, "Via Parser");

                buttonBar.add(directButton);
                buttonBar.add(parserButton);

                // switch the meter between its two layouts, even while a run is going
                RadioGroup<String> layout = RadioGroup.horizontal("two rows", "one row");
                layout.onChange((te) -> meter.setLayout("one row".equals(layout.getValue()) ? Layout.ONE_ROW : Layout.TWO_ROWS));
                buttonBar.add(new TextComponent("  Layout: "));
                buttonBar.add(layout);

                tApp.getMainApplicationPanel().add(0, 1, buttonBar);
                tApp.getMainApplicationPanel().add(3, 2, panel);

                // Each new test invalidates callbacks from older runs.
                final int[] generation = {0};

                Runnable directTest = () -> {
                    int run = ++generation[0];

                    resetMeter(meter, result, "Direct API running");

                    runDirectTest(
                        tApp.getEventBus(),
                        meter,
                        result,
                        generation,
                        run
                    );
                };

                Runnable parserTest = () -> {
                    int run = ++generation[0];

                    resetMeter(meter, result, "Parser running");

                    runParserTest(
                        tApp.getEventBus(),
                        meter,
                        result,
                        generation,
                        run
                    );
                };

                directButton.onChange((te) -> directTest.run());
                parserButton.onChange((te) -> parserTest.run());

                // Start with the direct API test.
                directTest.run();

            }).start();
    }

    private static void resetMeter(
            ProgressMeter meter,
            TextComponent result,
            String description) {

        meter.reset();
        meter.setStatus(Status.RUNNING);
        meter.setProgress(0, TICKS);
        meter.setStep(1, STEPS);
        meter.setMessage("Preparing backup...");

        result.setTextAndResize(description);
    }

    // ---------------------------------------------------------
    // Test 1: Direct calls to ProgressMeter
    // ---------------------------------------------------------

    private static void runDirectTest(
            EventBus eventBus,
            ProgressMeter meter,
            TextComponent result,
            int[] generation,
            int run) {

        for (int tick = 1; tick <= TICKS; tick++) {

            final int currentTick = tick;

            eventBus.asynchAfter(
                currentTick * INTERVAL_MS,
                (te) -> {

                    if (run != generation[0]) {
                        return;
                    }

                    int step = Math.min(
                        STEPS,
                        ((currentTick - 1) / (TICKS / STEPS)) + 1
                    );

                    meter.setProgress(currentTick, TICKS);
                    meter.setStep(step, STEPS);
                    meter.setMessage(messageForStep(step));

                    if (currentTick == TICKS) {
                        meter.setStatus(Status.SUCCESS);
                        meter.setMessage("Backup completed");
                        result.setTextAndResize("Direct API: completed!");
                    }
                }
            );
        }
    }

    // ---------------------------------------------------------
    // Test 2: In-memory stdout -> parser -> ProgressMeter
    // ---------------------------------------------------------

    private static void runParserTest(
            EventBus eventBus,
            ProgressMeter meter,
            TextComponent result,
            int[] generation,
            int run) {

        ProgressMeterParser parser = sampleFormat()
                .drive(meter)                            // progress, steps and status go to the meter
                .onText(line -> {
                    if (!line.isEmpty()) {
                        result.setTextAndResize(line);   // ordinary output, not a mark
                    }
                });

        List<String> output = buildSimulatedOutput();

        for (int i = 0; i < output.size(); i++) {

            final String line = output.get(i);
            final int delay = (i + 1) * INTERVAL_MS;

            eventBus.asynchAfter(delay, (te) -> {

                if (run != generation[0]) {
                    return;
                }

                parser.accept(line);
            });
        }
    }

    /**
     * The marks this example's simulated script prints - each app picks its own:
     * <pre>
     *     ##progress done=25 total=100 msg="Backing up"
     *     ##step 2 of 5 msg="Database"
     *     ##status ok msg="Backup completed"
     * </pre>
     */
    static ProgressMeterParser sampleFormat() {
        return new ProgressMeterParser()
                .addProgressPattern("##progress done=(?<done>\\d+) total=(?<total>\\d+) msg=\"(?<msg>[^\"]*)\"")
                .addStepPattern("##step (?<step>\\d+) of (?<total>\\d+) msg=\"(?<msg>[^\"]*)\"")
                .addStatusPattern("##status (?<status>\\w+) msg=\"(?<msg>[^\"]*)\"");
    }

    // ---------------------------------------------------------
    // Simulated shell output held entirely in memory
    // ---------------------------------------------------------

    private static List<String> buildSimulatedOutput() {

        List<String> lines = new ArrayList<>();

        lines.add("$ backup-website.sh");
        lines.add("Starting simulated backup...");

        for (int step = 1; step <= STEPS; step++) {

            lines.add(String.format(
                "##step %d of %d msg=\"%s\"",
                step, STEPS, messageForStep(step)
            ));

            lines.add("Working on Step " + step + "...");

            for (int pct = 10; pct <= 100; pct += 10) {

                int done = (step - 1) * 100 + pct;
                int total = STEPS * 100;

                lines.add(String.format(
                    "##progress done=%d total=%d msg=\"%s: %d%%\"",
                    done, total, messageForStep(step), pct
                ));
            }
        }

        lines.add("##status ok msg=\"Backup completed\"");
        lines.add("done.");

        return lines;
    }

    private static String messageForStep(int step) {
        switch (step) {
            case 1:
                return "Checking directories";
            case 2:
                return "Backing up database";
            case 3:
                return "Archiving website files";
            case 4:
                return "Compressing backup";
            default:
                return "Verifying backup";
        }
    }
}
