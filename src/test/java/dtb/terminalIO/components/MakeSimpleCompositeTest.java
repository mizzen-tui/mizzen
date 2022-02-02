package dtb.terminalIO.components;

import dtb.terminalIO.components.composite.Composite;
import junit.framework.TestCase;

public class MakeSimpleCompositeTest extends TestCase {
    
    
    
    public void testMakeBasicComposite() {
        
        
        Composite innerComposite = new Composite().setName("innerComposite");
        innerComposite.setBorder(true);
        innerComposite.setFillOnClear(true);
        innerComposite.setEmptyChar(' ');
        innerComposite.add(1, 4, new TextComponent("[Inner composite]"));
        innerComposite.add(3, 2, new TextComponent("With 2 text components"));
        
        
        innerComposite.getSegmentsBuffer().getSegments();
        
        //Composite outer = new Composite();
        //outer.add(22, 3, innerComposite );
        //outer.getSegmentsBuffer().getSegments();
        
    }
    
    
    

}
