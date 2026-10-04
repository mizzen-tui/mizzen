package dtb.terminalIO.events.focus;

import java.util.List;

import dtb.terminalIO.components.AbstractWrappedComponentWithFocusManager;
import dtb.terminalIO.components.CompositeWithFocusManager;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.composite.Composite;
import dtb.terminalIO.components.composite.HorizontalPanel;
import dtb.terminalIO.components.composite.VerticalPanel;
import dtb.terminalIO.components.input.Button;
import dtb.terminalIO.components.input.DropdownListBox;
import dtb.terminalIO.components.input.ListBox;
import dtb.terminalIO.components.input.TextInput;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.events.EventBus;
import junit.framework.TestCase;

public class FindFocusableTargetTest  extends TestCase {

    
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

        result =  FocusManager.UTIL.findTargetAsPath(mainFocusManager, ddlb.asFocusable());

        assertNotNull(result);

        assertTrue(result.size()>0);

        FocusManager.UTIL.verifyPathsToFocusables(mainFocusManager, vp);

    }

    public void testFindWithCompositeFocusManager() {

        TestComponent1 tc1 = new TestComponent1();

        EventBus eventBus = new EventBus.STUBBED();

        FocusManager mainFocusManager = FocusManager.UTIL.buildFocusManagerFromComponent(tc1, eventBus);

        List<Focusable> result = FocusManager.UTIL.findTargetAsPath(mainFocusManager, tc1.getButton());

        System.out.println(result);

        assertNotNull(result);

        assertTrue(result.size()>0);

        //FocusManager.UTIL.verifyPathsToFocusables(mainFocusManager, tc1);

    }

    static class TestComponent1 extends AbstractWrappedComponentWithFocusManager<CompositeWithFocusManager> {

        Button button;

        public TestComponent1() {
            super(Colors.RCol, new CompositeWithFocusManager());

            this.button = new Button("TEST");

            this.getWrappedComponent().add(button);
        }

        Button getButton() {
            return this.button;
        }

    }

}
