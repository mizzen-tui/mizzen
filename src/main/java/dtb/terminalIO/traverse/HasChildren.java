package dtb.terminalIO.traverse;

import java.util.Collection;
import java.util.Collections;

public interface HasChildren<T> {
    
    Collection<? extends T> getChildren();
    
    public static class Empty<T> implements HasChildren<T> {

        @Override
        public Collection<? extends T> getChildren() {
            return Collections.emptyList();
        }
        
    }
    

}
