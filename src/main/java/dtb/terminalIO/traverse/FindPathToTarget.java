package dtb.terminalIO.traverse;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class FindPathToTarget<T> {

    /**
     * Find a path of related objects (parent/child/grandchild/..) relationship
     * between root and target.
     * Returns relation in the form of a list ( root, child, grandchild, .. , target )
     * otherwise returns null when no relation (path to target) found.
     */
    public List<T> findPath(T root, T target) {

        Traverser<T> objectTraverser = new Traverser<T>();
        
        int depth[] = new int[1];
        depth[0] = 0;
        
        Stack<T> theSTACK = new Stack<T>();
        
        boolean found[] = new boolean[]{false};
        
        objectTraverser.traverse(root, new Traverser.Visitor<T>() {

            @Override
            public int visit(T o) {
                
                depth[0]++;
                
                theSTACK.push(o);
                
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
