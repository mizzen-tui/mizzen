package dtb.terminalIO.components.frameset;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import dtb.terminalIO.components.AbstractComponentWithContainer;
import dtb.terminalIO.components.BlockChars;
import dtb.terminalIO.components.BoundedComponent;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.GetComponentAtOffset;
import dtb.terminalIO.components.HasName;
import dtb.terminalIO.components.HasTitleAndBorder;
import dtb.terminalIO.components.ParentContainer;
import dtb.terminalIO.components.composite.Composite;
import dtb.terminalIO.components.composite.HorizontalPanel;
import dtb.terminalIO.components.input.DraggableTextComponent;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.events.mouse.ApplicationMouseEvent;
import dtb.terminalIO.events.mouse.Draggable;
import dtb.terminalIO.geom.CanResize;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.textsegments.IScreenLineSegmentsBuffer;
import dtb.terminalIO.util.ElapsedTime;



public class FrameSet extends BoundedComponent<FrameSet> implements HasTitleAndBorder<FrameSet>,  ParentContainer, GetComponentAtOffset {

    Composite container;

    List<VerticalDivider> vDividers = new ArrayList<VerticalDivider>();
    List<HorizontalDivider> hDividers = new ArrayList<HorizontalDivider>();

    List<VerticalDraggable> vDraggables = new ArrayList<VerticalDraggable>();
    List<HorizontalDraggable> hDraggables = new ArrayList<HorizontalDraggable>();
 
    public FrameSet(Color color, Dimension d) {
        super(color, d);
        container = new Composite(color);
        container.setBounds(d);
        container.setParentContainer(this);
    }

    Rectangle childComponentAsRectangle(Component<?> child) {
        Rectangle childAsRectangle = new Rectangle.Simple(container.childAsRectangleOffset(child));
        if (hasBorder()) {
            childAsRectangle.moveDelta(1, 1);
        }
        return childAsRectangle;
    }

    public FrameSet setName(String name) {
        super.setName(name);
        HasName.UTIL.decorateWithParentsNamePrefix(this.container, this);
        return this;
    }

    public FrameSet add(Component<?> component) {
        this.container.add(component);
        return this;
    }

    public FrameHorizontalPanel addRow() {
        FrameHorizontalPanel row = new  FrameHorizontalPanel(this.getColor());
        // TODO:  make this smart.  if children already added ... add below bottomost child
        this.add(row);
        return row;
    }

    private Rectangle findNextRightChildComponent(Position offset) {

        int row = offset.getRow();
        int col = offset.getCol();

        Optional<Rectangle> found = container.getChildren().stream()
                                                           .filter((c)-> container.isChildVisible(c) )
                                                           .map((c)-> childComponentAsRectangle(c))
                                                           .filter((c)-> c.containsRow( row ) && c.getCol()>col)
                                                           .sorted( (c1,c2) -> c1.getCol() - c2.getCol()  )
                                                           .findFirst();
        return found.isPresent() ? found.get() : null ;
    }

    private Rectangle findNextBelowChildComponent(Position offset) {

        int row = offset.getRow();
        int col = offset.getCol();

        Optional<Rectangle> found = container.getChildren().stream()
                                                           .filter((c)-> container.isChildVisible(c) )
                                                           .map((c)-> childComponentAsRectangle(c))
                                                           .filter((c)-> c.containsCol( col ) && c.getRow()>row)
                                                           .sorted( (c1,c2) -> c1.getRow() - c2.getRow()  )
                                                           .findFirst();

        return found.isPresent() ? found.get() : null ;
    }

    public FrameSet addRight(Component<?> component, Component<?> anchorComponent, boolean draggable) {
        container.addRight(component, anchorComponent, 1);
        VerticalDivider vd = new VerticalDivider(anchorComponent, component);
        vDividers.add(vd);
        if (draggable) {
            VerticalDraggable vDraggable = new VerticalDraggable(vd, this.getColor(), this.getColor().bold());
            vDraggable.setParentContainer(this); 
            vDraggables.add(vDraggable);
            return this;
        }
        return this;
    }

