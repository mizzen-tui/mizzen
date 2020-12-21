package dtb.terminalIO.components.input.tree;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import dtb.terminalIO.components.BlockChars;
import dtb.terminalIO.components.BoundedComponent;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.controlcodes.ControlCode.Color;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.textsegments.IScreenLineSegmentsBuffer;

//  TODO:  This control needs a lot of work before its considered done ..
public class TreeView<T> extends BoundedComponent<TextComponent> {

    TreeViewModel<T> model;
    Function<T, String> nodeAsString;

    public TreeView(Color color, Dimension d, Function<T, String> nodeAsString) {
        super(color, d);
        this.nodeAsString = nodeAsString;
    }
    
    public TreeView(Color color, Dimension d) {
        this(color, d, (node)->String.valueOf(node));
    }

    public void setModel(TreeViewModel<T> model) {
        this.model = model;
        this.clear();
        this.markAsDirty();
    }

    private int drawNode(Color color, 
                         IScreenLineSegmentsBuffer segments, 
                         T node, 
                         int depth, 
                         int callCount,
                         List<T> ancestor) {

        int newCallCount = callCount;

        String prefix = "";
        if (depth > 0) {
            BlockChars blockChars = this.getBlockChars();
            String shortHLine = String.valueOf(blockChars.getHORIZONTAL_LINE())
                              + String.valueOf(blockChars.getHORIZONTAL_LINE());
            for (int i = 0; i < depth; i++) {
                T parent = ancestor.get(i);
                int parentChildCount = model.getChildCount(parent);
                if (i + 1 == depth) { // end of branch
                    if (parentChildCount == 1 || model.getIndexOfChild(parent, node)==parentChildCount-1) {
                        prefix += blockChars.getBOTTOM_LEFT() + shortHLine;
                    } else {
                        prefix += blockChars.getVERTICAL_RIGHT() + shortHLine;
                    }
                } else { // middle of branch
                    if (parentChildCount > 1) {
                        prefix += blockChars.getVERTICAL_LINE() + "   ";
                    } else {
                        prefix += "    ";
                    }
                }
            }
            prefix += " ";
        }
        String output = /*newCallCount +*/ prefix + nodeAsString.apply(node);
        int theRow = getRow() + newCallCount;
        segments.merge(
                makeChildComponent(() -> new TextComponent(color, theRow, getCol(), output)).getSegmentsBuffer());

        int childCount = this.model.getChildCount(node);
        ancestor.add(node);
        depth++;
        for (int i = 0; i < childCount; i++) {
            T child = this.model.getChild(node, i);
            newCallCount = drawNode(color, segments, child, depth, newCallCount + 1, ancestor);
        }
        ancestor.remove(node);
        return newCallCount;

        // segments.merge(makeChildComponent(()-> new TextComponent(color, getRow(),
        // getCol() + 1, this.getBlockChars().getBOTTOM_LEFT() +
        // ""+this.getBlockChars().getHORIZONTAL_LINE() + " " +
        // String.valueOf(root))).getSegmentsBuffer() );

    }

    public void draw(Color color, IScreenLineSegmentsBuffer segments) {

        if (this.model == null) {
            throw new NullPointerException(
                    "Attempted to draw TreeView before setting TreeViewModel! Call setModel(..) first!");
        }
        T root = model.getRoot();

        drawNode(color, segments, root, 0, 0, new ArrayList<T>());

//        segments.merge(makeChildComponent(()->new CharComponent(color, this.getRow(), this.getCol(), '+' )).getSegmentsBuffer());
//        segments.merge(makeChildComponent(()->new CharComponent(color, this.getRow(), this.getCol(),this.getBlockChars().getTOP_LEFT())).getSegmentsBuffer());
        // segments.merge(makeChildComponent(()-> new TextComponent(color, getRow(),
        // getCol() + 1, this.getBlockChars().getBOTTOM_LEFT() +
        // ""+this.getBlockChars().getHORIZONTAL_LINE() + " " +
        // String.valueOf(root))).getSegmentsBuffer() );

    }

}
