package mizzen.components;

import java.util.function.Function;

public interface ComponentFinder {

    // 
    Component<?> findFirstComponentForCondition(Function<Component<?>, Boolean> condition);

    default Component<?> findComponentByName(String name) {
        return findFirstComponentForCondition((c)-> name.equals( HasName.UTIL.getName(c))  );
    }


    @SuppressWarnings("unchecked")
    default <T> T findComponentByName(String name, Class<T> clazz) {
        return (T) findFirstComponentForCondition((c)-> name.equals( HasName.UTIL.getName(c)) && clazz.isAssignableFrom(c.getClass()) );
    }

    default <T> T findComponentByNameOrFailWithMessage(String name, Class<T> clazz, String error) {
        T found = findComponentByName(name, clazz);
        if (found ==  null) {
            throw new IllegalArgumentException(error);
        }
        return found;
    }

    default <T> T findComponentByNameOrFail(String name, Class<T> clazz) {
        return findComponentByNameOrFailWithMessage(name, clazz, "Component of type " +clazz.getSimpleName() + " not found with name of "+name);
    }

}
