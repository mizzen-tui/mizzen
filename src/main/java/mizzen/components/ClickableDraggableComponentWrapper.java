package mizzen.components;

import java.util.List;

import mizzen.controlcodes.color.Color;
import mizzen.events.ConditionalEventHandler;
import mizzen.events.EventBus;
import mizzen.events.focus.FocusBehaviour;
import mizzen.events.focus.FocusManager;
import mizzen.events.focus.Focusable;
import mizzen.events.focus.RequestFocusEvent;
import mizzen.events.focus.ToFocusable;
import mizzen.events.mouse.ApplicationMouseEvent;
import mizzen.events.mouse.Clickable;
import mizzen.events.mouse.Draggable;
import mizzen.events.mouse.MOUSE_ACTION;

public class ClickableDraggableComponentWrapper extends AbstractComponentWrappper<Component<?>> implements ToFocusable, Clickable, Draggable {

    public ClickableDraggableComponentWrapper(Color color, Component<?> container) {
        super(color, container);
        if (container instanceof Clickable) {
            this.setClickableAction((Clickable)container);
        }
    }

    Focusable meAsFocusable;
    boolean focusDecided = false;            // asFocusable() worked out (null can be the answer)
    FocusBehaviour focusBehaviour;
    ConditionalEventHandler eventHandler;
    
    public void setFocusBehaviour(FocusBehaviour focusBehaviour) {
        this.focusBehaviour = focusBehaviour;
    }

    /**
     * The wrapped component's focusable if it has one; null if components inside it take focus
     * themselves (e.g. a grid holding controls) - the wrapper is then transparent to focus and focus
     * goes straight to them, while drags still come to the wrapper; otherwise the wrapper is one stop
     * itself. Worked out on first use.
     */
    @Override
    public Focusable asFocusable() {
        if (focusDecided) {
            return meAsFocusable;
        }
        focusDecided = true;

        Focusable focusable = ToFocusable.UTIL.toFocusable(this.getWrappedComponent());
        if (focusable != null) {
            meAsFocusable = focusable;
            return meAsFocusable;
        }

        List<Focusable> inside = FocusManager.UTIL.getFocusablesFromComponent(this.getWrappedComponent());
        if (inside != null && !inside.isEmpty()) {
            meAsFocusable = null;
            return null;
        }

        if (focusBehaviour == null && this.getWrappedComponent() instanceof FocusBehaviour) {
            focusBehaviour = (FocusBehaviour) this.getWrappedComponent();
        }

        meAsFocusable = new Focusable.FocusableFromParts(this, focusBehaviour, null);
        return meAsFocusable;
    }
    
    Draggable meAsDraggable;
    
    public ClickableDraggableComponentWrapper setDraggable(Draggable draggable) {
        this.meAsDraggable = draggable;
        return this;
    }
    
    Draggable asDraggable() {
        if (meAsDraggable != null) {
            return meAsDraggable;
        }
        return Draggable.NOOP;
    }

    @Override
    public boolean onDragStart(ApplicationMouseEvent appMouseEvent) {
        return asDraggable().onDragStart(appMouseEvent);
    }

    @Override
    public boolean onDrag(ApplicationMouseEvent appMouseEvent) {
        return asDraggable().onDrag(appMouseEvent);
    }

    @Override
    public boolean onDragStop(ApplicationMouseEvent appMouseEvent) {
        return asDraggable().onDragStop(appMouseEvent);
    }

    Clickable meAsClickable;

    public ClickableDraggableComponentWrapper setClickableAction(Clickable clickable) {
        this.meAsClickable = clickable;
        return this;
    }

    @Override
    public void onClick(ApplicationMouseEvent appMouseEvent) {
        if (meAsClickable != null) {
            meAsClickable.onClick(appMouseEvent);
            return;
        }
        if (MOUSE_ACTION.DOWN_LEFT.equals(appMouseEvent.getMouseAction())) {
            onClick( appMouseEvent.getEventBus());
        }
    }

    @Override
    public void onClick(EventBus eventBus) {
        if (meAsClickable != null) {
            meAsClickable.onClick(eventBus);
            return;
        }
        Focusable focusable = asFocusable();
        if (focusable != null && !focusable.hasFocus()) {
            // focus manager should have already put focus here - but just in case ..
            eventBus.raiseEvent(new RequestFocusEvent(focusable));
        }
    }


}
