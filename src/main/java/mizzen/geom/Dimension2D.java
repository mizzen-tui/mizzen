package mizzen.geom;

/**
 * A size in floats - a width and a height in any units (dots, your plot's numbers). The float
 * partner of Dimension (whole character cells).
 */
public interface Dimension2D {

    float getWidth();

    float getHeight();

    default boolean contains(Dimension2D other) {
        return this.getWidth() >= other.getWidth() && this.getHeight() >= other.getHeight();
    }

    default boolean isSameDimension(Dimension2D other) {
        return this.getWidth() == other.getWidth() && this.getHeight() == other.getHeight();
    }

    Dimension2D ZERO = new Dimension2D() {
        @Override
        public float getWidth() {
            return 0;
        }

        @Override
        public float getHeight() {
            return 0;
        }
    };
}
