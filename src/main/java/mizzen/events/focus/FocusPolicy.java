package mizzen.events.focus;

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
     * <p/>
     * The easy way to get one: ParentFocusBehaviour.oneStop(prev, next, wrap) (e.g. RadioGroup). By
     * hand: setFocusPolicy(FocusPolicy.ONE_STOP), its nav keys (e.g. new DefaultFocusManager(Keys.UP_KEYSTROKE,
     * Keys.DOWN_KEYSTROKE)), and any keys of its own (KeyboardHandler.onKey) - e.g. "DOWN opens my list".
     * Keys go the same way for every one: the focused part first, then the component's own keys,
     * then moving focus.
     */
    ONE_STOP,

    /**
     * Focus never leaves (modal dialog): navigation wraps around inside. A TRAP decides keyboard
     * navigation for everything inside it itself, because a modal dialog gets keys before the
     * application's root does.
     */
    TRAP
}
