package dtb.terminalIO.components;

import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.geom.position.Position;

public interface ChildComponentManager<T extends ChildComponentManager<?>> extends ParentContainer, Rectangle {

    public T remove(Component<?> component);
    
    public T add(Position offset, Component<?> component);
 
    public T setDropShadow(boolean dropShadow, Component<?> component);
    
    public default T add(Component<?> component) {
        return this.add(Position.ZERO, component);
    }

    public default T add(int offsetRow, int offsetCol, Component<?> component) {
        return this.add(new Position.Simple(offsetRow, offsetCol), component);
    }
    
    
}
