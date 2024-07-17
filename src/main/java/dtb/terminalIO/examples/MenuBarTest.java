package dtb.terminalIO.examples;

import java.io.IOException;
import java.util.function.BiConsumer;

import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.components.BorderedComponent;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.input.TextInput;
import dtb.terminalIO.components.menu.Menu;
import dtb.terminalIO.components.menu.MenuBar;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.util.Logging;

public class MenuBarTest {
    
    public static void main(String[] args) throws IOException {

        Logging.setEnabled(true);
        
        new TUIApp(args)
        .setEmptyChar(' ')
        .setDefaultMainApplicationPanel()
        .confirmOnExit()
        .setOnReadyStart((tApp)-> {

            TextComponent textView = new TextComponent("Here is where\ntext will be viewed.");
            BorderedComponent bc = new BorderedComponent(Colors.IGre, textView);

            tApp.getMainApplicationPanel().add(10, 10, bc);

            TextInput testInput = new TextInput(30);
            testInput.setText("Type something here");
            
            BiConsumer<String, TerminalEvent<?>> displayText = (theText, te) -> {
                 Logging.info("Display-Text called with: " + theText);
                 textView.setTextAndResize(theText);
                 //te.getEventBus().raiseEvent(new RequestFocusEvent(testInput));
            };

            Menu menu1 = new Menu("Menu-1")
                            .addItem("New", new Menu("Menu-1-1")
                                                .addItem("New what?",(te)-> displayText.accept("What could you possible want", te))
                                                .addItem("New Car",(te)-> displayText.accept("Are you kidding?  A new Car?", te))
                                                .addItem("New Boat",(te)->  displayText.accept("OK now you are crazy!!!???\nA hole in which to pour money!", te))
                                                .addItem("BoatDamage", new Menu("Menu-1-1-2")
                                                                            .addItem("Salvage",     (te)->  displayText.accept("40K", te))
                                                                            .addItem("Keel Repair", (te)->  displayText.accept("53K", te))
                                                                            .addItem("Engine",      (te)->  displayText.accept("2K", te))
                                                        )
                                                )
                            .addDivider()
                            .addItem("Open File",(te)-> displayText.accept("Here I would open a file", te))
                            .addItem("Close",(te)->  displayText.accept("OK time to close the file", te))
                            .addDivider()
                            .addItem("Exit",(te)->  tApp.getTerminateAppHotKeyEventHandler().handle(te) );

            Menu menu2 = new Menu("Menu-2")
                    .addItem("Find/Replace",(te)-> displayText.accept("OK let's find something.\nWhat should we look for?\nSomething interesting\nI should hope!", te))
                    .addItem("Delete",(te)-> displayText.accept("Let's delete something shall we?\nMaybe our purpose?\nMaybe the next character?\nLet's go for the gusto!\nLet's delete the next word!\nYeah!! We're so brave!", te));

            Menu menu3 = new Menu("Menu-3")
                    .addItem("Surf",(te)-> displayText.accept("OK let's go surfing!", te))
                    .addItem("Sleep!!",(te)-> displayText.accept("Sleep?  Now yer talking!", te));

            MenuBar mBar = new MenuBar(Colors.IBlu);
            mBar.addDropDownMenu("File", menu1);
            mBar.addDropDownMenu("Edit", menu2);
            mBar.addDropDownMenu("Misc", new Menu("Menu-1-1-2")
                                                .addItem("ODDS and SODS", (te)->  displayText.accept("40K", te))
                                                .addItem("Do the Dishes", (te)->  displayText.accept("53K", te))
                                                .addItem("Get groceries", (te)->  displayText.accept("2K", te))
                                                .addItem("Fun stuff", menu3));
            

            
            
            tApp.getMainApplicationPanel().add(0, 1, mBar);
            tApp.getMainApplicationPanel().add(20, 1, testInput);

            Logging.info("App started");

        }).start();
        
        
    }


}
