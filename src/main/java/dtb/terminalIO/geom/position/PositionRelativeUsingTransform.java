package dtb.terminalIO.geom.position;

public class PositionRelativeUsingTransform implements Position {

    Vec2<?> originalPosition;
    PositionTransform tranform;

    public PositionRelativeUsingTransform(Position originalPosition, PositionTransform tranform) {
        super();
        this.originalPosition = originalPosition;
        this.tranform = tranform;
    }

    @Override
    public int getCol() {
        return (int) tranform.transform(originalPosition).getX();
    }

    @Override
    public int getRow() {
        return (int) tranform.transform(originalPosition).getY();
    }

}
