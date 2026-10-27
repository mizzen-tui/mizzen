/**
 * Ready-made shapes to reuse: each is placed by its centre and drawn from a few settings that can
 * be changed later - a star's points, a gear's teeth, a ring's hole.
 * <pre>
 *     Star        a star: points, outer and inner radius
 *     RegularPolygon  equal sides (a hexagon, a hex nut with setHoleRadius)
 *     Ring        a washer: a circle with a round hole
 *     Gear        teeth, root circle, axle hole
 *     Spring      a coil spring from the side
 *     Heart, House, Smiley, Spirograph
 * </pre>
 * They are ordinary shapes - moved, turned and scaled by their transform, drawn by a pen or any
 * PointPlotter - and the fillable ones fill with their holes left empty. The plain shapes they are
 * built from (Polygon, Circle, Line, CompositeShape) are in the package above.
 */
package mizzen.components.plot.shape.catalog;
