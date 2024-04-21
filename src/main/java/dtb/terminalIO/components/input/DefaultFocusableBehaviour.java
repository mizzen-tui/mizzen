package dtb.terminalIO.components.input;

import dtb.terminalIO.components.ColorHiliter;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.EmptyAttributes;
import dtb.terminalIO.components.FocusSetterToHiliteAction;
import dtb.terminalIO.components.Hiliter;
import dtb.terminalIO.events.ConditionalEventHandler;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.focus.FocusBehaviour;
import dtb.terminalIO.events.focus.FocusManager;
import dtb.terminalIO.events.focus.Focusable;
import dtb.terminalIO.events.focus.FocusableComponentMixin;

public class DefaultFocusableBehaviour implements FocusBehaviour  {

    boolean hasFocus = false;

    @Override
    public boolean hasFocus() {
        return hasFocus;
    }

    @Override
    public void setFocus(boolean focus, EventBus eventBus) {
        this.hasFocus = focus;
    }

    @Override
    public void unFocus(Focusable newFocusable, EventBus eventBus) {
        this.hasFocus = false;
    }

    @Override
    public void focus(Focusable oldFocusable, EventBus eventBus) {
        this.hasFocus = true;
    }

    protected static abstract class ComponentHiliterFocusableBehaviour extends DefaultFocusableBehaviour {

        Component<?> component;
        ColorHiliter colorHiliter;
        ColorHiliter emptyColorHiliter;

        protected ComponentHiliterFocusableBehaviour(FocusableComponentMixin<?> component, boolean hiliteEmptyColor) {
            super();
            
            this.component = component;
            colorHiliter =  new ColorHiliter(this.component);
            if (hiliteEmptyColor) {
                emptyColorHiliter = new ColorHiliter(this.component);
            }
        }
        
        protected ComponentHiliterFocusableBehaviour(Component<?> component, boolean hiliteEmptyColor) {
            super();
            this.component = component;
            colorHiliter =  new ColorHiliter(this.component);
            if (hiliteEmptyColor) {
                emptyColorHiliter = new ColorHiliter(this.component);
            }
        }

        protected ComponentHiliterFocusableBehaviour(Component<?> component) {
            this(component, false);
        }

        protected ComponentHiliterFocusableBehaviour(FocusableComponentMixin<?> component) {
            this(component, false);
        }

        @Override
        public void setFocus(boolean focus, EventBus eventBus) {
            if (focus == hasFocus()) {
                return;
            }
            super.setFocus(focus, eventBus);  // set the focus flag
            new FocusSetterToHiliteAction().hiliteAction(this, colorHiliter); // hilite
            if (this.component instanceof Hiliter) {
                if (focus) 
                     ((Hiliter)this.component).hilite(); 
                else 
                     ((Hiliter)this.component).unHilite();
            }
            else {
                this.component.setColor( colorHiliter.asColor() );
            }
            if (this.emptyColorHiliter != null && this.component instanceof EmptyAttributes) {
                new FocusSetterToHiliteAction().hiliteAction(this, emptyColorHiliter); // hilite
                ((EmptyAttributes<?>)this.component).setEmptyColor( emptyColorHiliter.asColor() );
            }
            if (focus) {
               Component.UTIL.bringToFront(component);
            }
        }
        
        @Override
        public void unFocus(Focusable newFocusable, EventBus eventBus) {
            super.unFocus(newFocusable, eventBus);
            new FocusSetterToHiliteAction().hiliteAction(this, colorHiliter); // hilite
            if (this.component instanceof Hiliter) {
                     ((Hiliter)this.component).unHilite();
            }
            else {
                this.component.setColor( colorHiliter.asColor() );
            }
            if (this.emptyColorHiliter != null && this.component instanceof EmptyAttributes) {
                new FocusSetterToHiliteAction().hiliteAction(this, emptyColorHiliter); // hilite
                ((EmptyAttributes<?>)this.component).setEmptyColor( emptyColorHiliter.asColor() );
            }
        }
        
        public void focus(Focusable oldFocusable, EventBus eventBus) {
            super.focus(oldFocusable, eventBus);
            new FocusSetterToHiliteAction().hiliteAction(this, colorHiliter); // hilite
            if (this.component instanceof Hiliter) {
                 ((Hiliter)this.component).hilite(); 
            }
            else {
                this.component.setColor( colorHiliter.asColor() );
            }
            if (this.emptyColorHiliter != null && this.component instanceof EmptyAttributes) {
                new FocusSetterToHiliteAction().hiliteAction(this, emptyColorHiliter); // hilite
                ((EmptyAttributes<?>)this.component).setEmptyColor( emptyColorHiliter.asColor() );
            }
            Component.UTIL.bringToFront(component);
        }
        
        public String toString() {
            return this.getClass().getSimpleName() + ":[" +String.valueOf(component)+"]";
        }
    }

    /**
     * A focusable implementation that will hilite a component and then
     * redirect all other events to the passed in ConditionalEventHandler
     */
    public static class Simple extends ComponentHiliterFocusableBehaviour implements Focusable {
        
        private FocusManager parentFocusManager;
        
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

        public FocusManager getParentFocusManager() {
            return parentFocusManager;
        }

        public void setParentFocusManager(FocusManager parentFocusManager) {
            Focusable.UTIL.setParentFocusManager(this, parentFocusManager, (fm)-> this.parentFocusManager = fm);
        }

        @Override
        public EventHandler canHandle(TerminalEvent<?> te) {
            return eventHandler.canHandle(te);
        }
        
        @Override
        public void handle(TerminalEvent<?> te) {
            eventHandler.handle(te);
        }

    }

    /**
     * A Focusable implementation that will hilite a component (if it can be hilited) and 
     * ignore any other event types
     */
    public static class HiliteOnFocus extends Simple {

        public HiliteOnFocus(Component<?> component) {
            super(component, ConditionalEventHandler.NOOP);
        }
    }

}
