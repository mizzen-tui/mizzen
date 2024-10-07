package dtb.terminalIO.components;

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

}
