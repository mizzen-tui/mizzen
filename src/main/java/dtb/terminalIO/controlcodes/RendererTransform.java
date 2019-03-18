package dtb.terminalIO.controlcodes;

@FunctionalInterface
public interface RendererTransform<I extends Renderer,R extends Renderer> {
    R transform(I renderer);
}
