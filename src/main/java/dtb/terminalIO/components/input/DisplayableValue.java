package dtb.terminalIO.components.input;

public interface DisplayableValue<V> extends HasValue<V> {
    
    String getDisplayValue();
    
    public class SimpleDisplayValue<V> implements  DisplayableValue<V> {

        V value;
        
        public SimpleDisplayValue(V value) {
            this.value = value;
        }
        
        @Override
        public V getValue() {
            return value;
        }

        @Override
        public String getDisplayValue() {
            return String.valueOf(getValue());
        }
        
    }
    
    static String[][] test = new String[][] {{"1","2"}};
    
    public class StringDisplayValue extends  SimpleDisplayValue<String> {
        
        public StringDisplayValue(String value) {
            super(value);
        }
        
        public static StringDisplayValue[] fromStringValues(String[] values) {
            if (values == null) {
                return null;
            }
            StringDisplayValue[] displayValues = new StringDisplayValue[values.length];
            
            for (int i=0; i<values.length; i++) {
                displayValues[i] = new StringDisplayValue(values[i]);
            }
            
            return displayValues;
        }
        
    }
    

}
