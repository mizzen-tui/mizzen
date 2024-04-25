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
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.geom.Dimension;

public class TabPanelTest2 {

    public static void main(String[] args) throws IOException {

        // Logging.setEnabled(true);

        new TUIApp(args)
        .setDefaultMainApplicationPanel()
        .setOnReadyStart((tApp)-> {

            ScreenCanvasBackend canvasBackend = ((ScreenCanvasBackend)tApp.getCanvas().getCanvasBackend());
            canvasBackend.setUpdateStrategy(UpdateStrategy.UPDATE_FULL);

            TabPanel tp = new TabPanel(Colors.IGre, new Dimension.Simple(24, 96));
            tp.setName("MyTest2TabPanel");

            tApp.getMainApplicationPanel().add(4, 0, tp);

            tp.setPrevKeyStroke(Keys.CTRL_LEFT_KEYSTROKE);
            tp.setNextKeyStroke(Keys.CTRL_RIGHT_KEYSTROKE);

            tp.addTab("TextComponent", Keys.F2_KEYSTROKE, new TabAction() {
                TextComponent testText1;
                @Override
                public Component<?> getComponent(TabContext tabContext) {
                    testText1 = new TextComponent("Here is a test text").setName("testTextInput");
                    return testText1;
                }
            });

            tp.addTab("Someother text", Keys.F3_KEYSTROKE,  new TabAction() {
                Button testText2;
                @Override
                public Component<?> getComponent(TabContext tabContext) {
                    testText2 = new Button("Here is a another test text", true); // true = toggle button
                    return testText2;
                }
            });

            tp.addTab("Test input", Keys.F4_KEYSTROKE,  new TabAction() {
                TextInput firstName;
                @Override
                public Component<?> getComponent(TabContext tabContext) {
                    firstName = new TextInput(Colors.IYel, 10).setName("firstName");
                    return firstName;
                }
                public void resize(TabContext tabContext) {
                    firstName.setLength(tabContext.getBounds().getWidth()-2);
                }
            });

            tp.addTab("Address", Keys.F5_KEYSTROKE,  new TabAction() {
                Composite testFormVP;
                @Override
                public Component<?> getComponent(TabContext tabContext) {
                    if (testFormVP == null) {
                        testFormVP = new VerticalPanel();
                        TextInput strnum = new TextInput(Colors.IYel, 10).setName("street");
                        TextInput city = new TextInput(Colors.IYel, 10).setName("city");
                        TextInput province = new TextInput(Colors.IYel, 10).setName("province");

                        testFormVP.add(new HorizontalPanel().addText("Street   : ").add(strnum));
                        testFormVP.add(new HorizontalPanel().addText("City     : ").add(city));
                        testFormVP.add(new HorizontalPanel().addText("Province : ").add(province));
                    }
                    return testFormVP;
                }
            });

            InspectorButton inspectorButton = new InspectorButton(Colors.ICya, tApp);
            tApp.getMainApplicationPanel().add(1,1, inspectorButton);

            tp.displayCurrentTab(tApp.getEventBus());


        }).start();
        
    }

}
