package dtb.terminalIO.traverse;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Consumer;

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
     * Returned by visitor to indicate continue travers
     */
    public static int CONTINUE_TRAVERSE = 1;

    /**
     * Returned by visitor to indicate to stop traversing
     */
    public static int STOP_TRAVERSE = -1;
    
    /**
     * A class that visits an object of type T
     */
    @FunctionalInterface
    public static interface Visitor<T> {

       /**
        * @param o 
        * @return success failure of visit.
        */
       int visit(T o);
    }
    
    
    public static interface NodeVisitor<T> extends Visitor<T> {
        
        void postVisit(T o);
        
    }
    
    static class SimpleNodeVisitor<T> implements NodeVisitor<T> {

        Visitor<T> visitNode;
        Consumer<T> visitAfterChildrenVisited;
        
        public SimpleNodeVisitor(Visitor<T> visitNode, Consumer<T> visitAfterChildrenVisited) {
            this.visitNode = visitNode;
            this.visitAfterChildrenVisited = visitAfterChildrenVisited;
        }

        public SimpleNodeVisitor(Visitor<T> visitNode) {
            this(visitNode, null);
        }
        
        
        @Override
        public int visit(T o) {
            return this.visitNode.visit(o);
        }

        @Override
        public void postVisit(T o) {
            if (visitAfterChildrenVisited != null) {
                visitAfterChildrenVisited.accept(o);
            }
        }
        
    }
    
    @SuppressWarnings("unchecked")
    private Collection<? extends T> getChildren(T o) {
       if (o instanceof HasChildren) {
           return ((HasChildren<T>)o).getChildren();
       }
       return Collections.emptyList();
    }
    
    
    /**
     * Performs a pre-order tree traversal of the object o and all its child objects and executes 
     * the visitor on each object.
     * 
     * @param w  parent widget to traverse 
     * @param visitor  will 'visit' the passed node
     */
    private int _traverseObject(T o, NodeVisitor<T> visitor) {
       int result = visitor.visit(o);
       if (result == CONTINUE_TRAVERSE) {
          Collection<? extends T> children = getChildren(o);
          for (T child : children) {
              result = _traverseObject(child, visitor);
              if (result != CONTINUE_TRAVERSE) {
                  return result;
              }
          }
          visitor.postVisit(o);
       }
       return result;
    }

    public void traverse(T o, NodeVisitor<T> visitor) {
        _traverseObject(o, visitor);
     }
    
    /**
     * Performs a pre-order tree traversal of the object o and all its child objects and executes 
     * the visitor on each object.
     * 
     * @param w  parent widget to traverse 
     * @param visitor  will 'visit' the passed node
     */
    public void traverse(T o, Visitor<T> visitor) {
       traverse(o, new SimpleNodeVisitor<T>(visitor));
    }

    /**
     * Performs a pre-order tree traversal of the object o and all its child objects and executes 
     * the visitor on each object.
     * 
     * @param w  parent widget to traverse 
     * @param visitor  will 'visit' the passed node
     */
    public void traverse(T o, Visitor<T> visitor, Consumer<T> visitAfterChildrenVisited) {
       traverse(o, new SimpleNodeVisitor<T>(visitor, visitAfterChildrenVisited));
    }

    
}
