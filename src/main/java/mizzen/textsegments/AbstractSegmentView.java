package mizzen.textsegments;

import mizzen.controlcodes.color.Color;
import mizzen.geom.position.Position;

public abstract class AbstractSegmentView implements ScreenLineSegment {

    ScreenLineSegment sourceSegment;

    AbstractSegmentView(ScreenLineSegment sourceSegment) {
        this.sourceSegment = sourceSegment;
    }

    public Color getColor() {
        return sourceSegment.getColors();
    }

    int rememberedLength = -1; // will this become a problem?
    String _clippedText = null;

    @Override
    public int length() {
        if (rememberedLength == -1) {
            rememberedLength = this.getText().length();
        }
        return rememberedLength;
    }
    
    abstract protected Position offset();
    abstract protected int getMaxRightMostCol();

    @Override
    public int getRow() {
        int row = sourceSegment.getRow() - AbstractSegmentView.this.offset().getRow();
        return row < 0 ? 0 : row;
    }

    @Override
    public int getCol() {
        int col = sourceSegment.getCol() - AbstractSegmentView.this.offset().getCol();
        return col < 0 ? 0 : col;
    }

    public String toString() {
        return String.format("VS(row=%d,lcol=%d,rcol=%d,[id=%d]):'%s'", getRow(), getCol(), getRightMostCol(),
                             getId(), getText());
    }

    @Override
    public Color getColors() {
        return sourceSegment.getColors();
    }

    @Override
    public void moveTo(int row, int col) {
        rememberedLength = -1; // unremember my length
        _clippedText = null;
        throw new IllegalStateException("No implemented yet!");
    }

    @Override
    public int getId() {
        return sourceSegment.getId();
    }

    @Override
    public String getText() {

        if (_clippedText != null) {
            return _clippedText;
        }

        String sourceText = sourceSegment.getText();
        int textBeginIndex = 0;
        int srcLeftCol = sourceSegment.getCol();
        int viewerLeftCol = offset().getCol();
        if (viewerLeftCol > 0) {
            textBeginIndex = 0;
        }
        if (viewerLeftCol > srcLeftCol) {
            textBeginIndex = viewerLeftCol - srcLeftCol;
            //rightColOffsetClipLen = textBeginIndex;
        }
        int textEndIndex = sourceSegment.getWidth();
        int viewerRightCol = getMaxRightMostCol();

        if (srcLeftCol + textEndIndex > viewerRightCol) {
            textEndIndex = textEndIndex - ((srcLeftCol + textEndIndex) - viewerRightCol)+1;
        }


        if (textBeginIndex < 0 || textBeginIndex > textEndIndex)
            return " ";
        
        String clippedText = sourceText.substring(textBeginIndex, textEndIndex);
        _clippedText = clippedText;

        return clippedText;
    }

    @Override
    public ScreenLineSegment fromOffset(Position offset) {
        return new SegmentImpl(getColor(), new Position.Simple(this).offsetAdd(offset), getId(), this.getText());
    }

}