    public FrameSet addRight(Component<?> component, Component<?> anchorComponent) {
        return addRight(component, anchorComponent, false);
    }

    VerticalDivider findVerticalDivider(Component<?> left, Component<?> right) {
        for (VerticalDivider vd : this.vDividers) {
            if (vd.left == left && vd.right == right) {
                return vd;
            }
        }
        throw new IllegalArgumentException("No vertical devider for left/right components. " + 
                              HasName.UTIL.getName(left)+" / " + HasName.UTIL.getName(left));
    }

    public FrameSet moveLeft(int charsLeft, Component<?> left, Component<?> right) {
        VerticalDivider vd = findVerticalDivider(left, right);
        if (vd.canMoveLeft(charsLeft)) {
            vd.moveLeft(charsLeft);
        }
        return this;
    }

    public FrameSet moveRight(int charsLeft, Component<?> left, Component<?> right) {
        VerticalDivider vd = findVerticalDivider(left, right);
        if (vd.canMoveRight(charsLeft)) {
            vd.moveRight(charsLeft);
        }
        return this;
    }

    HorizontalDivider findHorizontalDivider(Component<?> above, Component<?> below) {
        for (HorizontalDivider hd : this.hDividers) {
            if (hd.above == above && hd.below == below) {
                return hd;
            }
        }
        throw new IllegalArgumentException("No horizontal devider for above/below components. " + 
                              HasName.UTIL.getName(above)+" / " + HasName.UTIL.getName(below));
    }

    public FrameSet moveUp(int charsUp, Component<?> above, Component<?> below) {
        HorizontalDivider hd = findHorizontalDivider(above, below);
        if (hd.canMoveUp(charsUp)) {
            hd.canMoveUp(charsUp);
        }
        return this;
    }

    public FrameSet addBelow(Component<?> component, Component<?> anchorComponent, boolean draggable) {
        container.addBelow(component, anchorComponent, 1);
        HorizontalDivider hd = new HorizontalDivider(anchorComponent, component);
        hDividers.add(hd);
        if (draggable) {
            HorizontalDraggable vDraggable = new HorizontalDraggable(hd, this.getColor(), this.getColor().bold());
            vDraggable.setParentContainer(this); 
            hDraggables.add(vDraggable);
            return this;
        }
        return this;
    }
    
    public FrameSet addBelow(Component<?> component, Component<?> anchorComponent) {
        return addBelow(component, anchorComponent, false);
    }
    
    public FrameHorizontalPanel addRowBelow(Component<?> anchorComponent, boolean draggable) {
        FrameHorizontalPanel row = new  FrameHorizontalPanel(this.getColor());
        this.addBelow(row, anchorComponent, draggable);
        return row;
    }

    public FrameHorizontalPanel addRowBelow(Component<?> anchorComponent) {
        return addRowBelow(anchorComponent, false);
    }

    public void onResizeExpandRight(Component<?> component, int percent) {
        container.onResizeExpandRight(component, percent);
    }

    public void onResizeExpandDown(Component<?> component) {
        container.onResizeExpandDown(component);
    }

    public void onResizeExpandDown(Component<?> component, int percent) {
        container.onResizeExpandDown(component, percent);
    }

    public void onResizeExpandDownAndRight(Component<?> component) {
        container.onResizeExpandDownAndRight(component);
    }

    @Override
    public void resize(Dimension d) {
        if (this.isSameDimension(d)) {
            return ;
        }
        super.resize(d);
        this.container.resize(d);
    }

    private boolean resizeRulesAppliedAtLeastOnce = false;

    public void applyResizeRules() {
        this.container.applyResizeRules();
        resizeRulesAppliedAtLeastOnce = true;
    }

    protected void applyResizeRulesIfNeverDoneBefore() {
        if (resizeRulesAppliedAtLeastOnce) {
            return;
        }
        this.container.applyResizeRules();
    }

