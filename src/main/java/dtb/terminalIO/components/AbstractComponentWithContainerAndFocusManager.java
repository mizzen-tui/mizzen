package dtb.terminalIO.components;

import java.util.Collection;

import dtb.terminalIO.controlcodes.ControlCode.Color;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.focus.FocusManager;
import dtb.terminalIO.events.focus.Focusable;

public abstract class AbstractComponentWithContainerAndFocusManager<T extends Composite> extends AbstractComponentWithContainerAndFocusable<T> implements FocusManager {

    protected AbstractComponentWithContainerAndFocusManager(Color color) {
        super(color);
    }

    abstract protected FocusManager getFocusManager();
    
    @Override
    protected Focusable asFocusable() {
        return getFocusManager();
    }

    @Override
    public void setChildAsCurrentFocus(Focusable focusableChild, EventBus eventBus) {
        getFocusManager().setChildAsCurrentFocus(focusableChild, eventBus);
    }

    @Override
    public Focusable getCurrentChildFocus() {
        return getFocusManager().getCurrentChildFocus();
    }

    @Override
    public Collection<Focusable> getChildFocusables() {
        return getFocusManager().getChildFocusables();
    }

}
