package dtb.terminalIO.components;

import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.traverse.HasChildren;

public interface ParentContainer extends  HasChildren<Component<?>> {

    Position getPositionRelativeToParent(Component<?> child);

    void childResized(Component<?> child);

}