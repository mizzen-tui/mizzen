package mizzen.core;

import java.util.ArrayList;
import java.util.List;

import mizzen.components.TextComponent;
import mizzen.components.composite.VerticalPanel;
import mizzen.controlcodes.color.Colors;
import mizzen.examples.SnapshotReportDemo;
import mizzen.geom.Dimension;
import junit.framework.TestCase;

/** Every kind of component as a String: exact layout, no escapes by default, 7-bit if asked, repeatable. */
public class TextSnapshotTest extends TestCase {

    private static void assertExactly(String what, String text, int height, int width) {
        String[] lines = text.split("\n", -1);
        assertEquals(what + ": lines", height, lines.length);
        for (String line : lines) {
            assertEquals(what + ": [" + line + "]", width, line.length());
        }
    }

    public void testEveryKindHasTheExactSizeAndNoEscapes() {
        for (SnapshotReportDemo.Section section : SnapshotReportDemo.SECTIONS) {
            String text = new TextSnapshot(section.height, section.width).render(section.make());
            if (section.height > 0) {
                assertExactly(section.name, text, section.height, section.width);
            }
            assertEquals(section.name + " has no escape", -1, text.indexOf(0x1b));
            assertFalse(section.name + " is not blank", text.trim().isEmpty());
        }
    }

    public void testAsciiIsSevenBitWithTheSameLayout() {
        for (SnapshotReportDemo.Section section : SnapshotReportDemo.SECTIONS) {
            String text = new TextSnapshot(section.height, section.width).asciiOnly().render(section.make());
            if (section.height > 0) {
                assertExactly(section.name, text, section.height, section.width);
            }
            for (char c : text.toCharArray()) {
                assertTrue(section.name + " has " + (int) c, c == '\n' || (c >= ' ' && c < 127));
            }
        }
    }

    public void testBrailleAndLinesSurviveByDefault() {
        String line = TextSnapshot.of(SnapshotReportDemo.lineChart(), 12, 60);
        String shapes = TextSnapshot.of(SnapshotReportDemo.shapes(), 12, 48);
        assertTrue(shapes, shapes.chars().anyMatch((c) -> c > 0x2800 && c <= 0x28FF));
        assertTrue(line, line.chars().anyMatch((c) -> c > 0x2800 && c <= 0x28FF));
        assertTrue(TextSnapshot.of(SnapshotReportDemo.table(), 8, 40).contains("│"));
    }

    public void testColoursAreOnlyColours() {
        String text = new TextSnapshot(8, 40).ansiColors().render(SnapshotReportDemo.table());
        assertTrue(text.indexOf(0x1b) >= 0);
        String without = text.replaceAll("\u001b\\[[0-9;]*m", "");
        assertEquals("only colour codes", -1, without.indexOf(0x1b));
        assertEquals("the same text as without colours", TextSnapshot.of(SnapshotReportDemo.table(), 8, 40), without);
    }

    public void testRepeatableWithADifferentSizeBetween() {
        for (SnapshotReportDemo.Section section : SnapshotReportDemo.SECTIONS) {
            String first = new TextSnapshot(section.height, section.width).render(section.make());
            new TextSnapshot(40, 90).render(section.make());
            assertEquals(section.name, first, new TextSnapshot(section.height, section.width).render(section.make()));
        }
    }

    public void testTheSameComponentAtAnotherSize() {
        TextComponent text = new TextComponent(Colors.Whi, "hello");
        assertExactly("wide", new TextSnapshot(2, 10).render(text), 2, 10);
        assertExactly("cut", new TextSnapshot(1, 3).render(text), 1, 3);
    }

    /** with a size, exactly that - even when the component draws far less */
    public void testASizeIsExact() {
        assertExactly("padded", new TextSnapshot(30, 40).render(SnapshotReportDemo.panels()), 30, 40);
        assertExactly("by Dimension", TextSnapshot.of(SnapshotReportDemo.panels(), new Dimension.Simple(30, 40)), 30, 40);
    }

    /** with none, the component's own rectangle, not just what it drew: a panel sized 30 x 40 stays so */
    public void testNoSizeIsTheComponentsOwn() {
        VerticalPanel panel = SnapshotReportDemo.panels();
        panel.resize(new Dimension.Simple(30, 40));
        assertExactly("its size", TextSnapshot.of(panel), 30, 40);
        assertExactly("text", TextSnapshot.of(new TextComponent(Colors.Whi, "one\nthree")), 2, 5);
    }

    public void testATableWorksOutWhatItLeavesOut() {
        String[] natural = TextSnapshot.of(SnapshotReportDemo.table()).split("\n");
        assertEquals("top, heading, its line, 3 rows, bottom", 7, natural.length);
        assertTrue(natural[3], TextSnapshot.of(SnapshotReportDemo.table()).contains("West"));
        String[] narrow = TextSnapshot.of(SnapshotReportDemo.table(), 16).split("\n");
        assertEquals(16, narrow[0].length());
        assertTrue(String.join("\n", narrow), String.join("\n", narrow).contains("West"));
    }

    public void testADrawingWithNoSizeSaysSo() {
        try {
            TextSnapshot.of(SnapshotReportDemo.shapes());
            fail("a drawing has no size");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("no size"));
        }
    }

    public void testAMeterTellsFilledFromEmptyInAscii() {
        String ascii = new TextSnapshot(1, 40).asciiOnly().render(SnapshotReportDemo.meter());
        assertTrue(ascii, ascii.contains("#") && ascii.contains("."));
    }

    public void testBadInputThrows() {
        for (int[] size : new int[][] { { 0, 5 }, { 5, 0 }, { -2, 5 } }) {
            try {
                new TextSnapshot(size[0], size[1]);
                fail("size " + size[0] + "x" + size[1]);
            } catch (IllegalArgumentException expected) {
                assertTrue(expected.getMessage(), expected.getMessage().contains("height"));
            }
        }
        try {
            TextSnapshot.of(null, 1, 1);
            fail("no component");
        } catch (IllegalArgumentException expected) {
            assertNotNull(expected.getMessage());
        }
    }

    public void testOnSeveralThreadsEachWithItsOwn() throws Exception {
        List<SnapshotReportDemo.Section> sections = SnapshotReportDemo.SECTIONS;
        List<String> expected = new ArrayList<String>();
        for (SnapshotReportDemo.Section section : sections) {
            expected.add(new TextSnapshot(section.height, section.width).render(section.make()));
        }
        List<Throwable> failures = new ArrayList<Throwable>();
        List<Thread> threads = new ArrayList<Thread>();
        for (int t = 0; t < 8; t++) {
            Thread thread = new Thread(() -> {
                try {
                    for (int round = 0; round < 10; round++) {
                        for (int i = 0; i < sections.size(); i++) {
                            SnapshotReportDemo.Section section = sections.get(i);
                            assertEquals(section.name, expected.get(i), new TextSnapshot(section.height, section.width).render(section.make()));
                        }
                    }
                } catch (Throwable e) {
                    synchronized (failures) {
                        failures.add(e);
                    }
                }
            });
            threads.add(thread);
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
        assertTrue(failures.toString(), failures.isEmpty());
    }
}
