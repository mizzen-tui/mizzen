package mizzen.geom;

/** something with a float box around it - e.g. a shape, after its transform */
public interface AsRectangle2D {

    Rectangle2D getRectangle2D();

    /** (the same: kept for what implements it by this name) */
    public static interface AsFloat2D extends AsRectangle2D {
    }
}
