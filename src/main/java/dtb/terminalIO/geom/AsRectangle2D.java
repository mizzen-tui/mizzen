package dtb.terminalIO.geom;

public interface AsRectangle2D<T extends Number> {

    Rectangle2D<T>  getRectangle2D();

    public static interface AsFloat2D extends  AsRectangle2D<Float> {
    }

}
