package dtb.terminalIO.components.table;

import java.util.function.Function;

import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.textsegments.CleanString;
import dtb.terminalIO.textsegments.IScreenLineSegmentsBuffer;
import dtb.terminalIO.util.ElapsedTime;
import dtb.terminalIO.util.Logging;
import dtb.terminalIO.util.StringUtils;

public interface TableColumnRenderer {

    IScreenLineSegmentsBuffer render(Object cellValue, int preferredWidth, int maxCellHeight);

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
        public IScreenLineSegmentsBuffer render(Object cellValue, int preferredWidth, int maxCellHeight) {
            if (clearComponentBeforeRendering) {
                Logging.info(" cell clear " + ElapsedTime.onRunOf(()-> componentForRendering.clear() ));

                // componentForRendering.clear();
            }

            Logging.info(" cell renderObjectOntoComponent " + ElapsedTime.onRunOf(()-> renderObjectOntoComponent(componentForRendering, cellValue, preferredWidth, maxCellHeight)));
            return  ElapsedTime.getAndTime(()-> componentForRendering.getSegmentsBuffer(),(t)-> Logging.info("Cell draw time: " + t));
        }

        protected T getComponent() {
            return this.componentForRendering;
        }

        protected abstract void renderObjectOntoComponent(Component<?> componentForRendering2, Object cellValue, int preferredWidth, int maxCellHeight);

    }

    public static final Function<Object, String> DEFAULT_CELL_TO_STRING = (cellValue) ->  (cellValue == null) ? "" : cellValue.toString();

    public static class TableColumnRendererFromTextComponent extends TableColumnRendererFromComponent<TextComponent>  {

        Function<Object, String> cellValueToString = DEFAULT_CELL_TO_STRING;

        public TableColumnRendererFromTextComponent() {
            super(new TextComponent(new CleanString.Simple("")), true);
        }

        public TableColumnRendererFromTextComponent(Color color, Function<Object, String> cellValueToString) {
            super(new TextComponent(color, new CleanString.Simple("")), true);
            this.cellValueToString = cellValueToString;
        }
        
        protected void setCellText(String text, int preferredWidth, int preferredHeight) {
            String theText = text;
            
            // wrap(String str, int wrapLength, String newLineStr, boolean wrapLongWords, int maxHeight)
            
            if (text != null && text.length() > preferredWidth) {
                theText = ElapsedTime.getAndTime(()-> StringUtils.wrap(text, preferredWidth, "\n", true, preferredHeight),
                                               (t)->Logging.info("wrap "+t))  ;
            }
            getComponent().setTextAndResize(theText);
        }

        protected void setCellColor(Color drawingcolor) {
            getComponent().setColor(drawingcolor);
        }

        @Override
        protected void renderObjectOntoComponent(Component<?> componentForRendering2, Object cellValue, int preferredWidth, int maxCellHeight) {
            String value = cellValueToString.apply(cellValue);
            setCellText(value, preferredWidth, maxCellHeight);
        }

    }
    
    
}
