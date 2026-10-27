package mizzen.examples.plot.gameoflife;

import mizzen.components.Component;
import mizzen.components.input.DisplayableValue.SimpleDisplayValue;
import mizzen.controlcodes.color.Color;
import mizzen.controlcodes.color.Colors;
import mizzen.examples.plot.gameoflife.GameGridTextComponentRenderer.BoxStringRender;
import mizzen.examples.plot.gameoflife.GameGridTextComponentRenderer.GOLasString;
import mizzen.geom.Dimension;

public interface GameGridRenderer {
    
    
    default Dimension componentSizeToGridSize(Dimension d) {
        return new Dimension.Simple(d);
    }

    void resizeComponent(Dimension d);

    public void render(GridData gridContainer);
    
    public Component<?> getComponent();

    
    @FunctionalInterface
    public static interface Builder {
        GameGridRenderer build(Color color); 
    }
    
    public static class RendererOption extends SimpleDisplayValue<Builder> {

        public RendererOption(String displayValue, Builder value) {
            super(displayValue, value);
        }
        
    }
    
    
    public static class UTIL {
        public static RendererOption[] standardGameRenderers() {
            RendererOption[] renderers = new RendererOption[] {
                new RendererOption("Small yellow",(color)-> new GameGridTextComponentRenderer(color, new GOLasString(Colors.BIYel))),
                new RendererOption("Small blue"  ,(color)-> new GameGridTextComponentRenderer(color, new GOLasString(Colors.BIBlu))),
                new RendererOption("Boxes"       ,(color)-> new GameGridTextComponentRenderer(color, new BoxStringRender(Colors.BIYel))),
                new RendererOption("X's"         ,(color)-> new GameGridTextComponentRenderer(color, new BoxStringRender(Colors.BIYel, "X"))),
                new RendererOption("Weird"       ,(color)-> new GameGridTextComponentRenderer(color, new BoxStringRender(Colors.BIYel, "/---\\\n"+
                                                                                                                                     "| @ |\n"+
                                                                                                                                     "\\---/\n"
                        ))),
                new RendererOption("Jar-Head"    ,(color)-> new GameGridTextComponentRenderer(color, new BoxStringRender(Colors.BIYel, "┌───┐\n"+
                                                                                                                                       "├O─O┤\n"+
                                                                                                                                       "└───┘\n"
                                                                                             ))),
            };
            return renderers;
        }
    }
    
}
