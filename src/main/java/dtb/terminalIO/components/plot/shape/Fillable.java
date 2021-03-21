package dtb.terminalIO.components.plot.shape;

/**
 * A Shape of type T that can be filled.
 */
public interface Fillable<T> {

    T setFilled(boolean filled);
    
}
