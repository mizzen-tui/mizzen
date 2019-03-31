package dtb.terminalIO.traverse;

import java.util.Collection;
import java.util.Collections;

/**
 * Provides the ability to traverse (visit) an object and all its children.
 * 
 * <p>
 * 
 * The visitor can then apply some operation on the visited object and return a status
 * indicating if its children should be visited (VISIT_MY_CHILDREN) or not visited (DO_NOT_VISIT_MY_CHILDREN).
 *
 */
public class Traverser<T> {
    
    /**
     * A class that visits an object of type T
     */
    @FunctionalInterface
    public static interface Visitor<T> {
       /**
        * Returned by visitor to indicate continue and visit my children
        */
       public int VISIT_MY_CHILDREN = 1;

       /**
        * Returned by visitor to indicate do **not** visit my children
        */
       public int DO_NOT_VISIT_MY_CHILDREN = -1;

       /**
        * @param o 
        * @return success failure of visit.
        */
       int visit(T o);
    }
    
    
    @SuppressWarnings("unchecked")
    private Collection<? extends T> getChildren(T o) {
       if (o instanceof HasChildren) {
           return ((HasChildren<T>)o).getChildren();
       }
       return Collections.emptyList();
    }
    
    
    /**
     * Performs a preorder tree traversal of the object o and all its child objects and executes 
     * the visitor on each object.
     * 
     * @param w  parent widget to traverse 
     * @param visitor  will 'visit' the passed node
     */
    public void traverse(T o, Visitor<T> visitor) {
       int result = visitor.visit(o);
       if (result == Visitor.VISIT_MY_CHILDREN) {
          for (T child : getChildren(o)) {
             traverse(child, visitor);
          }
       }
    }
    

    
}
