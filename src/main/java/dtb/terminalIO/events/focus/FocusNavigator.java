package dtb.terminalIO.events.focus;

import dtb.terminalIO.events.ConditionalEventHandler;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.keyboard.KeyEvent;
import dtb.terminalIO.events.keyboard.KeyStroke;

/**
 * Keyboard navigation (TAB / SHIFT-TAB, a widget's arrows ...) for a navigation root: the
 * application's root focus manager (no parent), or a modal dialog (TRAP), which gets keys first.
 * Focus redesign, docs/ARCHITECTURE.md §4.3.
 *
 * When a key arrives the root asks decide() first:
 *   1. Is it a nav key at all? Only if a focus manager around the focused component uses it to
 *      move focus (TAB, or a widget's arrows). Anything else goes on to the normal routing untouched.
 *   2. Does the focused component want it anyway? (e.g. a multi-line editor typing a TAB)
 *      Then the key goes straight to that component.
 *   3. Otherwise ask the focus managers around the focused component, innermost first:
 *      - a WIDGET first offers the key to its own handlers (e.g. a dropdown's "DOWN opens my list")
 *      - then: where does focus go next (advance)? MOVED -> move focus there, STAY -> keep it where
 *        it is, DONE -> ask the next one out.
 * So the most specific wish wins: the focused component, then a widget's own handlers, then moving focus.
 *
 * When nothing has focus - not yet, or the focused component went away (e.g. a dropdown's list
 * closed) - navigation carries on from the most recently focused component that is still there;
 * if there is none, TAB focuses the first component and SHIFT-TAB the last (as browsers do).
 *
 * Focus is moved with FocusManager.UTIL.moveFocus, the same as a mouse click (so hilite,
 * unFocus/focus hand-off etc. behave the same) - minus the click's setFocus(true) re-send.
 */
final class FocusNavigator {

    /**
     * The answer decide() gives for a navigation key - one of three things:
     *   giveKeyTo   set: give it the key - the focused component wants it (e.g. an editor typing a
     *                    TAB), or a widget's own handler does (e.g. a dropdown's "DOWN opens my list")
     *   moveFocusTo set: move keyboard focus to this component (e.g. TAB from field A -> field B)
     *   both null      : the key is handled but focus stays where it is - a WIDGET answered STAY
     *                    (e.g. DOWN on the last item of a widget that doesn't wrap around)
     * act() carries it out.
     */
    static final class Decision {
        final EventHandler giveKeyTo;
        final Focusable moveFocusTo;

        private Decision(EventHandler giveKeyTo, Focusable moveFocusTo) {
            this.giveKeyTo = giveKeyTo;
            this.moveFocusTo = moveFocusTo;
        }
    }

    private FocusNavigator() {
    }

    /**
     * Is this key keyboard navigation - and if so, what should happen?
     * Called by a navigation root (the application's root focus manager, or a modal dialog's) for
     * every key, BEFORE its normal routing.
     *
     * Returns null when the key is not navigation (e.g. a letter typed into a field): the key then
     * goes through the normal routing as usual. If nothing has focus, navigation starts from where
     * focus last was, or from the first / last component (see the class comment).
     * Otherwise returns a Decision (see Decision):
     *   - the focused component wants the key itself  -> give it the key
     *   - otherwise the focus managers around the focused component are asked, innermost first:
     *     a WIDGET's own handler that wants the key -> give it the key;
     *     else where focus goes next (advance) -> move focus there, or keep it where it is
     * Example: form A, [B, C], D with focus on C, TAB pressed -> group [B, C] answers "DONE, no more
     * after C", the form answers "D" -> Decision: move focus to D.
     * Example: a closed dropdown's button has focus, DOWN pressed -> the button doesn't want it, the
     * dropdown's own handler does ("open my list") -> Decision: give it the key.
     *
     * Does not change anything itself (safe to call from canHandle); act() carries the decision out.
     */
    static Decision decide(FocusManager root, KeyEvent ke) {
        Focusable focused = FocusManager.UTIL.findCurrentFocus(root);
        if (isFocusedComponent(focused, root)) {
            return decideFrom(root, focused, true, ke);
        }
        // nothing has focus: carry on from the most recently focused component that is still there ...
        Focusable lastPlace = recentlyFocusedStillInside(root);
        if (lastPlace != null) {
            return decideFrom(root, lastPlace, false, ke);
        }
        // ... or, if there is none (nothing was focused yet), go into root: TAB -> first, SHIFT-TAB -> last
        FocusDirection dir = root.navDirectionFor(ke.getKeyStroke());
        Focusable target = dir != null ? root.enter(dir) : null;
        return target == null ? null : new Decision(null, target);
    }

