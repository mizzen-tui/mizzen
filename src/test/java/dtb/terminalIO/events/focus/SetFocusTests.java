package dtb.terminalIO.events.focus;

import dtb.terminalIO.components.composite.Composite;
import dtb.terminalIO.components.input.RockerButton;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.events.EventBus;
import junit.framework.TestCase;

public class SetFocusTests extends TestCase {

    
    public void testRockerButtonSetFocus() {
        Composite panel = new Composite();
        
        RockerButton rb = new RockerButton(Colors.BIBlu, "TEST");
        
        panel.add(rb);
        
        EventBus eventBus = new EventBus.STUBBED();
        
        FocusManager mainFocusManager = FocusManager.UTIL.buildFocusManagerFromComponent(panel, eventBus); 
        
        //mainFocusManager.setChildAsCurrentFocus(rb, eventBus);
        
        //rb.setFocus(true, eventBus);
        
        rb.getWrappedComponent().setFocus(true, eventBus);
        
        
    }
    
}
