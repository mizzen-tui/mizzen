package dtb.terminalIO.components.plot.shape;

import dtb.terminalIO.components.plot.PointPlotter;
import dtb.terminalIO.controlcodes.ControlCode.Color;
import dtb.terminalIO.geom.position.Position;

public class DotShape implements Shape {
    
    Position p;

    Color color;

    public DotShape(Position p, Color color) {
        this.p = p;
        this.color = color;
    }

    public DotShape(Position p) {
        this(p, null);
    }
    
    public DotShape(int x, int y, Color color) {
        this(new Position.Simple(y, x), color);
    }

    public DotShape(int x, int y) {
        this(x, y, null);
    }
    
    public int getX() {
        return p.getCol();
    }

    public int getY() {
        return p.getRow();
    }

    public Color getColor() {
        return this.color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    @Override
    public void draw(PointPlotter pointPlotter) {
        Color lastColor = null;
        
        if (color != null) {
            lastColor = pointPlotter.setColor(color);
        }
        
        pointPlotter.plot(getX(), getY());

        if (color != null) {
            pointPlotter.setColor(lastColor);
        }

    }
}
