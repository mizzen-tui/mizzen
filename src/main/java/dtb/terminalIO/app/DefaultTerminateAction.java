package dtb.terminalIO.app;

import java.util.function.Consumer;

import dtb.terminalIO.controlcodes.ControlCode;
import dtb.terminalIO.controlcodes.Renderer;
import dtb.terminalIO.core.ScreenCanvasBackend;
import dtb.terminalIO.util.Stty;

public class DefaultTerminateAction implements Consumer<TUIApp> {

    @Override
    public void accept(TUIApp tApp) {

        resetTerminalForExiting(tApp);

        // a full redraw leaves the cursor below the canvas (the shell prompt goes under the output)
        if (tApp.getCanvas().getCanvasBackend() instanceof ScreenCanvasBackend) {
            ((ScreenCanvasBackend) tApp.getCanvas().getCanvasBackend()).fullUpdateOnNextRefresh();
        }
        tApp.getCanvas().refresh();

        if (tApp.lastException != null) {
            
            tApp.lastException.printStackTrace();
            
        }
        
        return;
    }

    public static void resetTerminalForExiting(TUIApp tApp) {
        resetTerminalForExiting(tApp, null);
    }
    
    public static void resetTerminalForExiting(TUIApp tApp, Renderer r) {
        tApp.getEventBus().stopConsuming();
        if (tApp.mouseEventOchestrator != null) {
        	tApp.mouseEventOchestrator.disableMouseHandling();
        	
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
