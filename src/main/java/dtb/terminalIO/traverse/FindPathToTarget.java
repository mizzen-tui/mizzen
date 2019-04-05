package dtb.terminalIO.traverse;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class FindPathToTarget<T> {

    /**
     * Find a path of related objects (parrent/child/grandchild/..) relationship
     * between root and target.
     * Returns relation in the form of a list
     * otherwise returns null when no relation
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
    
    
}
