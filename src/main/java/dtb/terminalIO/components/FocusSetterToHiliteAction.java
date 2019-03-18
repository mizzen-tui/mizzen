package dtb.terminalIO.components;

import dtb.terminalIO.events.Focusable;

public class FocusSetterToHiliteAction {

    public void hiliteAction(Focusable focusable, Hiliter hiliter) {
        if (focusable.hasFocus()) {
            hiliter.hilite();
        } else {
            hiliter.unHilite();
        }
    }

    

}
