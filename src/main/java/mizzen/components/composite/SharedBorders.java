package mizzen.components.composite;

import java.util.ArrayList;
import java.util.List;

import mizzen.components.BlockChars;
import mizzen.controlcodes.color.Color;
import mizzen.geom.Rectangle;
import mizzen.geom.position.Position;
import mizzen.textsegments.IScreenLineSegmentsBuffer;
import mizzen.textsegments.ScreenLineSegment;

/**
 * Borders shared between neighbouring components (docs/ARCHITECTURE.md, "Shared borders"): after a container has drawn
 * its children, each child's box edge that sits next to a line is moved one cell outward onto that line and joined
 * with it - so two boxes side by side, or a box inside a bordered panel, show one line between them, not two.
 * <pre>
 *     ┌ Example ─┐ ┌ Docs ─┐            ┌ Example ──┬─ Docs ─┐       the gap between them is their shared line;
 *     │          │ │       │     →      │           │        │       FrameSet's divider, drawn over it, joins it
 *     └──────────┘ └───────┘            └───────────┴────────┘
 * </pre>
 * The children are not touched: no size or position changes, so nothing is laid out again. What moves is their drawing,
 * as the container copies it: the edge's cells go one cell out, and the cells they leave become a blank inside (a
 * column or row of padding) - the corners' rows and columns carry the line on, so the box stays closed.
 * <p/>
 * Which children have a box is read from what they drew: a side is an edge when its two corner cells are that side's
 * corners (┌ and └ for the left). A child whose top corners aren't corners (a tab panel, its tab strip on top) has the
 * box lower down that reaches its sides and bottom: its frame. So a component that holds a bordered one filling it (a plain panel around a bordered
 * slot) counts, and a scroll pane whose scrollbar sits on its border column moves it with the column.
 * <p/>
 * An edge moves onto: the container's own border; the side of another child's box it sits right inside (a tab panel's
 * frame - a top only onto a plain line, not one carrying tabs' joins); a line its owner draws over it later (a FrameSet's divider - see
 * Composite.sharedLinesAt); a bordered neighbour's edge right next to it (only the one on the right, or below, moves);
 * a gap of one cell between two bordered neighbours (both move into it). It doesn't move when
 * both lines carry something of their own there (two titles: the developer asked for both), or onto a child kept on
 * top or a popup (other layers).
 */
final class SharedBorders {

    private SharedBorders() {
    }

    enum Side { LEFT, RIGHT, TOP, BOTTOM }

    /** a child's edge moved onto a line: clicks on that line go to the child, at its edge */
    static final class Move {
        final Object child;
        final Side side;
        final Rectangle box;                                 // the child's box before moving, in the container's cells

        Move(Object child, Side side, Rectangle box) {
            this.child = child;
            this.side = side;
            this.box = box;
        }

        /** the cell of the child this shared-line cell came from; null: not on this move's line */
        Position sourceOf(int row, int col) {
            int top = box.getRow(), left = box.getCol(), bottom = box.getBottomMostRow(), right = box.getRightMostCol();
            switch (side) {
                case LEFT:   return col == left - 1 && row >= top - 1 && row <= bottom + 1 ? new Position.Simple(clamp(row, top, bottom), left) : null;
                case RIGHT:  return col == right + 1 && row >= top - 1 && row <= bottom + 1 ? new Position.Simple(clamp(row, top, bottom), right) : null;
                case TOP:    return row == top - 1 && col >= left - 1 && col <= right + 1 ? new Position.Simple(top, clamp(col, left, right)) : null;
                default:     return row == bottom + 1 && col >= left - 1 && col <= right + 1 ? new Position.Simple(bottom, clamp(col, left, right)) : null;
            }
        }

        private static int clamp(int v, int least, int most) {
            return Math.max(least, Math.min(most, v));
        }
    }

    /** a child taking part: its box in the container's cells */
    static final class Box {
        final Object child;
        final Rectangle rect;

        Box(Object child, Rectangle rect) {
            this.child = child;
            this.rect = rect;
        }

        int top()    { return rect.getRow(); }
        int left()   { return rect.getCol(); }
        int bottom() { return rect.getBottomMostRow(); }
        int right()  { return rect.getRightMostCol(); }
    }

