package dtb.terminalIO.components.input;

/**
 * How a component you pick from (TreeView, TableComponent ...) shows its selected row while it
 * doesn't have focus. (With focus the row is hilited - its selected colour, inverted.)
 */
public enum UnfocusedSelection {
    /** in the selected colour, not inverted */
    DIM,
    /** "[Circle]" (a tree; a table shows DIM) */
    BRACKETS,
    /** "> Circle" (a tree; a table shows DIM) */
    ARROW,
    /** not marked */
    NONE
}
