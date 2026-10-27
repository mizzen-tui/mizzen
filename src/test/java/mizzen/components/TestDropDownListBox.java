package mizzen.components;

import java.io.UnsupportedEncodingException;
import java.util.function.Consumer;

import mizzen.components.input.DropdownListBox;
import mizzen.components.input.TextInput;
import mizzen.core.Canvas;
import mizzen.events.EventBus;
import mizzen.events.focus.FocusManager;
import mizzen.events.keyboard.KeyEvent;
import mizzen.events.keyboard.KeyStroke;
import mizzen.events.keyboard.Keys;
import mizzen.supportfortesting.CanvasForTesting;
import junit.framework.TestCase;

public class TestDropDownListBox extends TestCase {
    
    Canvas buildCanvasForTesting() throws UnsupportedEncodingException {
        return CanvasForTesting.buildCanvasForTestingNoControlCodesInOutput();
    }

    public void test1() throws UnsupportedEncodingException {

        CompositeWithFocusManager mainPanel = new CompositeWithFocusManager();

        buildCanvasForTesting().addComponentSegments(mainPanel);

        DropdownListBox<String> ddlb = DropdownListBox.fromStrings(new String[] {"ABC", "XYZ"});
        ddlb.setName("ddlb");

        mainPanel.add(1,1, ddlb);

        TextInput ti = new TextInput(20);

        mainPanel.add(2,1, ti);

        EventBus eventBus = new EventBus.STUBBED();

        FocusManager mainFocusManager = FocusManager.UTIL.buildFocusManagerFromComponent(mainPanel, eventBus);
        assertNotNull(mainFocusManager);
        assertEquals(2, mainFocusManager.getChildFocusables().size());
        
        Consumer<KeyStroke> hitKey = (keyStroke)-> {
            KeyEvent tabKey =  new KeyEvent(this, keyStroke);
            tabKey.setEventBus(eventBus);
            mainFocusManager.handle(tabKey);
        };

        hitKey.accept(Keys.SHIFT_TAB_KEYSTROKE);
        assertEquals(ti, mainFocusManager.getCurrentChildFocus());

        hitKey.accept(Keys.SHIFT_TAB_KEYSTROKE);
        assertEquals(ddlb.asFocusable(), mainFocusManager.getCurrentChildFocus());

        System.out.println(ddlb.getValue());

        hitKey.accept(Keys.DOWN_KEYSTROKE);
        hitKey.accept(Keys.DOWN_KEYSTROKE);

        System.out.println(ddlb.getValue());

        System.out.println( HasName.UTIL.getName(((mizzen.events.focus.FocusManager) ddlb.asFocusable()).getCurrentChildFocus(), "<no current child>"));

    }

}
