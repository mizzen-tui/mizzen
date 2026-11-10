package mizzen.components.plot.effects;

import java.util.Random;

import mizzen.components.plot.PlotMiddleware;
import mizzen.components.plot.PointPlotter;
import mizzen.controlcodes.color.Color;

/**
 * Makes a thing look like it is being blown apart: put it between the thing and the real plotter,
 * then redraw the thing as the sequence climbs from 0 (intact) to maxSequence (gone). Along the way:
 * <pre>
 *   more and more points are dropped       the shape erodes (slowly at first, faster near the end)
 *   the rest are pushed further outward     debris flying away
 *   some flash in the colour cycle          e.g. orange / red / yellow
 * </pre>
 * Example - one step per timer tick:
 * <pre>
 *     ExplodingPointPlotter boom = new ExplodingPointPlotter(grid.asPointPlotter(), fireColors);
 *     ... on each tick:  boom.incrementSequence(5); grid.clearPlots(); shape.draw(boom);
 * </pre>
 * It remembers nothing between points - each one is decided afresh from the sequence - so redraw
 * the whole thing each time. As a middleware step: asStep().
 */
public class ExplodingPointPlotter implements PointPlotter {

    /** where the (possibly moved, possibly recoloured) points go */
    protected PointPlotter target;

    protected Random random;

    /** 0: intact (nothing dropped or moved); maxSequence: gone (every point dropped) */
    protected int sequence = 0;
    protected int maxSequence = 100;

    /** how far (in grid units) a point can be pushed once fully destroyed */
    protected float maxDisplacement = 6f;

    /** the flash colours; null: none */
    protected Color[] colorCycle;
    protected int colorCycleIndex = 0;

    /** chance (0..1) that a surviving point flashes in the next cycle colour instead of its own */
    protected float colorCycleChance = 0.35f;

    /** the colour last asked for (setNextPlotColor) */
    protected Color requestedColor = null;

    public ExplodingPointPlotter(PointPlotter target) {
        this(target, null);
    }

    public ExplodingPointPlotter(PointPlotter target, Color[] colorCycle) {
        this.target = target;
        this.colorCycle = colorCycle;
        this.random = new Random();
    }

    @Override
    public void plot(float x, float y) {
        float progress = getProgress();

        // dropped? the chance grows with progress squared: barely thinned at first, falls apart near the end
        if (random.nextFloat() < progress * progress) {
            return;
        }

        // pushed outward, in a random direction, further as progress grows
        float displacement = maxDisplacement * progress * random.nextFloat();
        double angle = random.nextDouble() * Math.PI * 2.0;
        float dx = (float) (Math.cos(angle) * displacement);
        float dy = (float) (Math.sin(angle) * displacement);

        // its own colour, or a flash
        Color colorToUse = requestedColor;
        if (colorCycle != null && colorCycle.length > 0 && random.nextFloat() < colorCycleChance) {
            colorToUse = nextCycleColor();
        }

        Color prevTargetColor = target.setNextPlotColor(colorToUse);
        target.plot(x + dx, y + dy);
        target.setNextPlotColor(prevTargetColor);        // leave the target's colour as it was
    }

    @Override
    public Color setNextPlotColor(Color color) {
        Color prev = requestedColor;
        requestedColor = color;
        return prev;
    }

    @Override
    public void clearPlots() {
        target.clearPlots();
    }

    /**
     * This explosion as a middleware step: the thing is drawn through it, into whatever plotter the
     * drawing was going to (the target is set from the drawing each time).
     */
    public PlotMiddleware asStep() {
        return (ctx, chain) -> {
            PointPlotter real = ctx.plotter;
            this.target = real;
            ctx.plotter = this;
            try {
                chain.forward(ctx);
            } finally {
                ctx.plotter = real;
            }
        };
    }

    /** 0..1: how destroyed the thing is */
    public float getProgress() {
        float p = (float) sequence / (float) maxSequence;
        return Math.max(0f, Math.min(1f, p));
    }

    public int getSequence() {
        return sequence;
    }

    /** how far along the destruction is (kept within 0..maxSequence) */
    public void setSequence(int sequence) {
        this.sequence = Math.max(0, Math.min(sequence, maxSequence));
    }

    /** one step further, e.g. per hit or per tick */
    public void incrementSequence() {
        setSequence(this.sequence + 1);
    }

    public void incrementSequence(int amount) {
        setSequence(this.sequence + amount);
    }

    public int getMaxSequence() {
        return maxSequence;
    }

    public void setMaxSequence(int maxSequence) {
        this.maxSequence = Math.max(1, maxSequence);
        setSequence(this.sequence);
    }

    public float getMaxDisplacement() {
        return maxDisplacement;
    }

    public void setMaxDisplacement(float maxDisplacement) {
        this.maxDisplacement = maxDisplacement;
    }

    public Color[] getColorCycle() {
        return colorCycle;
    }

    public void setColorCycle(Color[] colorCycle) {
        this.colorCycle = colorCycle;
        this.colorCycleIndex = 0;
    }

    public float getColorCycleChance() {
        return colorCycleChance;
    }

    /** chance: 0..1 that a surviving point flashes in a cycle colour instead of its own */
    public void setColorCycleChance(float chance) {
        this.colorCycleChance = Math.max(0f, Math.min(1f, chance));
    }

    /** for repeatable runs (tests, recordings) */
    public void setRandomSeed(long seed) {
        this.random = new Random(seed);
    }

    public PointPlotter getTarget() {
        return target;
    }

    public void setTarget(PointPlotter target) {
        this.target = target;
    }

    /** true once the sequence reached maxSequence: nothing is drawn any more */
    public boolean isFullyDestroyed() {
        return sequence >= maxSequence;
    }

    private Color nextCycleColor() {
        Color c = colorCycle[colorCycleIndex % colorCycle.length];
        colorCycleIndex++;
        return c;
    }
}
