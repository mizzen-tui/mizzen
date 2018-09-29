package dtb.terminalIO.examples;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

import dtb.terminalIO.LineTrackingCLIEmmitter;
import dtb.terminalIO.TerminalContext.StringContext;
import dtb.terminalIO.TerminalScreen;
import dtb.terminalIO.geom.Dimension;

public class LineTrackingCLIEmmitterTest {

    public static void main(String args[]) throws UnsupportedEncodingException {
        
        
        LineTrackingCLIEmmitter lt = new LineTrackingCLIEmmitter(new StringContext());
        
        assertEQ(lt.getLineCounter(),0);
        
        String testStr = "Hello\n\n\n\nddd";
        
        lt.print(testStr);
        
        assertEQ(lt.getLineCounter(),Dimension.UTIL.stringToDimension(testStr).getHeight());
        
        
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PrintStream ps = new PrintStream(baos, true, "UTF-8")) {
            
            TerminalScreen screen = new TerminalScreen();
            screen.setPrintStream(ps);
            
            lt = screen.getEmitter();
            
            assertEQ(lt.getLineCounter(),0);

            testStr = "Hello\n\n\n\nddd";
            
            screen.t(testStr);
            
            screen.refresh();
            
            assertEQ(lt.getLineCounter(),Dimension.UTIL.stringToDimension(testStr).getHeight());
            
            screen.clear();
            
            assertEQ(lt.getLineCounter(),0);
            
            
        }
        
        // raw mode
        
        lt = new LineTrackingCLIEmmitter(new StringContext(true));  // true means raw mode
        
        assertEQ(lt.getLineCounter(),0);
        
        testStr = "Hello\n\n\n\nddd";
        
        lt.print(testStr);
        
        assertEQ(lt.getLineCounter(),Dimension.UTIL.stringToDimension(testStr).getHeight());
        
        
    }
    
    
    static void assertEQ(int value, int testValue) {
        if (value != testValue) {
            throw new IllegalStateException(String.format("%d != %d",value, testValue));
        }
    }
    
    
}
