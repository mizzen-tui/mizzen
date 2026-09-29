package dtb.terminalIO.events.focus;

import dtb.terminalIO.components.HasHiliter;
import dtb.terminalIO.components.Hiliter;
import dtb.terminalIO.events.EventBus;

public class FocusableAsHiliter implements HasHiliter {

    Focusable focusable;
    EventBus eventBus;

    public Hiliter getHiliter() {
        if (focusable instanceof Hiliter) {
            return (Hiliter) focusable;
        }

        if (focusable instanceof HasHiliter) {
            return ((HasHiliter) focusable).getHiliter();
        }

        return new Hiliter() {

            @Override
            public void unHilite() {
                FocusableAsHiliter.this.focusable.setFocus(false, eventBus);
            }

            @Override
            public void hilite() {
                FocusableAsHiliter.this.focusable.setFocus(true, eventBus);
            }
        };
    }

}
