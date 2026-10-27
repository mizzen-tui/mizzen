package mizzen.examples.plot;

import mizzen.components.BorderedComponent;
import mizzen.components.TextComponent;
import mizzen.components.plot.PointPlotter;
import mizzen.components.plot.TransformPointPlotter;
import mizzen.components.plot.XYTranslate;
import mizzen.components.plot.braille.BrailleGrid;
import mizzen.components.plot.shape.Circle;
import mizzen.components.plot.shape.CompositeShape;
import mizzen.components.plot.shape.Line;
import mizzen.components.plot.shape.Shape;
import mizzen.components.plot.shape.Shape.AbstractShape;
import mizzen.controlcodes.color.Color;
import mizzen.controlcodes.color.Colors;
import mizzen.core.Canvas;
import mizzen.core.TerminalScreen;
import mizzen.examples.TestsCommon;
import mizzen.geom.Dimension;
import mizzen.geom.position.Position;
import mizzen.geom.position.PositionTransform;
import mizzen.geom.position.PositionTransform.RotateAroundFixedPoint;

public class BrailleScratchPadTests {

    static void plotDOT(BrailleGrid grid) {
        grid.plotPosition(10, 10);
    }

    static class EnemyOneShape extends AbstractShape<EnemyOneShape> {

        CompositeShape componentShapes;
        Line l1;
        Line l2;
        Line l3;
        Line l4;

        Position midPoint = new Position.Simple(6, 4);
        RotateAroundFixedPoint pointRotator = new RotateAroundFixedPoint(0, midPoint);
        TransformPointPlotter rotatingPlotter = new TransformPointPlotter(null, pointRotator);

        int drawCounter = 0;

        public EnemyOneShape() {
            this.componentShapes = new CompositeShape();
            componentShapes.addShape(this.l1 = new Line(-3, -3, midPoint.getRow(), midPoint.getCol(), Colors.BBlu));
            componentShapes.addShape(this.l2 = new Line(midPoint.getRow(), midPoint.getCol(), 15, -3, Colors.BCya));
            componentShapes.addShape(this.l3 = new Line(-5, 3, midPoint.getRow(), midPoint.getCol(), Colors.BBlu));
            componentShapes.addShape(this.l4 = new Line(midPoint.getRow(), midPoint.getCol(), 12, 3, Colors.BCya));
            componentShapes.addShape(new Circle(midPoint.getRow(), midPoint.getCol(), 6, Colors.BGre));
            componentShapes.addShape(new Circle(midPoint.getRow(), midPoint.getCol(), 1, Colors.BRed));
        }

        Color getLineColor(Line l) {
            if (l.getColor().equals(Colors.BBlu)) {
                return Colors.BCya;
            }
            return Colors.BBlu;
        }

        @Override
        protected void drawWithTransform(PointPlotter pointPlotter, PositionTransform positionTransform) {
            drawCounter++;
            if (drawCounter % 6 == 0) {
                l1.setColor(getLineColor(l1));
                l2.setColor(getLineColor(l2));
                l3.setColor(getLineColor(l3));
                l4.setColor(getLineColor(l4));
            }
            
            rotatingPlotter.setPointPlotter(pointPlotter);
            pointRotator.setTheta(drawCounter*10);
            
            this.componentShapes.draw(rotatingPlotter);
        }
    };

    static Shape[] enemies = new Shape[]{ new EnemyOneShape(),new EnemyOneShape(),new EnemyOneShape()};
    
    static void plotEnemyONE(Shape _enemyONE, PointPlotter pointPlotter) {
        _enemyONE.draw(pointPlotter);
    }


    public static void main(String args[]) throws Exception {
        // System.out.println("HEllow world!");

        TerminalScreen screen = TestsCommon.getTerminalScreenForTesting(args);

        Dimension size = TestsCommon.terminalSize();
        Canvas master_canvas = new Canvas(screen);
        master_canvas.setSize(size.getHeight()-2, size.getWidth()-2);

        screen.hideCursor();
        
        char EMPTY_CHAR = ' ';

        master_canvas.setEmptyChar(EMPTY_CHAR);

        master_canvas.t("testing 123");

        BrailleGrid grid = new BrailleGrid(null, master_canvas.getHeight() - 6, master_canvas.getWidth() - 10);
        
        grid.setFillOnClear(true);

        BorderedComponent bcGRID = new BorderedComponent(grid);
        bcGRID.setTitle(Colors.BPur, "Space Invaders");

        plotDOT(grid);

        PointPlotter p0 = grid.asPointPlotter();
        XYTranslate pPrime1 = new XYTranslate(p0, 0, 0);
        XYTranslate pPrime2 = new XYTranslate(p0, 70, 0);
        XYTranslate pPrime3 = new XYTranslate(p0, 35, 70);

        TextComponent iLabel = new TextComponent("i = 0   ");
        BorderedComponent bc = new BorderedComponent(iLabel);
        grid.add(10,10, bc);


        Position gridMidpoint = grid.getGridRectangle().centerPosition().offsetAdd(-40, -40);
        RotateAroundFixedPoint gridPointRotator = new RotateAroundFixedPoint(0, gridMidpoint);
        TransformPointPlotter gridRotatingPlotter = new TransformPointPlotter(null, gridPointRotator);


        for (int i = 0; i < 220; i++) {
            //grid.clear();
            grid.clearPlots();
            pPrime1.setxDelta(i);
            pPrime1.setyDelta(i);
            pPrime2.setxDelta(70 - pPrime1.getxDelta());
            pPrime2.setyDelta(pPrime1.getyDelta());
            pPrime3.setxDelta(35);
            pPrime3.setyDelta(70 - i);

            gridPointRotator.setTheta(i/3);

            gridRotatingPlotter.setPointPlotter(pPrime1);
            plotEnemyONE(enemies[0], gridRotatingPlotter);

            gridRotatingPlotter.setPointPlotter(pPrime2);
            plotEnemyONE(enemies[1], gridRotatingPlotter);

            gridRotatingPlotter.setPointPlotter(pPrime3);
            plotEnemyONE(enemies[2], gridRotatingPlotter);
            
            iLabel.setText("i = "+i);

            
            master_canvas.addComponentSegments(4,2,bcGRID);
            master_canvas.refresh();
            Thread.sleep(15);
        }
        
        
        screen.showCursor();
        master_canvas.refresh();


    }

}
