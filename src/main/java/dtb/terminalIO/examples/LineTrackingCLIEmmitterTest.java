package dtb.terminalIO.examples;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

import dtb.terminalIO.core.LineTrackingCLIEmmitter;
import dtb.terminalIO.core.TerminalScreen;
import dtb.terminalIO.controlcodes.ControlCodeContext.StringContext;
import dtb.terminalIO.geom.Dimension;

public class LineTrackingCLIEmmitterTest {

    public static void main(String args[]) throws UnsupportedEncodingException {
        
        
        StringContext sc = new StringContext();
        
        sc.setControlCodesEnabled(false);
        
        LineTrackingCLIEmmitter lt = new LineTrackingCLIEmmitter(sc);
        
        assertEQ(lt.getLineCounter(),0);
        
        String testStr = "Hello\n\n\n\nddd";
        
        lt.print(testStr);
        
        assertEQ(lt.getLineCounter(),Dimension.UTIL.stringToDimension(testStr).getHeight());
        
        System.out.println(sc.toString());
        
        
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PrintStream ps = new PrintStream(baos, true, "UTF-8")) {
            
            TerminalScreen screen = new TerminalScreen();
            screen.setPrintStream(ps);
            screen.setControlCodesEnabled(false);
            
            lt = screen.getEmitter();
            
            assertEQ(lt.getLineCounter(),0);

            testStr = "Hello\n\n\n\nddd";
            
            screen.t(testStr);
            
            screen.refresh();
            
            assertEQ(lt.getLineCounter(),Dimension.UTIL.stringToDimension(testStr).getHeight());
            
            screen.clear();
            
            assertEQ(lt.getLineCounter(),0);
            
           System.out.println( new String(baos.toByteArray() ) );
            
            
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