    /**
     * decide() for a key pressed while from has focus (or last had it - fromHasFocus false: e.g. it
     * was removed). Only a component that has focus gets offered the key itself.
     */
    private static Decision decideFrom(FocusManager root, Focusable from, boolean fromHasFocus, KeyEvent ke) {
        KeyStroke key = ke.getKeyStroke();
        if (navDirectionAround(from, root, key) == null) {
            return null;
        }

        if (fromHasFocus && from.canHandle(ke) != null) {
            return new Decision(from, null);
        }

        for (FocusManager focusManager = from.getParentFocusManager(); focusManager != null;
                          focusManager = nextOut(focusManager, root)) {
            FocusDirection dir = focusManager.navDirectionFor(key);
            if (dir == null) {
                continue;   // not one of this focus manager's nav keys
            }
            if (focusManager.getFocusPolicy() == FocusPolicy.WIDGET) {
                // a widget's own wishes come before moving focus, e.g. a dropdown's "DOWN opens my list"
                ConditionalEventHandler own = ownEventHandlerOf(focusManager);
                EventHandler wantsKey = own != null ? own.canHandle(ke) : null;
                if (wantsKey != null) {
                    return new Decision(wantsKey, null);
                }
            }
            Navigation next = focusManager.advance(from, dir);
            if (next.getKind() == Navigation.Kind.MOVED) {
                return new Decision(null, next.getTarget());
            }
            if (next.getKind() == Navigation.Kind.STAY) {
                return new Decision(null, null);
            }
            // DONE: nothing more in that focus manager - ask the next one out
        }
        return null;
    }

    /**
     * Carries out a Decision from decide() for key ke: gives the key to whoever wants it, or moves
     * focus (with FocusManager.UTIL.moveFocus - the same unFocus / focus notifications a click sends, so hilite etc.
     * happen as usual), or does nothing (focus stays).
     */
    static void act(FocusManager root, Decision decision, KeyEvent ke) {
        if (decision.giveKeyTo != null) {
            decision.giveKeyTo.handle(ke);
        } else if (decision.moveFocusTo != null) {
            FocusManager.UTIL.moveFocus(root, decision.moveFocusTo, ke.getEventBus(), false);
        }
    }

    /**
     * Does any focus manager around from (up to root) use key to move focus (e.g. TAB, or UP / DOWN
     * in a dropdown)? Returns the direction of the innermost one that does; null = the key is
     * ordinary input. "Around from" includes from itself if it is a focus manager.
     */
    private static FocusDirection navDirectionAround(Focusable from, FocusManager root, KeyStroke key) {
        FocusManager focusManager = from instanceof FocusManager ? (FocusManager) from : from.getParentFocusManager();
        for (; focusManager != null; focusManager = nextOut(focusManager, root)) {
            FocusDirection dir = focusManager.navDirectionFor(key);
            if (dir != null) {
                return dir;
            }
        }
        return null;
    }

    /** the focus manager around focusManager - null once root is reached (a dialog: never look outside it) */
    private static FocusManager nextOut(FocusManager focusManager, FocusManager root) {
        return FocusManager.UTIL.isSameNode(focusManager, root) ? null : focusManager.getParentFocusManager();
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

    /**
     * The widget's own handlers. A wrapper component's inner manager is asked through the wrapper,
     * which knows both its own handlers and the manager's (see ComponentWithFocusManager).
     */
    private static ConditionalEventHandler ownEventHandlerOf(FocusManager focusManager) {
        FocusManager owner = focusManager instanceof DefaultFocusManager
                           ? ((DefaultFocusManager) focusManager).meOrActual() : focusManager;
        return owner.getOwnEventHandler();
    }
}
