package dtb.terminalIO.events.focus;

import dtb.terminalIO.events.keyboard.KeyEvent;
import dtb.terminalIO.events.keyboard.KeyStroke;

/**
 * Keyboard navigation (TAB / SHIFT-TAB etc.) for the ROOT focus manager - the one with no parent.
 * Focus redesign, docs/ARCHITECTURE.md §4.3.
 *
 * When a key arrives the root asks decide() first:
 *   1. Is it a nav key at all? Only if a GROUP or TRAP focus manager around the focused component
 *      uses it to move focus (e.g. TAB). Anything else goes on to today's routing untouched.
 *   2. Does the focused component want it anyway? (e.g. a multi-line editor typing a TAB)
 *      Then the key goes straight to that component.
 *   3. Otherwise ask the focus managers around the focused component, innermost first, where focus goes
 *      next (advance): MOVED -> move focus there, STAY -> do nothing, DONE -> ask the next one out.
 *
 * WIDGET focus managers (dropdown, tab strip) keep handling their OWN keys (arrows) the old way for
 * now - only entering / leaving them goes through here.
 *
 * Focus is moved with FocusManager.UTIL.moveFocus, the same as a mouse click (so hilite,
 * unFocus/focus hand-off etc. behave the same) - minus the click's setFocus(true) re-send.
 */
final class FocusNavigator {

    /**
     * The answer decide() gives for a navigation key - one of three things:
     *   giveKeyTo   set: the focused component wants the key itself (e.g. an editor typing a TAB) - give it the key
     *   moveFocusTo set: move keyboard focus to this component (e.g. TAB from field A -> field B)
     *   both null      : the key is handled but focus stays where it is - a focus manager answered STAY
     *                    (e.g. DOWN on the last item of a list; today only WIDGETs answer STAY, and their
     *                    keys still go through the old routing, so this case is for later)
     * act() carries it out.
     */
    static final class Decision {
        final Focusable giveKeyTo;
        final Focusable moveFocusTo;

        private Decision(Focusable giveKeyTo, Focusable moveFocusTo) {
            this.giveKeyTo = giveKeyTo;
            this.moveFocusTo = moveFocusTo;
        }
    }

    private FocusNavigator() {
    }

    /**
     * Is this key keyboard navigation - and if so, what should happen?
     * Called by the root focus manager for every key, BEFORE its normal routing.
     *
     * Returns null when the key is not navigation (e.g. a letter typed into a field), or nothing has
     * focus yet: the key then goes through the normal routing as usual.
     * Otherwise returns a Decision (see Decision):
     *   - the focused component wants the key itself  -> give it the key
     *   - otherwise the focus managers around the focused component are asked, innermost first, where focus
     *     goes next (advance) -> move focus there, or keep it where it is
     * Example: form A, [B, C], D with focus on C, TAB pressed -> group [B, C] answers "DONE, no more
     * after C", the form answers "D" -> Decision: move focus to D.
     *
     * Does not change anything itself (safe to call from canHandle); act() carries the decision out.
     */
    static Decision decide(FocusManager root, KeyEvent ke) {
        Focusable focused = FocusManager.UTIL.findCurrentFocus(root);
        if (focused == null || focused == root) {
            return null;    // nothing focused yet: leave it to today's routing
        }
        KeyStroke key = ke.getKeyStroke();

        if (!isNavKeyAroundFocusedComponent(focused, key)) {
            return null;
        }

        if (focused.canHandle(ke) != null) {
            return new Decision(focused, null);
        }

        for (FocusManager focusManager = focused.getParentFocusManager(); focusManager != null;
                          focusManager = focusManager.getParentFocusManager()) {
            if (focusManager.getFocusPolicy() == FocusPolicy.WIDGET) {
                continue;   // widgets keep their own keys for now; the focus manager around them decides
            }
            FocusDirection dir = focusManager.navDirectionFor(key);
            if (dir == null) {
                continue;
            }
            Navigation next = focusManager.advance(focused, dir);
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
     * Carries out a Decision from decide() for key ke: gives the key to the focused component, or moves
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
     * Does any GROUP / TRAP focus manager around the focused component use key to move focus (e.g. TAB)?
     * If not, the key is ordinary input. (WIDGETs' own keys - arrows in a dropdown - are left to the
     * old routing for now.)
     */
    private static boolean isNavKeyAroundFocusedComponent(Focusable focused, KeyStroke key) {
        for (FocusManager focusManager = focused.getParentFocusManager(); focusManager != null;
                          focusManager = focusManager.getParentFocusManager()) {
            if (focusManager.getFocusPolicy() != FocusPolicy.WIDGET && focusManager.navDirectionFor(key) != null) {
                return true;
            }
        }
        return false;
    }
}
