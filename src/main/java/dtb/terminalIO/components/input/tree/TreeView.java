package dtb.terminalIO.components.input.tree;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import dtb.terminalIO.components.BlockChars;
import dtb.terminalIO.components.BoundedComponent;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.GetComponentAtOffset;
import dtb.terminalIO.components.HasName;
import dtb.terminalIO.components.ParentContainer;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.textsegments.IScreenLineSegmentsBuffer;
import dtb.terminalIO.util.StringUtils;

//  TODO:  This control needs a lot of work before its considered done ..
public class TreeView<T> extends BoundedComponent<BoundedComponent<?>> 
                         implements ParentContainer,  
                                    GetComponentAtOffset {

    TreeViewModel<T> model;
    Function<T, String> nodeAsString;
    List<BoundedComponent<?>> children;

    public TreeView(Color color, Dimension d, Function<T, String> nodeAsString) {
        super(color, d);
        this.nodeAsString = nodeAsString;
        this.children = new ArrayList<BoundedComponent<?>>();
    }
    
    public TreeView(Color color, Dimension d) {
        this(color, d, (node)->String.valueOf(node));
    }

    public void setModel(TreeViewModel<T> model) {
        this.model = model;
        buildFromModel();
        this.clear();
        this.markAsDirty();
    }

    private boolean isLastChild(T parent, T child) {
        int parentChildCount = model.getChildCount(parent);
        int childIdx = model.getIndexOfChild(parent, child);
        return childIdx == parentChildCount-1;
    }
    
    private int drawNode(Color color, 
                         T node, 
                         int callCount,
                         List<T> ancestor) {  //ancestor.get(0) is parent node

        int newCallCount = callCount;  // needed to track treeview row

        String _branchComponentText = "";
        int depth = ancestor.size();
        if (depth > 0) {
            BlockChars blockChars = this.getBlockChars();
            String shortHLine = StringUtils.repeat(blockChars.getHORIZONTAL_LINE(),2);
            T currentNode = node;
            for (int i = 0; i < depth; i++) {
                T parent = ancestor.get(i);
                boolean isLastChild = isLastChild(parent, currentNode);
                if (i == 0) {
                   _branchComponentText = " "+(isLastChild ? blockChars.getBOTTOM_LEFT() : blockChars.getVERTICAL_RIGHT()) + shortHLine + _branchComponentText;
                } else {
                    _branchComponentText = " "+(isLastChild ? "    " : (""+blockChars.getVERTICAL_LINE() + "   ") ) + _branchComponentText;
                }
                currentNode = parent;
            }
            _branchComponentText += " ";
        }
        String output = nodeAsString.apply(node);
        int theRow = getRow() + newCallCount;
        int col = getCol();
        String branchComponentText = _branchComponentText;
        if ( ! StringUtils.isEmptyStr(_branchComponentText)) {
            col +=  makeChildComponent(()->  new TextComponent(color, theRow, getCol(), branchComponentText)).getWidth()-1;
        }
        int theNodeComponentCol = col;
        // makeChildComponent(() -> new Button(color, theRow, theNodeComponentCol, output, false));
        makeChildComponent(() -> new TextComponent(color, theRow, theNodeComponentCol, output));

        int childCount = this.model.getChildCount(node);
        ancestor.add(0, node);

        for (int i = 0; i < childCount; i++) {
            T child = this.model.getChild(node, i);
            newCallCount = drawNode(color, child, newCallCount + 1, ancestor);
        }

        ancestor.remove(node);

        return newCallCount;

    }
    
    private void buildFromModel() {
        T root = model.getRoot();
        this.children.clear();
        drawNode(this.getColor(), root, 0, new ArrayList<T>());

        this.resize(Rectangle.UTIL.calcDimensionsWith(children));

    }
    
    protected <T extends BoundedComponent<?>> T makeChildComponent(Supplier<T> createComponent) {
        T child = super.makeChildComponent(createComponent);
        this.children.add(child);
        return child;
    }


    public void draw(Color color, IScreenLineSegmentsBuffer segments) {

        if (this.model == null) {
            throw new NullPointerException(
                    "Attempted to draw TreeView before setting TreeViewModel! Call setModel(..) first!");
        }

        for ( Component<?> child : children ) {
               segments.merge(child.getSegmentsBuffer());
        }

    }

    @Override
    public Collection<? extends Component<?>> getChildren() {
        return children;
    }

    @Override
    public Position getPositionRelativeToParent(Component<?> child) {
        if ( ! this.children.contains(child)) {
            throw new IllegalArgumentException(HasName.UTIL.getName(child) + " not a child of " + HasName.UTIL.getName(this));
        }
        return child.getPostion_TopLeft();
    }

    @Override
    public void childResized(Component<?> child) {
        this.markAsDirty();
    }

    @Override
    public Component<?> getComponentAtOffset(Position offset) {

        if ( ! this.containsPosition(offset)) {
            return null;
        }
        for (BoundedComponent<?> child : children) {
            if (child.containsPosition(offset)) {
                if (child instanceof GetComponentAtOffset) {
                    Position relativeOffset =  offset.offsetAdd(child.getPostion_TopLeft().negate());
                    return ((GetComponentAtOffset)child).getComponentAtOffset(relativeOffset);
                } else {
                    return child;
                }
            }
        }
        return this;
    }

}
