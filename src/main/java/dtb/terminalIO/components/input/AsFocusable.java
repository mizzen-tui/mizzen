package dtb.terminalIO.components.input;

import dtb.terminalIO.components.ColorHiliter;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.FocusSetterToHiliteAction;
import dtb.terminalIO.events.ConditionalEventHandler;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.Focusable;
import dtb.terminalIO.events.TerminalEvent;

abstract class AsFocusable  implements Focusable {
     
    public static interface ToFocusable {
        Focusable asFocusable(EventBus eventBus);
    }
    
    Component<?> component;
    
    EventBus eventBus;
    
    boolean hasFocus = false;
    
    ColorHiliter colorHiliter;
    
    public AsFocusable(Component<?> component, EventBus eventBus) {
        this.component = component;
        this.eventBus = eventBus;
        colorHiliter =  new ColorHiliter(this.component.getColor());
    }
    
    @Override
    public boolean hasFocus() {
        return hasFocus;
    }

    @Override
    public void setFocus(boolean focus) {
        this.hasFocus = focus;
        new FocusSetterToHiliteAction().hiliteAction(this, colorHiliter);
        this.component.setColor( colorHiliter.asColor() );
    }

    public static class Simple extends AsFocusable {
        
        private ConditionalEventHandler eventHandler;

        public Simple(Component<?> component, EventBus eventBus, ConditionalEventHandler eventHandler) {
            super(component, eventBus);
            this.eventHandler = eventHandler;
        }
        
        @Override
        public void handle(TerminalEvent<?> te) {
            eventHandler.handle(te);
        }

        @Override
        public EventHandler canHandle(TerminalEvent<?> te) {
            return eventHandler.canHandle(te);
        }

        
    }
    

}
