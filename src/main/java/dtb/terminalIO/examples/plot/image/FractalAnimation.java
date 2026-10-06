package dtb.terminalIO.examples.plot.image;

import java.awt.image.BufferedImage;

import dtb.terminalIO.components.ButtonBar;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.composite.Composite;
import dtb.terminalIO.components.input.DropdownListBox;
import dtb.terminalIO.components.plot.image.ImageViewerFactory;
import dtb.terminalIO.components.plot.image.ImageViewerFactory.IMAGE_RENDER_TYPE;
import dtb.terminalIO.components.plot.image.animate.ImageAnimatorComponent;
import dtb.terminalIO.components.plot.image.animate.ImageIterator;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.controlcodes.color.Colors.FGBGColors;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.geom.Dimension;

/**
 * Fractals, a new random one every interval, under a bar choosing how they are drawn: the render
 * style (ASCII, Braille, blocks) and, for ASCII, the gradient of characters. Used by FractalsViewer
 * (full screen) and TreeViewTest (a node's view).
 * <p/>
 * It holds its panel (it is not one): add component() where it should show; start / stop the
 * animation while it is shown.
 * <pre>
 *     FractalAnimation fractals = new FractalAnimation(size, 1000);
 *     panel.add(fractals.component());
 *     fractals.start(eventBus);         // stop() when it goes off screen; start again later
 * </pre>
 */
public class FractalAnimation {

    private final Composite panel = new Composite();
    private final ImageAnimatorComponent animator;
    private final int interval;

    /** size: the whole thing - the bar takes its top row, the fractal the rest; interval: ms between fractals */
    public FractalAnimation(Dimension size, int interval) {
        this.interval = interval;
        animator = new ImageAnimatorComponent(Colors.DEFAULT_COLOR, endlessFractals(), imageSize(size));

        DropdownListBox<IMAGE_RENDER_TYPE> renderStyle = new DropdownListBox<IMAGE_RENDER_TYPE>(ImageViewerFactory.imageRenderTypeOptions());
        renderStyle.setSelected(0);
        TextComponent gradientLabel = new TextComponent(" Gradient : ");
        DropdownListBox<String> gradients = new DropdownListBox<String>(Colors.BICya, ImageViewerFactory.standardAsciiGradients());
        gradients.setMaxItemWidth(15);
        gradients.setSelected(0);

        // draw as the dropdowns show
        animator.getImageRenderRequest().setImageRenderType(renderStyle.getValue());
        animator.getImageRenderRequest().setGradientStr(gradients.getValue());

        renderStyle.onChange((te) -> {
            animator.getImageRenderRequest().setImageRenderType(renderStyle.getValue());
            boolean ascii = renderStyle.getValue().equals(IMAGE_RENDER_TYPE.ASCII);      // the gradient is for ASCII only
            if (ascii) {
                gradientLabel.show();
                gradients.show();
            } else {
                gradientLabel.hide();
                gradients.hide();
            }
        });
        gradients.onChange((te) -> animator.getImageRenderRequest().setGradientStr(gradients.getValue()));

        ButtonBar bar = new ButtonBar().setHorizontalSpacer(1);
        bar.setBorder(false);
        bar.setNoBorderChars();
        bar.addComponent(new TextComponent(new FGBGColors(Colors.BCya, Colors.On_Whi), "Fractals"));
        bar.addComponent(new TextComponent(" Render style : "));
        bar.addComponent(renderStyle);
        bar.addComponent(gradientLabel);
        bar.addComponent(gradients);

        panel.add(bar);
        panel.add(1, 0, animator);
    }

    public Component<?> component() {
        return panel;
    }

    /** start showing a new fractal every interval */
    public void start(EventBus eventBus) {
        animator.setupTimer(eventBus, interval);
    }

    /** stop the animation (start can resume it) */
    public void stop() {
        animator.stop();
    }

    /** the whole thing's new size */
    public void resize(Dimension size) {
        animator.resize(imageSize(size));
    }

    private static Dimension imageSize(Dimension size) {
        return new Dimension.ComputedDelegate(size, (s) -> s.getHeight() - 1, (s) -> s.getWidth());
    }

    /** a new random fractal, for ever */
    private static ImageIterator endlessFractals() {
        return new ImageIterator() {
            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public BufferedImage next() {
                return FractalGenerator.makeFractalImage();
            }

            @Override
            public void close() {
            }
        };
    }
}
