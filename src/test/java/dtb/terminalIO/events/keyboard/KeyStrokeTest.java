package dtb.terminalIO.events.keyboard;

import junit.framework.TestCase;

public class KeyStrokeTest extends TestCase {

    public void testKeyStrokeIntArrayArrayString() {
        
        KeyStroke ks = new KeyStroke(new int[][] {{1,2,3},{4,5,6}}, "test");
        
        KeyStroke ks2 = new KeyStroke(new int[][] {{1,2,3},{4,5,6}}, "test");
        
        KeyStroke ks3 = new KeyStroke(new int[][] {{2,2,3},{4,5,6}}, "test");
        
        KeyStroke ks4 = new KeyStroke(new int[][] {{1,2,3},{7,5,6}}, "test");
        
        KeyStroke ks5 = new KeyStroke(new int[][] {{1,2,3},{4,5,6}}, "testDifferent");

        assertEquals("test", ks.name);
        
        assertEquals(ks, ks2);
        
        assertFalse(ks.equals(ks3));
        
        assertFalse(ks.equals(ks4));
        
        assertFalse(ks.equals(ks5));
    }

    public void testKeyStrokeIntArrayString() {
        KeyStroke ks = new KeyStroke(new int[] {1,2,3}, "test");
        
        KeyStroke ks2 = new KeyStroke(new int[] {1,2,3}, "test");

        KeyStroke ks3 = new KeyStroke(new int[] {1,2,4}, "test");
        
        KeyStroke ks4 = new KeyStroke(new int[] {1,2,3}, "testDifferent");
        
        assertEquals("test", ks.name);
        
        assertEquals(ks, ks2);
        
        assertFalse(ks.equals(ks3));
        
        assertFalse(ks.equals(ks4));
        
    }

    public void testKeyStrokeIntString() {
        KeyStroke ks = new KeyStroke(1, "test");
        
        KeyStroke ks2 = new KeyStroke(1, "test");

        KeyStroke ks3 = new KeyStroke(2, "test");
        
        KeyStroke ks4 = new KeyStroke(1, "testDifferent");
        
        assertEquals("test", ks.name);
        
        assertEquals(ks, ks2);
        
        assertFalse(ks.equals(ks3));
        
        assertFalse(ks.equals(ks4));
    }

    public void testKeyStrokeChar() {
        KeyStroke ks = new KeyStroke('y', "test");
        
        KeyStroke ks2 = new KeyStroke('y', "test");

        KeyStroke ks3 = new KeyStroke('n', "test");
        
        KeyStroke ks4 = new KeyStroke('y', "testDifferent");
        
        assertEquals("test", ks.name);
        
        assertEquals(ks, ks2);
        
        assertFalse(ks.equals(ks3));
        
        assertFalse(ks.equals(ks4));
    }

    public void testCaseInsensitive() {
        
        KeyStroke ks = KeyStroke.caseInsensitive('y');

        KeyStroke ks2 = KeyStroke.caseInsensitive('y');
        
        KeyStroke ks3 = KeyStroke.caseInsensitive('Y');
        
        KeyStroke ks4 = new KeyStroke('y',"y");

        KeyStroke ks5 = new KeyStroke('Y',"Y");

        // test equals using ks
        assertEquals(ks, ks2);
        
        assertEquals(ks, ks3);

        assertEquals(ks, ks4);

        assertEquals(ks, ks5);
        
        // now reverse the equals
        assertEquals(ks2, ks);
        
        assertEquals(ks3, ks);

        assertEquals(ks4, ks);

        assertEquals(ks5, ks);
        
    }

}
