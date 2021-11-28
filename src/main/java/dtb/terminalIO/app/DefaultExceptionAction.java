package dtb.terminalIO.app;

import java.util.function.BiConsumer;

import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.TerminalEvent.TYPE;

public class DefaultExceptionAction implements BiConsumer<TUIApp, TerminalEvent<?>> {

    @Override
    public void accept(TUIApp tApp, TerminalEvent<?> te) {

        // DefaultTerminateAction.resetTerminalForExiting(tApp);

        Exception e = (Exception) te.getEventData();
        tApp.lastException = e;
        // e.getEventBus().raiseEvent(new TerminalEvent<Void>(e.getEventBus(), TYPE.TERMINATE, null)
        te.getEventBus().raiseEvent(new TerminalEvent<Void>(this, TYPE.TERMINATE, null));
    }

}
