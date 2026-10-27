package mizzen.components;

import java.util.function.Function;

import mizzen.geom.position.Position;
import mizzen.traverse.HasChildren;

public interface ParentContainer extends  HasChildren<Component<?>>, DirtyFlag, ComponentFinder {

    Position getPositionRelativeToParent(Component<?> child);

    void childResized(Component<?> child);

    default boolean canMoveChild(Component<?> child) {
        return false;
    }

    // used for dragging components
    default Component<?> findChildOrAncestorThatCanBeMoved(Component<?> child) {
        if (child == null) {
            return null;
        }
        if (this.canMoveChild(child)) {
            return child;
        }

        ParentContainer parent = child.getParentContainer();
        if (parent == null) {
            return null;
        }

        if (!(parent instanceof Component)) {
            return null;
        }

        ParentContainer grandParent = ((Component<?>)parent).getParentContainer();

        if (grandParent == null) {
            return null;
        }

        return grandParent.findChildOrAncestorThatCanBeMoved((Component<?>)parent);
    }

    //
    default Component<?> findFirstComponentForCondition(Function<Component<?>, Boolean> condition) {
        if (this instanceof Component<?>) {
            if (condition.apply((Component<?>)this)) {
                return (Component<?>)this;
            }
        }

        for (Component<?> child : this.getChildren()) {
            if (child instanceof ParentContainer) {
                // child is a ParentContainer
                Component<?> found = ((ParentContainer)child).findFirstComponentForCondition(condition);
                if (found != null) {
                    return found;
                }
            } else {
                // child is a simple component (not ParentContainer)
                if (condition.apply(child)) {
                    return child;
                }
            }
        }
        return null;
    }

    default boolean moveChildToPosition(Position position, Component<?> child) {
        // child.moveTo(position.getRow(), position.getCol());
        return false;
    }
    

}