package mizzen.examples.plot;

import java.io.IOException;

import mizzen.app.TUIApp;
import mizzen.components.ComponentWithFocus;
import mizzen.components.ClickableDraggableComponentWrapper;
import mizzen.components.TextComponent;
import mizzen.components.composite.HorizontalPanel;
import mizzen.components.composite.VerticalPanel;
import mizzen.components.input.DropdownListBox;
import mizzen.components.input.FloatInputWithIncrDecrButtons;
import mizzen.components.input.HasValue;
import mizzen.components.input.OnChangeListeners;
import mizzen.components.input.RadioGroup;
import mizzen.components.input.TextInputFloat;
import mizzen.components.plot.braille.BrailleGrid;
import mizzen.components.plot.braille.BrailleUtils;
import mizzen.components.plot.grid.DragRotate3DOnGrid;
import mizzen.components.plot.grid.DraggableOnGrid;
import mizzen.components.plot.shape.threeD.AbstractShape3D;
import mizzen.components.plot.shape.threeD.Cube3D;
import mizzen.components.plot.shape.threeD.Sphere3D;
import mizzen.components.plot.shape.threeD.projection.Plot2D;
import mizzen.components.plot.shape.threeD.projection.Plot2DFromProjection;
import mizzen.components.plot.shape.threeD.projection.Plot2DSimple;
import mizzen.components.scroll.ScrollBarHorizontal;
import mizzen.components.scroll.ScrollBarVertical;
import mizzen.controlcodes.color.Color;
import mizzen.controlcodes.color.Colors;
import mizzen.events.ChainedEventHandler;
import mizzen.events.EventHandler;
import mizzen.events.Registration;
import mizzen.events.OnIdleEventEventHandler.RepeatingInterval;
import mizzen.events.mouse.Draggable;
import mizzen.geom.Dimension;
import mizzen.geom.position.Position;
import mizzen.geom.position.Vec3.BaseVec3;
import mizzen.geom.position.Vec3;
import mizzen.util.Logging;

public class TestCube {

