package dtb.terminalIO.app;

import java.util.function.BiConsumer;

import dtb.terminalIO.events.TerminalEvent;

public class DefaultExceptionAction implements BiConsumer<TUIApp, TerminalEvent<?>> {

    @Override
    public void accept(TUIApp tApp, TerminalEvent<?> te) {

        DefaultTerminateAction.resetTerminalForExiting(tApp);

        Exception e = (Exception) te.getEventData();
        tApp.lastException = e;
    }

}