    /**
     * Moves the children's edges onto the lines next to them, in segments (the container's drawing, its children already
     * in it). own: the container's border box, in the same cells; null: it has none. ownTitled: its top line has a title.
     * style: the container's BlockChars - it picks the joining characters. Returns the moves, for clicks.
     */
    static List<Move> share(IScreenLineSegmentsBuffer segments, List<Box> children, Rectangle own, boolean ownTitled, BlockChars style,
                            List<Rectangle> lines) {
        List<Box> boxes = new ArrayList<Box>();
        for (Box b : children) {
            if (b.rect.getWidth() >= 2 && b.rect.getHeight() >= 2) {
                boxes.add(boxOf(segments, b));
            }
        }
        // decide first, on the drawing as it is; then move - the sides across, then the top and bottom over the
        // widened rows, so a corner that moves both ways ends on the diagonal
        List<Move> moves = new ArrayList<Move>();
        for (Box b : boxes) {
            for (Side side : Side.values()) {
                if (isEdge(segments, b, side) && (hasLineOutside(segments, b, side, boxes, own, ownTitled) || besideALine(b, side, lines))) {
                    moves.add(new Move(b.child, side, b.rect));
                }
            }
        }
        // each box with a side to share is drawn again, larger: first every one's old outline wiped (its text kept to
        // carry over), then every one's new outline joined with what is there - wiped first, so no box's join is lost
        // to another's wipe, and joining is the same in any order
        List<Outline> outlines = new ArrayList<Outline>();
        for (Box b : boxes) {
            Outline o = Outline.of(segments, b, moves);
            if (o != null) {
                outlines.add(o);
            }
        }
        for (Outline o : outlines) {
            o.wipe(segments);
        }
        for (Outline o : outlines) {
            o.draw(segments, style);
        }
        return moves;
    }

    /**
     * The box a child drew: all of it, or - when its top corners aren't corners (a tab panel: its top rows are its tab
     * strip) - the first box lower down that reaches its sides and bottom (the tab panel's frame). Above that, its
     * drawing is left as it is.
     */
    private static Box boxOf(IScreenLineSegmentsBuffer s, Box b) {
        if (isEdge(s, b, Side.TOP) || !isEdge(s, b, Side.BOTTOM)) {
            return b;
        }
        int up = BlockChars.UTIL.UP, down = BlockChars.UTIL.DOWN, left = BlockChars.UTIL.LEFT, right = BlockChars.UTIL.RIGHT;
        for (int row = b.top() + 1; row < b.bottom(); row++) {
            if (reaches(s, row, b.left(), right | down) && reaches(s, row, b.right(), left | down)) {
                return new Box(b.child, new Rectangle.Simple(row, b.left(), b.bottom() - row + 1, b.rect.getWidth()));
            }
        }
        return b;
    }

    // ---- which sides --------------------------------------------------------------------------------------------

    // its two corners on that side are that side's corners: ┌ └ on the left (a joined ├ counts: it reaches the same ways)
    private static boolean isEdge(IScreenLineSegmentsBuffer s, Box b, Side side) {
        int up = BlockChars.UTIL.UP, down = BlockChars.UTIL.DOWN, left = BlockChars.UTIL.LEFT, right = BlockChars.UTIL.RIGHT;
        switch (side) {
            case LEFT:   return reaches(s, b.top(), b.left(), right | down) && reaches(s, b.bottom(), b.left(), right | up);
            case RIGHT:  return reaches(s, b.top(), b.right(), left | down) && reaches(s, b.bottom(), b.right(), left | up);
            case TOP:    return reaches(s, b.top(), b.left(), right | down) && reaches(s, b.top(), b.right(), left | down);
            default:     return reaches(s, b.bottom(), b.left(), right | up) && reaches(s, b.bottom(), b.right(), left | up);
        }
    }

    private static boolean reaches(IScreenLineSegmentsBuffer s, int row, int col, int arms) {
        return (BlockChars.UTIL.armsOf(charAt(s, row, col)) & arms) == arms;
    }

