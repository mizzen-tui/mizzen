package dtb.terminalIO.components.plot.shape.threeD.projection;

import dtb.terminalIO.components.plot.shape.threeD.Vec3Utils;
import dtb.terminalIO.geom.position.Vec2;
import dtb.terminalIO.geom.position.Vec3;

public class Plot2DSimple implements Plot2D {

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
        return Vec3Utils.plot2D(position3d, camera, distance, thetaX, thetaY, thetaZ);
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