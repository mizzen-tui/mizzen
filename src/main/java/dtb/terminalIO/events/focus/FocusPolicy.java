package dtb.terminalIO.events.focus;

/**
 * How a focus manager answers {@link Focusable#enter(FocusDirection)} and
 * {@link Focusable#advance(Focusable, FocusDirection)}. The focus manager decides; its parent focus manager never
 * looks at the policy (docs/ARCHITECTURE.md §4.3, §4.4).
 */
public enum FocusPolicy {

    /**
     * Children are separate stops in the surrounding navigation order (form section, panel of
     * fields). Entered forward: first child; backward: last child. At either end: done, the parent
     * moves on (the root focus manager wraps around instead).
     */
    GROUP,

    /**
     * One stop (ListBox, DropdownListBox, menu, tab strip). Entering resumes the remembered child
     * (falls back to the first). Its own nav keys move between children; at either end focus stays
     * (or wraps around, if set to cycle - e.g. the tab strip).
     *
     * To make a widget, declare three things - keyboard navigation does the rest, in the same order
     * for every widget (the focused part first, then the widget's own handlers, then moving focus):
     *   1. setFocusPolicy(FocusPolicy.WIDGET)
     *   2. its nav keys, e.g. new DefaultFocusManager(Keys.UP_KEYSTROKE, Keys.DOWN_KEYSTROKE)
     *   3. any handlers of its own for those keys, with appendEventHandler - e.g. "DOWN opens my list"
     * Example: DropdownListBox.
     */
    WIDGET,

    /**
     * Focus never leaves (modal dialog): navigation wraps around inside. A TRAP decides keyboard
     * navigation for everything inside it itself, because a modal dialog gets keys before the
     * application's root does.
     */
    TRAP
}
