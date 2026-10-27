package mizzen.components.table;

import java.util.function.Function;

import mizzen.components.Component;
import mizzen.components.TextComponent;
import mizzen.controlcodes.color.Color;
import mizzen.geom.Align;
import mizzen.textsegments.CleanString;
import mizzen.textsegments.IScreenLineSegmentsBuffer;
import mizzen.util.StringUtils;

/**
 * Defines how to render a table column
 */
public interface TableColumnRenderer {

    IScreenLineSegmentsBuffer render(TableColumn tableColumn, Object cellValue, int preferredWidth, int maxCellHeight, Align align);
    IScreenLineSegmentsBuffer renderHeader(TableColumn tableColumn, String headerValue, int preferredWidth, int maxCellHeight);

    /**
     * An abstract implementation of TableColumnRenderer that renders to an in memory Component with the notion that
     * we use the components built in draw function to render the cell values.
     * 
     * @see TableColumnRendererFromTextComponent
     */
    public static abstract class TableColumnRendererFromComponent<T extends Component<?>> implements TableColumnRenderer {

        T componentForRendering;
        boolean clearComponentBeforeRendering = true;

        public TableColumnRendererFromComponent(T componentForRendering,
                boolean clearComponentBeforeRendering) {
            super();
            this.componentForRendering = componentForRendering;
            this.clearComponentBeforeRendering = clearComponentBeforeRendering;
        }

        @Override
        public IScreenLineSegmentsBuffer renderHeader(TableColumn tableColumn, String cellValue, int preferredWidth, int maxCellHeight) {
            componentForRendering.clear();

            renderHeaderOntoComponent(componentForRendering, tableColumn, cellValue, preferredWidth, maxCellHeight, Align.CENTER);
            return componentForRendering.getSegmentsBuffer();
        }

        @Override
        public IScreenLineSegmentsBuffer render(TableColumn tableColumn, Object cellValue, int preferredWidth, int maxCellHeight, Align align) {
            componentForRendering.clear();

            renderObjectOntoComponent(componentForRendering, tableColumn, cellValue, preferredWidth, maxCellHeight, align);
            return componentForRendering.getSegmentsBuffer();
        }

        protected T getComponent() {
            return this.componentForRendering;
        }

        protected abstract void renderObjectOntoComponent(Component<?> componentForRendering2, TableColumn tableColumn, Object cellValue, int preferredWidth, int maxCellHeight, Align align);
        protected abstract void renderHeaderOntoComponent(Component<?> componentForRendering2, TableColumn tableColumn, String cellValue, int preferredWidth, int maxCellHeight, Align align);

    }

    public static final Function<Object, String> DEFAULT_CELL_TO_STRING = (cellValue) ->  (cellValue == null) ? "" : cellValue.toString();

    public static final TableColumnRenderer DEFAULT = new TableColumnRendererFromTextComponent(); 


    /**
     * An implementation of TableColumnRenderer that renders to an in memory TextComponent in order
     * to leverage TextComponent's rendering capabilities
     */
    public static class TableColumnRendererFromTextComponent extends TableColumnRendererFromComponent<TextComponent>  {

        Function<Object, String> cellValueToString = DEFAULT_CELL_TO_STRING;

        boolean wrapLongWords = true;
        boolean wrap = true;

        public TableColumnRendererFromTextComponent() {
            super(new TextComponent(new CleanString.Simple("")), true);
        }

        public TableColumnRendererFromTextComponent(Function<Object, String> cellValueToString) {
            super(new TextComponent(new CleanString.Simple("")), true);
            this.cellValueToString = cellValueToString;
        }

        public TableColumnRendererFromTextComponent setWrapLongWords(boolean wrapLongWords) {
            this.wrapLongWords = wrapLongWords;
            return this;
        }

        public TableColumnRendererFromTextComponent setWrap(boolean wrap) {
            this.wrap = wrap;
            return this;
        }

        public TableColumnRendererFromTextComponent(Color color, Function<Object, String> cellValueToString) {
            super(new TextComponent(color, new CleanString.Simple("")), true);
            this.cellValueToString = cellValueToString;
        }
        
        protected void setCellText(String text, int preferredWidth, int preferredHeight) {
            String theText = text;

            // wrap(String str, int wrapLength, String newLineStr, boolean wrapLongWords, int maxHeight)
            if (text != null && text.length() > preferredWidth && this.wrap) {
                theText = StringUtils.wrap(text, preferredWidth, "\n", wrapLongWords, preferredHeight);
            }
            getComponent().setTextAndResize(theText);
        }

        protected void setCellColor(Color drawingcolor) {
            getComponent().setColor(drawingcolor);
        }

        @Override
        protected void renderObjectOntoComponent(Component<?> componentForRendering2, TableColumn tableColumn, Object cellValue, int preferredWidth, int maxCellHeight, Align align) {
            String value = cellValueToString.apply(cellValue);

            // padAndAlignColumnData(TableColumn columnDataItem, String displayString, int allowedDataWidth, Align align )
            value =  TableColumn.UTIL.padAndAlignColumnData(tableColumn, value, preferredWidth, align); //  Align.UTIL.formatToAlignedData(value, preferredWidth, tableColumn.getAlign(), 0, tableColumn.isTruncationAllowed());

            setCellText(value, preferredWidth, maxCellHeight);
        }

        @Override
        protected void renderHeaderOntoComponent(Component<?> componentForRendering2, TableColumn tableColumn, String cellValue, int preferredWidth, int maxCellHeight, Align align) {
            renderObjectOntoComponent(componentForRendering2, tableColumn, cellValue, preferredWidth, maxCellHeight, align);
        }
    }


}
