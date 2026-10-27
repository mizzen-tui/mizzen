package mizzen.geom.position;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiFunction;

public interface Vec2<T extends Vec2<?>> {

    /** a point given as x (across), then y - the maths order; Position.rowCol is the screen order (row, then column) */
    static Vec2<?> xy(float x, float y) {
        return new BaseVec2(x, y);
    }
    
    public default boolean isSameLocation(T other) {
        if (other == null) {
            return false;
        }
        return this.getY() == other.getY() && 
               this.getX() == other.getX();
    }

    public float getX();

    public float getY();

    public T newFrom(float x, float y);

    public default T offsetTo(Vec2<?>  target) {
        return offsetTo(target.getX(), target.getY());
    }

    public default T offsetTo(float x, float y) {
        return newFrom(x - this.getX(), y - this.getY());
    }

    public default T add(float x, float y) {
        return newFrom(x + this.getX(), y + this.getY());
    }

    public default T offsetAdd(Vec2<?> offsetToAdd) {
        return add(offsetToAdd.getX(), offsetToAdd.getY());
    }

    public default T sub(Vec2<?> other) {
        float newX = this.getX() - other.getX();
        float newY = this.getY() - other.getY();
        return newFrom(newX, newY);
    }

    public default T mul(float factor) {
        return newFrom(factor * this.getX(), factor * this.getY());
    }

    public default T mul(double factor) {
        return newFrom((float)factor * this.getX(), (float)factor * this.getY());
    }

    public default T mul(Vec2<?> other) {
        return newFrom(this.getX() * other.getX(), this.getY() * other.getY());
    }
    
    public default T negate() {
        return newFrom(this.getX()*-1, this.getY()*-1);
    }

    public default float dot(Vec2<?> other) {
        return this.getX() * other.getX() + this.getY() * other.getY();
    }

    public default float angle(Vec2<?> other) {
        float dot = dot(other);
        float det = this.getX() * other.getY() - this.getY() * other.getX();
        return (float) Math.atan2((double)det, (double)dot);
    }

    public static float lengthSquared(float x, float y) {
        return x * x + y * y;
    }

    public static float length(float x, float y) {
        return (float)Math.sqrt((double)lengthSquared(x, y));
    }

    public default float lengthSquared() {
        return lengthSquared(this.getX(), this.getY());
    }

    // I would use length(), but length() is used elsewhere and would cause conflicts in some other core classes
    public default float magnitude() {
        return length(this.getX(), this.getY());
    }


    public static class Vec2Comparator implements Comparator<Vec2<?>> {
        @Override
        public int compare(Vec2<?> vec1, Vec2<?> vec2) {
            if (vec1 == vec2)
                return 0;
            int cmp = Float.compare(vec1.getY(), vec2.getY());
            if (cmp != 0)
                return cmp;
            return Float.compare(vec1.getX(), vec2.getX());
        }
    }

    public static class BaseVec2 implements Vec2<BaseVec2> {

        public float y;
        public float x;

        public BaseVec2(float x, float y) {
            super();
            this.x = x;
            this.y = y;
        }

        @Override
        public float getX() {
            return x;
        }

        @Override
        public float getY() {
            return y;
        }

        @Override
        public BaseVec2 newFrom(float x, float y) {
            return new BaseVec2(x, y);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + Float.floatToIntBits(x);
            result = prime * result + Float.floatToIntBits(y);
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
            BaseVec2 other = (BaseVec2) obj;
            if (Float.floatToIntBits(x) != Float.floatToIntBits(other.x))
                return false;
            if (Float.floatToIntBits(y) != Float.floatToIntBits(other.y))
                return false;
            return true;
        }

        public String toString() {
            return UTIL.asString(this);
        }

    }

    public static class UTIL {
        public static final Vec2Comparator VEC2_COMPARE = new Vec2Comparator();

        public static List<Vec2<?>> getSorted(Vec2<?> ...v) {
            List<Vec2<?>> points = Arrays.asList(v); 
            points.sort( Vec2.UTIL.VEC2_COMPARE);
            return points;
        }

        public static Collection<Vec2<?>>  dotsToVector(float[][] dots) {
            return dotsToVector(dots, (x,y)-> new BaseVec2(x,y));
        }

        public static Collection<Vec2<?>> dotsToVector(float[][] dots, BiFunction<Float, Float, Vec2<?>> newVec) {
            if (dots == null) {
                throw new NullPointerException("float array parameter null!");
            }
            Collection<Vec2<?>> points = new ArrayList<Vec2<?>>(dots.length);
            for (int i = 0; i<dots.length; i++) {
                float[] p = dots[i];
                if (p == null) {
                    throw new NullPointerException("dots["+i+"] null. Was expecting an array size of 2");
                }
                if (p.length != 2) {
                    throw new IllegalArgumentException("dots["+i+"] length = "+p.length+". Was expecting an array size of 2");
                }
                points.add( newVec.apply(p[0], p[1]) );
            }
            return points;
        }

        public static String asString(Vec2<?> p) {
            if (p == null) {
                return  "Vec2[null, null]";
            }
            return String.format("%s[x=%4f, y=%4f]", p.getClass().getSimpleName(), p.getX(), p.getY());
        }

    }

}
