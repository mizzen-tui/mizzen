package mizzen.components.input.form;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import mizzen.components.ComponentFinder;
import mizzen.components.input.HasValue;
import mizzen.components.input.SettableHasValue;
import mizzen.events.EventHandler;
import mizzen.util.Logging;
import mizzen.util.ReflectionUtils;

public class NamedComponentsFormBinder<T> {

    protected ComponentFinder componentFinder;
    protected T targetToBindTo;

    public NamedComponentsFormBinder(ComponentFinder componentFinder, T targetToBindTo) {
        super();
        this.componentFinder = componentFinder;
        this.targetToBindTo = targetToBindTo;
        if (componentFinder == null) {
            throw new NullPointerException("componentFinder null!");
        }
        if (targetToBindTo == null) {
            throw new NullPointerException("targetToBindTo null!");
        }
    }

    public NamedComponentsFormBinder<T> bind() {
        FormValueExtractor<T> formValueExtractor = FormValueExtractor.UTIL.buildFrom(this.targetToBindTo);
        formValueExtractor.setValues(this.componentFinder, this.targetToBindTo);
        formValueExtractor.bindToOnChange(this.componentFinder, this.targetToBindTo);
        return this;
    }

    public static <T> NamedComponentsFormBinder<T> bindNamed(ComponentFinder componentFinder, T targetToBindTo) {
        return new NamedComponentsFormBinder<T>(componentFinder, targetToBindTo).bind();
    }

    static abstract public class BaseFormValueExtractor<T> implements FormValueExtractor<T> {

