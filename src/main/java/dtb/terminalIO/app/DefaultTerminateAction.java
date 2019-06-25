package dtb.terminalIO.app;

import java.io.IOException;
import java.util.function.Consumer;

import dtb.terminalIO.controlcodes.ControlCode;
import dtb.terminalIO.util.Stty;

public class DefaultTerminateAction implements Consumer<TUIApp> {

    @Override
    public void accept(TUIApp tApp) {
        tApp.getEventBus().stopConsuming();
        tApp.getScreen().add(ControlCode.SHOW_CURSOR);
        tApp.getScreen().refresh();

        try {
            Stty.setCooked();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // next refresh() is necessary to get cursor to be below
        // rendered canvas
        tApp.getCanvas().refresh();
        return;
    }

}
