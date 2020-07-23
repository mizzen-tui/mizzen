package dtb.terminalIO.events.focus;

import java.util.List;

import dtb.terminalIO.components.Composite;
import dtb.terminalIO.components.HorizontalPanel;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.VerticalPanel;
import dtb.terminalIO.components.input.DropdownListBox;
import dtb.terminalIO.components.input.ListBox;
import dtb.terminalIO.components.input.TextInput;
import junit.framework.TestCase;

public class FindFocusableTargetTests  extends TestCase {

    
    public void testFind1() {
        
        VerticalPanel vp = new VerticalPanel();
        
        TextInput ti_1 = new TextInput(7);
        
        vp.add(ti_1);
        
        
        FocusManager mainFocusManager = FocusManager.UTIL.buildFocusManagerFromComponent(vp, null);

        assertNotNull(mainFocusManager);
        
       // assertEquals(vp,mainFocusManager);
        
        List<Focusable> result =  FocusManager.UTIL.findTargetAsPath(mainFocusManager, ti_1);
        
        assertNotNull(result);
        
        assertTrue(result.size()>0);
        
        FocusManager.UTIL.verifyPathsToFocusables(mainFocusManager, vp);
      
    }

    public void testFindWithListBox() {
        
        VerticalPanel vp = new VerticalPanel();
        
        TextInput ti_1 = new TextInput(7);
        
        vp.add(ti_1);
        
        ListBox<String> lb = ListBox.fromStrings(new String[] {"Vanessa","David","Hoochi","coochi"});
        
        vp.add(lb);
        
        FocusManager mainFocusManager = FocusManager.UTIL.buildFocusManagerFromComponent(vp, null);

        assertNotNull(mainFocusManager);
        
    //    assertEquals(vp,mainFocusManager);
        
        List<Focusable> result =  FocusManager.UTIL.findTargetAsPath(mainFocusManager, ti_1);
        
        assertNotNull(result);
        
        assertTrue(result.size()>0);
        
        result =  FocusManager.UTIL.findTargetAsPath(mainFocusManager, lb.asFocusable());
        
        assertNotNull(result);
        
        assertTrue(result.size()>0);
        
        
        Composite cp = new Composite();
        
        cp.add(vp);
        
        
        mainFocusManager = FocusManager.UTIL.buildFocusManagerFromComponent(cp, null);
        
        assertNotNull(mainFocusManager);
        
      //  assertEquals(vp,mainFocusManager);
        
        result =  FocusManager.UTIL.findTargetAsPath(mainFocusManager, ti_1);
        
        assertNotNull(result);
        
        assertTrue(result.size()>0);
        
        result =  FocusManager.UTIL.findTargetAsPath(mainFocusManager, lb.asFocusable());
        
        assertNotNull(result);
        
        assertTrue(result.size()>0);
        
        HorizontalPanel hp = new HorizontalPanel();
        
        hp.add(new TextComponent("Here is some text"));
        hp.add(cp);
        
        mainFocusManager = FocusManager.UTIL.buildFocusManagerFromComponent(hp, null);
        
        assertNotNull(mainFocusManager);

        //assertEquals(hp,mainFocusManager);

        result =  FocusManager.UTIL.findTargetAsPath(mainFocusManager, ti_1);

        assertNotNull(result);

        assertTrue(result.size()>0);
        
        result =  FocusManager.UTIL.findTargetAsPath(mainFocusManager, lb.asFocusable());

        assertNotNull(result);

        assertTrue(result.size()>0);

        FocusManager.UTIL.verifyPathsToFocusables(mainFocusManager, hp);

    }
    public void testFindWithDropDownListBox() {
        
        VerticalPanel vp = new VerticalPanel();
        
        TextInput ti_1 = new TextInput(7);
        
        vp.add(ti_1);
        
        DropdownListBox<String> ddlb = DropdownListBox.fromStrings(new String[] {"Test1","Test2","Test3"});
        ddlb.setSelectedValue("Test2");

        vp.add(ddlb);
        
        FocusManager mainFocusManager = FocusManager.UTIL.buildFocusManagerFromComponent(vp, null);

        assertNotNull(mainFocusManager);
        
       // assertEquals(vp,mainFocusManager);
        
        List<Focusable> result =  FocusManager.UTIL.findTargetAsPath(mainFocusManager, ti_1);
        
        assertNotNull(result);
        
        assertTrue(result.size()>0);

        result =  FocusManager.UTIL.findTargetAsPath(mainFocusManager, ddlb);
        
        assertNotNull(result);
        
        assertTrue(result.size()>0);
        
        FocusManager.UTIL.verifyPathsToFocusables(mainFocusManager, vp);
    
    
    }
    
}
