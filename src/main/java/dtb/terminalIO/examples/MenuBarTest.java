package dtb.terminalIO.examples;

import java.io.IOException;
import java.util.function.Consumer;

import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.components.BorderedComponent;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.composite.Composite;
import dtb.terminalIO.components.menu.Menu;
import dtb.terminalIO.components.menu.MenuBar;
import dtb.terminalIO.components.menu.MenuItem;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.events.focus.FocusManager;
import dtb.terminalIO.util.Logging;

public class MenuBarTest {
    
    public static void main(String[] args) throws IOException {

        Logging.setEnabled(true);
        
        new TUIApp(args)
        .setEmptyChar(' ')
        .setMainApplicationPanel(new Composite().setBounds(TestsCommon.terminalSize()).setEmptyChar(' ').setTitle("MenuBar Test").setBorder(true))
        .confirmOnExit()
        .setOnReadyStart((tApp)-> {

            TextComponent textView = new TextComponent("Here is where\ntext will be viewed.");
            BorderedComponent bc = new BorderedComponent(Colors.IGre, textView);

            tApp.getMainApplicationPanel().add(10, 10, bc);

            Consumer<String> displayText = (theText) -> {
                 textView.setTextAndResize(theText);
            };

            Menu menu1 = new Menu("Menu-1")
                            .addItem("New", new Menu("Menu-1-1")
                                                .addItem("New what?",(te)-> displayText.accept("What could you possible want"))
                                                .addItem("New Car",(te)-> displayText.accept("Are you kidding?  A new Car?"))
                                                .addItem("New Boat",(te)->  displayText.accept("OK now you are crazy.\nA hole in which to pour money!")))
                            .addDivider()
                            .addItem("Open File",(te)-> displayText.accept("Here I would open a file"))
                            .addItem("Close",(te)->  displayText.accept("OK time to close the file"));

            Menu menu2 = new Menu("Menu-2")
                    .addItem("Find/Replace",(te)-> displayText.accept("OK let's find something.\nWhat should we look for?\nSomething interesting\nI should hope!"))
                    .addItem("Delete",(te)-> displayText.accept("Let's delete something shall we?\nMaybe our purpose?\nMaybe the next character?\nLet's go for the gusto!\nLet's delete the next word!\nYeah!! We're so brave!"));

            MenuBar mBar = new MenuBar(Colors.IBlu);
            mBar.addDropDownMenu("File", menu1);
            mBar.addDropDownMenu("Edit", menu2);

            tApp.getMainApplicationPanel().add(0, 1, mBar);

            FocusManager mainFocusManager = FocusManager.UTIL.buildFocusManagerFromComponent(tApp.getMainApplicationPanel(), tApp.getEventBus());

            tApp.getEventBus().appendEventHandler(  mainFocusManager );

            // sanity check
            FocusManager.UTIL.verifyPathsToFocusables(mainFocusManager, (Component<?>)tApp.getMainApplicationPanel());

            Logging.info("App started");

        }).start();
        
        
    }


}
