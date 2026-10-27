package mizzen.core;

import mizzen.controlcodes.ControlCode;
import mizzen.controlcodes.ControlCodeContext.StringContext;
import mizzen.controlcodes.Renderer.RendererBuffer;
import mizzen.controlcodes.color.Colors;
import mizzen.geom.position.Position;
import mizzen.textsegments.ScreenLineSegment;

public class StringCanvasBackEnd implements CanvasBackend {

    RendererBuffer renderer = new RendererBuffer();
    
    StringContext ctx = new StringContext();
    
    String contentAsString;
    
    
    @Override
    public void writeToBackend(ScreenLineSegment sc) {
        renderer.append(sc.getColors());
        renderer.append(sc.getText());
    }

    @Override
    public void signal_eol() {
        renderer.append(Colors.RCol);
        
    }

    @Override
    public void refresh() {
        renderer.render(ctx);
        this.contentAsString = ctx.toString();
        ctx.clear();
        renderer.clear();
    }

    @Override
    public void cr() {
        renderer.append( ControlCode.CR );
        
    }

    @Override
    public void showCursorAt(Position showCursorPosition, boolean showCursor) {
        // do nothing
    }

    @Override
    public void hideCursor() {
        // do nothing
    }

    @Override
    public void showCursor() {
        // do nothing
    }
    
    public String toString() {
        return contentAsString != null ? contentAsString : "";
    }

    @Override
    public void setSize(int height, int width) {
        // do nothing
        
    }

}
