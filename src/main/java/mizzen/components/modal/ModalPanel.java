package mizzen.components.modal;

import mizzen.components.composite.Composite;
import mizzen.components.input.ParentFocusBehaviour;
import mizzen.controlcodes.color.Color;
import mizzen.events.ConditionalEventHandler;
import mizzen.events.focus.Focusable;
import mizzen.events.focus.ToFocusable;
import mizzen.events.keyboard.AsKeyboardHandler;
import mizzen.events.keyboard.KeyboardHandler;

/** a dialog's panel: a Composite whose focus never leaves it - TAB / SHIFT-TAB go round its components */
public class ModalPanel extends Composite implements ToFocusable, AsKeyboardHandler {

    private final ParentFocusBehaviour focus = ParentFocusBehaviour.of(this).keepsFocusInside();

    public ModalPanel(Color color) {
        super(color);
    }

    @Override
    public Focusable asFocusable() {
        return focus.asFocusable();
    }

    @Override
    public KeyboardHandler asKeyboardHandler() {
        return focus.asKeyboardHandler();
    }

    /** the dialog's focus as an event handler: a modal handler routes every event to it while the dialog is open */
    public ConditionalEventHandler asEventHandler() {
        return (ConditionalEventHandler) focus.asFocusable();
    }
}
