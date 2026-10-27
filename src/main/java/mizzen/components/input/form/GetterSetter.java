package mizzen.components.input.form;

import java.util.function.Consumer;
import java.util.function.Supplier;

import mizzen.components.input.HasValue;
import mizzen.components.input.SettableHasValue;


public class GetterSetter<T> {

    private Supplier<T> getter;
    private Consumer<T> setter;

    public GetterSetter(Supplier<T> getter, Consumer<T> setter) {
        super();
        this.getter = getter;
        this.setter = setter;
    }

    public GetterSetter(SettableHasValue<T> sg) {
        this(()      -> sg.getValue(), 
             (value) -> sg.setValue(value));
    }

    public GetterSetter(HasValue<T> hv) {
        this(()      -> hv.getValue(), 
             (value) -> {});
    }

    public Supplier<T> getGetter() {
        return getter;
    }

    public void setGetter(Supplier<T> getter) {
        this.getter = getter;
    }

    public Consumer<T> getSetter() {
        return setter;
    }

    public void setSetter(Consumer<T> setter) {
        this.setter = setter;
    }

    public T get() {
        return getGetter().get();
    }

    public void set(T o) {
        getSetter().accept(o);
    }

    public static <T>  void swapXY(GetterSetter<T> x, GetterSetter<T> y) {
        T tmpX = x.get();
        x.set(y.get());
        y.set(tmpX);
    }

}