    private static boolean hasLineOutside(IScreenLineSegmentsBuffer s, Box b, Side side, List<Box> boxes, Rectangle own, boolean ownTitled) {
        // the container's own border, right outside
        if (own != null) {
            boolean onOwn = side == Side.LEFT ? b.left() - 1 == own.getCol()
                          : side == Side.RIGHT ? b.right() + 1 == own.getRightMostCol()
                          : side == Side.TOP ? b.top() - 1 == own.getRow()
                          : b.bottom() + 1 == own.getBottomMostRow();
            if (onOwn) {
                return !(side == Side.TOP && ownTitled && hasText(s, b, side));    // two titles: two lines
            }
        }
        for (Box o : boxes) {
            if (o != b && encloses(o, b, side) && isEdge(s, o, side)) {
                // inside another child's box, against its side: that side is its line, as a panel's own border is - a top
                // only onto a plain line (a tab panel's frame top carries its tabs' joins: a title would land in them)
                if (side == Side.TOP && !plainTop(s, o, b)) {
                    return false;
                }
                return !(hasText(s, b, side) && hasText(s, o, side));
            }
        }
        for (Box o : boxes) {
            if (o == b) {
                continue;
            }
            boolean overlap = side == Side.LEFT || side == Side.RIGHT ? o.top() <= b.bottom() && o.bottom() >= b.top()
                                                                      : o.left() <= b.right() && o.right() >= b.left();
            if (!overlap) {
                continue;
            }
            Side facing = side == Side.LEFT ? Side.RIGHT : side == Side.RIGHT ? Side.LEFT : side == Side.TOP ? Side.BOTTOM : Side.TOP;
            if (!isEdge(s, o, facing)) {
                continue;
            }
            int gap = side == Side.LEFT ? b.left() - o.right() - 1
                    : side == Side.RIGHT ? o.left() - b.right() - 1
                    : side == Side.TOP ? b.top() - o.bottom() - 1
                    : o.top() - b.bottom() - 1;
            boolean ontoTheirs = gap == 0 && (side == Side.LEFT || side == Side.TOP);      // touching: only one moves
            if ((gap == 1 && !anotherIn(boxes, b, o, side)) || ontoTheirs) {
                return !(hasText(s, b, side) && hasText(s, o, facing));                    // two titles: two lines
            }
        }
        return false;
    }

    // o's box holds b, b's side right inside o's same side
    private static boolean encloses(Box o, Box b, Side side) {
        boolean holds = o.top() < b.top() && o.bottom() > b.bottom() && o.left() < b.left() && o.right() > b.right();
        boolean against = side == Side.LEFT ? o.left() == b.left() - 1 : side == Side.RIGHT ? o.right() == b.right() + 1
                        : side == Side.TOP ? o.top() == b.top() - 1 : o.bottom() == b.bottom() + 1;
        return holds && against;
    }

    // o's top line above b is only line: ─ all along
    private static boolean plainTop(IScreenLineSegmentsBuffer s, Box o, Box b) {
        int across = BlockChars.UTIL.LEFT | BlockChars.UTIL.RIGHT;
        for (int col = b.left(); col <= b.right(); col++) {
            if (BlockChars.UTIL.armsOf(charAt(s, o.top(), col)) != across) {
                return false;
            }
        }
        return true;
    }

    // a line drawn over the container later (a FrameSet's divider) right outside the side, along all of it
    private static boolean besideALine(Box b, Side side, List<Rectangle> lines) {
        for (Rectangle line : lines) {
            boolean beside = side == Side.LEFT ? line.getRightMostCol() == b.left() - 1 && line.getWidth() == 1
                           : side == Side.RIGHT ? line.getCol() == b.right() + 1 && line.getWidth() == 1
                           : side == Side.TOP ? line.getBottomMostRow() == b.top() - 1 && line.getHeight() == 1
                           : line.getRow() == b.bottom() + 1 && line.getHeight() == 1;
            boolean along = side == Side.LEFT || side == Side.RIGHT ? line.getRow() <= b.top() && line.getBottomMostRow() >= b.bottom()
                                                                    : line.getCol() <= b.left() && line.getRightMostCol() >= b.right();
            if (beside && along) {
                return true;
            }
        }
        return false;
    }

    // a third child in the one-cell gap between b and o (then it is not theirs to share)
    private static boolean anotherIn(List<Box> boxes, Box b, Box o, Side side) {
        for (Box x : boxes) {
            if (x == b || x == o) {
                continue;
            }
            int line = side == Side.LEFT ? b.left() - 1 : side == Side.RIGHT ? b.right() + 1 : side == Side.TOP ? b.top() - 1 : b.bottom() + 1;
            boolean across = side == Side.LEFT || side == Side.RIGHT ? x.left() <= line && x.right() >= line && x.top() <= b.bottom() && x.bottom() >= b.top()
                                                                     : x.top() <= line && x.bottom() >= line && x.left() <= b.right() && x.right() >= b.left();
            if (across) {
                return true;
            }
        }
        return false;
    }

