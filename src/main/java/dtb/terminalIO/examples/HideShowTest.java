package dtb.terminalIO.examples;

import java.io.IOException;

import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.components.AbstractWrappedComponentAndFocusable;
import dtb.terminalIO.components.BorderedComponent;
import dtb.terminalIO.components.ButtonBar;
import dtb.terminalIO.components.ChildComponentManager;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.HorizontalPanel;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.VerticalPanel;
import dtb.terminalIO.components.input.Button;
import dtb.terminalIO.components.input.Clickable;
import dtb.terminalIO.components.input.DefaultFocusableBehaviour;
import dtb.terminalIO.components.modal.MessageBox;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.focus.DefaultFocusManager;
import dtb.terminalIO.events.focus.FocusManager;
import dtb.terminalIO.events.focus.Focusable;

/**
 * Test hide and show component functions.
 * 
 * Note:  Explicitly testing VerticalPanel and HorizontalPanel
 * hiding/showing their children to ensure that the panels properly resize themselves
 * 
 */
public class HideShowTest {

    public static void main(String[] args) throws IOException {

        new TUIApp(args).setDefaultMainApplicationPanel().confirmOnExit().setOnReadyStart((tApp) -> {

            tApp.getMainApplicationPanel().add(1, 4, new TextComponent("Test hide/show functionality of components"));

            HorizontalPanel mainLayout = new HorizontalPanel().setHorizontalSpacer(3);

            tApp.getMainApplicationPanel().add(2, 4, mainLayout);

            // Test vertical panel -----------------------------------------
            VerticalPanel testVertical = new VerticalPanel();

            testVertical.setVerticalSpacer(1);
            testVertical.setBorder(true);

            ButtonBar vertMainMenu = new ButtonBar().setNoBorderChars().setHorizontalSpacer(1).setCycling(false);

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

            FocusManager mainFocusManager = FocusManager.UTIL.buildFocusManagerFromComponent(
                    tApp.getMainApplicationPanel(), tApp.getEventBus(),
                    () -> new DefaultFocusManager().setNavKeysFirst(false).setCycling(false));

            tApp.getEventBus().appendEventHandler(mainFocusManager);

        }).start();

    }

    public static class ClickableRectangle extends AbstractWrappedComponentAndFocusable<BorderedComponent> implements Clickable {

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
        protected Focusable asFocusable() {
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
