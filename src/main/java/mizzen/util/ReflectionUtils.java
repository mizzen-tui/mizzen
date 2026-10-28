package mizzen.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ReflectionUtils {
    

    public static Method getSetValueMethod(Object object, Class<?> paramType) {
        return getSetPropertyMethod(object, "setValue", paramType);
    }

    /**
     * The public method setMethodName(x) that takes a paramType - an int parameter takes an Integer.
     * paramType null: one that takes null (not a primitive). Inherited ones count.
     */
    public static Method getSetPropertyMethod(Object object, String setMethodName, Class<?> paramType) {
        for (Method method : object.getClass().getMethods()) {
            if (method.getName().equals(setMethodName) && method.getParameterCount() == 1) {
                Class<?> methodParamType = method.getParameterTypes()[0];
                if (paramType == null ? !methodParamType.isPrimitive() : boxed(methodParamType).isAssignableFrom(paramType)) {
                    return method;
                }
            }
        }

        return null; // No appropriate setMethod method found
    }

    /** the public method getMethodName() - inherited ones count */
    public static Method getGetPropertyMethod(Object object, String getMethodName) {
        Class<?> clazz = object.getClass();
        Method[] methods = clazz.getMethods();

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
        if (boxed(getter.getReturnType()) == Boolean.class) {      // isX() is for a boolean or a Boolean
            return getter;
        }
        return null;
    }

    // the public field fieldName; null if there is none
    private static Field publicField(Object target, String fieldName) {
        try {
            return target.getClass().getField(fieldName);
        } catch (NoSuchFieldException e) {
            return null;
        }
    }

    /**
     * The type of a property: its getter's return type, else its public field's; null if it has
     * neither (a Map's entries have none).
     */
    public static Class<?> getPropertyType(Object target, String fieldName) {
        Method getter = getGetPropertyMethodFromFieldName(target, fieldName);
        if (getter != null) {
            return getter.getReturnType();
        }
        Field field = publicField(target, fieldName);
        return field == null ? null : field.getType();
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

        // its getter first - getX(), or isX() for a boolean: the class's own way to read it - else its public field
        Method getter = getGetPropertyMethodFromFieldName(target, fieldName);
        if (getter != null) {
            return getter.invoke(target);
        }
        Field field = publicField(target, fieldName);
        if (field == null) {
            throw new NoSuchFieldException(target.getClass().getSimpleName() + " has no " + gettName(fieldName) + "() and no public field "
                    + fieldName + " (its properties: " + getFieldNames(target) + ")");
        }
        field.setAccessible(true);
        return field.get(target);
    }
    
    @SuppressWarnings("unchecked")
    public static List<String> getFieldNames(Object target) {
        if (target == null) {
            throw new NullPointerException("getFieldNames(target) ; target = null");
        }

        if (target instanceof Map) {
            return ((Map<String, Object>)target).keySet().stream().collect(Collectors.toList());
        }
        // public fields, then properties read by a getter - getX() / isX(), not Object's getClass()
        Set<String> names = new LinkedHashSet<String>();
        for (Field field : target.getClass().getFields()) {
            names.add(field.getName());
        }
        for (Method method : target.getClass().getMethods()) {
            String name = method.getName();
            int prefix = name.startsWith("get") ? 3 : name.startsWith("is") ? 2 : 0;
            if (prefix > 0 && name.length() > prefix && method.getParameterCount() == 0 && method.getDeclaringClass() != Object.class
                    && getGetPropertyMethodFromFieldName(target, decapitalize(name.substring(prefix))) != null) {
                names.add(decapitalize(name.substring(prefix)));
            }
        }
        return new ArrayList<String>(names);
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

        // its setter first - setX(value): the class's own way to change it - else its public field
        Method setter = getSetPropertyMethod(target, "set" + capitalize(fieldName), value == null ? null : value.getClass());
        if (setter != null) {
            setter.invoke(target, value);
            return;
        }
        Field field = publicField(target, fieldName);
        if (field == null) {
            Class<?> type = getPropertyType(target, fieldName);
            throw new NoSuchFieldException(target.getClass().getSimpleName() + "." + fieldName + (type == null
                    ? ": no set" + capitalize(fieldName) + "(..) and no public field (its properties: " + getFieldNames(target) + ")"
                    : ": read only - a getter, no set" + capitalize(fieldName) + "(..) for " + (value == null ? "null" : "a " + value.getClass().getSimpleName())));
        }
        Class<?> fieldType = field.getType();
        if (value == null) {                            // "nothing": an object field takes it; a primitive one can't
            if (fieldType.isPrimitive()) {
                throw new IllegalArgumentException(target.getClass().getSimpleName() + "." + fieldName + " is a " + fieldType + ": it can't be set to null");
            }
            field.setAccessible(true);
            field.set(target, null);
            return;
        }
        if (fieldType.isAssignableFrom(value.getClass()) || boxed(fieldType) == value.getClass()) {   // an int field takes an Integer
            field.setAccessible(true);
            field.set(target, value);
        }
    }

    // the wrapper of a primitive type (int -> Integer); other types as they are
    private static Class<?> boxed(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }
        if (type == int.class) return Integer.class;
        if (type == long.class) return Long.class;
        if (type == boolean.class) return Boolean.class;
        if (type == double.class) return Double.class;
        if (type == float.class) return Float.class;
        if (type == short.class) return Short.class;
        if (type == byte.class) return Byte.class;
        if (type == char.class) return Character.class;
        return type;
    }

    public static String gettName(String fieldName) {
        String getterName = "get" + capitalize(fieldName);
        return getterName;
    }

    static String decapitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toLowerCase() + str.substring(1);
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
