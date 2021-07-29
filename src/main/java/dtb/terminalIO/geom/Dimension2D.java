package dtb.terminalIO.geom;

public interface Dimension2D<T extends Number> {

    public T getWidth();

    public T getHeight();

    public default boolean contains(Dimension2D<?> other) 
    {
        return this.getWidth().longValue() >= other.getWidth().longValue() && 
               this.getHeight().longValue() >= other.getHeight().longValue();
    }

    public default boolean isSameDimension(Dimension2D<?> other) {
        return this.getWidth() == other.getWidth() && this.getHeight() == other.getHeight();
    }

    
    public static final Dimension2D<Long> ZERO = new Dimension2D<Long>() {

        @Override
        public Long getWidth() {
            return 0L;
        }
        @Override
        public Long getHeight() {
            return 0L;
        }
        
    };
    
}
