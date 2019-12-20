package dtb.terminalIO.examples;

import java.io.IOException;

import dtb.terminalIO.components.ButtonBar;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.ComponentInspector;
import dtb.terminalIO.components.Composite;
import dtb.terminalIO.components.HorizontalPanel;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.VerticalPanel;
import dtb.terminalIO.components.input.Button;
import dtb.terminalIO.components.input.CheckBoxInput;
import dtb.terminalIO.components.input.HasValue;
import dtb.terminalIO.components.input.TextInput;
import dtb.terminalIO.components.scroll.ScrollableComponent;
import dtb.terminalIO.components.scroll.SearchFunctionForScrollableComponent;
import dtb.terminalIO.controlcodes.Colors;
import dtb.terminalIO.controlcodes.ControlCode;
import dtb.terminalIO.core.Canvas;
import dtb.terminalIO.core.TerminalScreen;
import dtb.terminalIO.events.ConditionalEventHandler;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventBusImpl;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.OnIdleEventEventHandler;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.TerminalEvent.IS_Event_TYPE;
import dtb.terminalIO.events.TerminalEvent.TYPE;
import dtb.terminalIO.events.focus.DefaultFocusManager;
import dtb.terminalIO.events.keyboard.InputEventsProducer;
import dtb.terminalIO.events.keyboard.KeyInputEventHandler;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.events.mouse.MouseEventOrchestrator;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.util.Stty;

/**
 * 
 * <pre>
 * 
 * This example uses the 'old' style of using this library..
 * 
 * ie .. it does not use a TUIApp object to orchestrate building the
 *       UI environment.    I highly suggest not building UI apps in this style.
 *                          instead take a look at SparkLineTest
 * 
 * </pre>
 * 
 * @author dbalme
 *
 */
public class TextBoxTest {

