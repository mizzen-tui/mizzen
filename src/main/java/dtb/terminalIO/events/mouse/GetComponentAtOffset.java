package dtb.terminalIO.events.mouse;

import dtb.terminalIO.components.Component;
import dtb.terminalIO.geom.Position;

public interface GetComponentAtOffset {
    public Component<?> getComponentAtOffset(Position offset);
    
    public static class UTIL {
        public static Component<?> getComponentAtOffset(Component<?> parentComponent, Position offset) {
            
            if ( ! parentComponent.containsPosition(offset)) {
                return null;
            }
            
            // Danger Will Robinson .. danger -- infinite recursion can happen here!!!
            //  fix before using!!
            if (parentComponent instanceof GetComponentAtOffset) {
                Component<?> component = ((GetComponentAtOffset)parentComponent).getComponentAtOffset(offset); 
                return component;
            }
            
            return parentComponent;
        }
    }
    
}
