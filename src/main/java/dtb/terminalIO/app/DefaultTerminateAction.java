package dtb.terminalIO.app;

import java.util.function.Consumer;

import dtb.terminalIO.controlcodes.ControlCode;
import dtb.terminalIO.controlcodes.Renderer;
import dtb.terminalIO.util.Stty;

public class DefaultTerminateAction implements Consumer<TUIApp> {

    @Override
    public void accept(TUIApp tApp) {

        resetTerminalForExiting(tApp);

        // inline: a full redraw leaves the cursor below the canvas (the shell prompt goes under the
        // output); full screen: back to the shell's screen as it was
        tApp.getScreen().getScreenMode().handBackTerminal(tApp.getCanvas());

        if (tApp.terminal().lastException != null) {
            
            tApp.terminal().lastException.printStackTrace();
            
        }
        
        return;
    }

    public static void resetTerminalForExiting(TUIApp tApp) {
        resetTerminalForExiting(tApp, null);
    }
    
    public static void resetTerminalForExiting(TUIApp tApp, Renderer r) {
        tApp.getEventBus().stopConsuming();
        if (tApp.terminal().mouseEventOchestrator != null) {
        	tApp.terminal().mouseEventOchestrator.disableMouseHandling();
        	
        }
//        tApp.getScreen().add( ControlCode.DISABLE_MOUSE_TRACKING );
//        tApp.getScreen().add( ControlCode.SET_BTN_MOTION_TRACKING_DISABLE );
        tApp.getScreen().add(ControlCode.SHOW_CURSOR);
        if (r != null) {
            tApp.getScreen().add(r);
        }
        tApp.getScreen().refresh();

        try {
            Stty.setCooked();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
