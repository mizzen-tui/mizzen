package dtb.terminalIO.components;

public class HiliterAdapter<T> implements HasHiliter {
    
    private T source;
    
    public HiliterAdapter(T source) {
        this.source = source;
    }
    
    public Hiliter getHiliter() {

        if (source == null) {
            return Hiliter.NOOP;
        }

        if (source instanceof Hiliter) {
            return (Hiliter) source;
        }

        if (source instanceof HasHiliter) {
            return ((HasHiliter) source).getHiliter();
        }

        return Hiliter.NOOP;
    }


}
