package dtb.terminalIO.components;

import dtb.terminalIO.controlcodes.Colors;
import dtb.terminalIO.events.keyboard.Keys;
import junit.framework.TestCase;

public class TestButtonBarAddInsert extends TestCase {
    
    
    ButtonBar2 bbar1;
    
    public void setUp() throws Exception
    {
        bbar1 = new ButtonBar2(Colors.IGre);
        
        bbar1.addButton("F1", Keys.F1_KEYSTROKE, (te) -> {
            // do nothing
        }).setBorder(true);
        
        bbar1.addButton("F2", Keys.F2_KEYSTROKE, (te) -> {
            // do nothing
        }).setBorder(true);
        
        bbar1.addButton("F3", Keys.F3_KEYSTROKE, (te) -> {
            // do nothing
        }).setBorder(true);
        
    }
    
    protected void tearDown() {
        bbar1 = null;
    }
    
    public void testSize() {

        assertNotNull(bbar1.getChildFocusables());
        assertEquals(bbar1.getChildFocusables().size(),3);
        
        
    }

    public void testAppend() {

        Component<?>  t = new TextComponent("TestMe");
        
        bbar1.addComponent(t);
        
        assertNotNull(bbar1.getChildFocusables());
        // t is not a focusable
        assertEquals(bbar1.getChildFocusables().size(),3);
        
        assertTrue(bbar1.containsChild(t));
        assertEquals(3,bbar1.indexOf(t));
        
    }

    public void testInsertAtBeginning() {

        Component<?>  t = new TextComponent("TestMe");
        

        bbar1.insertComponent(0, t);
        
        assertNotNull(bbar1.getChildFocusables());
        // t is not a focusable
        assertEquals(bbar1.getChildFocusables().size(),3);
        
        assertTrue(bbar1.containsChild(t));
        assertEquals(0,bbar1.indexOf(t));
        
    }

    public void testInsertMiddle() {

        Component<?>  t = new TextComponent("TestMe");
        

        bbar1.insertComponent(1, t);
        
        assertNotNull(bbar1.getChildFocusables());
        // t is not a focusable
        assertEquals(bbar1.getChildFocusables().size(),3);
        
        assertTrue(bbar1.containsChild(t));
        assertEquals(1,bbar1.indexOf(t));
        
    }

    public void testInsertEnd() {

        Component<?>  t = new TextComponent("TestMe");
        

        bbar1.insertComponent(2, t);
        
        assertNotNull(bbar1.getChildFocusables());
        // t is not a focusable
        assertEquals(bbar1.getChildFocusables().size(),3);
        
        assertTrue(bbar1.containsChild(t));
        assertEquals(2,bbar1.indexOf(t));
        
    }

}