    @Override
    protected void draw(Color color, IScreenLineSegmentsBuffer mySegments) {
        ElapsedTime drawContainerTime = new ElapsedTime();
        applyResizeRulesIfNeverDoneBefore();
        if (mySegments.isEmpty()) {
            // fast!
            mySegments.getSegments().addAll(container.getSegmentsBuffer().getSegments());
         } else {
            // slow - but safe
             mySegments.merge(container.getSegmentsBuffer().getSegments());
         }
        int x = 1;
        if (x == 2) {
            return;
        }

//        Logging.info(String.format("Parts Frameset container draw: #segments %d   time:  %d",  
//                                   container.getSegmentsBuffer().getSegments().size(), 
//                                   drawContainerTime.elapsed() ));

        Rectangle bounds = new Rectangle.Simple(this);

        Component.UTIL.assertSegmentsIntegrity(bounds, mySegments.getSegments());

        int iCount = 0;

        // Please note:  There is a bug in the line drawing such that it favors drawing vertical lines first 
        //               and then horizontal lines 2nd.  If you do the reverse, the code might not use the correct
        //               intersection character.
        boolean doHorizonal = false;
        
//        if (doHorizonal) {
//            for (HorizontalDivider hDivider : hDividers) {
//                iCount++;
//                if (iCount == 1) {
//                    iCount = 0;
//                }
//                Position startPos = hDivider.getStartPositionOfHorizontalDivider();
//                int hLineLen = hDivider.getWidthOfHorizontalDivider();
//                BlockChars.UTIL.drawHorizontalLine(mySegments, this.getColor(), this.getBlockChars(), startPos, hLineLen);
//                Component.UTIL.assertSegmentsIntegrity(bounds, mySegments.getSegments());
//            }
//        }

        for (VerticalDivider vDivider : vDividers) {
            if (bounds.containsPosition(vDivider.getPositionOfVerticalDivider())) {
                int lineHeight = vDivider.getHeightOfVerticalDivider();
                if (lineHeight + vDivider.getPositionOfVerticalDivider().getRow() > bounds.getBottomMostRow()) {
                    lineHeight = bounds.getBottomMostRow() - vDivider.getPositionOfVerticalDivider().getRow() + 1;
                }
                BlockChars.UTIL.drawVerticalLine(mySegments, this.getColor(), this.getBlockChars(), vDivider.getPositionOfVerticalDivider(), lineHeight);
            }
            Component.UTIL.assertSegmentsIntegrity(bounds, mySegments.getSegments());
        }

        if (!doHorizonal) {
            for (HorizontalDivider hDivider : hDividers) {
                iCount++;
                if (iCount == 1) {
                    iCount = 0;
                }
                Position startPos = hDivider.getStartPositionOfHorizontalDivider();
                int hLineLen = hDivider.getWidthOfHorizontalDivider();
                BlockChars.UTIL.drawHorizontalLine(mySegments, this.getColor(), this.getBlockChars(), startPos, hLineLen);
                Component.UTIL.assertSegmentsIntegrity(bounds, mySegments.getSegments());
            }
        }

        for (VerticalDraggable vd : vDraggables) {
            segments.mergeAt(vd.getPositionInFrameSet(), vd.getSegmentsBuffer());
        }

        for (HorizontalDraggable hd : hDraggables) {
            segments.mergeAt(hd.getPositionInFrameSet(), hd.getSegmentsBuffer());
        }

//        Logging.info(String.format("Parts Frameset full draw: #segments %d   time:  %d",
//                mySegments.getSegments().size(), 
//                drawContainerTime.recalcElapsed() ));
    }


    @Override
    public void hilite() {
        container.hilite();
    }


    @Override
    public void unHilite() {
        container.unHilite();
    }


    @Override
    public boolean hasBorder() {
        return container.hasBorder();
    }


    @Override
    public FrameSet setBorder(boolean hasBorder) {
        container.setBorder(hasBorder);
        return this;
    }


