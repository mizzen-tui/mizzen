package mizzen.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ReflectionUtils {
    

    public static Method getSetValueMethod(Object object, Class<?> paramType) {
        return getSetPropertyMethod(object, "setValue", paramType);
    }

    public static Method getSetPropertyMethod(Object object, String setMethodName, Class<?> paramType) {
        Class<?> clazz = object.getClass();
        Method[] methods = clazz.getDeclaredMethods();

        for (Method method : methods) {
            if (method.getName().equals(setMethodName) && method.getParameterCount() == 1) {
                Class<?> methodParamType = method.getParameterTypes()[0];
                if ((paramType == null && methodParamType.isPrimitive()) || (paramType != null && methodParamType.isAssignableFrom(paramType))) {
                    return method;
                }
            }
        }

        return null; // No appropriate setMethod method found
    }

    public static Method getGetPropertyMethod(Object object, String getMethodName) {
        Class<?> clazz = object.getClass();
        Method[] methods = clazz.getDeclaredMethods();

        for (Method method : methods) {
            if (method.getName().equals(getMethodName) && method.getParameterCount() == 0) {
                return method;
            }
        }

        return null; // No appropriate getMethod method found
    }

    public static Method getGetPropertyMethodFromFieldName(Object object, String fieldName) {
        Method getter =  getGetPropertyMethod(object, gettName(fieldName));
        if (getter != null) {
            return getter;
        }
        getter =  getGetPropertyMethod(object, "is" + capitalize(fieldName));
        if (getter == null) {
            return null;
        }
        if (getter.getReturnType().isAssignableFrom(Boolean.class)) {
            return getter;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static Object getValueFromTarget(Object target, String fieldName) throws Exception {

        if (target == null) {
            throw new NullPointerException("getValueFromTarget(target,"+fieldName+") ; target = null");
        }

        if (fieldName == null) {
            throw new NullPointerException("getValueFromTarget(target,null) ; fieldName = null");
        }

        if (target instanceof Map) {
            return ((Map<String, Object>)target).get(fieldName);
        }

        Field field = target.getClass().getField(fieldName);
        // Try to find a getter method
        Method getter = getGetPropertyMethodFromFieldName(target, fieldName);

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
    
    @SuppressWarnings("unchecked")
    public static List<String> getFieldNames(Object target) {
        if (target == null) {
            throw new NullPointerException("getFieldNames(target) ; target = null");
        }

        if (target instanceof Map) {
            return ((Map<String, Object>)target).keySet().stream().collect(Collectors.toList());
        }
        return Stream.of( target.getClass().getFields() ).map(f-> f.getName()).collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    public static void setValueOnTarget(Object target, String fieldName, Object value) throws Exception {

        if (target == null) {
            throw new NullPointerException("setValueOnTarget(target,"+fieldName+",value) ; target = null");
        }

        if (fieldName == null) {
            throw new NullPointerException("setValueOnTarget(target,null, value) ; fieldName = null");
        }

        if (target instanceof Map) {
            ((Map<String, Object>)target).put(fieldName, value);
            return;
        }

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

    public static String gettName(String fieldName) {
        String getterName = "get" + capitalize(fieldName);
        return getterName;
    }

    static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

//    public static <T> List<String> getFieldNames(T target) {
//        return Stream.of( target.getClass().getFields() ).map(f-> f.getName()).collect(Collectors.toList());
//    }

}
