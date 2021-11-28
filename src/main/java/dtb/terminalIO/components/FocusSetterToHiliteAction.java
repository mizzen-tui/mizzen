package dtb.terminalIO.components;

import dtb.terminalIO.events.focus.FocusBehaviour;

public class FocusSetterToHiliteAction {

    public void hiliteAction(FocusBehaviour focusable, Hiliter hiliter) {
        if (focusable.hasFocus()) {
            hiliter.hilite();
        } else {
            hiliter.unHilite();
        }
    }

}
