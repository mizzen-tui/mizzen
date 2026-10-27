package mizzen.components;

import java.util.List;

import mizzen.components.input.DefaultFocusableBehaviour;
import mizzen.controlcodes.color.Color;
import mizzen.events.EventBus;
import mizzen.events.focus.FocusManager;
import mizzen.events.focus.Focusable;
import mizzen.events.focus.ToFocusable;
import mizzen.events.mouse.Clickable;

public class ClickableFocusableWrapper<T extends Component<?>> extends AbstractComponentWrappper<T> implements ToFocusable, Clickable {

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
        // components inside take focus themselves: the wrapper has none of its own (asFocusable() null),
        // so focus goes straight to them and the focus tree follows the component tree
        focusable = null;
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
    public Focusable asFocusable() {
        return focusable;
    }
    
    
    public static <T  extends Component<?>> Component<?> wrappedIfNeeded(T component) {
        if (ToFocusable.UTIL.isFocusable(component) && component instanceof Clickable) {
            return component;
        }
        return new ClickableFocusableWrapper<T>(component);
    }

}
