package dtb.terminalIO.components;

import java.util.List;

import dtb.terminalIO.components.input.DefaultFocusableBehaviour;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.focus.FocusManager;
import dtb.terminalIO.events.focus.Focusable;
import dtb.terminalIO.events.focus.ToFocusable;
import dtb.terminalIO.events.mouse.Clickable;

public class ClickableFocusableWrapper<T extends Component<?>> extends AbstractWrappedComponentAndFocusable<T> implements Clickable {

    Focusable focusable = null;

    protected ClickableFocusableWrapper(Color color, T component) {
        super(color, component);
        focusable = ToFocusable.UTIL.toFocusable(getWrappedComponent());
        if (focusable != null ) {
            return;
        }
        List<Focusable> focusables = FocusManager.UTIL.getFocusablesFromComponent(getWrappedComponent());
        if (focusables == null || focusables.size()<1) {
            focusable = new DefaultFocusableBehaviour.HiliteOnFocus(this.getWrappedComponent());
            return;
        }
        focusable = FocusManager.UTIL.buildFocusManagerFromComponent(getWrappedComponent(), null);
    }

    public ClickableFocusableWrapper(T component) {
        this(component.getColor(), component);
    }

    @Override
    public void onClick(EventBus eventBus) {
        if (getWrappedComponent() instanceof Clickable) {
            ((Clickable)getWrappedComponent()).onClick(eventBus);
            return;
        }
        // what here ?  bringToFront?
    }

    @Override
    protected Focusable asFocusable() {
        return focusable;
    }
    
    
    public static <T  extends Component<?>> Component<?> wrappedIfNeeded(T component) {
        if (ToFocusable.UTIL.isFocusable(component) && component instanceof Clickable) {
            return component;
        }
        return new ClickableFocusableWrapper<T>(component);
    }

}
