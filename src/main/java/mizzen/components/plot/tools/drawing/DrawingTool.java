package mizzen.components.plot.tools.drawing;

public interface DrawingTool {

    default void activate(DrawingCursor cursor) {}
    default void deactivate(DrawingCursor cursor) {}

    default void onMouseMove(int row, int col, DrawingCursor cursor) {}
    void onMouseDown(int row, int col);
    void onMouseDrag(int row, int col);
    void onMouseUp(int row, int col);
}