    @Override
    public FrameSet setBorderColor(Color borderColors) {
        container.setBorderColor(borderColors);
        return this;
    }


    @Override
    public FrameSet setTitle(Color titleColors, String title) {
        container.setTitle(titleColors, title);
        return this;
    }


    @Override
    public FrameSet setTitle(String title) {
        container.setTitle(title);
        return this;
    }


    @Override
    public String getTitle() {
        return container.getTitle();
    }

    @Override
    public Collection<? extends Component<?>> getChildren() {
        List<Component<?>> children = new ArrayList<Component<?>>();
        children.add(container);
        children.addAll(vDraggables);
        return children;
    }

    @Override
    public Position getPositionRelativeToParent(Component<?> child) {
        if (child == this.container) {
            return Position.ZERO;
        }
        int childVIndex = this.vDraggables.indexOf(child); 
        if (childVIndex >-1) {
            return this.vDraggables.get(childVIndex).getPositionInFrameSet();
        }
        childVIndex = this.hDraggables.indexOf(child); 
        if (childVIndex >-1) {
            return this.hDraggables.get(childVIndex).getPositionInFrameSet();
        }
        return null;
    }


    @Override
    public void childResized(Component<?> child) {
        markAsDirty();
        if (child == this.container) {
           this.resize(child.getPreferredRectangleForLayout());
        }
    }

    @Override
    public Component<?> getComponentAtOffset(Position offset) {

        if ( ! this.containsPosition(offset)) {
            return null;
        }
        // NOTE: We must check the draggables before the internal container otherwise container 
        //       is always returned - remember this detail if/when you make changes to this!
        for (VerticalDraggable draggable : vDraggables) {
            if (draggable.containsPosition( offset.offsetAdd( draggable.getPositionInFrameSet().negate()))) {
                return draggable;
            }
        }
        for (HorizontalDraggable draggable : hDraggables) {
            if (draggable.containsPosition( offset.offsetAdd( draggable.getPositionInFrameSet().negate()))) {
                return draggable;
            }
        }
        return this.container.getComponentAtOffset(offset);
    }

    @Override
    public boolean canMoveChild(Component<?> child) {
        int childVIndex = this.vDraggables.indexOf(child); 
        if (childVIndex > -1) {
            return true;
        }
        childVIndex = this.hDraggables.indexOf(child); 
        if (childVIndex > -1) {
            return true;
        }
        return false;
    }

    public void markAsDirty() {
        if (isDirty()) {
            return;
        }
        super.markAsDirty();
        container.markAsDirty();
        for (VerticalDraggable draggableVD : vDraggables) {
            if (!draggableVD.isDirty()) {
                draggableVD.markAsDirty();
            }
        }
        for (HorizontalDraggable draggableHD : hDraggables) {
            if (!draggableHD.isDirty()) {
                draggableHD.markAsDirty();
            }
        }
    }

    public class FrameHorizontalPanel extends AbstractComponentWithContainer<HorizontalPanel> {
        public FrameHorizontalPanel(Color color) {
            super(color, new HorizontalPanel(color,1));
        }
    }

    // .....................................................................................................
    class VerticalDivider {
        Component<?> left;
        Component<?> right;
        public VerticalDivider(Component<?> left, Component<?> right) {
            super();
            this.left = left;
            this.right = right;
        }

        public int getHeightOfVerticalDivider() {
            Rectangle nextComponentBelow = findNextBelowChildComponent(getPositionOfVerticalDivider());
            int row = nextComponentBelow != null 
                             ? nextComponentBelow.getPostion_BottomLeft().getRow() - 1
                             : FrameSet.this.getHeight() + (hasBorder()?1:0);
            return row - Math.min(container.getPositionRelativeToParent(left).getRow(), container.getPositionRelativeToParent(right).getRow());
        }

        public Position getPositionOfVerticalDivider() {
            return childComponentAsRectangle(left).getPostion_TopRight().offsetAdd(hasBorder()?-1:0,1);
        }

