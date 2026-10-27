package mizzen.events.focus;

import mizzen.traverse.HasParentAdapter;

/**
 * This is different from FocusManagerProvider.
 */
public class HasParentFocusableAdapter extends HasParentAdapter<Focusable, FocusManager> {
    
    public HasParentFocusableAdapter() {
        super();
    }

    @Override
    public FocusManager getParent(Focusable focusable) {
        return FocusManager.UTIL.parentOf(focusable);
    }
    

}
