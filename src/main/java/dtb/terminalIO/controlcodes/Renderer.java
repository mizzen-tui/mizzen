package dtb.terminalIO.controlcodes;

import java.util.ArrayList;
import java.util.Collection;

import dtb.terminalIO.controlcodes.ControlCode.CursorDown;
import dtb.terminalIO.controlcodes.ControlCodeContext.StringContext;

/**
 * Something that can render itself usually to a Terminal.
 * 
 * @author david
 *
 */
public interface Renderer {

    public void render(ControlCodeContext ctx);
    
    public default String asString() {
            StringContext sCTX = new StringContext();
            this.render(sCTX);
            return sCTX.toString();
    }

    public static class TextRenderer implements Renderer {

        private String text;

        public TextRenderer(String text) {
            this.text = text;
        }

        public String getAsString() {
            return this.text;
        }

        @Override
        public void render(ControlCodeContext ctx) {
            ctx.write(this.getAsString());
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((text == null) ? 0 : text.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            TextRenderer other = (TextRenderer) obj;
            if (text == null) {
                if (other.text != null)
                    return false;
            } else if (!text.equals(other.text))
                return false;
            return true;
        }
        
        public String toString() {
            return getAsString();
        }
    }

    public static class RendererBuffer implements Renderer {

        private Collection<Renderer> renderers;

        public RendererBuffer append(Renderer r) {
            if (r != null) {
                if (this.renderers == null) {
                    this.renderers = new ArrayList<Renderer>();
                }
                this.renderers.add(r);
            }
            return this;
        }

        public RendererBuffer append(String text) {
            return append(new TextRenderer(text));
        }

        @Override
        public void render(ControlCodeContext ctx) {
            if (this.renderers == null) {
                return;
            }

            for (Renderer renderer : renderers) {
                renderer.render(ctx);
            }
        }

        public void clear() {
            renderers.clear();
        }

        @Override
        public String toString() {
            ControlCodeContext sctx = new ControlCodeContext.StringContext();
            render(sctx);
            return "RendererBuffer \n"+sctx.toString();
        }
        
    }

    // TODO - should this be moved to ControlCode class?
    public static Renderer CR = new Renderer() {

        TextRenderer cooked_CR = new TextRenderer("\n");
        Renderer raw_CR = new RendererBuffer().append(ControlCode.CURSOR_LEFT ).append( new CursorDown(1));
        
        @Override
        public void render(ControlCodeContext ctx) {
            if (ctx.isRaw()) {
                raw_CR.render(ctx);
            } else {
                cooked_CR.render(ctx);
            }
        }
        
    };
    

    public static TextRenderer LF = new TextRenderer("\r");

}
