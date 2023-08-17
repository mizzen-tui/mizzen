package dtb.terminalIO.components;

import dtb.terminalIO.components.input.Button;
import dtb.terminalIO.components.input.CheckBoxInput;
import dtb.terminalIO.components.input.HasValue;
import dtb.terminalIO.util.StringUtils;

/**
 * 
 * Objects that have names as well as useful utility functions
 * to extract name from objects that do not implement this interface.
 * 
 * <p/>
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
            return deriveName(o);
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
        
        public static String deriveName(Object node) {
            if (node == null) {
                return null;
            }
            if ((node instanceof HasName)) {
                if (((HasName<?>)node).getName() != null) {
                    return ((HasName<?>)node).getName();
                }
            }
            
            String suffix=null;
            if (node instanceof CheckBoxInput) {
                suffix = null;
            }
            if (suffix == null && node instanceof HasTitleAndBorder<?>) {
                String title = ((HasTitleAndBorder<?>)node).getTitle();
                if (title != null) {
                   suffix = title;
                   suffix = StringUtils.truncate(suffix, 10);
                }
            }
            if (suffix == null && node instanceof Button) {
                suffix = ((Button)node).getButtonText();
            }
            if (suffix == null && node instanceof TextComponent) {
                suffix = ((TextComponent)node).getText();
                suffix = StringUtils.truncate(suffix, 10);
            }
            if (suffix == null && node instanceof HasValue<?>) {
                Object value = ((HasValue<?>)node).getValue();
                if (value != null) {
                   suffix = String.valueOf(value);
                   suffix = StringUtils.truncate(suffix, 10);
                }
            }
            if ( suffix != null) {
                suffix = "["+suffix+"]";
            }
            String output = node.getClass().getSimpleName() + (suffix!=null?suffix:"");
            if (output == null || output.trim().length()==0) {
                output = node.toString();
            }
            return output;
        }
        
        
    }
    



}
