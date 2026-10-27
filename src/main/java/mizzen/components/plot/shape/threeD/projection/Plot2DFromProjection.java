package mizzen.components.plot.shape.threeD.projection;

import mizzen.components.plot.shape.threeD.RenderContext;
import mizzen.components.plot.shape.threeD.Vec3Utils;
import mizzen.geom.position.Vec2;
import mizzen.geom.position.Vec3;
import mizzen.geom.position.Vec2.BaseVec2;

public class Plot2DFromProjection implements Plot2D {

    public static final float DEFAULT_DISTANCE = 100f;

    float distance;

    float win_width;
    float win_height;
    float fov;

    float thetaX;
    float thetaY;
    float thetaZ;

    float panX = 0;
    float panY = 0;

    private RenderContext renderContext = RenderContext.DEFAULT;

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
         this(DEFAULT_DISTANCE, 100f, 100f, 80f, 0, 0, 0);
    }

    public void setWindowSize(float win_width, float win_height) {
        this.win_width = win_width;
        this.win_height = win_height;
    }        

    @Override
    public Vec2<?> to2D(Vec3<?> position3d) {

        Vec3<?> rotated = Vec3Utils.rotateX(position3d, thetaX);
        rotated = Vec3Utils.rotateY(rotated, thetaY);
        rotated = Vec3Utils.rotateZ(rotated, thetaZ);
        
        Vec3<?> projected = Vec3Utils.project(rotated, win_width, win_height, fov, distance);
        
        //BaseVec2 p2D = new BaseVec2(projected.getX() , projected.getY());
        BaseVec2 p2D = new BaseVec2(
            projected.getX() + panX,
            projected.getY() + panY
        );
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

    // panning 

    public float getPanX() {
        return panX;
    }

    public void setPanX(float panX) {
        this.panX = panX;
    }

    public float getPanY() {
        return panY;
    }

    public void setPanY(float panY) {
        this.panY = panY;
    }

    public void panBy(float dx, float dy) {
        this.panX += dx;
        this.panY += dy;
    }
    
    public void setRenderContext(RenderContext ctx) {
        this.renderContext = ctx;
    }

    @Override
    public RenderContext getRenderContext() {
        return this.renderContext;
    }    
    
    public void zoomBy(float amount) {
        this.distance += amount;
        if (this.distance < 10f) {
            this.distance = 10f;
        }
    }

}