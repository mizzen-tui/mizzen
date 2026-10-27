package mizzen.components.plot.tools.drawing;

import mizzen.components.plot.PointPlotter;

public class BrushTool implements DrawingTool {

    private final PointPlotter pp;

    public BrushTool(PointPlotter pp) {
        this.pp = pp;
    }

    @Override
    public void activate(DrawingCursor cursor) {
        cursor.setText("✎");
        cursor.show();
    }

    @Override
    public void deactivate(DrawingCursor cursor) {
        cursor.hide();
    }

    @Override
    public void onMouseMove(int r, int c, DrawingCursor cursor) {
       // cursor.moveToGrid(pp.getGrid(), r, c);
    }

    @Override
    public void onMouseDown(int r, int c) {
        pp.plot(c, r);
    }

    @Override
    public void onMouseDrag(int r, int c) {
        pp.plot(c, r);
    }

    @Override
    public void onMouseUp(int r, int c) {}
}
