package mizzen.components;

import mizzen.events.focus.FocusBehaviour;

public class FocusSetterToHiliteAction {

    public void hiliteAction(FocusBehaviour focusable, Hiliter hiliter) {
        if (focusable.hasFocus()) {
            hiliter.hilite();
        } else {
            hiliter.unHilite();
        }
    }

}
