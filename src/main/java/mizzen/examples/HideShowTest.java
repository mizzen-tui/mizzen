package mizzen.examples;

import java.io.IOException;

import mizzen.app.TUIApp;
import mizzen.components.AbstractComponentWrappper;
import mizzen.components.BorderedComponent;
import mizzen.components.ButtonBar;
import mizzen.components.ChildComponentManager;
import mizzen.components.CollapsiblePanel;
import mizzen.components.Component;
import mizzen.components.TextComponent;
import mizzen.components.composite.HorizontalPanel;
import mizzen.components.composite.VerticalPanel;
import mizzen.components.input.Button;
import mizzen.components.input.DefaultFocusableBehaviour;
import mizzen.components.modal.MessageBox;
import mizzen.controlcodes.color.Color;
import mizzen.controlcodes.color.Colors;
import mizzen.events.EventBus;
import mizzen.events.focus.Focusable;
import mizzen.events.focus.ToFocusable;
import mizzen.events.mouse.Clickable;

/**
 * Test hide and show component functions.
 * 
 * Note:  Explicitly testing VerticalPanel and HorizontalPanel
 * hiding/showing their children to ensure that the panels properly resize themselves
 * 
 */
public class HideShowTest {

    public static void main(String[] args) throws IOException {

        new TUIApp(args).setDefaultMainApplicationPanel()
                        .confirmOnExit()
                        .enableComponentInspector()
                        .setOnReadyStart((tApp) -> {

            tApp.getMainApplicationPanel().add(1, 4, new TextComponent("Test hide/show functionality of components"));

            HorizontalPanel mainLayout = new HorizontalPanel().setHorizontalSpacer(3);

            tApp.getMainApplicationPanel().add(2, 4, mainLayout);

            // Test vertical panel -----------------------------------------
            VerticalPanel testVertical = new VerticalPanel();

            testVertical.setVerticalSpacer(1);
            testVertical.setBorder(true);

            ButtonBar vertMainMenu = new ButtonBar().setNoBorderChars().setHorizontalSpacer(1);

            mainLayout.add(testVertical);

            testVertical.add(vertMainMenu);

            Component<?> vTest1 = new ClickableRectangle(Colors.On_Blu, "vTest1");
            testVertical.add(vTest1);
            vertMainMenu.addComponent(hideShowButton(vTest1.getColor(), "(1)", vTest1));

            Component<?> vTest2 = new ClickableRectangle(Colors.On_Cya, "vTest2");
            testVertical.add(vTest2);
            vertMainMenu.addComponent(hideShowButton(vTest2.getColor(), "(2)", vTest2));

            Component<?> vTest3 = new ClickableRectangle(Colors.On_Gre, "vTest3");
            testVertical.add(vTest3);
            vertMainMenu.addComponent(hideShowButton(vTest3.getColor(), "(3)", vTest3));

            // Test horizontal panel -----------------------------------------
            VerticalPanel containerForHorizontalTest = new VerticalPanel().setVerticalSpacer(1);
            containerForHorizontalTest.setBorder(true);
            mainLayout.add(containerForHorizontalTest);
            ButtonBar horizMainMenu = new ButtonBar().setNoBorderChars().setHorizontalSpacer(1);
            containerForHorizontalTest.add(horizMainMenu);

            HorizontalPanel testHoriz = new HorizontalPanel().setHorizontalSpacer(2);
            containerForHorizontalTest.add(testHoriz);

            Component<?> hTest1 = new ClickableRectangle(Colors.On_Blu, "hTest1");
            testHoriz.add(hTest1);
            horizMainMenu.addComponent(hideShowButton(vTest1.getColor(), "(1)", hTest1));

            Component<?> hTest2 = new ClickableRectangle(Colors.On_Cya, "hTest2");
            testHoriz.add(hTest2);
            horizMainMenu.addComponent(hideShowButton(vTest2.getColor(), "(2)", hTest2));

            Component<?> hTest3 = new ClickableRectangle(Colors.On_Gre, "hTest3");
            testHoriz.add(hTest3);
            horizMainMenu.addComponent(hideShowButton(vTest3.getColor(), "(3)", hTest3));
            
            
            mainLayout.add(new CollapsiblePanel(Colors.IYel, "Collapsible Panel Test", new TextComponent("Here is the text that will get hidden\nor shown!")));

        }).start();

    }

    public static class ClickableRectangle extends AbstractComponentWrappper<BorderedComponent> implements ToFocusable, Clickable {

        Focusable focusable = null;
        String text;

        public ClickableRectangle(Color color, String text) {
            super(color, new BorderedComponent(new TextComponent(text).setColor(color)));
            focusable = new DefaultFocusableBehaviour.HiliteOnFocus(this);
            this.text = text;
        }

        @Override
        public void onClick(EventBus eventBus) {
            MessageBox.showInfoDialog(eventBus, (ChildComponentManager<?>) Component.UTIL.getRootPanelOf(this)  , this.text + " ClickableRectangle clicked!!");
        }

        @Override
        public Focusable asFocusable() {
            return focusable;
        }
    }

    public static Button hideShowButton(Color color, String buttonTextSuffix, Component<?> componentToHideShow) {

        Button hideShow = new Button(color, "Hide " + buttonTextSuffix, true);

        hideShow.onChange((te) -> {

            if (hideShow.getValue()) {
                componentToHideShow.hide();
                hideShow.setButtonText("Show " + buttonTextSuffix);
            } else {
                componentToHideShow.show();
                hideShow.setButtonText("Hide " + buttonTextSuffix);
            }

        });

        return hideShow;

    }

}
