package dtb.terminalIO.examples;

import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.Set;

import dtb.terminalIO.controlcodes.ControlCode;
import dtb.terminalIO.controlcodes.ControlCodeContext.StringContext;
import dtb.terminalIO.core.LineTrackingCLIEmmitter;

public class LineTrackingCLIEmmitterTest {

    public static void main(String args[]) throws UnsupportedEncodingException {

        StringContext sc = new StringContext();

        sc.setControlCodesEnabled(true);
        
        Set<Integer> labelledLines = new HashSet<Integer>();
        
        LineTrackingCLIEmmitter lt = new LineTrackingCLIEmmitter(sc) {
            
            protected void resetLineCounter() {
                super.resetLineCounter();
                labelledLines.clear();
            }
              
            public void print(String s) {
                int ln = this.getLineCounter();
                if (!labelledLines.contains(ln)) {
                    s = String.format("%3d %s", ln, s);
                    super.print(s);
                    labelledLines.add(ln);
                } else {
                    super.print(s);
                }
            }
            
            
            protected void CR() {
                super.CR();
                this.print("");
            }

        };

        assertEQ(lt.getLineCounter(),0);

        String testStr = "Hello (0)\n\nddd (2)";
        
        lt.print(testStr);
        lt.print(" two? ");
        lt.print(""+lt.getLineCounter());
        
       // lt.print("");
        
        lt.outputRenderer( ControlCode.moveToDeltaRow(-2) );
        lt.print("zero?");
        
        lt.outputRenderer( ControlCode.moveToDeltaRow(10) );
        lt.print("10?");
        
        lt.clearLastTrackedLines(false);
        
        lt.outputRenderer(ControlCode.moveToDeltaRow(11));
        lt.print("11?");
        lt.outputRenderer(ControlCode.moveToDeltaRow(-11));
        lt.print("Where am I? " + lt.getLineCounter());
        

        System.out.println(sc.toString());
        
        
    }
    
    
    static void assertEQ(int value, int testValue) {
        if (value != testValue) {
            throw new IllegalStateException(String.format("%d != %d",value, testValue));
        }
    }
    
    
}
