package dtb.terminalIO.traverse;

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
