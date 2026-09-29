package dtb.terminalIO.events.focus;

import dtb.terminalIO.components.AbstractComponent;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.events.EventBus;

public interface ToFocusable {
    Focusable asFocusable();

    public static class FocusableComponentPair implements ToFocusable {
        public Focusable    focusable;
        public Component<?> component;

        public FocusableComponentPair() {}

        public FocusableComponentPair(Focusable focusable, Component<?> component) {
            super();
            this.focusable = focusable;
            this.component = component;
        }

        public Focusable asFocusable() {
            return focusable;
        }

    }

    public static class UTIL {

        /**
         * return true if object (o) is a Focusable or ToFocusable
         */
        public static boolean isFocusable(Object o) {
            if (o instanceof Focusable) {
                return true;
            }
            if (o instanceof ToFocusable) {
                return true;
            }
            return false;
        }

        /**
         * return o  as a Focusable if o is actually a Focusable or if o is a ToFocusable
         * <br/>
         * return null if o is neither of the above.
         */
        public static Focusable toFocusable(Object o) {
            if (o instanceof Focusable) {
                return (Focusable) o;
            }
            if (o instanceof ToFocusable) {
                return ((ToFocusable)o).asFocusable();
            }
            return null;
        }

        /**
         * return true if focusable == o , or focusable == ((ToFocusable)o).asFocusable()
         */
        public static boolean isObjectSameAsFocusable(Focusable focusable, Object o) {
            if (toFocusable(o) ==  focusable) {
                return true;
            }
            return false;
        }

        /**
         * if component is Focusable or is ToFocusable, returns it as a Focusable,
         * otherwise traverse up parent tree until it either finds a focusable (and returns it)
         * or returns null if no focusable found in the ancestor branch
         * @param component
         * @return
         */
        public static FocusableComponentPair componentToFocusable(Component<?> component) {
            while (component != null && (! ToFocusable.UTIL.isFocusable(component))) {
                component = (Component<?>)component.getParentContainer();
            }
            if (component == null) {
                return null;
            }
            return new FocusableComponentPair(toFocusable(component),component);
        }


    }

}