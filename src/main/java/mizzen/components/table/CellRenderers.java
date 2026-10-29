package mizzen.components.table;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

import mizzen.components.chart.SparkLineComponent;
import mizzen.components.plot.Pen;
import mizzen.components.plot.PlotArea;
import mizzen.components.plot.image.ImageViewerFactory;
import mizzen.components.plot.image.ImageViewerFactory.IMAGE_RENDER_TYPE;
import mizzen.components.plot.image.ImageViewerFactory.ImageRenderRequest;
import mizzen.components.progressmeter.ProgressMeter;
import mizzen.controlcodes.color.Color;
import mizzen.controlcodes.color.Colors;
import mizzen.geom.Dimension;

/**
 * Cells that are not text - each a ComponentCellRenderer with its size rule:
 * <pre>
 *     CellRenderers.drawing((part, pen) -&gt; pen.draw(part.shape()), -1.3, 1.3, -1.3, 1.3, 6, 24)
 *     CellRenderers.image((photo) -&gt; photo.image, 6, 30)
 *     CellRenderers.meter(color, (part) -&gt; part.inStock / 100.0)
 *     CellRenderers.sparkline(color, (part) -&gt; part.salesByWeek)
 * </pre>
 */
public final class CellRenderers {

    private CellRenderers() {
    }

    /**
     * What draw draws for a value, in x from xMin to xMax and y from yMin to yMax, kept in
     * proportion (braille: a character is 2 dots wide, 4 high) - from least to most wide.
     */
    public static <V> ComponentCellRenderer<V> drawing(BiConsumer<V, Pen> draw, double xMin, double xMax, double yMin,
            double yMax, int least, int most) {
        Object[] shown = new Object[1];                     // the value being drawn: the area's onDraw reads it
        @SuppressWarnings("unchecked")
        PlotArea area = new PlotArea(Colors.DEFAULT_COLOR).showing(xMin, xMax, yMin, yMax).scaling(PlotArea.Scaling.EQUAL).axes(false)
                .onDraw((pen) -> draw.accept((V) shown[0], pen));
        double rowsPerColumn = 0.5 * (yMax - yMin) / (xMax - xMin);
        return new ComponentCellRenderer<V>(ComponentCellRenderer.SizeRule.byAspect((v) -> rowsPerColumn, least, most),
                (value, size) -> {
                    shown[0] = value;
                    area.resize(size);
                    area.redraw();                          // a cell the same size as the last: drawn again for this value
                    return area;
                });
    }

    /** a value's image, in coloured blocks, kept in proportion - from least to most wide */
    public static <V> ComponentCellRenderer<V> image(Function<V, BufferedImage> imageOf, int least, int most) {
        return new ComponentCellRenderer<V>(ComponentCellRenderer.SizeRule
                .byAspect((v) -> 0.5 * imageOf.apply(v).getHeight() / imageOf.apply(v).getWidth(), least, most),
                (value, size) -> {
                    ImageRenderRequest request = new ImageRenderRequest().setImageToView(imageOf.apply(value))
                            .setDesiredImgSizeInCharacters(size).setColorMap(ImageViewerFactory.rgbColorMapRCol)
                            .setToPlotOrNot(ImageViewerFactory.PLOT_ALWAYS);
                    request.setImageRenderType(IMAGE_RENDER_TYPE.BLOCK);
                    try {
                        return ImageViewerFactory.fromImage(request);         // drawn now - not on a timer, as ResizableImageViewer is
                    } catch (Exception e) {
                        throw new IllegalStateException("could not draw the image of " + value, e);
                    }
                });
    }

    /** a value's fraction done, 0 to 1, as a one-row progress meter - its percentage after the bar */
    public static <V> ComponentCellRenderer<V> meter(Color color, Function<V, Double> fraction) {
        ProgressMeter meter = new ProgressMeter(color, 1).setLayout(ProgressMeter.Layout.ONE_ROW);
        int least = meter.getWidth();                       // narrower, it would not fit its percentage
        return new ComponentCellRenderer<V>(ComponentCellRenderer.SizeRule.fixedHeight(1, least, 40), (value, size) -> {
            meter.setProgress(fraction.apply(value));
            meter.resize(size);
            return meter;
        });
    }

    /** a value's numbers as a one-row sparkline, one column each - cut to the cell if narrower */
    public static <V> ComponentCellRenderer<V> sparkline(Color color, Function<V, List<Integer>> values) {
        SparkLineComponent line = new SparkLineComponent(color);
        ComponentCellRenderer.SizeRule<V> oneColumnEach = new ComponentCellRenderer.SizeRule<V>() {
            public CellWidths widths(V value) {
                int count = values.apply(value).size();
                return new CellWidths(Math.min(1, count), count, count);
            }

            public int heightAt(V value, int width) {
                return 1;
            }
        };
        return new ComponentCellRenderer<V>(oneColumnEach, (value, size) -> {
            line.renderPoints(values.apply(value));
            line.resize(new Dimension.Simple(1, size.getWidth()));
            return line;
        });
    }
}
