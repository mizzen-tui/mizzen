package dtb.terminalIO.events;

import dtb.terminalIO.components.HasHiliter;
import dtb.terminalIO.components.Hiliter;

public class FocusableAsHiliter implements HasHiliter {
    
    Focusable focusable;
    
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
                FocusableAsHiliter.this.focusable.setFocus(false);
            }
            
            @Override
            public void hilite() {
                FocusableAsHiliter.this.focusable.setFocus(true);
            }
        };
    }

}