    // something other than line on that side between its corners: a title, a footer
    private static boolean hasText(IScreenLineSegmentsBuffer s, Box b, Side side) {
        boolean across = side == Side.TOP || side == Side.BOTTOM;
        int fixed = side == Side.LEFT ? b.left() : side == Side.RIGHT ? b.right() : side == Side.TOP ? b.top() : b.bottom();
        int from = across ? b.left() + 1 : b.top() + 1;
        int to = across ? b.right() - 1 : b.bottom() - 1;
        for (int i = from; i <= to; i++) {
            char ch = across ? charAt(s, fixed, i) : charAt(s, i, fixed);
            if (ch != 0 && ch != ' ' && BlockChars.UTIL.armsOf(ch) == 0) {
                return true;
            }
        }
        return false;
    }

    // ---- moving -----------------------------------------------------------------------------------------------

    /** a box drawn again one cell larger on its shared sides: its new outline, and the text on its old one */
    private static final class Outline {
        final Rectangle was;                                 // the box as its child drew it
        final Rectangle now;                                 // larger on the shared sides
        final char[][] edgeChars;                            // the old outline's cells, by side: [side][i]
        final Color[][] edgeColors;
        final Color lineColor;                               // its line's colour: its top-left corner's
        final boolean[] movedSide = new boolean[4];          // left, right, top, bottom
        boolean heavy;                                       // drawn double (focused): its new pieces double too

        private Outline(Rectangle was, Rectangle now, char[][] edgeChars, Color[][] edgeColors, Color lineColor) {
            this.was = was;
            this.now = now;
            this.edgeChars = edgeChars;
            this.edgeColors = edgeColors;
            this.lineColor = lineColor;
        }

        static Outline of(IScreenLineSegmentsBuffer s, Box b, List<Move> moves) {
            boolean left = moved(moves, b.child, Side.LEFT), right = moved(moves, b.child, Side.RIGHT);
            boolean top = moved(moves, b.child, Side.TOP), bottom = moved(moves, b.child, Side.BOTTOM);
            if (!left && !right && !top && !bottom) {
                return null;
            }
            Rectangle was = b.rect;
            Rectangle now = new Rectangle.Simple(was.getRow() - (top ? 1 : 0), was.getCol() - (left ? 1 : 0),
                                                 was.getHeight() + (top ? 1 : 0) + (bottom ? 1 : 0),
                                                 was.getWidth() + (left ? 1 : 0) + (right ? 1 : 0));
            int h = was.getHeight(), w = was.getWidth();
            char[][] chars = new char[4][];
            Color[][] colors = new Color[4][];
            chars[0] = new char[h]; colors[0] = new Color[h];        // left, by row
            chars[1] = new char[h]; colors[1] = new Color[h];        // right
            chars[2] = new char[w]; colors[2] = new Color[w];        // top, by column
            chars[3] = new char[w]; colors[3] = new Color[w];        // bottom
            for (int i = 0; i < h; i++) {
                chars[0][i] = charAt(s, was.getRow() + i, was.getCol());
                colors[0][i] = colorAt(s, was.getRow() + i, was.getCol());
                chars[1][i] = charAt(s, was.getRow() + i, was.getRightMostCol());
                colors[1][i] = colorAt(s, was.getRow() + i, was.getRightMostCol());
            }
            for (int i = 0; i < w; i++) {
                chars[2][i] = charAt(s, was.getRow(), was.getCol() + i);
                colors[2][i] = colorAt(s, was.getRow(), was.getCol() + i);
                chars[3][i] = charAt(s, was.getBottomMostRow(), was.getCol() + i);
                colors[3][i] = colorAt(s, was.getBottomMostRow(), was.getCol() + i);
            }
            Outline outline = new Outline(was, now, chars, colors, colorAt(s, was.getRow(), was.getCol()));
            outline.movedSide[0] = left;
            outline.movedSide[1] = right;
            outline.movedSide[2] = top;
            outline.movedSide[3] = bottom;
            outline.heavy = BlockChars.NICE_DOUBLE.isBlockChar(charAt(s, was.getRow(), was.getCol()))
                         || BlockChars.NICE_DOUBLE.isBlockChar(charAt(s, was.getBottomMostRow(), was.getRightMostCol()));
            return outline;
        }

