package dtb.terminalIO;

import java.util.ArrayList;
import java.util.Collection;

public interface Renderer {

    public void render(TerminalContext ctx);

    public static class TextRenderer implements Renderer {

        private String text;

        public TextRenderer(String text) {
            this.text = text;
        }

        public String getAsString() {
            return this.text;
        }

        @Override
        public void render(TerminalContext ctx) {
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
        public void render(TerminalContext ctx) {
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
            TerminalContext sctx = new TerminalContext.StringContext();
            render(sctx);
            return "RendererBuffer \n"+sctx.toString();
        }

        
        
    }

    public static TextRenderer CR = new TextRenderer("\n");

    public static TextRenderer LF = new TextRenderer("\r");

}