        protected boolean canMoveRight(int numSpaces) {
            if (right.getWidth() - numSpaces > 1) {
                return true;
            }
            return false;
        }

        protected void moveRight(int numSpaces) {
            if (!canMoveRight(numSpaces)) {
                return;
            }
            Dimension newSizeRight = new Dimension.Simple(right.getHeight(), right.getWidth() - numSpaces);
            Dimension newSizeLeft = new Dimension.Simple(left.getHeight(), left.getWidth() + numSpaces);
            ((CanResize)left).resize(newSizeLeft);
            ((CanResize)right).resize(newSizeRight);
            vDraggables.stream().filter(vd-> vd.vdivider == VerticalDivider.this).forEach(vd-> vd.markAsDirty());
            FrameSet.this.markAsDirty();
            FrameSet.this.container.markBorderAsDirty(); // don't ask ..
        }

        protected boolean canMoveLeft(int numSpaces) {
            if (left.getWidth() - numSpaces > 1) {
                return true;
            }
            return false;
        }

        protected void moveLeft(int numSpaces) {
            if (!canMoveLeft(numSpaces)) {
                return;
            }
            Dimension newSizeLeft = new Dimension.Simple(left.getHeight(), left.getWidth() - numSpaces);
            Dimension newSizeRight = new Dimension.Simple(right.getHeight(), right.getWidth() + numSpaces);
            ((CanResize)left).resize(newSizeLeft);
            ((CanResize)right).resize(newSizeRight);
            vDraggables.stream().filter(vd-> vd.vdivider == this).forEach(vd-> vd.markAsDirty());
            FrameSet.this.markAsDirty();
            FrameSet.this.container.markBorderAsDirty(); // don't ask ..
        }

        public String toString() {
            return String.format("VerticalDivider[left=(%s),right=(%s)]",String.valueOf(left), String.valueOf(right));
        }
    }

    // .....................................................................................................
    class VerticalDraggable extends DraggableTextComponent {

        VerticalDivider vdivider;

        public VerticalDraggable(VerticalDivider vd, Color color,Color draggingColor) {
            super(color, "⣿\n⣿" , draggingColor, "⢾\n⡷" , (Draggable)null);
            this.vdivider = vd;

            Draggable draggableMoveComponent = new DragAndMoveComponentBehaviour(this) {
                protected boolean canDrag(ApplicationMouseEvent appMouseEventRelativeToCanvas) {

                    ApplicationMouseEvent appMouseEvent = ApplicationMouseEvent.asRelativeToComponent(appMouseEventRelativeToCanvas, FrameSet.this);

                    Position currentPos = new Position.Simple(VerticalDraggable.this.getPositionInFrameSet());
                    if (appMouseEvent.getMousePosition().getCol()<currentPos.getCol()) {
                        int diff = currentPos.getCol() - appMouseEvent.getMousePosition().getCol();
                        return vd.canMoveLeft(diff);
                    }
                    int diff = appMouseEvent.getMousePosition().getCol() - currentPos.getCol();
                    return vd.canMoveRight(diff);
                }
                public boolean onDrag(ApplicationMouseEvent appMouseEventRelativeToCanvas) {
                    boolean baseResult = super.onDrag(appMouseEventRelativeToCanvas);
                    if (!baseResult) {
                        return false;
                    }
                    ApplicationMouseEvent appMouseEvent = ApplicationMouseEvent.asRelativeToComponent(appMouseEventRelativeToCanvas, FrameSet.this);
                    Position currentPos = new Position.Simple(VerticalDraggable.this.getPositionInFrameSet());
                    if (appMouseEvent.getMousePosition().getCol()<currentPos.getCol()) {
                        int diff = currentPos.getCol() - appMouseEvent.getMousePosition().getCol();
                        if ( vd.canMoveLeft(diff) ) {
                            vd.moveLeft(diff);
                            return true;
                        }
                    }
                    int diff = appMouseEvent.getMousePosition().getCol() - currentPos.getCol();
                    if (vd.canMoveRight(diff) ) {
                        vd.moveRight(diff);
                        return true;
                    }
                    return false;
                }
            }.freezeRow();

            Draggable draggable = draggableMoveComponent;
            this.setDraggableBehaviour(draggable);
            this.vdivider = vd;
        }
        
