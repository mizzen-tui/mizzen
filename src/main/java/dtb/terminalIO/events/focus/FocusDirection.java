package dtb.terminalIO.events.focus;

/**
 * Direction of keyboard focus navigation (e.g. TAB = FORWARD, SHIFT-TAB = BACKWARD).
 * See {@link Focusable#enter(FocusDirection)} and {@link Focusable#advance(Focusable, FocusDirection)}.
 */
public enum FocusDirection {
    FORWARD,
    BACKWARD
}
