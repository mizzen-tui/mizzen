package mizzen.examples;

import junit.framework.TestCase;

/** Which class a link in a document names, for the examples' desktop to run. */
public class DesktopLinksTest extends TestCase {

    public void testALinkToASourceFile() {
        assertEquals("mizzen.examples.Table01Test", EmbeddedAppTest.classLinkedTo("src/main/java/mizzen/examples/Table01Test.java"));
        assertEquals("mizzen.examples.plot.TestScale", EmbeddedAppTest.classLinkedTo("src/main/java/mizzen/examples/plot/TestScale.java"));
    }

    public void testAnythingElseNamesNoClass() {
        assertNull(EmbeddedAppTest.classLinkedTo("docs/ARCHITECTURE.md"));
        assertNull(EmbeddedAppTest.classLinkedTo("docs/img/cube.gif"));
        assertNull(EmbeddedAppTest.classLinkedTo("https://example.com/A.java"));
    }
}
