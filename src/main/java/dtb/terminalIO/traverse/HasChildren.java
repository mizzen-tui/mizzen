package dtb.terminalIO.traverse;

import java.util.Collection;

public interface HasChildren<T> {
    
    Collection<? extends T> getChildren();

}
