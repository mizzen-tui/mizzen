package dtb.terminalIO.geom.position;

public class PositionRelativeUsingTransform implements Position {

    Position originalPosition;
    PositionTransform tranform;
    
    public PositionRelativeUsingTransform(Position originalPosition, PositionTransform tranform) {
        super();
        this.originalPosition = originalPosition;
        this.tranform = tranform;
    }

    @Override
    public int getCol() {
        return tranform.transform(originalPosition).getCol();
    }

    @Override
    public int getRow() {
        return tranform.transform(originalPosition).getRow();
    }

}
