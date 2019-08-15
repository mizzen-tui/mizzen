package dtb.terminalIO.components.input;

import dtb.terminalIO.components.ColorHiliter;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.EmptyAttributes;
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
        ColorHiliter emptyColorHiliter;
   
        public ComponentHiliterFocusableBehaviour(Component<?> component, boolean hiliteEmptyColor) {
            super();
            this.component = component;
            colorHiliter =  new ColorHiliter(this.component.getColor());
            if (hiliteEmptyColor) {
                emptyColorHiliter = new ColorHiliter(this.component.getColor());
            }
        }

        public ComponentHiliterFocusableBehaviour(Component<?> component) {
            this(component, false);
        }

        @Override
        public void setFocus(boolean focus, EventBus eventBus) {
            super.setFocus(focus, eventBus);  // set the focus flag
            new FocusSetterToHiliteAction().hiliteAction(this, colorHiliter); // hilite
            this.component.setColor( colorHiliter.asColor() );
            if (this.emptyColorHiliter != null && this.component instanceof EmptyAttributes) {
                new FocusSetterToHiliteAction().hiliteAction(this, emptyColorHiliter); // hilite
                ((EmptyAttributes)this.component).setEmptyColor( emptyColorHiliter.asColor() );
            }
            if (focus) {
               Component.UTIL.bringToFront(component);
            }
        }
    }

    // a focus
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

        public Simple(Component<? extends ConditionalEventHandler> component, boolean hiliteEmptyColor) {
            super(component, hiliteEmptyColor);
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
