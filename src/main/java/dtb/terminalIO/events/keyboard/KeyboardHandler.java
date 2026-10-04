package dtb.terminalIO.events.keyboard;

/**
 * Something that handles key presses for a component.
 * <p/>
 * Keys go to the focused component first, then to each component around it (its parents), until
 * one handles it - "bubbling", as in a web browser. Only if nobody did does focus move (TAB, a
 * dropdown's arrows) or a hot key fire.
 * <p/>
 * Example: a dropdown whose button has focus. DOWN goes to the button first (it doesn't use it),
 * then to the dropdown, whose onKey opens the list and returns true - so focus does not move.
 * <p/>
 * A component gets one by implementing this interface, or by handing one out (AsKeyboardHandler) -
 * e.g. its usual event handlers, adapted with AsKeyboardHandler.UTIL.toKeyboardHandler.
 * Works for the focused component and for any component around it; nothing to register.
 */
public interface KeyboardHandler {

    /**
     * A key press for this component (it has focus, or something inside it does).
     * Return true if you used the key; false lets it go on to your parent.
     */
    boolean onKey(KeyEvent ke);

    /**
     * Would onKey use this key? Asked without acting (e.g. "does anyone on screen want this key at
     * all?"). Default: yes - onKey can't be asked without acting. Override only if you can tell.
     */
    default boolean wantsKey(KeyEvent ke) {
        return true;
    }
}
