package dtb.terminalIO.components;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.function.Consumer;

import dtb.terminalIO.components.input.DropdownListBox;
import dtb.terminalIO.components.input.TextInput;
import dtb.terminalIO.core.Canvas;
import dtb.terminalIO.core.TerminalScreen;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.focus.FocusManager;
import dtb.terminalIO.events.keyboard.KeyEvent;
import dtb.terminalIO.events.keyboard.KeyStroke;
import dtb.terminalIO.events.keyboard.Keys;
import junit.framework.TestCase;

public class TestDropDownListBox extends TestCase {
    
    Canvas buildCanvasForTesting() throws UnsupportedEncodingException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos, true, "UTF-8");

        TerminalScreen screen = new TerminalScreen();
        screen.setPrintStream(ps);
        screen.setControlCodesEnabled(false);

        Canvas master_canvas = new Canvas(screen);
        master_canvas.setSize(25, 80);
        return master_canvas;
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
        assertEquals(ddlb, mainFocusManager.getCurrentChildFocus());

        System.out.println(ddlb.getValue());

        hitKey.accept(Keys.DOWN_KEYSTROKE);
        hitKey.accept(Keys.DOWN_KEYSTROKE);

        System.out.println(ddlb.getValue());

        System.out.println( HasName.UTIL.getName(ddlb.getCurrentChildFocus()));

    }

}
