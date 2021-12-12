package dtb.terminalIO.geom.position;

import java.util.Comparator;

import dtb.terminalIO.geom.position.Vec2.BaseVec2;

public interface Vec3<T extends Vec3<?>> {

    public float getZ();

    public float getX();

    public float getY();

    public T newFrom(float x, float y, float z);
    
    public default Vec2<?> plot2D(Plot2D functionTransform2D) {
        return functionTransform2D.to2D(this);
    }

    public default Vec2<?> plot2D(Vec3<?> camera, float distance, float thetaX, float thetaY, float thetaZ) {
        return UTIL.plot2D(this, camera, distance, thetaX, thetaY, thetaZ);
    }

    public default Vec2<?> plot2D(float distance, float thetaX, float thetaY, float thetaZ) {
        return UTIL.plot2D(this, distance, thetaX, thetaY, thetaZ);
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
            return UTIL.asString(this);
        }
        
    }
    
    @FunctionalInterface
    public static interface Plot2D  {
        /**
         * Map a 3D vector into 2D space
         * @param position3D
         * @return
         */
        Vec2<?> to2D(Vec3<?> position3D);
    }
    
    public static class Plot2DSimple implements Plot2D {

        Vec3<?> camera;
        float distance;
        float thetaX;
        float thetaY;
        float thetaZ;
        
        public Plot2DSimple(Vec3<?> camera, float distance, float thetaX, float thetaY, float thetaZ) {
            super();
            this.camera = camera;
            this.distance = distance;
            this.thetaX = thetaX;
            this.thetaY = thetaY;
            this.thetaZ = thetaZ;
        }

        public Plot2DSimple(float distance, float thetaX, float thetaY, float thetaZ) {
            this(Vec3.ZERO, distance, thetaX, thetaY, thetaZ);
        }

        public Plot2DSimple() {
            this(Vec3.ZERO, .5f , 0, 0, 0);
        }

        @Override
        public Vec2<?> to2D(Vec3<?> position3d) {
            return UTIL.plot2D(position3d, camera, distance, thetaX, thetaY, thetaZ);
        }

        public Vec3<?> getCamera() {
            return camera;
        }

        public void setCamera(Vec3<?> camera) {
            this.camera = camera;
        }

        public float getDistance() {
            return distance;
        }

        public void setDistance(float distance) {
            this.distance = distance;
        }

        public float getThetaX() {
            return thetaX;
        }

        public void setThetaX(float thetaX) {
            this.thetaX = thetaX;
        }

        public float getThetaY() {
            return thetaY;
        }

        public void setThetaY(float thetaY) {
            this.thetaY = thetaY;
        }

        public float getThetaZ() {
            return thetaZ;
        }

        public void setThetaZ(float thetaZ) {
            this.thetaZ = thetaZ;
        }
        
    }

    public static class Plot2DFromProjection implements Plot2D {

        float distance;

        float win_width;
        float win_height;
        float fov;

        float thetaX;
        float thetaY;
        float thetaZ;

        public Plot2DFromProjection(float distance, float win_width, float win_height, float fov, 
                                    float thetaX,
                                    float thetaY, float thetaZ) {
            super();
            this.distance = distance;
            this.win_width = win_width;
            this.win_height = win_height;
            this.fov = fov;
            this.thetaX = thetaX;
            this.thetaY = thetaY;
            this.thetaZ = thetaZ;
        }

        public Plot2DFromProjection() {
            this(50f,50f,50f,50f , 0, 0, 0);
        }

        @Override
        public Vec2<?> to2D(Vec3<?> position3d) {

            Vec3<?> rotated = UTIL.rotateX(position3d, thetaX);
            rotated = UTIL.rotateY(rotated, thetaY);
            rotated = UTIL.rotateY(rotated, thetaZ);
            
            Vec3<?> projected = UTIL.project(rotated, win_width, win_height, fov, distance);
            
            BaseVec2 p2D = new BaseVec2(projected.getX() , projected.getY());
            
            return p2D;
        }

        public float getDistance() {
            return distance;
        }

        public void setDistance(float distance) {
            this.distance = distance;
        }

        public float getThetaX() {
            return thetaX;
        }

        public void setThetaX(float thetaX) {
            this.thetaX = thetaX;
        }

        public float getThetaY() {
            return thetaY;
        }

        public void setThetaY(float thetaY) {
            this.thetaY = thetaY;
        }

        public float getThetaZ() {
            return thetaZ;
        }

        public void setThetaZ(float thetaZ) {
            this.thetaZ = thetaZ;
        }
        
    }
    
    public static class UTIL {
        
        /**
         * Rotates the point around the X axis by the given angle in degrees.
         */
        static Vec3<?> rotateX(Vec3<?> p3D, float angle) {
            
            double rad = angle * Math.PI / 180 ;
            double cosa = Math.cos(rad);
            double sina = Math.sin(rad);
            
            double y = p3D.getY()  * cosa - p3D.getZ() * sina ;
            double z = p3D.getY()  * sina + p3D.getZ() * cosa ;
            
            return p3D.newFrom(p3D.getX(), (float)y, (float)z);
        }
        
        /**
        def rotateY(self, angle):
            """ Rotates the point around the Y axis by the given angle in degrees. """
            rad = angle * math.pi / 180
            cosa = math.cos(rad)
            sina = math.sin(rad)
            x = self.z * sina + self.x * cosa
            z = self.z * cosa - self.x * sina
            return Point3D(x, self.y, z)
        */
        static Vec3<?> rotateY(Vec3<?> p3D, float angle) {
            double rad = angle * Math.PI / 180 ;
            double cosa = Math.cos(rad);
            double sina = Math.sin(rad);
            
            double x = p3D.getZ()  * sina + p3D.getX() * cosa ;
            double z = p3D.getZ()  * cosa - p3D.getX() * sina ;
            
            return p3D.newFrom((float) x, p3D.getY(), (float)z);
        }
        
        /**
         *     def rotateZ(self, angle):
        """ Rotates the point around the Z axis by the given angle in degrees. """
        
        rad = angle * math.pi / 180
        cosa = math.cos(rad)
        sina = math.sin(rad)
        
        x = self.x * cosa - self.y * sina
        y = self.x * sina + self.y * cosa
        return Point3D(x, y, self.z)
        
        
         * @param p3D
         * @param angle
         * @return
         */
        static Vec3<?> rotateZ(Vec3<?> p3D, float angle) {
            double rad = angle * Math.PI / 180 ;
            double cosa = Math.cos(rad);
            double sina = Math.sin(rad);

            double x = p3D.getX()  * cosa - p3D.getY() * sina ;
            double y = p3D.getX()  * sina + p3D.getY() * cosa ;

            return p3D.newFrom((float) x, (float) y, p3D.getZ());
        }
        
        /**
        def project(self, win_width, win_height, fov, viewer_distance):
            """ Transforms this 3D point to 2D using a perspective projection. """
            factor = fov / (viewer_distance + self.z)
            x = self.x * factor + win_width / 2
            y = -self.y * factor + win_height / 2
            return Point3D(x, y, 1)
        */
        static Vec3<?> project(Vec3<?> p3D, float win_width, float win_height, float fov, float viewer_distance) {
            double factor = fov / (viewer_distance + p3D.getZ());
            double x = p3D.getX() * factor + win_width / 2;
            double y = p3D.getY() * factor + win_width / 2;
            return p3D.newFrom((float)x, (float)y, 1);
        }
        
        static Vec2<?> plot2D(Vec3<?> p3D, float distance, float thetaX, float thetaY, float thetaZ) {
            return plot2D(p3D, Vec3.ZERO, distance, thetaX, thetaY, thetaZ);
        }

        static Vec2<?> plot2D(Vec3<?> p3D, Vec3<?> camera, float distance, float thetaX, float thetaY, float thetaZ) {
            
            Vec3<?> pRotated = rotateVector(p3D, thetaX, thetaY, thetaZ);
            
            float Z = distance + pRotated.getZ();
            BaseVec2 p2D = new BaseVec2(pRotated.getX() / Z, pRotated.getY() / Z);
            
            return p2D;
        }

        static public Vec3<?> rotateVector(Vec3<?> p3D, float thetaX, float thetaY, float thetaZ) {
            return rotateVector(p3D, Vec3.ZERO, thetaX, thetaY, thetaZ);
        }

        static public Vec3<?> rotateVector(Vec3<?> p3D, Vec3<?> camera, float thetaX, float thetaY, float thetaZ) {
            float aX, aY, aZ; 
            float bX, bY, bZ; 

            float camX = camera.getX();
            float camY = camera.getY();
            float camZ = camera.getZ();

            aX = p3D.getX();
            aY = p3D.getY();
            aZ = p3D.getZ();

            // 3D -> 2D transformation matrix calculation with rotation
            // and camera coordinate parameters

            // Rotation um x-Achse
            // p[i][x] = px;
            bY = (float) ((aY - camY) * Math.cos(thetaX) - (aZ - camZ) * Math.sin(thetaX));
            bZ = (float) ((aY - camY) * Math.sin(thetaX) + (aZ - camZ) * Math.cos(thetaX));

            aZ = bZ;
            // Rotation um y-Achse
            bX = (float) ((aX - camX) * Math.cos(thetaY) + (aZ - camZ) * Math.sin(thetaY));
            // p[i][y]= py;
            bZ = (float) (-(aX - camX) * Math.sin(thetaY) + (aZ - camZ) * Math.cos(thetaY));

            aY = bY;
            aX = bX;
            // Rotation um z-Achse
            bX = (float) ((aX - camX) * Math.cos(thetaZ) - (aY - camY) * Math.sin(thetaZ));
            bY = (float) ((aY - camY) * Math.cos(thetaZ) + (aX - camX) * Math.sin(thetaZ));

            return  p3D.newFrom(bX, bY, bZ);
            
      }
        
        
        public static String asString(Vec3<?> p) {
            if (p == null) {
                return  "Vec3[null, null, null]";
            }
            return String.format("%s[x=%4f, y=%4f, z=%4f]", p.getClass().getSimpleName(), p.getX(), p.getY(), p.getZ());
        }
        

    }
}
