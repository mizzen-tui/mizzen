package mizzen.traverse;

import java.util.Collection;
import java.util.Collections;

/**
 * An object that has children of type T
 *
 * @param <T>
 */
public interface HasChildren<T> {

    Collection<? extends T> getChildren();

    public static class Empty<T> implements HasChildren<T> {

        @Override
        public Collection<? extends T> getChildren() {
            return Collections.emptyList();
        }
    }

}
