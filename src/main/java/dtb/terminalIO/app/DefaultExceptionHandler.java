package dtb.terminalIO.app;

import java.io.IOException;
import java.util.function.BiConsumer;

import dtb.terminalIO.controlcodes.ControlCode;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.util.Stty;

public class DefaultExceptionHandler implements BiConsumer<TUIApp, TerminalEvent<?>> {

    @Override
    public void accept(TUIApp tApp, TerminalEvent<?> te) {
        tApp.getEventBus().stopConsuming();
        tApp.getScreen().add(ControlCode.SHOW_CURSOR);
        tApp.getScreen().refresh();

        try {
            Stty.setCooked();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("\n");
        ((Exception) te.getEventData()).printStackTrace();
        System.out.println("\n");
    }

}
