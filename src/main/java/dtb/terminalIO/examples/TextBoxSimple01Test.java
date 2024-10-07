package dtb.terminalIO.examples;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.components.ComponentFinder;
import dtb.terminalIO.components.composite.Composite;
import dtb.terminalIO.components.composite.HorizontalPanel;
import dtb.terminalIO.components.composite.VerticalPanel;
import dtb.terminalIO.components.input.Button;
import dtb.terminalIO.components.input.HasValue;
import dtb.terminalIO.components.input.SettableHasValue;
import dtb.terminalIO.components.input.TextInput;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.events.focus.RequestFocusEvent;
import dtb.terminalIO.util.Logging;

public class TextBoxSimple01Test {

    public static void main(String[] args) throws IOException {

        Logging.setEnabled(true);

        new TUIApp(args)
           .setDefaultMainApplicationPanel()
           .setOnReadyStart((tApp)->{

               Address address = new Address("98 Noel St", "Ottawa", "On");
               Map<String, String> addressMap = new HashMap<String, String>();
               addressMap.put("street", "123 main st");
               addressMap.put("city", "Ottawa");
               addressMap.put("province", "On");

               Composite testForm = new VerticalPanel().setName("Address");
               TextInput strnum = new TextInput(Colors.IYel, 10).setName("street");
               TextInput city = new TextInput(Colors.IYel, 10).setName("city");
               TextInput province = new TextInput(Colors.IYel, 10).setName("province");

               strnum.onChange((te)->{
                   ValueExtractor.UTIL.buildFrom(address).setValues(tApp, address);
                   Logging.info("ON-strnum changed: "+address.toString());
               });

               Button saveButton = new Button("Save");
               saveButton.onChange((te)-> {
                   ValueExtractor.UTIL.buildFrom(address).extractValues(tApp, address );
                   Logging.info("ON-SAVE-bean: "+address.toString());

                   ValueExtractor.UTIL.buildFrom(addressMap).extractValues(tApp, addressMap );
                   Logging.info("ON-SAVE-map : "+addressMap.toString());
               });

               testForm.add(new HorizontalPanel().addText("Street   : ").setName("streetLabel").add(strnum));
               testForm.add(new HorizontalPanel().addText("City     : ").setName("cityLabel").add(city));
               testForm.add(new HorizontalPanel().addText("Province : ").setName("provinceLabel").add(province));
               testForm.add(saveButton);

               tApp.getMainApplicationPanel().add(2,2, testForm);

               tApp.getEventBus().raiseEvent(new RequestFocusEvent( strnum  ));

               tApp.findComponentByName("Address", Composite.class) // <-- this will return a composite composite which 
                   .findComponentByName("street", TextInput.class).setText("98 Noel St.");

           }).start();
    }


    static class Address {
        public String street;
        public String city;
        public String province;
        public Address(String street, String city, String province) {
            super();
            this.street = street;
            this.city = city;
            this.province = province;
        }
        @Override
        public String toString() {
            return "Address [street=" + street + ", city=" + city + ", province=" + province + "]";
        }
    }


static public class BeanWidgetExtractor<T> implements ValueExtractor<T> {
    
    public void extractValues(ComponentFinder componentFinder, T bean) {
        Class<?> beanClass = bean.getClass();
        Field[] fields = beanClass.getDeclaredFields();

        for (Field field : fields) {
            String fieldName = field.getName();
            Class<?> fieldType = field.getType();

            // Assuming widget names follow a convention (e.g., fieldName + "Input")
            String widgetName = fieldName ; // + "Input";
            HasValue<?> widget = componentFinder.findComponentByName(widgetName, HasValue.class);

            if (widget != null && fieldType.isAssignableFrom(widget.getValue().getClass())) {
                try {
                    field.setAccessible(true);
                    field.set(bean, widget.getValue());
                } catch (IllegalAccessException e) {
                    // Handle any exceptions (e.g., log or ignore)
                }
            }
        }
    }

    public void setBeanPropertiesToWidgets(ComponentFinder componentFinder, T bean) {
        Class<?> beanClass = bean.getClass();
        Field[] fields = beanClass.getDeclaredFields();

        for (Field field : fields) {
            String fieldName = field.getName();
            Class<?> fieldType = field.getType();

            String widgetName = fieldName;
            SettableHasValue<?> widget = componentFinder.findComponentByName(widgetName, SettableHasValue.class);

            if (widget != null && fieldType.isAssignableFrom(widget.getValue().getClass())) {
                try {
                    // Try to find a getter method
                    String getterName = "get" + capitalize(fieldName);
                    Method getter = beanClass.getMethod(getterName);
                    Method setValueMethod = widget.getClass().getMethod("setValue");

                    if (getter != null) {
                        Object propertyValue = getter.invoke(bean);
                        setValueMethod.invoke(widget, propertyValue);
                        //widget.setValue(propertyValue);
                    } else {
                        // If no getter, try to access the field directly
                        field.setAccessible(true);
                        Object propertyValue = field.get(bean);
                        setValueMethod.invoke(widget, propertyValue);
                        //widget.setValue(propertyValue);
                    }
                } catch (Exception e) {
                    // Handle any exceptions (e.g., log or ignore)
                }
            }
        }
    }

    public void setValues(ComponentFinder componentFinder, T bean) {
        setBeanPropertiesToWidgets(componentFinder, bean);
    }


    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

}

static public class MapWidgetExtractor implements ValueExtractor<Map<String, Object>> {
    @Override
    public void extractValues(ComponentFinder componentFinder, Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            // Assuming widget names follow a convention (e.g., key + "Input")
            String widgetName = key;// + "Input";
            HasValue<?> widget = componentFinder.findComponentByName(widgetName, HasValue.class);

            if (widget != null) {
                try {
                    value = widget.getValue();
                    map.put(key, value);
                } catch (Exception e) {
                    // Handle any exceptions (e.g., log or ignore)
                }
            }
        }
    }
    
    public void setMapPropertiesOntoWidgets(ComponentFinder componentFinder, Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            // Assuming widget names follow a convention (e.g., key + "Input")
            String widgetName = key; //+ "Input";
            HasValue<?> widget = componentFinder.findComponentByName(widgetName, SettableHasValue.class);

            if (widget != null) {
                try {
                    Method setValueMethod = widget.getClass().getMethod("setValue");
                    setValueMethod.invoke(widget, value);
                    
                } catch (Exception e) {
                    // Handle any exceptions (e.g., log or ignore)
                }
            }
        }
    }

    public void setValues(ComponentFinder componentFinder, Map<String, Object> map) {
        setMapPropertiesOntoWidgets(componentFinder, map);
    }



}

public interface ValueExtractor<T> {
    void extractValues(ComponentFinder componentFinder, T target);
    void setValues(ComponentFinder componentFinder, T target);

    static class UTIL {
        @SuppressWarnings("unchecked")
        static <T> ValueExtractor<T> buildFrom(T o) {
            if (o == null) {
                throw new NullPointerException("Cannnot build a ValueExtractor from a null object");
            }
            if (o instanceof Map) {
                return (ValueExtractor<T>) new MapWidgetExtractor();
            }

            return new BeanWidgetExtractor<T>();
        }
    }
    
}
// not used yet
static public class ValueExtractorDelagate<T> implements ValueExtractor<T>{

    ValueExtractor<T> delegate;

    public void extractValues(ComponentFinder componentFinder, T target) {
        delegate.extractValues(componentFinder, target);
    }
    public void setValues(ComponentFinder componentFinder, T target) {
        delegate.setValues(componentFinder, target);
    }

}


}