    public static void main(String[] args) throws IOException {

        Logging.setEnabled(true);

        new TUIApp(args)
                .setEmptyChar(' ')
                .setDefaultMainApplicationPanel()
                .confirmOnExit()
                .setOnReadyStart((tApp) -> {

                    float initialCubeDistanceFromCamera = 70f;

                    Dimension gridSize = gridSizeFor(tApp.getMainApplicationPanel());

                    VerticalPanel vpMainPanel = new VerticalPanel().setVerticalSpacer(1);

                    BrailleGrid grid = new BrailleGrid(Colors.BGre, gridSize);
                    // Cube3D cube3D = new Cube3D(50, Vec3.ZERO, Colors.BYel, Colors.BBlu);
                    // Sphere3D sphere3D = new Sphere3D(30, Vec3.ZERO, 15, Colors.IYel);
                    Cube3D cube3D = new Cube3D(50, new Vec3.BaseVec3(-60, 0, 0), Colors.BYel, Colors.BBlu);
                    Sphere3D sphere3D = new Sphere3D(30, new Vec3.BaseVec3(60, 0, 0), 15, Colors.IYel);

                    AbstractShape3D<?> activeShape[] = new AbstractShape3D<?>[]{cube3D};

                    Plot2DConfigFormProjection plot2DInputForm = new Plot2DConfigFormProjection(grid.getColor());

                    Plot2DFromProjection projection = (Plot2DFromProjection) plot2DInputForm.getValue();

                    projection.setDistance(initialCubeDistanceFromCamera);

                    projection.setWindowSize(
                            grid.getGridRectangle().getWidth(),
                            grid.getGridRectangle().getHeight());
                    projection.setPanX(0);
                    projection.setPanY(0);
                    plot2DInputForm.refreshFromProjection();

                    DropdownListBox<String> shapeChoice = DropdownListBox.fromStrings(new String[]{"Cube", "Sphere", "Both"});
                    shapeChoice.setValue("Both");
                    final boolean[] showBoth = { "Both".equalsIgnoreCase(shapeChoice.getValue() )}; 

                    Runnable redrawShape = () -> {
                        grid.clearPlots();
                        activeShape[0].  as2DShape(projection).draw(grid.asPointPlotter());

                        grid.clearPlots();
                        if (showBoth[0]) {
                            cube3D.as2DShape(projection).draw(grid.asPointPlotter());
                            sphere3D.as2DShape(projection).draw(grid.asPointPlotter());
                        } else {
                            AbstractShape3D<?> s = shapeChoice.getValue().equals("Cube") ? cube3D : sphere3D;
                            s.as2DShape(projection).draw(grid.asPointPlotter());
                        }

                    };

                    DraggableOnGrid rotateOnGrid = new DragRotate3DOnGrid(
                            projection,
                            (p, position) -> {
                                // update controls with update project values
                                plot2DInputForm.refreshFromProjection();
                                // tell screen to redraw
                                redrawShape.run();
                            });

                    Draggable mouseXYToGridXYDraggableBehaviour = new DraggableOnGrid.AsOnMouseMoveDraggableAdapter(
                            rotateOnGrid,
                            grid::getGridRectangle,
                            mousePos -> BrailleUtils.componentPosToGridPos(mousePos));

                    ClickableDraggableComponentWrapper draggableGrid = new ClickableDraggableComponentWrapper(
                            grid.getColor(), grid);

                    draggableGrid.setDraggable(mouseXYToGridXYDraggableBehaviour);

                    // the pan bars: as long as the grid's sides, ranging half the grid each way - set by fitToWindow
                    ScrollBarHorizontal panXScroll = new ScrollBarHorizontal(grid.getColor(), 0, 0, 0);
                    ScrollBarVertical panYScroll = new ScrollBarVertical(grid.getColor(), 0, 0, 0);

                    panXScroll.onChange((te) -> {
                        Position offset = panXScroll.getValue();

                        float panX = grid.getGridRectangle().getWidth() / 2 - offset.getCol();     // the middle: no pan

                        projection.setPanX(panX);
                        redrawShape.run();
                    });

                    panYScroll.onChange((te) -> {
                        Position offset = panYScroll.getValue();

                        float panY = grid.getGridRectangle().getHeight() / 2 - offset.getRow();

                        projection.setPanY(panY);
                        redrawShape.run();
                    });

                    HorizontalPanel gridAndVerticalPan = new HorizontalPanel().setHorizontalSpacer(1);

                    gridAndVerticalPan.add(draggableGrid);

                    VerticalPanel vpForPanY = new VerticalPanel().addText("^");
                    vpForPanY.add(panYScroll);
                    vpForPanY.addText("v");
                    gridAndVerticalPan.add(vpForPanY);

                    vpMainPanel.add(gridAndVerticalPan);

                    HorizontalPanel hpPanX = new HorizontalPanel().addText("<");
                    hpPanX.add(panXScroll);
                    hpPanX.addText(">");
                    vpMainPanel.add(hpPanX);

                    boolean[] simulationActive = new boolean[] { true };

                    // tApp.getEventBus().appendIdleEventHandler(new RepeatingInterval(100, (te) -> {
                    //     if (!simulationActive[0]) {
                    //         return;
                    //     }
                    //     Position position = pp.nextPosition(15);
                    //     // simulate mouse drag (sort-of)
                    //     rotateOnGrid.onDrag(position, grid.getGridRectangle());
                    //     // update screen
                    //     redrawShape.run();
                    // }));

                    tApp.getEventBus().appendIdleEventHandler(new RepeatingInterval(100, (te) -> {
                        if (!simulationActive[0]) return;
                        
                        // Rotate the objects locally
                        cube3D.setRotX(cube3D.getRotX() + 0.1f);
                        sphere3D.setRotY(sphere3D.getRotY() + 0.1f);
                        
                        redrawShape.run();
                    }));                    

                    RadioGroup<Boolean> simulationToggle = RadioGroup.onOff("Simulation", "START", "STOP", true).setColor(grid.getColor());

                    Runnable startStopSimulation = () -> {
                        if (simulationToggle.getValue()) {
                            simulationActive[0] = true;
                        } else {
                            simulationActive[0] = false;

                        }
                    };

                    simulationToggle.onChange((te) -> {
                        startStopSimulation.run();
                    });

                    shapeChoice.setValue("Cube");

                    // 3. Update Dropdown logic
                    shapeChoice.onChange((te) -> {
                        String choice = shapeChoice.getValue();
                        showBoth[0] = choice.equals("Both");
                        redrawShape.run();
                    });

                    plot2DInputForm.onChange((te) -> {
                        simulationToggle.setValue(false);
                        redrawShape.run();
                    });

                    HorizontalPanel controlsPanel = new HorizontalPanel().setHorizontalSpacer(2);
                    controlsPanel.add(plot2DInputForm);
                    controlsPanel.add(simulationToggle);
                    controlsPanel.add(shapeChoice);

                    vpMainPanel.add(controlsPanel);

                    tApp.getMainApplicationPanel().add(vpMainPanel);

                    // everything sized by the window: the grid (what the controls leave), what the camera sees, the
                    // pan bars - recentred; then the shapes drawn again. Now, and whenever the window changes.
                    Runnable fitToWindow = () -> {
                        grid.resize(gridSizeFor(tApp.getMainApplicationPanel()));
                        int width = grid.getGridRectangle().getWidth(), height = grid.getGridRectangle().getHeight();
                        projection.setWindowSize(width, height);
                        projection.setPanX(0);
                        projection.setPanY(0);
                        Dimension viewer = new Dimension.Simple(height, width);
                        Dimension panRange = new Dimension.Simple(2 * height, 2 * width);     // the viewer, and half again each way
                        panXScroll.resize(new Dimension.Simple(1, Math.max(0, grid.getWidth() - 2)));    // between the corners
                        panYScroll.resize(new Dimension.Simple(Math.max(0, grid.getHeight() - 2), 1));
                        panXScroll.setViewedSize(panRange, viewer);
                        panYScroll.setViewedSize(panRange, viewer);
                        panXScroll.setOffset(new Position.Simple(height / 2, width / 2));
                        panYScroll.setOffset(new Position.Simple(height / 2, width / 2));
                        plot2DInputForm.refreshFromProjection();
                        redrawShape.run();
                    };
                    fitToWindow.run();
                    tApp.appendOnResizeAction((ta, size) -> fitToWindow.run());

                }).start();
    }

