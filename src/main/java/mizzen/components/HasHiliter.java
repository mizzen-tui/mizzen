package mizzen.components;

import java.util.function.Predicate;

public interface HasHiliter {

    Hiliter getHiliter();

    public static class HiliterPredicate<T> implements Predicate<T>  {

        @Override
        public boolean test(T input) {
            return input instanceof HasHiliter;
        }
    }

}
