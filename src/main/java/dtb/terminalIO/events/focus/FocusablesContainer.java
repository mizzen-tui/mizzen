package dtb.terminalIO.events.focus;

import dtb.terminalIO.traverse.HasChildren;

public interface FocusablesContainer extends HasChildren<Focusable> {
    
    public void insertFocusable(int index, Focusable focusable);
    
    public void addFocusable(Focusable focusable);
    
    public void removeFocusable(Focusable focusable);
    
    


}
