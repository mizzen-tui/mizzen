package dtb.terminalIO.components;

import dtb.terminalIO.controlcodes.ControlCode.Color;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.focus.Focusable;

public abstract class AbstractComponentWithContainerAndFocusable<T extends Composite> 
                        extends AbstractComponentWithContainer<T> implements Focusable {

    protected AbstractComponentWithContainerAndFocusable(Color color) {
        super(color);
    }

    @Override
    public EventHandler canHandle(TerminalEvent<?> te) {
        if ( asFocusable().canHandle(te) == null ) {
            return null;
        }
        return this;
    }

    @Override
    public void handle(TerminalEvent<?> te) {
        asFocusable().handle(te);
    }

    @Override
    public boolean hasFocus() {
        return asFocusable().hasFocus();
    }

    @Override
    public void setFocus(boolean focus, EventBus eventBus) {
        asFocusable().setFocus(focus, eventBus);
    }

    @Override
    abstract protected T getContainer();

    abstract protected Focusable asFocusable();

}
