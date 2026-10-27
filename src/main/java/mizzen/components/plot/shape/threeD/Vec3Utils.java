package mizzen.components.plot.shape.threeD;

import mizzen.geom.position.Vec2;
import mizzen.geom.position.Vec3;
import mizzen.geom.position.Vec2.BaseVec2;

public class Vec3Utils {
    
    /**
     * Rotates the point around the X axis by the given angle in degrees.
     */
    public static Vec3<?> rotateX(Vec3<?> p3D, float angle) {
        
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
    public static Vec3<?> rotateY(Vec3<?> p3D, float angle) {
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
    public static Vec3<?> rotateZ(Vec3<?> p3D, float angle) {
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
    public static Vec3<?> project(Vec3<?> p3D, float win_width, float win_height, float fov, float viewer_distance) {
        double factor = fov / (viewer_distance + p3D.getZ());
        double x = p3D.getX() * factor + win_width / 2;
        double y = p3D.getY() * factor + win_height / 2;
        return p3D.newFrom((float)x, (float)y, 1);
    }
    
    public static Vec2<?> plot2D(Vec3<?> p3D, float distance, float thetaX, float thetaY, float thetaZ) {
        return plot2D(p3D, Vec3.ZERO, distance, thetaX, thetaY, thetaZ);
    }

    public static Vec2<?> plot2D(Vec3<?> p3D, Vec3<?> camera, float distance, float thetaX, float thetaY, float thetaZ) {
        
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

  public static float calculateIntensity(Vec3<?> normal, Vec3<?> lightDir) {
        // Dot product: (Nx * Lx) + (Ny * Ly) + (Nz * Lz)
        // Assumes lightDir is normalized
        float dot = (normal.getX() * lightDir.getX()) + 
                    (normal.getY() * lightDir.getY()) + 
                    (normal.getZ() * lightDir.getZ());
        
        // Clamp to 0-1 range to avoid negative brightness
        return Math.max(0.0f, Math.min(1.0f, dot));
  }
    
  public static String asString(Vec3<?> p) {
        if (p == null) {
            return  "Vec3[null, null, null]";
        }
        return String.format("%s[x=%4f, y=%4f, z=%4f]", p.getClass().getSimpleName(), p.getX(), p.getY(), p.getZ());
  }
    

}