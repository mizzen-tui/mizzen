package dtb.terminalIO.examples;

import java.io.IOException;

import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.composite.Composite;
import dtb.terminalIO.components.composite.HorizontalPanel;
import dtb.terminalIO.components.composite.VerticalPanel;
import dtb.terminalIO.components.input.Button;
import dtb.terminalIO.components.input.TextInput;
import dtb.terminalIO.components.tab.TabAction;
import dtb.terminalIO.components.tab.TabPanel;
import dtb.terminalIO.components.tab.TabPanel.TabContext;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.focus.FocusManager;
import dtb.terminalIO.events.focus.RequestFocusEvent;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.geom.Dimension;

public class TabPanelTest2 {
    
    public static void main(String[] args) throws IOException {
        new TUIApp(args)
        .setCanvasSize(24, 100)
        .setEmptyChar(' ')
        .setMainApplicationPanel(new Composite().setBounds(20, 100).setEmptyChar(' ').setBorder(true))
        .confirmOnExit()
        .setOnReadyStart((tApp)-> {
            
            
            TabPanel tp = new TabPanel(Colors.IGre, new Dimension.Simple(tApp.getCanvas().getHeight()-4,tApp.getCanvas().getWidth()-4));
            tp.setEmptyColor(Colors.On_Yel);
            tApp.getMainApplicationPanel().add(0, 0, tp);
            
            TextComponent testText1 = new TextComponent("Here is a test text");
            Button testText2 = new Button("Here is a another test text");
            TextInput firstName = new TextInput(Colors.IYel, 10);
            
            Composite testForm = new VerticalPanel();
            TextInput strnum = new TextInput(Colors.IYel, 10);
            TextInput city = new TextInput(Colors.IYel, 10);
            TextInput province = new TextInput(Colors.IYel, 10);
            
            testForm.add(new HorizontalPanel().addText("Street   : ").add(strnum));
            testForm.add(new HorizontalPanel().addText("City     : ").add(city));
            testForm.add(new HorizontalPanel().addText("Province : ").add(province));
            
            tp.setPrevKeyStroke(Keys.CTRL_LEFT_KEYSTROKE);
            tp.setNextKeyStroke(Keys.CTRL_RIGHT_KEYSTROKE);

            tp.addTab("TextComponent", Keys.F2_KEYSTROKE, new TabAction() {
                @Override
                public Component<?> getComponent(TabContext tabContext) {
                    return testText1;
                }
            });
            tp.addTab("Someother text", Keys.F3_KEYSTROKE,  new TabAction() {
                @Override
                public Component<?> getComponent(TabContext tabContext) {
                    return testText2;
                }
            });

            tp.addTab("Test input", Keys.F4_KEYSTROKE,  new TabAction() {
                @Override
                public Component<?> getComponent(TabContext tabContext) {
                    return firstName;
                }
                public void resize(TabContext tabContext) {
                    firstName.setLength(11);
                }
            });

            tp.addTab("Address", Keys.F5_KEYSTROKE,  new TabAction() {
                @Override
                public Component<?> getComponent(TabContext tabContext) {
                    return testForm;
                }
                public void activateComponent(EventBus eventBus, TabContext tabContext) {
                    eventBus.raiseEvent( new RequestFocusEvent( strnum ));
                }
            });

            // traverse main app panel find focusable's and build a focusManager and attach to Eventbus
            FocusManager mainFocusManager = FocusManager.UTIL.buildFocusManagerFromComponent(tApp.getMainApplicationPanel(),tApp.getEventBus());
            
            tApp.getEventBus().appendEventHandler(  mainFocusManager );

            // sanity check
            //  FocusManager.UTIL.verifyPathsToFocusables(mainFocusManager, (Component<?>)tApp.getMainApplicationPanel());

        }).start();
        
    }

}
