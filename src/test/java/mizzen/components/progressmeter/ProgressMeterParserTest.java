package mizzen.components.progressmeter;

import java.util.ArrayList;
import java.util.List;

import mizzen.components.progressmeter.ProgressMeter.Status;
import junit.framework.TestCase;

/**
 * ProgressMeterParser: marked lines become progress, steps and status (drive(meter) wires them to a
 * meter), every other line is ordinary text, and a mark whose numbers don't fit is text too.
 */
public class ProgressMeterParserTest extends TestCase {

    private final List<String> text = new ArrayList<String>();
    private final List<String> progress = new ArrayList<String>();

    private ProgressMeterParser parser() {
        return new ProgressMeterParser()
                .addProgressPattern("##progress done=(?<done>\\d+) total=(?<total>\\d+)( msg=\"(?<msg>[^\"]*)\")?")
                .addStepPattern("##step (?<step>\\d+) of (?<total>\\d+)")
                .addStatusPattern("##status (?<status>\\w+) msg=\"(?<msg>[^\"]*)\"")
                .onText(text::add);
    }

    public void testMarkedLinesAreProgressAndTheRestIsText() {
        ProgressMeterParser parser = parser().onProgress((done, total, msg) -> progress.add(done + "/" + total + " " + msg));
        parser.accept("starting");
        parser.accept("##progress done=3 total=10 msg=\"copying\"");
        parser.accept("##progress done=4 total=10");
        assertEquals("[3/10 copying, 4/10 null]", progress.toString());
        assertEquals("[starting]", text.toString());
    }

    public void testNumbersTooLongAreTextNotAnError() {
        ProgressMeterParser parser = parser().onProgress((done, total, msg) -> progress.add(done + "/" + total));
        parser.accept("##progress done=99999999999999999999999 total=10");
        assertTrue(progress.isEmpty());
        assertEquals(1, text.size());
    }

    public void testNullLinesAreIgnored() {
        parser().accept(null);
        assertTrue(text.isEmpty());
    }

    public void testDriveSendsProgressStepsAndStatusToTheMeter() {
        ProgressMeter meter = new ProgressMeter(20);
        ProgressMeterParser parser = parser().drive(meter);
        parser.accept("##step 2 of 5");
        parser.accept("##progress done=5 total=10");
        assertEquals(Status.RUNNING, meter.getStatus());
        parser.accept("##status error msg=\"disk full\"");
        assertEquals(Status.FAILED, meter.getStatus());
        parser.accept("##status ok msg=\"all done\"");
        assertEquals(Status.SUCCESS, meter.getStatus());
        parser.accept("##status thinking msg=\"hmm\"");           // not a status the meter knows: message only
        assertEquals(Status.SUCCESS, meter.getStatus());
    }

    public void testMatchesSaysWhichLinesAreMarksWithoutActing() {
        ProgressMeterParser parser = parser().onProgress((done, total, msg) -> progress.add("acted"));
        assertTrue(parser.matches("##progress done=1 total=2"));
        assertFalse(parser.matches("plain output"));
        assertFalse(parser.matches(null));
        assertTrue("nothing was run", progress.isEmpty() && text.isEmpty());
    }

    public void testStatusWords() {
        assertEquals(Status.SUCCESS, ProgressMeterParser.statusFor("OK"));
        assertEquals(Status.FAILED, ProgressMeterParser.statusFor("Failed"));
        assertEquals(Status.CANCELLED, ProgressMeterParser.statusFor("cancelled"));
        assertNull(ProgressMeterParser.statusFor("running"));
        assertNull(ProgressMeterParser.statusFor(null));
    }
}
