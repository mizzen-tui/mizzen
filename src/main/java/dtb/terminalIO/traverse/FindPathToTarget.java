package dtb.terminalIO.traverse;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

public class FindPathToTarget<T> {
    
    
    private HasChildrenAdapter<T> hasChildrenAdapter;

    public FindPathToTarget(HasChildrenAdapter<T> toHasChildren) {
        this.hasChildrenAdapter = toHasChildren;
    }

    public FindPathToTarget() {
        this.hasChildrenAdapter = null;
    }
    

    
    @SuppressWarnings("serial")
    private static class UniqueStack<T> extends Stack<T> {
        
        public T push(T item) {
            if (this.contains(item)) {
                throw new IllegalArgumentException("attempted to add duplicate item to stack . Item: " + String.valueOf(item));
            }
            return super.push(item);
        }

    }
    
    /**
     * Find a path of related objects (parent/child/grandchild/..) relationship
     * between root and target.
     * Returns relation in the form of a list ( root, child, grandchild, .. , target )
     * otherwise returns null when no relation (path to target) found.
     */
    public <P extends T> List<T> findPath(P root, T target, HasParentAdapter<T,P> hasParent) {
        Stack<T> theSTACK = new UniqueStack<T>();
        
        T current = target;
        while (current != null) {
            
            theSTACK.push(current);
            current = hasParent.getParent(current);
            if (current == root) {
                theSTACK.push(current);
                break;
            }
        }
        if (current == null) {
            return null;
        }
        return new ArrayList<T>(theSTACK);
    }
    
    
    /**
     * Find a path of related objects (parent/child/grandchild/..) relationship
     * between root and target.
     * Returns relation in the form of a list ( root, child, grandchild, .. , target )
     * otherwise returns null when no relation (path to target) found.
     */
    public List<T> findPath(T root, T target) {

        Traverser<T> objectTraverser = hasChildrenAdapter!=null? new Traverser<T>(hasChildrenAdapter) : new Traverser<T>();
        
        int depth[] = new int[1];
        depth[0] = 0;
        
        Stack<T> theSTACK = new UniqueStack<T>();
        Set<Object> visited = new HashSet<Object>();
        
        boolean found[] = new boolean[]{false};
        
        objectTraverser.traverse(root, new Traverser.Visitor<T>() {

            @Override
            public int visit(T o) {

                depth[0]++;

                theSTACK.push(o);
                visited.add(o);

                if (o == target) {
                    found[0] = true;
                    return Traverser.STOP_TRAVERSE;
                }

                return Traverser.CONTINUE_TRAVERSE;
            }
            
        }, (o)->{depth[0]--;  if (!found[0]) theSTACK.pop(); });

        if (! found[0]) {
            return null;
        }

        return new ArrayList<T>(theSTACK);
    }
    

    /**
     * Returns true if path exists between root and target.
     * #see {@link #findPath(Object, Object)}
     * @param root
     * @param target
     * @return
     */
    public boolean exists(T root, T target) {
        return findPath(root, target) != null ;
    }
    
    
}
