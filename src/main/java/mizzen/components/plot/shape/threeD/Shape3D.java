
package mizzen.components.plot.shape.threeD;

import mizzen.components.HasName;

public interface Shape3D<T extends Shape3D<T>> extends HasName<T> {
    // Allows chaining if you want: shape.setRotation(x,y,z).someOtherMethod();
    T setRotation(double x, double y, double z);
}