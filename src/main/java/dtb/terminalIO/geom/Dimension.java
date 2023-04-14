package dtb.terminalIO.geom;

import java.util.Arrays;
import java.util.Comparator;
import java.util.function.Function;
import java.util.function.Supplier;

import dtb.terminalIO.controlcodes.color.ColorParsingUtils;
import dtb.terminalIO.textsegments.CleanString;

/**
 * Encapsulate Height and width
 */
public interface Dimension {

    public int getWidth();

    public int getHeight();

    public default boolean contains(Dimension other) {
        return this.getWidth() >= other.getWidth() && this.getHeight() >= other.getHeight();
    }

    public default boolean isSameDimension(Dimension other) {
        return this.getWidth() == other.getWidth() && this.getHeight() == other.getHeight();
    }


    public static abstract class AbstractDelegate implements Dimension {

        abstract protected Dimension getBaseSize();

        @Override
        public int getWidth() {
            return getBaseSize().getWidth();
        }

        @Override
        public int getHeight() {
            return getBaseSize().getHeight();
        }

        public String toString() {
            return new Simple(this).toString();
        }

    }

    public static class Delegate extends AbstractDelegate {
        Dimension size;

        public Delegate(Dimension size) {
            super();
            if (size == null) {
                throw new NullPointerException("Null for size parameter in new Dimension.Delegate(size)!");
            }
            this.size = size;
        }

        @Override
        public Dimension getBaseSize() {
            return this.size;
        }

        public void setBaseSize(Dimension size) {
            this.size = size;
        }

    }
    
    public static class ComputedDelegate extends Delegate {
        Dimension size;

        Function<Dimension, Integer> computeHeight;
        Function<Dimension, Integer> computeWidth;

        public ComputedDelegate(Dimension size, Function<Dimension, Integer> computeHeight, Function<Dimension, Integer> computeWidth) {
            super(size);
            this.computeHeight = computeHeight;
            this.computeWidth = computeWidth;
        }
        
        @Override
        public int getWidth() {
            return computeWidth.apply(getBaseSize());
        }

        @Override
        public int getHeight() {
            return computeHeight.apply(getBaseSize());
        }

    }

    public static class SupplierAsDimension extends AbstractDelegate {

        Supplier<Dimension> derivedDimension;

        public SupplierAsDimension(Supplier<Dimension> derivedDimension) {
            this.derivedDimension = derivedDimension;
        }

        @Override
        protected Dimension getBaseSize() {
            return derivedDimension.get();
        }

    }


    /**
     * Simple implementation of {@link Dimension}
     */
    public static class Simple implements Dimension {

        private int width;
        private int height;

        public Simple() {
        }

        public Simple(int height, int width) {
            super();
            this.width = width;
            this.height = height;
        }

        public Simple(Dimension d) {
            this(d.getHeight(), d.getWidth());
        }

        public int getWidth() {
            return width;
        }

        public void setWidth(int width) {

            this.width = width;
        }

        public int getHeight() {
            return height;
        }

        public void setHeight(int height) {
            this.height = height;
        }

        @Override
        public String toString() {
            return "Dimension [width=" + width + ", height=" + height + "]";
        }

    }

    public static final Dimension ZERO = new Simple(0, 0);
    public static final Dimension ONE = new Simple(1, 1);

    // UTILITIES ..

    public static class UTIL {
        
        public static String shortStr(Dimension d) {
            return d==null?"<NULL>" : "["+d.getHeight()+","+d.getWidth()+"]";
        }

        public static Dimension stringToDimension(String s) {

            if (s == null || s.length() == 0)
                return ZERO;

            s = ColorParsingUtils.removeColorCodes(s);

            String[] lines = s.split("\n",-1);

            int height = lines.length;
            int width = Arrays.stream(lines).max(Comparator.comparing(String::length)).get().length();

            return new Dimension.Simple(height, width);
        }

        public static Dimension stringToDimension(CleanString s) {

            if (s == null || s.length() == 0)
                return ZERO;

            return new Dimension.Simple(1, s.length());
        }
    }

      // TODO: move this to a unit test
    public static void main(String[] args) {

        System.out.println("Hello World");

        System.out.println("len = " + "1\n\2\n3".split("\n").length);

        System.out.println("len = " + "1\n\2\n3\n\n".split("\n").length);

        System.out.println("len = " + "1\n\2\n3\n\n".split("\n",-1).length);

        System.out.println("len = " + "1\n\2\n\n3".split("\n",-1).length);

    }
}