        @Override
        public void extractValues(ComponentFinder componentFinder, T target) {
            for (String fieldName : getFieldNames(target)) {
                HasValue<?> component = componentFinder.findComponentByName(fieldName, HasValue.class);
                Object value = component.getValue();
                try {
                    setValueOnTarget(target, fieldName, value);
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

        abstract protected List<String> getFieldNames(T target);

        @Override
        public void setValues(ComponentFinder componentFinder, T target) {
            for (String fieldName : getFieldNames(target)) {
                SettableHasValue<?> component = componentFinder.findComponentByName(fieldName, SettableHasValue.class);
                if (component == null) {
                    continue;
                }
                Object value;
                try {
                    value = getValueFromTarget(target, fieldName);
                    Method setValueMethod;
                    if (value == null) {
                        setValueMethod = ReflectionUtils.getSetValueMethod(component, null);
                        if (setValueMethod != null) {
                            setValueMethod.invoke(component, value);
                        }
                        return; //
                        //setValueMethod.invoke(component, value);
                    }
                    setValueMethod = ReflectionUtils.getSetValueMethod(component, value.getClass());
                    //fieldType.isAssignableFrom(component.getValue().getClass());
                    setValueMethod.invoke(component, value);
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                   // e.printStackTrace();
                    Logging.info("ERROR - setValues():  details " + e.toString());
                }
            }
        }

        abstract protected Object getValueFromTarget(T target, String fieldName) throws Exception;

        abstract protected void setValueOnTarget(T target, String fieldName, Object value) throws Exception;

        protected void bindToOnNamedWidgetChanged(ComponentFinder componentFinder, T target, String fieldName)  {

            HasValue<?> component = componentFinder.findComponentByName(fieldName, HasValue.class);
            if (component == null) {
                return;
            }
            // Check if the widget has an onChange method
            try {
                Method onChangeMethod = component.getClass().getMethod("onChange", EventHandler.class);
                if (onChangeMethod == null) {
                    return;
                }

                EventHandler widget_onChangeHandler = (te) -> {
                    try {
                        Object propertyValue = component.getValue();
                        setValueOnTarget(target, fieldName, propertyValue);
                    } catch (Exception e) {
                        // Handle any exceptions (e.g., log or ignore)
                    }
                };
                // Set up the onChange event handler
                onChangeMethod.invoke(component, widget_onChangeHandler);

            } catch (Exception  e) {
                e.printStackTrace();
            }
        }

        @Override
        public void bindToOnChange(ComponentFinder componentFinder, T target) {
            for (String fieldName : getFieldNames(target)) {
                bindToOnNamedWidgetChanged(componentFinder, target, fieldName);
            }
        }
    }

    static class BeanFormValueExtractor<T> extends BaseFormValueExtractor<T> {

        protected Object getValueFromTarget(T target, String fieldName) throws Exception {
            Class<?> beanClass = target.getClass();
            Field field = target.getClass().getField(fieldName);
            // Try to find a getter method
            String getterName = "get" + capitalize(fieldName);
            Method getter;
            
            try {
                getter = beanClass.getMethod(getterName);
            } catch (Exception e) {
                getter = null;
            }

            if (getter != null) {
                Object propertyValue = getter.invoke(target);
                return propertyValue;
                //widget.setValue(propertyValue);
            } else {
                // If no getter, try to access the field directly
                field.setAccessible(true);
                Object propertyValue = field.get(target);
                return propertyValue;
            }
        }

        protected void setValueOnTarget(T target, String fieldName, Object value) throws Exception {

            Field field = target.getClass().getField(fieldName);
            Class<?> fieldType = field.getType();
            if (fieldType.isAssignableFrom(value.getClass())) {
                String setterName = "set" + capitalize(fieldName);
                Method setter = ReflectionUtils.getSetPropertyMethod(target, setterName, value == null? null: value.getClass());
                if (setter != null) {
                    setter.invoke(target, value);
                } else {
                    field.setAccessible(true);
                    field.set(target, value);
                }
            }
        }
 
        private String capitalize(String str) {
            if (str == null || str.isEmpty()) {
                return str;
            }
            return str.substring(0, 1).toUpperCase() + str.substring(1);
        }

        @Override
        protected List<String> getFieldNames(T target) {
            return Stream.of( target.getClass().getFields() ).map(f-> f.getName()).collect(Collectors.toList());
        }
    }

    static class MapFormValueExtractor extends BaseFormValueExtractor<Map<String, Object>> {

        @Override
        protected List<String> getFieldNames(Map<String, Object> target) {
            return target.keySet().stream().collect(Collectors.toList());
        }

        @Override
        protected Object getValueFromTarget(Map<String, Object> target, String fieldName) throws Exception {
            return target.get(fieldName);
        }

        @Override
        protected void setValueOnTarget(Map<String, Object> target, String fieldName, Object value) throws Exception {
            target.put(fieldName, value);
        }
    }


    static public class BeanWidgetExtractor<T> implements FormValueExtractor<T> {

        public void extractValues(ComponentFinder componentFinder, T bean) {
            Class<?> beanClass = bean.getClass();
            Field[] fields = beanClass.getDeclaredFields();

            for (Field field : fields) {
                String fieldName = field.getName();
                Class<?> fieldType = field.getType();

                String widgetName = fieldName ;  
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

        public  <T> void bindValues(ComponentFinder componentFinder, T bean) {
            FormValueExtractor.UTIL.buildFrom(bean);
        }

        public  <T> void bindToOnChange(ComponentFinder componentFinder, T bean, String fieldName) {
            HasValue<?> widget = componentFinder.findComponentByName(fieldName, HasValue.class);

            if (widget != null) {
                try {
                    // Find the setter method for the field
                    String setterName = "set" + capitalize(fieldName);
                    Method setter = bean.getClass().getMethod(setterName, widget.getValue().getClass());

                    // Check if the widget has an onChange method
                    Method onChangeMethod = widget.getClass().getMethod("onChange", EventHandler.class);

                    EventHandler widget_onChangeHandler = (te) -> {
                        try {
                            Object propertyValue = widget.getValue();
                            setter.invoke(bean, propertyValue);
                        } catch (Exception e) {
                            // Handle any exceptions (e.g., log or ignore)
                        }
                    };

                    // Set up the onChange event handler
                    onChangeMethod.invoke(widget, widget_onChangeHandler);
                } catch (NoSuchMethodException e) {
                    // Widget doesn't have an onChange method; do nothing
                } catch (Exception e) {
                    // Handle any other exceptions (e.g., log or ignore)
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

        @Override
        public void bindToOnChange(ComponentFinder componentFinder, T target) {
            // TODO Auto-generated method stub
            
        }

    }
    static public class MapWidgetExtractor implements FormValueExtractor<Map<String, Object>> {
        @Override
        public void extractValues(ComponentFinder componentFinder, Map<String, Object> mapOfPropertyValues) {
            for (Map.Entry<String, Object> entry : mapOfPropertyValues.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();

                String widgetName = key;
                HasValue<?> widget = componentFinder.findComponentByName(widgetName, HasValue.class);

                if (widget != null) {
                    try {
                        value = widget.getValue();
                        mapOfPropertyValues.put(key, value);
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

                String widgetName = key;
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

        @Override
        public void bindToOnChange(ComponentFinder componentFinder, Map<String, Object> target) {
            // TODO Auto-generated method stub
            
        }
    }

    public interface FormValueExtractor<T> {
        void extractValues(ComponentFinder componentFinder, T target);
        void setValues(ComponentFinder componentFinder, T target);
        void bindToOnChange(ComponentFinder componentFinder, T target);

        static class UTIL {
            @SuppressWarnings("unchecked")
            static <T> FormValueExtractor<T> buildFrom(T o) {
                if (o == null) {
                    throw new NullPointerException("Cannnot build a ValueExtractor from a null object");
                }
                if (o instanceof Map) {
                    return (FormValueExtractor<T>) new MapFormValueExtractor();
                }

                return new BeanFormValueExtractor<T>();
            }
        }
    }

    public void validate() {
        // TODO Auto-generated method stub
    }


}
