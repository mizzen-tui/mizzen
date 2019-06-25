package dtb.terminalIO.components.input;

import dtb.terminalIO.components.ColorHiliter;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.FocusSetterToHiliteAction;
import dtb.terminalIO.events.ConditionalEventHandler;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.focus.Focusable;

public abstract class DefaultFocusableBehaviour  implements Focusable {
     
    boolean hasFocus = false;
    
    @Override
    public boolean hasFocus() {
        return hasFocus;
    }

    @Override
    public void setFocus(boolean focus, EventBus eventBus) {
        this.hasFocus = focus;
    }
    
    
    public static abstract class ComponentHiliterFocusableBehaviour extends DefaultFocusableBehaviour {

        Component<?> component;
        ColorHiliter colorHiliter;
   
        public ComponentHiliterFocusableBehaviour(Component<?> component) {
            super();
            this.component = component;
            colorHiliter =  new ColorHiliter(this.component.getColor());
        }

        @Override
        public void setFocus(boolean focus, EventBus eventBus) {
            super.setFocus(focus, eventBus);
            new FocusSetterToHiliteAction().hiliteAction(this, colorHiliter);
            this.component.setColor( colorHiliter.asColor() );
        }
    }
    

    public static class Simple extends ComponentHiliterFocusableBehaviour {
        
        private ConditionalEventHandler eventHandler;

        public Simple(Component<?> component, ConditionalEventHandler eventHandler) {
            super(component);
            this.eventHandler = eventHandler;
        }

        public Simple(Component<? extends ConditionalEventHandler> component) {
            super(component);
            this.eventHandler = (ConditionalEventHandler) component;
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
