package mizzen.components.plot.shape.threeD;

import mizzen.components.HasName;
import mizzen.components.plot.shape.Shape;
import mizzen.components.plot.shape.threeD.projection.Plot2D;
import mizzen.geom.position.Vec3;

public abstract class AbstractShape3D<T extends AbstractShape3D<T>> implements Shape3D<T> {
    protected String name;
    protected double rotX, rotY, rotZ;

    protected Vec3<?> center;

    public AbstractShape3D() {
        this.name = HasName.UTIL.getName(this);  // default to simple class name becomes its name
    }

    @Override
    @SuppressWarnings("unchecked")
    public T setRotation(double x, double y, double z) {
        this.rotX = x;
        this.rotY = y;
        this.rotZ = z;
        return (T) this; // Returning 'this' as type T for chaining
    }

    public abstract Shape<?> as2DShape(Plot2D projection);

    @Override
    public String getName() {
        return name;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T setName(String name) {
        return (T)this;
    }
    public double getRotX() { return rotX; }
    public void setRotX(double rotX) { this.rotX = rotX; }

    public double getRotY() { return rotY; }
    public void setRotY(double rotY) { this.rotY = rotY; }

    public double getRotZ() { return rotZ; }
    public void setRotZ(double rotZ) { this.rotZ = rotZ; }

}