    public static void main(String[] args) throws IOException {

        TerminalScreen screen = TestsCommon.getTerminalScreenForTesting(args);

        Canvas master_canvas = TestsCommon.getMasterCanvasForTesting(screen);
        Dimension desiredCanvasSize = new Dimension.Simple(40, 120);
        Dimension terminalSize = TestsCommon.terminalSize();
        master_canvas.setSize(terminalSize.contains(desiredCanvasSize)?desiredCanvasSize:terminalSize);
        
        master_canvas.setEmptyChar(' ');
        
        master_canvas.t("Some text");
        
        master_canvas.refresh();
        
        VerticalPanel panel1 = new VerticalPanel();
        
        
        VerticalPanel panel2 = new VerticalPanel();
        panel2.setTitle("Panel 2");
        
        TextInput ti = new TextInput(10);
        
        panel1.add(ti);
                
        ti.setText("Test");
        
        TextInput ti_2 = new TextInput(15);
        
        ti_2.setText("Test 2");
        panel1.add(ti_2);
        

        TextInput ti_3 = new TextInput(15);
        ti_3.setText("Test3");
        panel2.add(ti_3);
        
        TextInput ti_4 = new TextInput(15);
        ti_4.setText("Test4");
        panel2.add(ti_4);
        
        CheckBoxInput cb_1 = new CheckBoxInput();
        cb_1.setValue(false);
        panel1.add(cb_1);
        
        Button b_1 = new Button("Press Me", true);
        panel1.add(b_1);
        
        Button b_2 = new Button("Press Too");
        panel1.add(b_2);

        Composite topPanel = new Composite();
        topPanel.setTitle("Form input testing");
        HorizontalPanel horizontalPanel = new HorizontalPanel();
        horizontalPanel.setHorizontalSpacer(5);

        topPanel.add(2,1, horizontalPanel);
        
        ScrollableComponent sc_1 = new ScrollableComponent(Colors.Blu, new Dimension.Simple(panel1.getHeight()+1, 50), panel1);

        SearchFunctionForScrollableComponent.addFinderToScrollable(sc_1);
        horizontalPanel.add(sc_1);
        horizontalPanel.add(panel2);
        
        TextComponent eventDisplayer = new TextComponent("                                       ");

        EventBus eventBus = new EventBusImpl();
        
        ButtonBar bannerMenu = new ButtonBar().setHorizontalSpacer(1);
        bannerMenu.setNoBorderChars();
        
        Button toggleTreeviewButton = new Button(Colors.BIGre,"");
        toggleTreeviewButton.setHotKey( Keys.CTRL_T_KEYSTROKE );
        toggleTreeviewButton.setDisplayHotKey(true);
        bannerMenu.addComponent(toggleTreeviewButton);
        
        bannerMenu.add(new TextComponent(" switch Treeview Components/Focusables  "));

        Button refreshTreeviewButton = new Button(Colors.BIGre," ");
        refreshTreeviewButton.setHotKey(Keys.CTRL_R_KEYSTROKE);
        refreshTreeviewButton.setDisplayHotKey(true);
        bannerMenu.add(refreshTreeviewButton);
        
        bannerMenu.add(new TextComponent(" refresh Treeview"));
        
        Button quitButton = new Button(Colors.BIGre,"Quit");
        quitButton.setHotKey(Keys.CTRL_C_KEYSTROKE);
        quitButton.setDisplayHotKey(true);
        bannerMenu.add(quitButton);
        
        VerticalPanel applicationPanel = new VerticalPanel().setVerticalSpacer(1);
        
        applicationPanel.add(bannerMenu);
        applicationPanel.add(topPanel);
        

        Runnable renderComponents = () -> {
            
            if (! topPanel.isDirty()) {
                return;
            }
            
            master_canvas.clearCanvas();
            
            master_canvas.addComponentSegments(3,5, applicationPanel);

            master_canvas.addComponentSegments(master_canvas.getHeight()-2, 2, eventDisplayer);
            
            TestsCommon.renderScreenDEBUG(master_canvas, screen,  eventBus);
            
            master_canvas.refresh();
            
        };
        
        renderComponents.run();
        
        InputEventsProducer keyInputEventsProducer = null;
        try {
            keyInputEventsProducer = new InputEventsProducer(eventBus);
        } catch (Exception e) {
            eventBus.stopConsuming();
            return;
        }

        StringBuffer logger = new StringBuffer();

        Runnable stopThePresses = new Runnable() {
            
            @Override
            public void run() {
                eventBus.stopConsuming();
                screen.add(ControlCode.SHOW_CURSOR);
                screen.refresh();

                try {
                    Stty.setCooked();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                // next refresh() is necessary to get cursor to be below
                // rendered canvas
                master_canvas.refresh();
                
                System.out.println("\n\n"+logger.toString());
                
                return;
            }
        };
        
        
        
        MouseEventOrchestrator mouser = new MouseEventOrchestrator(screen, master_canvas, eventBus);
        mouser.enableMouseHandling(applicationPanel);
        
        EventHandler terminate = new EventHandler() {

            @Override
            public void handle(TerminalEvent<?> te) {
                mouser.disableMouseHandling();
                stopThePresses.run();
                if (te.getType().equals(TYPE.EXCEPTION)) {
                    System.out.println("\n");
                    ((Exception) te.getEventData()).printStackTrace();
                } else {
                    // next refresh() is necessary to get cursor to be below
                    // rendered canvas
                    master_canvas.refresh();
                }
                
            }
        };
        
        
        eventBus.appendEventHandler(master_canvas.asShowCursorEventHandler());
        eventBus.appendEventHandler(master_canvas.asHideCursorEventHandler());

        
        eventBus.appendEventHandler(new KeyInputEventHandler(Keys.CTRL_C_KEYSTROKE, terminate));

        eventBus.appendEventHandler(new ConditionalEventHandler.Impl(new IS_Event_TYPE(TYPE.EXCEPTION), terminate));
        
        DefaultFocusManager focusManager = new DefaultFocusManager(Keys.TAB_KEYSTROKE) {
            @Override
            public void handle(TerminalEvent<?> te) {
                super.handle(te);
            }
            @Override
            public EventHandler canHandle(TerminalEvent<?> te) {
                return super.canHandle(te);
            }
        };
        
        focusManager.setNavKeysFirst(false);
        
        ComponentInspector componentInspector = new ComponentInspector(Colors.BIGre, new Dimension.Simple(20,40), topPanel, focusManager);
        topPanel.add(2, 73, componentInspector);

        quitButton.setActionHandler(terminate);
        toggleTreeviewButton.setActionHandler( componentInspector.toggleViewEventHandler() );
        refreshTreeviewButton.setActionHandler(componentInspector.refreshEventHandler());
        
        
        DefaultFocusManager focusManager1 = new DefaultFocusManager().setNavKeysFirst(false);
        DefaultFocusManager focusManager2 = new DefaultFocusManager().setNavKeysFirst(false);

        focusManager1.addFocusable(ti);
        focusManager1.addFocusable(ti_2);
        focusManager1.addFocusable(cb_1);
        focusManager1.addFocusable(b_1);
        focusManager1.addFocusable(b_2);
        
        focusManager2.addFocusable(ti_3);
        focusManager2.addFocusable(ti_4); 

        focusManager.addFocusable(bannerMenu);
        
        focusManager.addFocusable(sc_1);
        
        
        focusManager.addFocusable(focusManager2, eventBus);
        
        focusManager.addFocusable(componentInspector);

        
        eventBus.appendEventHandler(focusManager);
        
        // this is a shitty way to keep screen updated ..  should not need a timer,
        eventBus.appendEventHandler( new OnIdleEventEventHandler.RepeatingInterval(100, (te) -> {
                                          renderComponents.run();                       
                                     }));
        
        eventBus.appendEventHandler( new HasValue.OnValueChangedEventHandler<String>(ti) {
            @Override
            public void valueChanged(TerminalEvent<?> te, HasValue<String> source) {
                String newValue = source.getValue();
                eventDisplayer.setText(newValue);
                renderComponents.run();
            }
        });
        
        eventBus.appendEventHandler( new HasValue.OnValueChangedEventHandler<Boolean>(b_1) {
            @Override
            public void valueChanged(TerminalEvent<?> te,HasValue<Boolean> source) {
                Boolean newValue = source.getValue();
                eventDisplayer.setText("(1) Pressed = "+newValue);
                renderComponents.run();
            }
        });
     
        
        eventBus.appendEventHandler( new HasValue.OnValueChangedEventHandler<Boolean>(b_2) {
            public void handle(TerminalEvent<?> te) {
                super.handle(te);
                ti_4.requestFocus(te);
                renderComponents.run();
            }

            @Override
            public void valueChanged(TerminalEvent<?> te,HasValue<Boolean> source) {
                Boolean newValue = source.getValue();
                eventDisplayer.setText("(2) Pressed = "+newValue);
            }
            
        });
        

        screen.add(ControlCode.SHOW_CURSOR);
        
    }

}
