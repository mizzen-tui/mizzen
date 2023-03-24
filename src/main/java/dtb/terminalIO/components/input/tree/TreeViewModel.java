package dtb.terminalIO.components.input.tree;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dtb.terminalIO.traverse.HasChildrenAdapter;

public interface TreeViewModel<T> {
    public T getRoot();
    public T getChild(T parent, int index);
    public int getChildCount(T parent);
    public boolean isLeaf(T node);
    public int getIndexOfChild(T parent, T child);
    
    
    public static class CachingTreeviewModel<NODE> implements TreeViewModel<NODE> {

        TreeViewModel<NODE> baseTreeviewModel;

        NODE root;
        Map<NODE,List<NODE>> parentToChild;
        Map<NODE,Integer> parentToChildCount;

        public CachingTreeviewModel(TreeViewModel<NODE> baseTreeviewModel) {
            this.baseTreeviewModel = baseTreeviewModel;
            this.parentToChild = new HashMap<NODE,List<NODE>>();
            this.parentToChildCount = new HashMap<NODE,Integer>();
        }

        @Override
        public NODE getRoot() {
            if (root != null) {
                return root;
            }
            root = baseTreeviewModel.getRoot();
            return root;
        }

        private List<NODE> getChildren(NODE parent) {
            List<NODE> children = parentToChild.get(parent);
            if (children == null) {
                int childCount = getChildCount(parent);
                if (childCount == 0) {
                    return null;
                }
                children = new ArrayList<NODE>(childCount);
                parentToChild.put(parent, children);
                for (int i=0; i < childCount; i++) {
                    NODE child = baseTreeviewModel.getChild(parent, i);
                    children.add(child);
                }
            }
            return children;
        }
        
        @Override
        public NODE getChild(NODE parent, int index) {
            if (index >= getChildCount(parent)) {
                return null;
            }
            return getChildren(parent).get(index);
        }

        @Override
        public int getChildCount(NODE parent) {
            Integer childCount = parentToChildCount.get(parent);
            if (childCount == null) {
                childCount = baseTreeviewModel.getChildCount(parent);
                parentToChildCount.put(parent, childCount);
            }
            return childCount;
        }

        @Override
        public boolean isLeaf(NODE node) {
            return getChildCount(node)==0;
        }

        @Override
        public int getIndexOfChild(NODE parent, NODE child) {
            List<NODE> children = getChildren(parent);
            if (children == null) {
                return -1;
            }
            return children.indexOf(child);
        } 
        
    }
    
    
    public static class HasChildrenTreeViewModel<NODE> implements TreeViewModel<NODE> {
        
        NODE root;
        HasChildrenAdapter<NODE> hasChildrenAdapter;

        public HasChildrenTreeViewModel(NODE root, HasChildrenAdapter<NODE> hasChildrenAdapter) {
            super();
            this.root = root;
            this.hasChildrenAdapter = hasChildrenAdapter;
        }

        public HasChildrenTreeViewModel(NODE root) {
            super();
            this.root = root;
            this.hasChildrenAdapter = new HasChildrenAdapter.Simple<NODE>();
        }


        @Override
        public NODE getRoot() {
            return root;
        }
        
        private Collection<? extends NODE> getChildren(NODE parent) {
            return this.hasChildrenAdapter.asHasChildren(parent).getChildren();
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