        // the moved sides' old cells: inside now (blank, in the colour of the inside next to them) - except an end that
        // is a corner of a side not moved: that side's line carries on through it to the new corner
        void wipe(IScreenLineSegmentsBuffer s) {
            int top = was.getRow(), left = was.getCol(), bottom = was.getBottomMostRow(), right = was.getRightMostCol();
            int across = BlockChars.UTIL.LEFT | BlockChars.UTIL.RIGHT, along = BlockChars.UTIL.UP | BlockChars.UTIL.DOWN;
            if (movedSide[0] || movedSide[1]) {
                for (int side = 0; side <= 1; side++) {
                    if (!movedSide[side]) {
                        continue;
                    }
                    int col = side == 0 ? left : right, inside = side == 0 ? Math.min(left + 1, right) : Math.max(right - 1, left);
                    for (int row = top; row <= bottom; row++) {
                        boolean end = row == top || row == bottom;
                        boolean endSideMoved = row == top ? movedSide[2] : movedSide[3];
                        if (end && !endSideMoved) {
                            put(s, row, col, BlockChars.UTIL.lineFor(heavy ? BlockChars.NICE_DOUBLE : BlockChars.NICE, across), lineColor);   // the top/bottom line goes on
                        } else if (!end && lineRunsInward(s, row, col, side == 0 ? BlockChars.UTIL.RIGHT : BlockChars.UTIL.LEFT, row, inside)) {
                            carryOn(s, row, col, across);                  // a line met the old edge here (├): it reaches the new one
                        } else {
                            blank(s, row, col, row, inside);
                        }
                    }
                }
            }
            for (int side = 2; side <= 3; side++) {
                if (!movedSide[side]) {
                    continue;
                }
                int row = side == 2 ? top : bottom, inside = side == 2 ? Math.min(top + 1, bottom) : Math.max(bottom - 1, top);
                for (int col = left; col <= right; col++) {
                    boolean end = col == left || col == right;
                    boolean endSideMoved = col == left ? movedSide[0] : movedSide[1];
                    if (end && !endSideMoved) {
                        put(s, row, col, BlockChars.UTIL.lineFor(heavy ? BlockChars.NICE_DOUBLE : BlockChars.NICE, along), lineColor);       // the side line goes on
                    } else if (!end && lineRunsInward(s, row, col, side == 2 ? BlockChars.UTIL.DOWN : BlockChars.UTIL.UP, inside, col)) {
                        carryOn(s, row, col, along);
                    } else {
                        blank(s, row, col, inside, col);
                    }
                }
            }
        }

        // the old edge's cell reaches inward (├ where a line inside met it) and the cell inside it reaches back: a line
        // comes in there - not a footer's ┤, whose stub up meets text
        private boolean lineRunsInward(IScreenLineSegmentsBuffer s, int row, int col, int inward, int insideRow, int insideCol) {
            int back = inward == BlockChars.UTIL.RIGHT ? BlockChars.UTIL.LEFT : inward == BlockChars.UTIL.LEFT ? BlockChars.UTIL.RIGHT
                     : inward == BlockChars.UTIL.DOWN ? BlockChars.UTIL.UP : BlockChars.UTIL.DOWN;
            return (BlockChars.UTIL.armsOf(charAt(s, row, col)) & inward) != 0
                && (BlockChars.UTIL.armsOf(charAt(s, insideRow, insideCol)) & back) != 0;
        }

        // the line that came in carries on across the cell, in its own colour and weight
        private void carryOn(IScreenLineSegmentsBuffer s, int row, int col, int arms) {
            char was = charAt(s, row, col);
            BlockChars weight = BlockChars.NICE_DOUBLE.isBlockChar(was) ? BlockChars.NICE_DOUBLE : BlockChars.NICE;
            Color color = colorAt(s, row, col);
            put(s, row, col, BlockChars.UTIL.lineFor(weight, arms), color != null ? color : lineColor);
        }

        private void blank(IScreenLineSegmentsBuffer s, int row, int col, int insideRow, int insideCol) {
            Color inside = colorAt(s, insideRow, insideCol);
            put(s, row, col, ' ', inside != null ? inside : lineColor);
        }

