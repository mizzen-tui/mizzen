package mizzen.core;

import mizzen.controlcodes.ControlCode;
import mizzen.controlcodes.ControlCodeContext.StringContext;
import junit.framework.TestCase;

public class LineTrackingCLIEmmitterTest extends TestCase {
    
    StringContext stringContext;
    LineTrackingCLIEmmitter out;

    public void setUp() {
        this.stringContext = new StringContext();
        this.out  = new LineTrackingCLIEmmitter(this.stringContext);
    }

    public void testSimpleLineCount0WithNoText() {
        assertEquals(0, this.out.getLineCounter());
    }

    public void testSimpleLineCount0() {
        this.out.print("hello world");
        assertEquals(0, this.out.getLineCounter());
    }

    public void testSimpleLineCount1() {
        this.out.println("hello world");
        assertEquals(1, this.out.getLineCounter());
    }

    public void testSimpleLineCount1_up() {
        this.out.println("hello world");
        this.out.outputRenderer( new ControlCode.CursorUp(1) );
        assertEquals(0, this.out.getLineCounter());
    }

    public void testSimpleLineCount0_up() {
        this.out.println("hello world");
        this.out.outputRenderer( new ControlCode.CursorUp(1) );
        assertEquals(0, this.out.getLineCounter());
    }

    public void testSimpleLineCount_neg1_up() {
        this.out.println("hello world");
        try {
           this.out.outputRenderer( new ControlCode.CursorUp(3) );
           fail("Expected an IllegalStateException");
        } catch (IllegalStateException e) {
           assertEquals(0, this.out.getLineCounter());
        }
    }


}
