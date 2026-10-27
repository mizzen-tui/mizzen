package mizzen.examples.plot.gameoflife;

import java.util.StringJoiner;

import mizzen.components.BlockChars;
import mizzen.components.Component;
import mizzen.components.TextComponent;
import mizzen.components.composite.Composite;
import mizzen.controlcodes.color.Color;
import mizzen.controlcodes.color.Colors;
import mizzen.geom.Dimension;
import mizzen.util.StringUtils;

/**
 * Draw game into a simple TextComponent
 */
public class GameGridTextComponentRenderer implements GameGridRenderer {

    
    private Composite     container;
    private TextComponent textComponent;
    private GameBoardAsString textRenderer;
    private Dimension         desiredSize = Dimension.ZERO;
    
    static public interface GameBoardAsString {
        String array2DAsString(int[][] a);
        Dimension calcGameBoardDimension(Dimension dataDimension);
    }
    
    
    public GameGridTextComponentRenderer(Color color) {
        this(color, new GOLasString(Colors.BIYel));
    }

    public GameGridTextComponentRenderer(Color color, GameBoardAsString textRenderer) {
        container = new Composite(color);
        textComponent = new TextComponent(color,"");
        container.add(textComponent);
        this.textRenderer = textRenderer;
    }
    
    public void resizeComponent(Dimension d) {
        this.container.resize(d);
        this.textComponent.resize(d);
        this.desiredSize =  d;
    }

    public Dimension componentSizeToGridSize(Dimension d) {
        Dimension d2 = textRenderer.calcGameBoardDimension(d);
        float heightRatio = (float)d.getHeight() / (float)d2.getHeight(); 
        float widthRatio =  (float)d.getWidth()  / (float)d2.getWidth();
        return new Dimension.Simple((int)Math.round( Math.floor(d.getHeight()*heightRatio)), (int)Math.round(Math.floor(d.getWidth()*widthRatio)));
    }

    @Override
    public void render(GridData gridContainer) {
        this.textComponent.setTextAndResize(textRenderer.array2DAsString(gridContainer.getGridData()));
        this.textComponent.resize(this.desiredSize);
    }

    @Override
    public Component<?> getComponent() {
        return container;
    }

    // ---------- 
    
    public static class GOLasString implements GameBoardAsString {

        public static char GRID_CELL_DIVIDER         = ' ';

        Color lifeColor = Colors.DEFAULT_COLOR;

        public GOLasString() {
            this.lifeColor = Colors.DEFAULT_COLOR;
        }

        public GOLasString(Color color) {
            this.lifeColor = color;
        }
        
        protected String cellAsString(int cellValue) {
           if (cellValue == 0) {
              return " ";
           }
           return Colors.coloredText(this.lifeColor,"▖");
        }

        
        private String array1DAsString(int[] a) {
            StringJoiner  columnJoiner = new StringJoiner(GRID_CELL_DIVIDER+"");
            for (int i=0; i<a.length;i++) {
                columnJoiner.add(cellAsString(a[i])+"");
            }
            return columnJoiner.toString();
        }

        public Dimension calcGameBoardDimension(Dimension dataDimension) {
            return new Dimension.Simple(dataDimension.getHeight(), dataDimension.getWidth()*2 - 1) ;
        }

        public String array2DAsString(int[][] a) {
            StringJoiner  rowJoiner = new StringJoiner("\n");
            for (int i=0; i<a.length; i++) {
                rowJoiner.add(array1DAsString(a[i]));
            }
            return rowJoiner.toString();
        }
    }

    public static class BoxStringRender implements GameBoardAsString {

        private static final BlockChars blockChars = new BlockChars.Nice();

        private static final String   default_cell_live = new String(new char[] {blockChars.getTOP_LEFT(), blockChars.getHORIZONTAL_LINE(), blockChars.getTOP_RIGHT()}) + "\n" +
                                                          new String(new char[] {blockChars.getBOTTOM_LEFT(), blockChars.getHORIZONTAL_LINE(), blockChars.getBOTTOM_RIGHT()});
        
        private static final String   default_cell_empty =  "   " + "\n" + 
                                                            "   ";

        Color lifeColor = Colors.DEFAULT_COLOR;
        Dimension cellSize;
        
        String[] cell_live; 
        String[] cell_empty; 

        public BoxStringRender() {
            this(Colors.DEFAULT_COLOR);
        }

        public BoxStringRender(Color color, String cellLive, String cellEmpty) {
            this.lifeColor = color;
            cell_live = cellLive.split("\n");
            cell_empty = cellEmpty.split("\n");
            this.cellSize =  new Dimension.Simple(cell_live.length, cell_live[0].length());
        }

        public BoxStringRender(Color color, String cellLive) {
            this(color, cellLive, cellLive);
            StringJoiner sj = new StringJoiner("\n");
            for (int row = 0; row<cellSize.getHeight(); row++) {
                sj.add(StringUtils.repeat(' ', cellSize.getWidth()) );
            }
            this.cell_empty = sj.toString().split("\n");
        }

        public BoxStringRender(Color color) {
            this(color, default_cell_live, default_cell_empty);
        }
        
        private String cellAsString(int cellValue, int row) {
           if (cellValue == 0) {
              return cell_empty[row];
           }
           return Colors.coloredText(this.lifeColor,cell_live[row]);
        }

        @Override
        public Dimension calcGameBoardDimension(Dimension dataDimension) {
            return new Dimension.Simple(dataDimension.getHeight()*cellSize.getHeight(), dataDimension.getWidth()*cellSize.getWidth()) ;
        }

        @Override
        public String array2DAsString(int[][] dataGrid) {
            int heightOfOneCell = cellSize.getHeight();
            StringBuilder sb = new StringBuilder();
            for (int rowInDataGrid=0; rowInDataGrid<dataGrid.length; rowInDataGrid++) {
                for (int rowInCell = 0; rowInCell < heightOfOneCell; rowInCell++) {
                    for (int columnInDataGrid=0; columnInDataGrid<dataGrid[rowInDataGrid].length;columnInDataGrid++) {
                        sb.append(cellAsString(dataGrid[rowInDataGrid][columnInDataGrid], rowInCell));
                    }
                    if (rowInDataGrid != dataGrid.length+1) {
                        sb.append("\n");
                    }
                }
            }
            return sb.toString();
        }

    }

}