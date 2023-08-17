package dtb.terminalIO.components;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import dtb.terminalIO.components.composite.Composite;
import dtb.terminalIO.components.composite.HorizontalPanel;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.textsegments.IScreenLineSegmentsBuffer;
import dtb.terminalIO.util.ElapsedTime;
import dtb.terminalIO.util.Logging;



public class FrameSet extends BoundedComponent<FrameSet> implements HasTitleAndBorder<FrameSet>,  ParentContainer, GetComponentAtOffset {

    Composite container;

    List<VerticalDivider> vDividers = new ArrayList<VerticalDivider>();
    List<HorizontalDivider> hDividers = new ArrayList<HorizontalDivider>();

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
        
        public String toString() {
            return String.format("VerticalDivider[left=(%s),right=(%s)]",String.valueOf(left), String.valueOf(right));
        }
    }

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

        public String toString() {
            return String.format("HorizontalDivider[above=(%s),below=(%s)]",String.valueOf(above), String.valueOf(below));
        }
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

    public FrameSet addRight(Component<?> component, Component<?> anchorComponent) {
        container.addRight(component, anchorComponent, 1);
        vDividers.add(new VerticalDivider(anchorComponent, component));
        return this;
    }

    public FrameSet addBelow(Component<?> component, Component<?> anchorComponent) {
        container.addBelow(component, anchorComponent, 1);
        hDividers.add(new HorizontalDivider(anchorComponent, component));
        return this;
    }

    public FrameHorizontalPanel addRowBelow(Component<?> anchorComponent) {
        FrameHorizontalPanel row = new  FrameHorizontalPanel(this.getColor());
        this.addBelow(row, anchorComponent);
        return row;
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
    
    public void applyResizeRules() {
        this.container.applyResizeRules();
    }
    
    
    @Override
    protected void draw(Color color, IScreenLineSegmentsBuffer mySegments) {
        ElapsedTime drawContainerTime = new ElapsedTime();
        if (segments.isEmpty()) {
            // fast!
            segments.getSegments().addAll(container.getSegmentsBuffer().getSegments());
         } else {
            // slow - but safe
            segments.merge(container.getSegmentsBuffer().getSegments());
         }
        
        Logging.info(String.format("Parts Frameset container draw: #segments %d   time:  %d",  
                                   container.getSegmentsBuffer().getSegments().size(), 
                                   drawContainerTime.elapsed() ));

        Rectangle bounds = new Rectangle.Simple(this);

        Component.UTIL.assertSegmentsIntegrity(bounds, mySegments.getSegments());

        int iCount = 0;

        
        // please note:  There is a bug in the line drawing such that it favors drawing vertical lines first 
        //               and then horizontal lines 2nd.  If you do the reverse, the code might not the correct
        //               intersection character.
        //boolean doHorizonal = true;
        boolean doHorizonal = false;
        
        if (doHorizonal) {
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
//        if (1+1==2) return;

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

        for (Component<?> child : this.container.getChildren()) {
            if (this.container.isChildVisible(child) && child instanceof FrameHorizontalPanel) {
                for (VerticalLineSpec vlineSpec : ((FrameHorizontalPanel)child).vLines()) {
                    BlockChars.UTIL.drawVerticalLine(mySegments, this.getColor(), this.getBlockChars(), vlineSpec.start, vlineSpec.height);
                    Component.UTIL.assertSegmentsIntegrity(bounds, mySegments.getSegments());
                }
            }
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

        Logging.info(String.format("Parts Frameset full draw: #segments %d   time:  %d",  
                mySegments.getSegments().size(), 
                drawContainerTime.recalcElapsed() ));
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
        return Collections.singleton(this.container);
    }


    @Override
    public Position getPositionRelativeToParent(Component<?> child) {
        return Position.ZERO;
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
        return this.container.getComponentAtOffset(offset);
    }


    static class VerticalLineSpec {
        Position start;
        int height;
        public VerticalLineSpec(Position start, int height) {
            super();
            this.start = start;
            this.height = height;
        }
    }
    
    public class FrameHorizontalPanel extends AbstractComponentWithContainer<HorizontalPanel> {

        public FrameHorizontalPanel(Color color) {
            super(color, new HorizontalPanel(color,1));
        }

        List<VerticalLineSpec> vLines() {
            return Collections.emptyList();
        }
        
    }
    
    

    
}
