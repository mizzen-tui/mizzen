package dtb.terminalIO.components;

/**
 * 
 * Objects that have names as well as useful utility functions
 * to extract name from objects that do not implement this interface.
 * 
 * Useful for labeling objects so that it makes it easier to identify them
 * when doing debugging.
 * 
 * @author dbalme
 *
 * @param <T>  Type of object implementing HasName
 */
public interface HasName<T> {
    
    public String getName();
    
    public T setName(String name);
    
    
    public static class UTIL {
        public static void setDefaultNameFromParentObject(Object objectWithName, Object parentContainer, String nameSuffix) {
            if (objectWithName == null || parentContainer == null) {
                return;
            }
            if (objectWithName instanceof HasName) {
                if (((HasName<?>)objectWithName).getName() == null) {
                    
                    String prefix = getName(parentContainer);
                                       
                   ((HasName<?>)objectWithName).setName(prefix+"-"+nameSuffix);
                }
            }
        }

        public static String getName(Object o) {
            if (o == null) {
                throw new NullPointerException("getName():  Object null!");
            }
            
            if (!(o instanceof HasName)) {
                return o.getClass().getSimpleName();
            }
            
            if (((HasName<?>)o).getName() == null) {
                return o.getClass().getSimpleName();
            }
            return ((HasName<?>)o).getName();
        }

        public static  <T> T decorateWithParentsNamePrefix(T objectWithName, Object parentContainerWithName) {
            if (objectWithName == null || parentContainerWithName == null) {
                return objectWithName;
            }
            
            if (objectWithName instanceof HasName) {
                if (((HasName<?>)objectWithName).getName() == null) {
                    
                    String prefix = getName(parentContainerWithName);
                    String suffix = getName(objectWithName);
                    
                    ((HasName<?>)objectWithName).setName(prefix+"-"+suffix);
                }
            }
            return objectWithName;
        }
    }
    



}
