package mizzen.components.plot.tools.drawing;


public final class ToolManager {

    private DrawingTool active;
    private final DrawingCursor cursor;

    public ToolManager(DrawingCursor cursor) {
        this.cursor = cursor;
    }

    public void setActiveTool(DrawingTool tool) {
        if (active != null) active.deactivate(cursor);
        active = tool;
        if (active != null) active.activate(cursor);
    }

    public void handleMouseMove(int row, int col) {
        if (active != null) active.onMouseMove(row, col, cursor);
    }

    public void handleMouseDown(int row, int col) {
        if (active != null) active.onMouseDown(row, col);
    }

    public void handleMouseDrag(int row, int col) {
        if (active != null) active.onMouseDrag(row, col);
    }

    public void handleMouseUp(int row, int col) {
        if (active != null) active.onMouseUp(row, col);
    }
}
