package dtb.terminalIO.geom.position;

import java.util.Comparator;

import dtb.terminalIO.components.plot.shape.threeD.Vec3Utils;
import dtb.terminalIO.components.plot.shape.threeD.projection.Plot2D;

public interface Vec3<T extends Vec3<?>> {

    public float getZ();

    public float getX();

    public float getY();

    public T newFrom(float x, float y, float z);
    
    public default Vec2<?> plot2D(Plot2D functionTransform2D) {
        return functionTransform2D.to2D(this);
    }

    public default Vec2<?> plot2D(Vec3<?> camera, float distance, float thetaX, float thetaY, float thetaZ) {
        return Vec3Utils.plot2D(this, camera, distance, thetaX, thetaY, thetaZ);
    }

    public default Vec2<?> plot2D(float distance, float thetaX, float thetaY, float thetaZ) {
        return Vec3Utils.plot2D(this, distance, thetaX, thetaY, thetaZ);
    }
    
    public static final Vec3<?> ZERO = new BaseVec3(0,0,0);
    
    public static class Vec3Comparator implements Comparator<Vec3<?>> {
        @Override
        public int compare(Vec3<?> vec1, Vec3<?> vec2) {
            if (vec1 == vec2)
                return 0;
            int cmp = Float.compare(vec1.getZ(), vec2.getZ());
            if (cmp != 0)
                return cmp;
            cmp = Float.compare(vec1.getY(), vec2.getY());
            if (cmp != 0)
                return cmp;
            return Float.compare(vec1.getX(), vec2.getX());
        }
    }

    public static class BaseVec3 implements Vec3<BaseVec3> {

        public float x;
        public float y;
        public float z;
        
        public BaseVec3(float x, float y, float z) {
            super();
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public BaseVec3(int x,int y, int z) {
            this((float)x, (float)y, (float)z);
        }

        @Override
        public float getX() {
            return x;
        }

        @Override
        public float getY() {
            return y;
        }

        @Override
        public float getZ() {
            return z;
        }


        @Override
        public BaseVec3 newFrom(float x, float y, float z) {
            return new BaseVec3(x, y, z);
        }
        
        public String toString() {
            return Vec3Utils.asString(this);
        }
        
    }
}
