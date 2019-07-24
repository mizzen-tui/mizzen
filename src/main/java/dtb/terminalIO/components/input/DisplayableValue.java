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
        
        @Override
        public String toString() {
            return getDisplayValue();
        }
        
    }
    
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
    
    public class BooleanDisplayValue extends  SimpleDisplayValue<Boolean> {
        
        String displayValue;
        
        public BooleanDisplayValue(String displayValue,  Boolean value) {
            super(value);
            this.displayValue = displayValue;
        }

        public BooleanDisplayValue(Boolean value) {
            this(null, value);
        }
        
        @Override
        public String getDisplayValue() {
            if (displayValue != null) {
                return displayValue;
            }
            return super.getDisplayValue();
        }

        
    }

}
