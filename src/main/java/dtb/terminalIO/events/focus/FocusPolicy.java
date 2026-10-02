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
     * (falls back to the first). Its own nav keys move between children; at either end focus stays.
     */
    WIDGET,

    /**
     * Focus never leaves (modal dialog): navigation wraps around inside.
     */
    TRAP
}
