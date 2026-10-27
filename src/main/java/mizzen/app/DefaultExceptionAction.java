package mizzen.app;

import java.util.function.BiConsumer;

import mizzen.events.TerminalEvent;
import mizzen.events.TerminalEvent.TYPE;

public class DefaultExceptionAction implements BiConsumer<TUIApp, TerminalEvent<?>> {

    @Override
    public void accept(TUIApp tApp, TerminalEvent<?> te) {

        // DefaultTerminateAction.resetTerminalForExiting(tApp);

        Exception e = (Exception) te.getEventData();
        tApp.terminal().lastException = e;
        // e.getEventBus().raiseEvent(new TerminalEvent<Void>(e.getEventBus(), TYPE.TERMINATE, null)
        te.getEventBus().raiseEvent(new TerminalEvent<Void>(this, TYPE.TERMINATE, null));
    }

}
