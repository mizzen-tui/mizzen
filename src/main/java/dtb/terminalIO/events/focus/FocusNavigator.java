package dtb.terminalIO.events.focus;

import dtb.terminalIO.events.keyboard.KeyEvent;
import dtb.terminalIO.events.keyboard.KeyStroke;

/**
 * Keyboard navigation (TAB / SHIFT-TAB etc.) for the ROOT focus manager - the one with no parent.
 * Focus redesign, docs/ARCHITECTURE.md §4.3.
 *
 * When a key arrives the root asks decide() first:
 *   1. Is it a nav key at all? Only if a GROUP or TRAP container around the focused field
 *      uses it to move focus (e.g. TAB). Anything else goes on to today's routing untouched.
 *   2. Does the focused field want it anyway? (e.g. a multi-line editor typing a TAB)
 *      Then the key goes straight to that field.
 *   3. Otherwise ask the containers around the field, innermost first, where focus goes
 *      next (advance): MOVED -> move focus there, STAY -> do nothing, DONE -> ask the next one out.
 *
 * WIDGET containers (dropdown, tab strip) keep handling their OWN keys (arrows) the old way for
 * now - only entering / leaving them goes through here.
 *
 * Focus is moved with FocusManager.UTIL.moveFocus, the same as a mouse click (so hilite,
 * unFocus/focus hand-off etc. behave the same) - minus the click's setFocus(true) re-send.
 */
final class FocusNavigator {

    /** what decide() wants done with the key; null from decide() means "not navigation" */
    static final class Decision {
        final Focusable giveKeyTo;      // the focused field wants the key
        final Focusable moveFocusTo;    // move focus here
        // both null: STAY (key handled, focus stays)

        private Decision(Focusable giveKeyTo, Focusable moveFocusTo) {
            this.giveKeyTo = giveKeyTo;
            this.moveFocusTo = moveFocusTo;
        }
    }

    private FocusNavigator() {
    }

    /** Only answers - changes nothing. */
    static Decision decide(FocusManager root, KeyEvent ke) {
        Focusable focused = FocusManager.UTIL.findCurrentFocus(root);
        if (focused == null || focused == root) {
            return null;    // nothing focused yet: leave it to today's routing
        }
        KeyStroke key = ke.getKeyStroke();

        if (!isNavKeyAroundFocusedField(focused, key)) {
            return null;
        }

        if (focused.canHandle(ke) != null) {
            return new Decision(focused, null);
        }

        for (FocusManager container = focused.getParentFocusManager(); container != null;
                          container = container.getParentFocusManager()) {
            if (container.getFocusPolicy() == FocusPolicy.WIDGET) {
                continue;   // widgets keep their own keys for now; the container around them decides
            }
            FocusDirection dir = container.navDirectionFor(key);
            if (dir == null) {
                continue;
            }
            Navigation next = container.advance(focused, dir);
            if (next.getKind() == Navigation.Kind.MOVED) {
                return new Decision(null, next.getTarget());
            }
            if (next.getKind() == Navigation.Kind.STAY) {
                return new Decision(null, null);
            }
            // DONE: nothing more in that container - ask the next one out
        }
        return null;
    }

    /** carry out a decision made by decide() for this key */
    static void act(FocusManager root, Decision decision, KeyEvent ke) {
        if (decision.giveKeyTo != null) {
            decision.giveKeyTo.handle(ke);
        } else if (decision.moveFocusTo != null) {
            FocusManager.UTIL.moveFocus(root, decision.moveFocusTo, ke.getEventBus(), false);
        }
    }

    private static boolean isNavKeyAroundFocusedField(Focusable focused, KeyStroke key) {
        for (FocusManager container = focused.getParentFocusManager(); container != null;
                          container = container.getParentFocusManager()) {
            if (container.getFocusPolicy() != FocusPolicy.WIDGET && container.navDirectionFor(key) != null) {
                return true;
            }
        }
        return false;
    }
}
