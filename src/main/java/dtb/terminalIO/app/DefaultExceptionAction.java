package dtb.terminalIO.app;

import java.util.function.BiConsumer;

import dtb.terminalIO.controlcodes.ControlCode;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.util.Stty;

public class DefaultExceptionAction implements BiConsumer<TUIApp, TerminalEvent<?>> {

    @Override
    public void accept(TUIApp tApp, TerminalEvent<?> te) {
        tApp.getEventBus().stopConsuming();
        tApp.getScreen().add( ControlCode.DISABLE_MOUSE_TRACKING );
        tApp.getScreen().add(ControlCode.SHOW_CURSOR);
        tApp.getScreen().refresh();

        try {
            Stty.setCooked();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Exception e = (Exception) te.getEventData();
        tApp.lastException = e;
    }

}
