package dtb.terminalIO.traverse;

/**
 * Adapts an object to look like an object that implements HasChildren.
 * 
 * Useful when object does not directly implement HasChildren but you want to 'adapt' (wrap) it
 * so that the resulting wrapper does.
 * 
 * @param <T>
 */
public interface HasChildrenAdapter<T> {

    HasChildren<? extends T> asHasChildren(T parent);

    public static class Simple<T> implements HasChildrenAdapter<T> {

        @SuppressWarnings("unchecked")
        @Override
        public HasChildren<? extends T> asHasChildren(T parent) {
            if (parent instanceof HasChildren) {
                return (HasChildren<? extends T>)parent;
            }
            return new HasChildren.Empty<T>();
        }
    }
}
