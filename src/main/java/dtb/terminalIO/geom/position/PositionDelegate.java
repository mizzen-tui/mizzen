package dtb.terminalIO.geom.position;

public class PositionDelegate implements Position {

    
    private Row    rowDelegate;
    private Column columnDelegate;
    
    public PositionDelegate(Row rowDelegate, Column columnDelegate) {
        super();
        this.rowDelegate = rowDelegate;
        this.columnDelegate = columnDelegate;
    }

    public PositionDelegate(Position rowColumnDelegate) {
        this(rowColumnDelegate, rowColumnDelegate);
    }
    
    
    @Override
    public int getRow() {
        return this.rowDelegate.getRow();
    }

    @Override
    public int getCol() {
        return this.columnDelegate.getCol();
    }

}
