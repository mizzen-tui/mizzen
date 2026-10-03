package dtb.terminalIO.events.focus;

import dtb.terminalIO.components.Component;
import dtb.terminalIO.events.ConditionalEventHandler;
import dtb.terminalIO.events.keyboard.AsKeyboardHandler;
import dtb.terminalIO.events.keyboard.KeyboardHandler;
import dtb.terminalIO.events.keyboard.KeyEvent;
import dtb.terminalIO.events.keyboard.KeyStroke;

/**
 * Delivers key presses for a navigation root: the application's root focus manager, or a modal
 * dialog's (TRAP), which gets keys first. The root hands it every key (DefaultFocusManager).
 *
 * A key goes to the focused component first, then up through the components around it, until
 * someone uses it ("bubbling"). At each focus manager on the way up (a panel of fields, a widget
 * such as a dropdown, the root) the order is:
 *   1. the focus manager's component, if it handles keys (KeyboardHandler, or one it hands out -
 *      AsKeyboardHandler; e.g. a dropdown's "DOWN opens my list", or a wrapper's appended handlers)
 *   2. a hot key of one of its other children (e.g. F2 on a button next to the focused field)
 *   3. the focus manager's custom handler (DefaultFocusManager.setCustomExtendedEventHandler)
 *   4. its navigation keys: move focus (advance) - MOVED: focus moves; STAY: focus stays (key used);
 *      DONE: no further child in it, so go on to the next focus manager out
 * Components between two focus managers that handle keys are asked on the way up too.
 * The walk ends at the root; if nobody used the key, the root doesn't take it.
 *
 * Example: form [name, dropdown(button, list)], focus on the dropdown's button, DOWN pressed:
 * the button doesn't use it -> the dropdown's own handler does ("open my list") -> done.
 * TAB instead: button no, dropdown no (TAB isn't its key) -> the form moves focus to the next field.
 *
 * When nothing has focus - not yet, or the focused component went away (e.g. a dropdown's list
 * closed) - navigation keys carry on from the most recently focused component that is still there;
 * if there is none, TAB focuses the first component and SHIFT-TAB the last (as browsers do).
 *
 * Focus is moved with FocusManager.UTIL.moveFocus, the same as a mouse click (so hilite,
 * unFocus/focus hand-off etc. behave the same).
 */
final class KeyRouter {

    /** what route() did, or would do, with a key */
    enum Outcome {
        /** someone used the key (or focus moved) */
        HANDLED,
        /** nobody on the way up wanted it: the root should not take it */
        NOT_WANTED,
        /** not routed here (nothing has focus and it's no navigation key): use the old routing */
        NOT_ROUTED
    }

    private KeyRouter() {
    }

    /**
     * Routes key ke for navigation root root. deliver false: only say what would happen, change
     * nothing (for canHandle). A keyboard handler on the way up is only asked whether it wants the
     * key (KeyboardHandler.wantsKey); one that can't tell without acting counts as "would handle".
     */
    static Outcome route(FocusManager root, KeyEvent ke, boolean deliver) {
        Focusable focused = FocusManager.UTIL.findCurrentFocus(root);
        if (isFocusedComponent(focused, root)) {
            return walk(root, focused, true, ke, deliver) ? Outcome.HANDLED : Outcome.NOT_WANTED;
        }
        // nothing has focus: navigation keys carry on from the most recently focused component that is still there ...
        Focusable lastPlace = recentlyFocusedStillInside(root);
        if (lastPlace != null) {
            if (!isNavigationKeyAround(lastPlace, root, ke.getKeyStroke())) {
                return Outcome.NOT_ROUTED;
            }
            return walk(root, lastPlace, false, ke, deliver) ? Outcome.HANDLED : Outcome.NOT_ROUTED;
        }
        // ... or, if there is none (nothing was focused yet), go into root: TAB -> first, SHIFT-TAB -> last
        FocusDirection dir = root.navDirectionFor(ke.getKeyStroke());
        Focusable target = dir != null ? root.enter(dir) : null;
        if (target == null) {
            return Outcome.NOT_ROUTED;
        }
        if (deliver) {
            FocusManager.UTIL.moveFocus(root, target, ke.getEventBus(), false);
        }
        return Outcome.HANDLED;
    }

    /**
     * Offers key ke to from (only if it has focus), then to everything around it up to root - see
     * the class comment. Returns true if someone used it.
     */
    private static boolean walk(FocusManager root, Focusable from, boolean fromHasFocus, KeyEvent ke, boolean deliver) {
        Component<?> fromComponent = from.getFocusComponent();

        if (fromHasFocus && offerToFocused(from, fromComponent, ke, deliver)) {
            return true;
        }

        Component<?> next = parentForKeys(fromComponent);
        for (FocusManager focusManager = FocusManager.UTIL.parentOf(from); focusManager != null;
                          focusManager = nextOut(focusManager, root)) {
            Component<?> focusManagerComponent = focusManager.getFocusComponent();

            // components between the previous focus manager and this one that handle keys themselves
            if (focusManagerComponent != null) {
                for (Component<?> c = next; c != null && c != focusManagerComponent; c = parentForKeys(c)) {
                    if (offerToKeyHandler(c, ke, deliver)) {
                        return true;
                    }
                }
            }

            if (offerToFocusManager(root, focusManager, from, ke, deliver)) {
                return true;
            }
            next = parentForKeys(focusManagerComponent);
        }
        return false;
    }

