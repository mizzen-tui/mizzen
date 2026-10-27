package mizzen.components.plot.tools.drawing;

import mizzen.components.ChildComponentManager;
import mizzen.components.Component;
import mizzen.components.ParentContainer;
import mizzen.components.TextComponent;
import mizzen.components.plot.braille.BrailleGrid;
import mizzen.controlcodes.color.Colors;
import mizzen.events.EventBus;
import mizzen.geom.position.Position;

public final class DrawingCursor {

    private final TextComponent cursorTC;
    private final ParentContainer parent;
    private boolean visible = false;

    public DrawingCursor(ChildComponentManager parent) {
        this.parent = parent;

        this.cursorTC = new TextComponent(Colors.IYel, "✎");
        this.cursorTC.setName("drawing-cursor");
        //this.cursorTC.setEnabled(false, new EventBus());  <-- why?  .. also never build eventBus .. only one in the app ever - this is an absolute
        this.cursorTC.hide();

        parent.add(cursorTC);  // todo: position?  defaults to 0,0 when not specified
    }

    public void setText(String txt) {
        cursorTC.setTextAndResize(txt);
        cursorTC.markAsDirty();
        Component.UTIL.bringToFront(cursorTC);
    }

    public void moveTo(int row, int col) {
        cursorTC.moveTo(row, col);
        cursorTC.markAsDirty();
        Component.UTIL.bringToFront(cursorTC);
    }

    public void moveTo(Position pos) {
        moveTo(pos.getRow(), pos.getCol());
    }

    public void moveToGrid(BrailleGrid grid, int gridRow, int gridCol) {
        Position gridAbs = Component.UTIL.getOffsetFromComponentContainer(grid, parent);
        if (gridAbs == null) gridAbs = new Position.Simple(0, 0);

        Position target = new Position.Simple(
            gridAbs.getRow() + gridRow,
            gridAbs.getCol() + gridCol
        );
        moveTo(target);
    }

    public void show() {
        if (!visible) {
            visible = true;
            cursorTC.show();
            cursorTC.markAsDirty();
            Component.UTIL.bringToFront(cursorTC);
        }
    }

    public void hide() {
        if (visible) {
            visible = false;
            cursorTC.hide();
            cursorTC.markAsDirty();
        }
    }
}