        @Override
        public void draw(Color color, IScreenLineSegmentsBuffer segments) {
            super.draw(color, segments);
        }
        
        protected Position getPositionInFrameSet() {
            int vd_row = vdivider.getPositionOfVerticalDivider().getRow();
            int vd_height = vdivider.getHeightOfVerticalDivider();
            int height = getHeight();
            int row = Math.max(0, (vd_height - height) / 2) + vd_row;
            int col = vdivider.getPositionOfVerticalDivider().getCol();
            return new Position.Simple(row, col);
        }

    }
    

    // .....................................................................................................
    class HorizontalDivider {
        Component<?> above;
        Component<?> below;

        public HorizontalDivider(Component<?> above, Component<?> below) {
            super();
            this.above = above;
            this.below = below;
        }

        public int getWidthOfHorizontalDivider() {
            
            Rectangle nextComponentToTheRight = findNextRightChildComponent(getStartPositionOfHorizontalDivider());
            if (nextComponentToTheRight != null) {
                int rightCol = nextComponentToTheRight.getPostion_TopLeft().getCol()  ;
                int leftColForLine = getStartPositionOfHorizontalDivider().getCol();
                return (rightCol - leftColForLine);
            } else {
                int rightCol = FrameSet.this.getWidth() + (FrameSet.this.hasBorder() ? 1 : 1);
                int leftColForLine = getStartPositionOfHorizontalDivider().getCol();
                return (rightCol - leftColForLine) -1 ;
            }
        }

        public Position getStartPositionOfHorizontalDivider() {
            Position bottomLeft = childComponentAsRectangle(above).getPostion_BottomLeft();
            if (bottomLeft.getCol()> 0) {
                return bottomLeft.offsetAdd(1,-1);
            }
            return childComponentAsRectangle(above).getPostion_BottomLeft().offsetAdd(1, hasBorder()?-1:0);
        }

        protected boolean canMoveUp(int numSpaces) {
            if (numSpaces < 0) {
                throw new IllegalArgumentException("canMoveUp("+numSpaces+") is not a valid parameter");
            }
            if (above.getHeight() - numSpaces > 1) {
                return true;
            }
            return false;
        }

        protected void moveUp(int numSpaces) {
            if (numSpaces < 0) {
                throw new IllegalArgumentException("moveUp("+numSpaces+") is not a valid parameter");
            }
            if (!canMoveUp(numSpaces)) {
                return;
            }
            Dimension newSizeAbove = new Dimension.Simple(above.getHeight() - numSpaces, above.getWidth());
            Dimension newSizeBelow = new Dimension.Simple(below.getHeight() + numSpaces, below.getWidth());
            ((CanResize)above).resize(newSizeAbove);
            ((CanResize)below).resize(newSizeBelow);
            hDraggables.stream().filter(hd-> hd.hdivider == this).forEach(hd-> hd.markAsDirty());
            FrameSet.this.markAsDirty();
            FrameSet.this.container.markBorderAsDirty(); // don't ask ..
        }

        protected boolean canMoveDown(int numSpaces) {
            if (numSpaces < 0) {
                throw new IllegalArgumentException("canMoveDown("+numSpaces+") is not a valid parameter");
            }
            if (below.getHeight() - numSpaces > 1) {
                return true;
            }
            return false;
        }

