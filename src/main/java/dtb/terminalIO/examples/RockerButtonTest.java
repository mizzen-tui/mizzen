package dtb.terminalIO.examples;

import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.components.ButtonBar;
import dtb.terminalIO.components.input.Button;
import dtb.terminalIO.components.input.RockerButton;
import dtb.terminalIO.controlcodes.Colors;
import dtb.terminalIO.events.focus.FocusManager;
import dtb.terminalIO.events.keyboard.KeyStroke;
import dtb.terminalIO.events.keyboard.Keys;

public class RockerButtonTest {

    public static void main(String args[]) {

        TUIApp app = new TUIApp(args).setEmptyChar(' ')
                                     .setDefaultMainApplicationPanel()
                                     .confirmOnExit();

        ButtonBar buttons = new ButtonBar(Colors.BIBlu);
        buttons.setNoBorderChars();

        buttons.setHorizontalSpacer(2);
        
        RockerButton rockerButton = new RockerButton(Colors.BIGre, "Test");
        rockerButton.setHotKey( new KeyStroke('1') );
        
        buttons.addComponent(rockerButton);

        Button quit = new Button(Colors.BIGre,"Quit");
        quit.setHotKey(Keys.CTRL_C_KEYSTROKE);
        quit.onChange((te)->app.getTerminateAppHotKeyEventHandler().handle(te));
        buttons.add(quit);

        app.getMainApplicationPanel().add(buttons);

        app.setOnReadyStart((tApp)->{

            FocusManager mainFocusManager = FocusManager.UTIL.buildFocusManagerFromComponent(tApp.getMainApplicationPanel(),tApp.getEventBus());

            tApp.getEventBus().appendEventHandler(  mainFocusManager );

        });

        app.start();

    }

}
