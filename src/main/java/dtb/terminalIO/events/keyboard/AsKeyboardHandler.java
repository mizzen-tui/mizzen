package dtb.terminalIO.events.keyboard;

import dtb.terminalIO.components.HasName;
import dtb.terminalIO.events.ChainedEventHandler;
import dtb.terminalIO.events.ConditionalEventHandler;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.focus.FocusManager;
import dtb.terminalIO.events.focus.ToFocusable;

/**
 * "I am not a KeyboardHandler, but here is one for me" - like ToFocusable for focus.
 *
 * Lets a component keep its key handling in the usual event handler style (a KeyInputEventHandler,
 * a chain of ConditionalEventHandlers ...) and still take part in key routing (bubbling, see
 * KeyboardHandler): hand that handler out, adapted with UTIL.toKeyboardHandler.
 *
 * Example (CheckBoxInput):
 *     private final KeyboardHandler keys = AsKeyboardHandler.UTIL.toKeyboardHandler(
 *             new KeyInputEventHandler(te -> toggle(), Keys.ENTER_KEYSTROKE, Keys.SPACE_KEYSTROKE));
 *     public KeyboardHandler asKeyboardHandler() { return keys; }
 */
public interface AsKeyboardHandler {

    /** the handler for this component's keys; null if it has none (right now) */
    KeyboardHandler asKeyboardHandler();

    public static class UTIL {

        /**
         * The KeyboardHandler for o: o itself if it is one, else the one it hands out
         * (AsKeyboardHandler); null if neither.
         */
        public static KeyboardHandler asKeyboardHandler(Object o) {
            if (o instanceof KeyboardHandler) {
                return (KeyboardHandler) o;
            }
            if (o instanceof AsKeyboardHandler) {
                return ((AsKeyboardHandler) o).asKeyboardHandler();
            }
            return null;
        }

        /** does o handle keys - itself, or with one it hands out? */
        public static boolean isKeyboardHandler(Object o) {
            return asKeyboardHandler(o) != null;
        }

        /**
         * Any object used as a KeyboardHandler, digging inside it if needed:
         *   - a KeyboardHandler: itself; an AsKeyboardHandler: the one it hands out
         *   - an event handler (ConditionalEventHandler, ChainedEventHandler): it takes a key if
         *     its canHandle says so - e.g. a KeyInputEventHandler for ENTER and SPACE
         *   - a component that is focusable (ToFocusable): its Focusable - which is an event handler
         * null if none of these. Unlike a plain onKey, an adapted event handler can tell whether it
         * wants a key without acting (wantsKey).
         * A focus manager is refused (IllegalArgumentException): its canHandle passes events on to
         * its children, which would then get the key a second time. Use its own handlers instead.
         */
        public static KeyboardHandler toKeyboardHandler(Object o) {
            KeyboardHandler handler = asKeyboardHandler(o);
            if (handler != null) {
                return handler;
            }
            if (o instanceof FocusManager) {
                throw new IllegalArgumentException("toKeyboardHandler(" + HasName.UTIL.getName(o) + "): a focus manager"
                        + " passes events on to its children - adapt its own handlers instead");
            }
            if (o instanceof ConditionalEventHandler) {
                return fromConditionalEventHandler((ConditionalEventHandler) o);
            }
            if (o instanceof ChainedEventHandler) {
                return fromConditionalEventHandler(((ChainedEventHandler) o).asConditionalEventHandler());
            }
            if (ToFocusable.UTIL.isFocusable(o)) {
                return toKeyboardHandler(ToFocusable.UTIL.toFocusable(o));
            }
            return null;
        }

        private static KeyboardHandler fromConditionalEventHandler(ConditionalEventHandler handler) {
            return new KeyboardHandler() {
                @Override
                public boolean onKey(KeyEvent ke) {
                    EventHandler taker = handler.canHandle(ke);
                    if (taker == null) {
                        return false;
                    }
                    taker.handle(ke);
                    return true;
                }

                @Override
                public boolean wantsKey(KeyEvent ke) {
                    return handler.canHandle(ke) != null;
                }
            };
        }
    }
}