        // each moved side's line, one cell out, from corner to corner of the larger box - joined with what is there; where
        // the old edge had text (a title, a scrollbar) that text, carried across
        void draw(IScreenLineSegmentsBuffer s, BlockChars style) {
            int top = now.getRow(), left = now.getCol(), bottom = now.getBottomMostRow(), right = now.getRightMostCol();
            int up = BlockChars.UTIL.UP, down = BlockChars.UTIL.DOWN, l = BlockChars.UTIL.LEFT, r = BlockChars.UTIL.RIGHT;
            for (int side = 0; side <= 1; side++) {
                if (!movedSide[side]) {
                    continue;
                }
                int col = side == 0 ? left : right, toward = side == 0 ? r : l;
                for (int row = top; row <= bottom; row++) {
                    int arms = (row > top ? up : 0) | (row < bottom ? down : 0) | (row == top || row == bottom ? toward : 0);
                    cell(s, row, col, arms, side, row - was.getRow(), style);
                }
            }
            for (int side = 2; side <= 3; side++) {
                if (!movedSide[side]) {
                    continue;
                }
                int row = side == 2 ? top : bottom, toward = side == 2 ? down : up;
                for (int col = left; col <= right; col++) {
                    int arms = (col > left ? l : 0) | (col < right ? r : 0) | (col == left || col == right ? toward : 0);
                    cell(s, row, col, arms, side, col - was.getCol(), style);
                }
            }
        }

        // side: which old edge the cell takes its text from; i: where along it (outside it, or its corners: none). Only
        // the plain line of that edge (─ along a top or bottom, │ down a side) is drawn again as the new line; anything
        // else on it is carried across as it was - text (a title), a scrollbar's thumb, a footer's separators (┤ │ ├)
        private void cell(IScreenLineSegmentsBuffer s, int row, int col, int arms, int side, int i, BlockChars style) {
            char[] chars = edgeChars[side];
            boolean inner = i > 0 && i < chars.length - 1;
            char old = inner ? chars[i] : 0;
            int plain = side <= 1 ? BlockChars.UTIL.UP | BlockChars.UTIL.DOWN : BlockChars.UTIL.LEFT | BlockChars.UTIL.RIGHT;
            if (old != 0 && BlockChars.UTIL.armsOf(old) != plain) {
                put(s, row, col, old, edgeColors[side][i]);           // carried: as it was
                return;
            }
            Color color = inner && edgeColors[side][i] != null ? edgeColors[side][i] : lineColor;
            char piece = BlockChars.UTIL.lineFor(heavy || BlockChars.NICE_DOUBLE.isBlockChar(old) ? BlockChars.NICE_DOUBLE : style, arms);
            moveCell(s, piece, color, row, col, style);
        }
    }

    private static boolean moved(List<Move> moves, Object child, Side side) {
        for (Move m : moves) {
            if (m.child == child && m.side == side) {
                return true;
            }
        }
        return false;
    }

    // a line piece onto a cell: two lines join (the heavier one's colour and weight - a focused frame is double - else
    // the piece's, the inner one's); onto anything else, the piece
    private static void moveCell(IScreenLineSegmentsBuffer s, char piece, Color color, int row, int col, BlockChars style) {
        char under = charAt(s, row, col);
        if (under != 0 && under != ' ' && BlockChars.UTIL.armsOf(under) == 0) {
            return;                                          // something carried there already (a neighbour's scrollbar): it stays
        }
        if (BlockChars.UTIL.armsOf(under) != 0) {
            boolean underDouble = BlockChars.NICE_DOUBLE.isBlockChar(under);
            boolean overDouble = BlockChars.NICE_DOUBLE.isBlockChar(piece);
            BlockChars joinStyle = underDouble || overDouble ? BlockChars.NICE_DOUBLE : style;
            Color joinColor = underDouble && !overDouble ? colorAt(s, row, col) : color;
            put(s, row, col, BlockChars.UTIL.join(under, piece, joinStyle), joinColor);
        } else {
            put(s, row, col, piece, color);
        }
    }

    private static char charAt(IScreenLineSegmentsBuffer s, int row, int col) {
        String text = s.getCharAt(row, col);
        return text == null || text.isEmpty() ? 0 : text.charAt(0);
    }

    private static Color colorAt(IScreenLineSegmentsBuffer s, int row, int col) {
        ScreenLineSegment segment = s.getAtCol(row, col);
        return segment == null ? null : segment.getColors();
    }

    private static void put(IScreenLineSegmentsBuffer s, int row, int col, char ch, Color color) {
        if (row < 0 || col < 0 || color == null) {
            return;
        }
        s.setCharAt(row, col, ch, color);
    }
}
