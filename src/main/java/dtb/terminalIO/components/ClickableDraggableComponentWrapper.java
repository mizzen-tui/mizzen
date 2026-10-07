package dtb.terminalIO.components;

import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.events.ConditionalEventHandler;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.focus.FocusBehaviour;
import dtb.terminalIO.events.focus.Focusable;
import dtb.terminalIO.events.focus.RequestFocusEvent;
import dtb.terminalIO.events.focus.ToFocusable;
import dtb.terminalIO.events.mouse.ApplicationMouseEvent;
import dtb.terminalIO.events.mouse.Clickable;
import dtb.terminalIO.events.mouse.Draggable;
import dtb.terminalIO.events.mouse.MOUSE_ACTION;

public class ClickableDraggableComponentWrapper extends AbstractComponentWrappper<Component<?>> implements ToFocusable, Clickable, Draggable {

    public ClickableDraggableComponentWrapper(Color color, Component<?> container) {
        super(color, container);
        if (container instanceof Clickable) {
            this.setClickableAction((Clickable)container);
        }
    }

    Focusable meAsFocusable;
    FocusBehaviour focusBehaviour;
    ConditionalEventHandler eventHandler;
    
    public void setFocusBehaviour(FocusBehaviour focusBehaviour) {
        this.focusBehaviour = focusBehaviour;
    }

    @Override
    public Focusable asFocusable() {
        if (meAsFocusable != null) {
            return meAsFocusable;
        }

        Focusable focusable = ToFocusable.UTIL.toFocusable(this.getWrappedComponent());
        if (focusable != null) {
            meAsFocusable = focusable;
            return meAsFocusable; //   WARNING: This might not work because we have nested a focusable - we will see
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
        if (!asFocusable().hasFocus()) {
            // focus manager should have already put focus here - but just in case ..
            eventBus.raiseEvent(new RequestFocusEvent(asFocusable()));
        }
    }


}
