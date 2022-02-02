package dtb.terminalIO.examples;

import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.composite.HorizontalPanel;
import dtb.terminalIO.components.input.Button;
import dtb.terminalIO.components.input.RockerButton;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.events.RequestHideCursorEvent;
import dtb.terminalIO.events.RequestShowCursorEvent;
import dtb.terminalIO.events.focus.DefaultFocusManager;
import dtb.terminalIO.events.focus.FocusManager;
import dtb.terminalIO.events.keyboard.KeyStroke;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.util.Logging;

public class RockerButtonTest2 {

    public static void main(String args[]) {

        Logging.setEnabled(true);

        TUIApp app = new TUIApp(args).setEmptyChar(' ')
                                     .setDefaultMainApplicationPanel()
                                     .confirmOnExit();

        app.setOnReadyStart((tApp)->{
            

            HorizontalPanel controls = new HorizontalPanel().setHorizontalSpacer(5);
            
            RockerButton rockerButtonShowCursor = new RockerButton(Colors.BIBlu, "Cursor");
            rockerButtonShowCursor.setHotKey( new KeyStroke('2') );
            rockerButtonShowCursor.onChange((te)->{
                if (rockerButtonShowCursor.getValue()) {
                    app.getEventBus().raiseEvent(new RequestShowCursorEvent(rockerButtonShowCursor, new Position.Simple(0,5)));
                } else {
                    app.getEventBus().raiseEvent(new RequestHideCursorEvent(rockerButtonShowCursor));
                }
            });
            
            controls.add(rockerButtonShowCursor);
            
            Button quit = new Button(Colors.BIGre,"Quit");
            quit.setHotKey(Keys.CTRL_C_KEYSTROKE);
            quit.onChange((te)->app.getTerminateAppHotKeyEventHandler().handle(te));
            controls.add(quit);
            
            
            tApp.getMainApplicationPanel().add(10, 10, controls);
            
            FocusManager mainFocusManager = FocusManager.UTIL.buildFocusManagerFromComponent(tApp.getMainApplicationPanel(),
                    tApp.getEventBus(),
                    ()-> new DefaultFocusManager().setNavKeysFirst(false)
                                                  .setCycling(false)
                                                  .setPrevKeyStroke(Keys.SHIFT_TAB_KEYSTROKE)
                                                  .setNextKeyStroke(Keys.TAB_KEYSTROKE));

            tApp.getEventBus().appendEventHandler( mainFocusManager );
            
            FocusManager.UTIL.verifyPathsToFocusables(mainFocusManager, (Component<?>) tApp.getMainApplicationPanel());

        });
        
        app.start();
        
    }


    
}
