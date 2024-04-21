package dtb.terminalIO.examples;

import java.io.IOException;

import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.ComponentInspector.InspectorButton;
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
import dtb.terminalIO.core.ScreenCanvasBackend;
import dtb.terminalIO.core.ScreenCanvasBackend.UpdateStrategy;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.focus.RequestFocusEvent;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.util.Logging;

public class TabPanelTest2 {

    public static void main(String[] args) throws IOException {

        Logging.setEnabled(true);

        new TUIApp(args)
        .setCanvasSize(24, 100)
        .setEmptyChar(' ')
        .setDefaultMainApplicationPanel()
        .setOnReadyStart((tApp)-> {

            ScreenCanvasBackend canvasBackend = ((ScreenCanvasBackend)tApp.getCanvas().getCanvasBackend());
            canvasBackend.setUpdateStrategy(UpdateStrategy.UPDATE_FULL);

            TabPanel tp = new TabPanel(Colors.IGre, new Dimension.Simple(tApp.getCanvas().getHeight()-4,tApp.getCanvas().getWidth()-4));
            tp.setName("MyTest2TabPanel");
            //tp.setEmptyColor(Colors.On_IYel);
            tApp.getMainApplicationPanel().add(4, 0, tp);

            TextComponent testText1 = new TextComponent("Here is a test text").setName("testTextInput");
            Button testText2 = new Button("Here is a another test text");
            TextInput firstName = new TextInput(Colors.IYel, 10).setName("firstName");

            Composite testFormVP = new VerticalPanel();
            TextInput strnum = new TextInput(Colors.IYel, 10).setName("street");
            TextInput city = new TextInput(Colors.IYel, 10).setName("city");
            TextInput province = new TextInput(Colors.IYel, 10).setName("province");

            testFormVP.add(new HorizontalPanel().addText("Street   : ").add(strnum));
            testFormVP.add(new HorizontalPanel().addText("City     : ").add(city));
            testFormVP.add(new HorizontalPanel().addText("Province : ").add(province));

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
                    return testFormVP;
                }
                public void activateComponent(EventBus eventBus, TabContext tabContext) {
                    eventBus.raiseEvent( new RequestFocusEvent( strnum ));
                }
            });

            InspectorButton inspectorButton = new InspectorButton(Colors.ICya, tApp);
            tApp.getMainApplicationPanel().add(1,1, inspectorButton);

            tp.displayCurrentTab(tApp.getEventBus());


        }).start();
        
    }

}
