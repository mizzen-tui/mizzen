package dtb.terminalIO.components;

import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.traverse.HasChildren;

public interface ParentContainer extends  HasChildren<Component<?>>, DirtyFlag {

    Position getPositionRelativeToParent(Component<?> child);

    void childResized(Component<?> child);
    
    default boolean canMoveChild(Component<?> child) {
        return false;
    }

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

    default boolean moveChildToPosition(Position position, Component<?> child) {
        // child.moveTo(position.getRow(), position.getCol());
        return false;
    }
    

}