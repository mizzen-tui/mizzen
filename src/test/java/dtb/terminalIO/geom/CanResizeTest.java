package dtb.terminalIO.geom;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

public class CanResizeTest extends TestCase {
    
    
    private CanResize makeSimpleResizer(List<String> tracer, String traceStr) {
        
        return new CanResize() {
            
            @Override
            public void resize(Dimension d) {
                tracer.add(traceStr);
            }
        };
        
    }
    
    
    public void testChainedRemoves() {
        
        List<String> tracer = new ArrayList<String>();
        
        CanResize cr1 = makeSimpleResizer(tracer,"cr1");
        CanResize cr2 = makeSimpleResizer(tracer,"cr2");
        CanResize cr3 = makeSimpleResizer(tracer,"cr3");
        CanResize cr4 = makeSimpleResizer(tracer,"cr4");
        CanResize cr5 = makeSimpleResizer(tracer,"cr5");
        CanResize cr6 = makeSimpleResizer(tracer,"cr6");
        
        CanResize.Chain chained = new CanResize.Chain(cr1);
        
        chained.append(cr2);
        chained.append(cr3);
        chained.append(cr4);
        chained.append(cr5);
        chained.append(cr6);
        
        tracer.clear();
        chained.resize(new Dimension.Simple(1,1));
        assertTrue(String.valueOf(tracer).contains("cr5"));
        System.out.println(tracer);
        
        chained.remove(cr5);
        tracer.clear();
        chained.resize(new Dimension.Simple(1,1));
        assertFalse(String.valueOf(tracer).contains("cr5"));
        System.out.println(tracer);

        chained.remove(cr6);
        tracer.clear();
        chained.resize(new Dimension.Simple(1,1));
        assertFalse(String.valueOf(tracer).contains("cr6"));
        System.out.println(tracer);
        
        chained.remove(cr1);
        tracer.clear();
        chained.resize(new Dimension.Simple(1,1));
        assertFalse(String.valueOf(tracer).contains("cr1"));
        System.out.println(tracer);

        chained.remove(cr3);
        tracer.clear();
        chained.resize(new Dimension.Simple(1,1));
        assertFalse(String.valueOf(tracer).contains("cr3"));
        System.out.println(tracer);
        
        chained.remove(cr2);
        tracer.clear();
        chained.resize(new Dimension.Simple(1,1));
        assertFalse(String.valueOf(tracer).contains("cr2"));
        System.out.println(tracer);
        
        chained.remove(cr4);
        tracer.clear();
        chained.resize(new Dimension.Simple(1,1));
        assertFalse(String.valueOf(tracer).contains("cr4"));
        System.out.println(tracer);

        chained.append(cr2);
        tracer.clear();
        chained.resize(new Dimension.Simple(1,1));
        assertTrue(String.valueOf(tracer).contains("cr2"));
        System.out.println(tracer);
    }

}
