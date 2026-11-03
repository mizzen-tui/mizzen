package mizzen.components.input;

import java.util.function.Function;
import java.util.function.Supplier;

import mizzen.components.ColorHiliter;
import mizzen.components.Component;
import mizzen.components.EmptyAttributes;
import mizzen.components.Enabled;
import mizzen.components.FocusSetterToHiliteAction;
import mizzen.components.Hiliter;
import mizzen.components.theme.Theme;
import mizzen.controlcodes.color.Color;
import mizzen.events.ConditionalEventHandler;
import mizzen.events.EventBus;
import mizzen.events.EventHandler;
import mizzen.events.TerminalEvent;
import mizzen.events.focus.FocusBehaviour;
import mizzen.events.focus.FocusManager;
import mizzen.events.focus.Focusable;
import mizzen.events.focus.FocusableComponentMixin;

public class DefaultFocusableBehaviour implements FocusBehaviour  {

    boolean hasFocus = false;
    Supplier<Enabled>  getEnabled = null;

    public DefaultFocusableBehaviour(Supplier<Enabled>  getEnabled) {
        this.getEnabled = getEnabled;
    }

    public DefaultFocusableBehaviour(Enabled objectThatCanBeEnabled) {
        this.getEnabled = ()-> { return objectThatCanBeEnabled; };
    }
    
    @Override
    public boolean hasFocus() {
        return hasFocus;
    }
    
    public boolean isEnabled() {
        return getEnabled.get().isEnabled(); 
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
        Function<Theme, Color> themeColorBefore;        // focused with no colour of its own: its theme colour before (showFocus)

        protected ComponentHiliterFocusableBehaviour(FocusableComponentMixin<?> component, boolean hiliteEmptyColor) {
            super(component);
            
            this.component = component;
            colorHiliter =  new ColorHiliter(this.component);
            if (hiliteEmptyColor) {
                emptyColorHiliter = new ColorHiliter(this.component);
            }
        }
        
        protected ComponentHiliterFocusableBehaviour(Component<?> component, boolean hiliteEmptyColor) {
            super(component);
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
            showFocus(focus);
            if (focus) {
               Component.UTIL.bringToFront(component);
            }
        }
        
        @Override
        public void unFocus(Focusable newFocusable, EventBus eventBus) {
            super.unFocus(newFocusable, eventBus);
            showFocus(false);
        }
        
        public void focus(Focusable oldFocusable, EventBus eventBus) {
            super.focus(oldFocusable, eventBus);
            showFocus(true);
            Component.UTIL.bringToFront(component);
        }
        
        /**
         * Shows focus in the component's colour. A Hiliter shows it its own way. A component with a colour of its own
         * shows that colour hilited (bold), as it always has. One with none keeps none: while focused it draws its theme
         * colour in bold, worked out as it is drawn - so a theme set later still reaches it.
         * <p/>
         * Not the resolved colour, set as its own: that is what ColorHiliter would give (it keeps the colour it saw at the
         * first focus), and the component would stay in the theme of that moment.
         * <pre>
         *     TextInput, no colour, Dark:   focused  Dark's input colour, bold     a switch to Light: Light's, bold
         *     TextInput given Colors.IYel:  focused  IYel bold                     (as before)
         * </pre>
         */
        private void showFocus(boolean focused) {
            if (!focused && themeColorBefore != null) {          // it was focused with its theme colour: follow it again
                component.setThemeColor(themeColorBefore);
                themeColorBefore = null;
            }
            boolean ownColor = component.getOwnColor() != null;
            if (this.component instanceof Hiliter) {
                if (focused) {
                    ((Hiliter) this.component).hilite();
                } else {
                    ((Hiliter) this.component).unHilite();
                }
            } else if (!ownColor) {
                if (focused && themeColorBefore == null) {
                    Function<Theme, Color> before = component.getThemeColor();
                    themeColorBefore = before;
                    component.setThemeColor((theme) -> before.apply(theme).bold());
                }
            } else {
                new FocusSetterToHiliteAction().hiliteAction(this, colorHiliter);
                this.component.setColor(colorHiliter.asColor());
            }
            // the empty colour: with no colour of its own, its gaps already follow its colour (bold or not) as they are drawn
            if (ownColor && this.emptyColorHiliter != null && this.component instanceof EmptyAttributes) {
                new FocusSetterToHiliteAction().hiliteAction(this, emptyColorHiliter);
                ((EmptyAttributes<?>) this.component).setEmptyColor(emptyColorHiliter.asColor());
            }
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

        @Override
        public Component<?> getFocusComponent() {
            return component;
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