    /** the grid: what the controls leave of the window - none in a small one */
    static Dimension gridSizeFor(Dimension window) {
        return new Dimension.Simple(Math.max(0, window.getHeight() - 7), Math.max(0, window.getWidth() - 12));
    }

    static class Plot2DConfigForm extends ComponentWithFocus<HorizontalPanel, Plot2DConfigForm>
            implements HasValue<Plot2D> {

        Plot2DSimple plot2D;

        Vector3DPanel cameraInput;
        TextInputFloat distance;
        Vector3DPanel anglesInput;

        ChainedEventHandler chainedEventHandler;

        public Plot2DConfigForm(Color color) {
            super(color, new HorizontalPanel());

            HorizontalPanel controlsInGrid = getWrappedComponent();

            cameraInput = new Vector3DPanel(color, "Camera");
            distance = new TextInputFloat(5);
            distance.setText(String.valueOf(0f));
            anglesInput = new Vector3DPanel(color, "Angles (degrees)");

            controlsInGrid.add(cameraInput);

            controlsInGrid.add(new TextComponent("Distance"));
            controlsInGrid.add(distance);

            controlsInGrid.add(anglesInput);

            plot2D = new Plot2DSimple();

            EventHandler evtHandlerBroadcastValueChanged = (te) -> {
                plot2D.setCamera(cameraInput.getValue());
                plot2D.setDistance(distance.getValue());
                plot2D.setThetaX(anglesInput.getValue().getX());
                plot2D.setThetaY(anglesInput.getValue().getY());
                plot2D.setThetaZ(anglesInput.getValue().getZ());
                te.getEventBus().raiseEvent(new HasValue.ValueChangedEvent<Plot2D>(Plot2DConfigForm.this));
            };

            cameraInput.onChange((te) -> {
                evtHandlerBroadcastValueChanged.handle(te);
            });

            distance.onChange((te) -> {
                if (distance.getValue() < 0.01) {
                    distance.setText(String.valueOf("0.01"));
                }
                evtHandlerBroadcastValueChanged.handle(te);
            });

            anglesInput.onChange((te) -> {
                evtHandlerBroadcastValueChanged.handle(te);
            });

            chainedEventHandler = ChainedEventHandler.useChainOfResponsibilityStrategy(this);
            focus().handleEvents(chainedEventHandler.asConditionalEventHandler());


        }


        private final OnChangeListeners<Plot2D> changeListeners = new OnChangeListeners<Plot2D>(this);

        public Registration onChange(EventHandler handler) {

            return changeListeners.add(chainedEventHandler, handler);

        }

        @Override
        public Plot2D getValue() {
            return plot2D;
        }

    }

