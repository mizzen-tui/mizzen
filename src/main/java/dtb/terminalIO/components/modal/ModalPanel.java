package dtb.terminalIO.components.modal;

import dtb.terminalIO.components.composite.Composite;
import dtb.terminalIO.components.input.ParentFocusBehaviour;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.events.ConditionalEventHandler;
import dtb.terminalIO.events.focus.Focusable;
import dtb.terminalIO.events.focus.ToFocusable;
import dtb.terminalIO.events.keyboard.AsKeyboardHandler;
import dtb.terminalIO.events.keyboard.KeyboardHandler;

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
