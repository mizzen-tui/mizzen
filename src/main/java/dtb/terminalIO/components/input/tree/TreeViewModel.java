package dtb.terminalIO.components.input.tree;

import java.util.Collection;

import dtb.terminalIO.traverse.HasChildren;

public interface TreeViewModel<T> {
    public T getRoot();
    public T getChild(T parent, int index);
    public int getChildCount(T parent);
    public boolean isLeaf(T node);
    public int getIndexOfChild(T parent, T child);
    
    
    
    public static class HasChildrenTreeViewModel<NODE> implements TreeViewModel<NODE> {
        
        NODE root;

        public HasChildrenTreeViewModel(NODE root) {
            super();
            this.root = root;
        }

        @Override
        public NODE getRoot() {
            return root;
        }
        
        private Collection<? extends NODE> getChildren(NODE parent) {
            if ( ! (parent instanceof HasChildren<?>)) {
                return null;
            }
            @SuppressWarnings("unchecked")
            Collection<? extends NODE> children = ((HasChildren<NODE>)parent).getChildren();
            return children;
        }
        
        
        @Override
        public NODE getChild(NODE parent, int index) {
            Collection<? extends NODE> children = getChildren(parent);
            if (children == null || children.size()<index-1) {
                return null;
            }
            int i=0;
            // arg!!!. slow..
            for (NODE childi : children) {
                if (i == index) {
                    return childi;
                }
                i++;
            }
            return null;
        }

        @Override
        public int getChildCount(NODE parent) {
            Collection<? extends NODE> children = getChildren(parent);
            if (children == null) {
                return 0;
            }
            return children.size();
        }

        @Override
        public boolean isLeaf(NODE node) {
            return getChildCount(node)==0;
        }

        @Override
        public int getIndexOfChild(NODE parent, NODE child) {
            Collection<? extends NODE> children = getChildren(parent);
            if (children == null) {
                return -1;
            }
            int i=0;
            for (NODE childi : children) {
                if (childi.equals(child)) {
                    return i;
                }
                i++;
            }
            return -1;
        }
        
        
    }

}