    static class Plot2DConfigFormProjection extends ComponentWithFocus<HorizontalPanel, Plot2DConfigFormProjection>
            implements HasValue<Plot2D> {

        Plot2DFromProjection plot2D;

        TextInputFloat distance;
        Vector3DPanel anglesInput;

        ChainedEventHandler chainedEventHandler;

        public Plot2DConfigFormProjection(Color color) {
            super(color, new HorizontalPanel());

            HorizontalPanel controlsInGrid = getWrappedComponent();

            distance = new TextInputFloat(5);
            distance.setText(String.valueOf(Plot2DFromProjection.DEFAULT_DISTANCE));

            anglesInput = new Vector3DPanel(color, "Angles (degrees)", true);

            controlsInGrid.add(new TextComponent("Zoom/Distance"));
            controlsInGrid.add(distance);

            controlsInGrid.add(anglesInput);

            plot2D = new Plot2DFromProjection();

            EventHandler evtHandlerBroadcastValueChanged = (te) -> {
                float zoomDistance = distance.getValue();

                if (zoomDistance < 10f) {
                    zoomDistance = 10f;
                    distance.setText(String.valueOf(zoomDistance));
                }

                plot2D.setDistance(zoomDistance);
                plot2D.setThetaX(anglesInput.getValue().getX());
                plot2D.setThetaY(anglesInput.getValue().getY());
                plot2D.setThetaZ(anglesInput.getValue().getZ());

                te.getEventBus().raiseEvent(
                        new HasValue.ValueChangedEvent<Plot2D>(Plot2DConfigFormProjection.this));
            };

            distance.onChange((te) -> {
                evtHandlerBroadcastValueChanged.handle(te);
            });

            anglesInput.onChange((te) -> {
                evtHandlerBroadcastValueChanged.handle(te);
            });

            chainedEventHandler = ChainedEventHandler.useChainOfResponsibilityStrategy(this);
            focus().handleEvents(chainedEventHandler.asConditionalEventHandler());

        }

        public void refreshFromProjection() {
            distance.setText(String.valueOf(plot2D.getDistance()));

            anglesInput.setValue(
                    new BaseVec3(
                            plot2D.getThetaX(),
                            plot2D.getThetaY(),
                            plot2D.getThetaZ()));
        }



        private final OnChangeListeners<Plot2D> changeListeners = new OnChangeListeners<Plot2D>(this);

        public Registration onChange(EventHandler handler) {

            return changeListeners.add(chainedEventHandler, handler);

        }

        @Override
        public Plot2D getValue() {
            return plot2D;
        }

    }

    static class Vector3DPanel extends ComponentWithFocus<HorizontalPanel, Vector3DPanel>
            implements HasValue<Vec3<?>> {

        Vec3<?> vector;
        FloatInputWithIncrDecrButtons xInput;
        FloatInputWithIncrDecrButtons yInput;
        FloatInputWithIncrDecrButtons zInput;

        ChainedEventHandler chainedEventHandler;

        public Vector3DPanel(Color color, String label, String x_label, String y_label, String z_label,
                boolean useUpDown) {
            super(color, new HorizontalPanel());

            HorizontalPanel controlsInGrid = getWrappedComponent().setHorizontalSpacer(2);

            vector = new BaseVec3(0f, 0f, 0f);
            xInput = new FloatInputWithIncrDecrButtons(color, x_label, "-", "+");
            yInput = new FloatInputWithIncrDecrButtons(color, y_label, "-", "+");
            zInput = new FloatInputWithIncrDecrButtons(color, z_label, "-", "+");

            setValue(vector);

            controlsInGrid.add(new TextComponent(label + " : "));

            controlsInGrid.add(xInput);

            controlsInGrid.add(yInput);

            controlsInGrid.add(zInput);

            // emit valueChanged events when the above controls are used
            EventHandler evtHandlerBroadcastValueChanged = (te) -> {
                vector = new BaseVec3(xInput.getValue(), yInput.getValue(), zInput.getValue());
                te.getEventBus().raiseEvent(new HasValue.ValueChangedEvent<Vec3<?>>(Vector3DPanel.this));
            };

            // handle incr/decr requests
            xInput.onChange((te) -> {
                evtHandlerBroadcastValueChanged.handle(te);
            });

            yInput.onChange((te) -> {
                evtHandlerBroadcastValueChanged.handle(te);
            });

            zInput.onChange((te) -> {
                evtHandlerBroadcastValueChanged.handle(te);
            });

            chainedEventHandler = ChainedEventHandler.useChainOfResponsibilityStrategy(this);
            focus().handleEvents(chainedEventHandler.asConditionalEventHandler());


        }

        public Vector3DPanel(Color color, String label, boolean useUpDown) {
            this(color, label, "X", "Y", "Z", useUpDown);
        }

        public Vector3DPanel(Color color, String label) {
            this(color, label, false);
        }

        public Vector3DPanel(Color color) {
            this(color, "Point3D");
        }


        private final OnChangeListeners<Vec3<?>> changeListeners = new OnChangeListeners<Vec3<?>>(this);

        public Registration onChange(EventHandler handler) {

            return changeListeners.add(chainedEventHandler, handler);

        }

        public void setValue(Vec3<?> value) {
            this.vector = value;
            this.xInput.setValue(this.vector.getX());
            this.yInput.setValue(this.vector.getY());
            this.zInput.setValue(this.vector.getZ());
        }

        @Override
        public Vec3<?> getValue() {
            return vector;
        }
    }

}
