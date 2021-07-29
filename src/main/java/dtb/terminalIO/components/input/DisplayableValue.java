package dtb.terminalIO.components.input;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 
 * @author dbalme
 *
 * @param <V>
 */
public interface DisplayableValue<V> extends HasValue<V> {
    
    String getDisplayValue();
    
    public class SimpleDisplayValue<V> implements  DisplayableValue<V> {

        V value;
        String displayValue;
        int maxDisplayValueLen = -1;
        
        public SimpleDisplayValue(String displayValue, V value) {
            this.displayValue = displayValue;
            this.value = value;
        }

        public SimpleDisplayValue(V value) {
            this(String.valueOf(value), value);
        }

        @Override
        public V getValue() {
            return value;
        }

        @Override
        public String getDisplayValue() {
            if (maxDisplayValueLen<4) {
               return displayValue;
            }
            if (maxDisplayValueLen < displayValue.length()) {
                return displayValue.substring(0,maxDisplayValueLen-4)+" .. ";
            }
            return displayValue;
        }

        public void setMaxDisplayValueLen(int maxDisplayValueLen) {
            this.maxDisplayValueLen = maxDisplayValueLen;
        }

        @Override
        public String toString() {
            return getDisplayValue();
        }

    }

    public class StringDisplayValue extends  SimpleDisplayValue<String> {

        public StringDisplayValue(String displayValue, String value) {
            super(displayValue, value);
        }

        public StringDisplayValue(String value) {
            super(value);
        }

        public static StringDisplayValue[] fromStringValues(String[] values) {
            return fromStringValues(values, -1); 
        }

        public static StringDisplayValue[] fromStringValues(String[] values, int maxDisplayValueLen) {
            if (values == null) {
                return null;
            }
            StringDisplayValue[] displayValues = new StringDisplayValue[values.length];

            for (int i=0; i<values.length; i++) {
                displayValues[i] = new StringDisplayValue(values[i]);
                displayValues[i].setMaxDisplayValueLen(maxDisplayValueLen);
            }

            return displayValues;
        }

        public static StringDisplayValue[] fromStringValues(Map<String,String> displayValuesMap) {
            if (displayValuesMap == null) {
                return null;
            }

            int length = displayValuesMap.size();
            StringDisplayValue[] displayValues = new StringDisplayValue[length];

            List<String> sortedKeys = displayValuesMap.keySet().stream().sorted().collect(Collectors.toList());

            int i = 0;
            for (String key : sortedKeys) {
                displayValues[i] = new StringDisplayValue(key, displayValuesMap.get(key));
                i++;
            }

            return displayValues;
        }

    }

    public class BooleanDisplayValue extends  SimpleDisplayValue<Boolean> {

        public BooleanDisplayValue(String displayValue,  Boolean value) {
            super(displayValue, value);
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

    
    public class IntegerDisplayValue extends  SimpleDisplayValue<Integer> {
        
        String format;
        
        public IntegerDisplayValue(String format, Integer value) {
            super(value);
            this.format = format;
        }

        public IntegerDisplayValue(Integer value) {
            this("%d", value);
        }

        @Override
        public String getDisplayValue() {
            if (format != null) {
                return String.format(format, getValue());
            }
            return super.getDisplayValue();
        }

    }

}