        protected void moveDown(int numSpaces) {
            if (numSpaces < 0) {
                throw new IllegalArgumentException("moveDown("+numSpaces+") is not a valid parameter");
            }
            if (!canMoveDown(numSpaces)) {
                return;
            }
            Dimension newSizeAbove = new Dimension.Simple(above.getHeight() + numSpaces, above.getWidth());
            Dimension newSizeBelow = new Dimension.Simple(below.getHeight() - numSpaces, below.getWidth());
            ((CanResize)above).resize(newSizeAbove);
            ((CanResize)below).resize(newSizeBelow);
            hDraggables.stream().filter(hd-> hd.hdivider == this).forEach(hd-> hd.markAsDirty());
            FrameSet.this.markAsDirty();
            FrameSet.this.container.markBorderAsDirty(); // don't ask ..
        }

        public String toString() {
            return String.format("HorizontalDivider[above=(%s),below=(%s)]",String.valueOf(above), String.valueOf(below));
        }
        
//        public Position getPositionOfHorizontalDivider() {
//            NOT CORRECT - fix
//            return childComponentAsRectangle(below).getPostion_TopLeft().offsetAdd(hasBorder()?-1:0,1);
//        }

    }
    
    // .....................................................................................................
    class HorizontalDraggable extends DraggableTextComponent {

        HorizontalDivider hdivider;

        public HorizontalDraggable(HorizontalDivider hd, Color color,Color draggingColor) {
            super(color, "⣿⣿" , draggingColor, "⢾⡷" , (Draggable)null);

            Draggable draggableMoveComponent = new DragAndMoveComponentBehaviour(this) {
                protected boolean canDrag(ApplicationMouseEvent appMouseEventRelativeToCanvas) {

                    ApplicationMouseEvent appMouseEvent = ApplicationMouseEvent.asRelativeToComponent(appMouseEventRelativeToCanvas, FrameSet.this);

                    Position currentPos = new Position.Simple(HorizontalDraggable.this.getPositionInFrameSet());
                    if (appMouseEvent.getMousePosition().getRow() < currentPos.getRow()) {
                        int diff = currentPos.getRow() - appMouseEvent.getMousePosition().getRow();
                        return hd.canMoveUp(diff);
                    }
                    int diff = appMouseEvent.getMousePosition().getRow() - currentPos.getRow();
                    return hd.canMoveDown(diff);
                }
                public boolean onDrag(ApplicationMouseEvent appMouseEventRelativeToCanvas) {
                    boolean baseResult = super.onDrag(appMouseEventRelativeToCanvas);
                    if (!baseResult) {
                        return false;
                    }
                    ApplicationMouseEvent appMouseEvent = ApplicationMouseEvent.asRelativeToComponent(appMouseEventRelativeToCanvas, FrameSet.this);
                    Position currentPos = new Position.Simple(HorizontalDraggable.this.getPositionInFrameSet());
                    if (appMouseEvent.getMousePosition().getRow() < currentPos.getRow()) {
                        int diff = currentPos.getRow() - appMouseEvent.getMousePosition().getRow();
                        if ( hd.canMoveUp(diff) ) {
                            hd.moveUp(diff);
                            return true;
                        }
                    }
                    int diff = appMouseEvent.getMousePosition().getRow() - currentPos.getRow();
                    if (hd.canMoveDown(diff) ) {
                        hd.moveDown(diff);
                        return true;
                    }
                    return false;
                }
            }.freezeColumn();

            Draggable draggable = draggableMoveComponent;
            this.setDraggableBehaviour(draggable);
            this.hdivider = hd;
        }

        @Override
        public void draw(Color color, IScreenLineSegmentsBuffer segments) {
            super.draw(color, segments);
        }

        protected Position getPositionInFrameSet() {
            int vd_row = hdivider.getStartPositionOfHorizontalDivider().getRow();
            int vd_width = hdivider.getWidthOfHorizontalDivider();
            int widthOfMe = getWidth();
            int row = vd_row;
            int col = Math.max(hdivider.getStartPositionOfHorizontalDivider().getCol() + ((vd_width - widthOfMe)/2),hdivider.getStartPositionOfHorizontalDivider().getCol());
            return new Position.Simple(row, col);
        }
    }
}