    /** the focused component: its onKey (if it handles keys itself), then its focusable's handler */
    private static boolean offerToFocused(Focusable focused, Component<?> component, KeyEvent ke, boolean deliver) {
        if (offerToKeyHandler(component, ke, deliver)) {
            return true;
        }
        if (focused.canHandle(ke) == null) {
            return false;
        }
        if (deliver) {
            focused.handle(ke);
        }
        return true;
    }

    /** steps 1 - 4 of the class comment for one focus manager on the way up */
    private static boolean offerToFocusManager(FocusManager root, FocusManager focusManager, Focusable from,
                                               KeyEvent ke, boolean deliver) {
        // 1. its component handles keys
        if (offerToKeyHandler(focusManager.getFocusComponent(), ke, deliver)) {
            return true;
        }

        DefaultFocusManager[] manager = { null };
        DefaultFocusManager.applyIfDefaultFocusManager(focusManager, m -> manager[0] = m);
        if (manager[0] != null) {
            // 2. a hot key of one of its other children
            boolean hotKey = deliver ? manager[0].handleHotKey(ke) : manager[0].hotKeyChildFor(ke) != null;
            if (hotKey) {
                return true;
            }
            // 3. its custom handler
            ConditionalEventHandler customHandler = manager[0].getCustomExtendedEventHandler();
            KeyboardHandler custom = customHandler != null ? AsKeyboardHandler.UTIL.toKeyboardHandler(customHandler) : null;
            if (custom != null && (deliver ? custom.onKey(ke) : custom.wantsKey(ke))) {
                return true;
            }
        }

        // 4. its navigation keys
        FocusDirection dir = focusManager.navDirectionFor(ke.getKeyStroke());
        if (dir == null) {
            return false;
        }
        Navigation next = focusManager.advance(from, dir);
        if (next.getKind() == Navigation.Kind.MOVED) {
            if (deliver) {
                FocusManager.UTIL.moveFocus(root, next.getTarget(), ke.getEventBus(), false);
            }
            return true;
        }
        return next.getKind() == Navigation.Kind.STAY;     // DONE: ask the next one out
    }

    /**
     * component's keyboard handler, if it has one (it is a KeyboardHandler, or hands one out - see
     * AsKeyboardHandler). Without delivering it is only asked whether it wants the key (wantsKey).
     */
    private static boolean offerToKeyHandler(Component<?> component, KeyEvent ke, boolean deliver) {
        KeyboardHandler handler = AsKeyboardHandler.UTIL.asKeyboardHandler(component);
        if (handler == null) {
            return false;
        }
        return deliver ? handler.onKey(ke) : handler.wantsKey(ke);
    }

    /** the parent as far as keys are concerned: a popup's owner (its list belongs to a dropdown), else its container */
    private static Component<?> parentForKeys(Component<?> component) {
        if (component == null) {
            return null;
        }
        if (component.getPopupOwner() != null) {
            return component.getPopupOwner();
        }
        Object up = component.getParentContainer();
        return up instanceof Component ? (Component<?>) up : null;
    }

    /** does any focus manager around from (up to root) use key to move focus? */
    private static boolean isNavigationKeyAround(Focusable from, FocusManager root, KeyStroke key) {
        FocusManager focusManager = from instanceof FocusManager ? (FocusManager) from : FocusManager.UTIL.parentOf(from);
        for (; focusManager != null; focusManager = nextOut(focusManager, root)) {
            if (focusManager.navDirectionFor(key) != null) {
                return true;
            }
        }
        return false;
    }

    /** the focus manager around focusManager - null once root is reached (a dialog: never look outside it) */
    private static FocusManager nextOut(FocusManager focusManager, FocusManager root) {
        return FocusManager.UTIL.isSameNode(focusManager, root) ? null : FocusManager.UTIL.parentOf(focusManager);
    }

    /** a real focused component - not nothing, and not a whole focus manager (that is: focus got lost) */
    private static boolean isFocusedComponent(Focusable focused, FocusManager root) {
        return focused != null && focused != root && !isFocusManagerWithChildren(focused);
    }

    private static boolean isFocusManagerWithChildren(Focusable focusable) {
        return focusable instanceof FocusManager && !((FocusManager) focusable).getChildFocusables().isEmpty();
    }

    /** the most recently focused component that is still inside root (not removed since); null if none */
    private static Focusable recentlyFocusedStillInside(FocusManager root) {
        DefaultFocusManager topRoot = FocusManager.UTIL.rootDefaultFocusManager(root);
        if (topRoot == null) {
            return null;
        }
        for (Focusable recent : topRoot.getRecentlyFocused()) {
            if (recent != root && !isFocusManagerWithChildren(recent) && FocusManager.UTIL.containsFocusable(root, recent)) {
                return recent;
            }
        }
        return null;
    }
}
