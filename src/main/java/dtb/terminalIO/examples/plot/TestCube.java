package dtb.terminalIO.examples.plot;

import java.io.IOException;

import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.components.AbstractWrappedComponentWithFocusManager;
import dtb.terminalIO.components.ClickableDraggableComponentWrapper;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.composite.HorizontalPanel;
import dtb.terminalIO.components.composite.VerticalPanel;
import dtb.terminalIO.components.input.DropdownListBox;
import dtb.terminalIO.components.input.FloatInputWithIncrDecrButtons;
import dtb.terminalIO.components.input.HasValue;
import dtb.terminalIO.components.input.RockerButton;
import dtb.terminalIO.components.input.TextInputFloat;
import dtb.terminalIO.components.plot.PathPlotter;
import dtb.terminalIO.components.plot.PathPlotter.CirclePathPlotter;
import dtb.terminalIO.components.plot.braille.BrailleGrid;
import dtb.terminalIO.components.plot.braille.BrailleUtils;
import dtb.terminalIO.components.plot.grid.DragRotate3DOnGrid;
import dtb.terminalIO.components.plot.grid.DraggableOnGrid;
import dtb.terminalIO.components.plot.shape.threeD.AbstractShape3D;
import dtb.terminalIO.components.plot.shape.threeD.Cube3D;
import dtb.terminalIO.components.plot.shape.threeD.Sphere3D;
import dtb.terminalIO.components.plot.shape.threeD.projection.Plot2D;
import dtb.terminalIO.components.plot.shape.threeD.projection.Plot2DFromProjection;
import dtb.terminalIO.components.plot.shape.threeD.projection.Plot2DSimple;
import dtb.terminalIO.components.scroll.ScrollBarHorizontal;
import dtb.terminalIO.components.scroll.ScrollBarVertical;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.events.ChainedEventHandler;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.OnIdleEventEventHandler.RepeatingInterval;
import dtb.terminalIO.events.RequestUpdateCanvasEvent;
import dtb.terminalIO.events.mouse.Draggable;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.geom.position.Vec3;
import dtb.terminalIO.geom.position.Vec3.BaseVec3;
import dtb.terminalIO.util.Logging;

public class TestCube {

    public static void main(String[] args) throws IOException {

        Logging.setEnabled(true);

        new TUIApp(args)
                .setEmptyChar(' ')
                .setDefaultMainApplicationPanel()
                .confirmOnExit()
                .setOnReadyStart((tApp) -> {

                    float initialCubeDistanceFromCamera = 50f;

                    Dimension gridSize = new Dimension.Simple(
                            tApp.getMainApplicationPanel().getHeight() - 7,
                            tApp.getMainApplicationPanel().getWidth() - 12);

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
                    final boolean[] showBoth = { false }; 

                    Runnable redrawShape = () -> {
                        grid.clearPlots();
                        activeShape[0].  as2DShape(projection).draw(grid.asPointPlotter());
                        tApp.getEventBus().raiseEvent(new RequestUpdateCanvasEvent(vpMainPanel));

                        grid.clearPlots();
                        if (showBoth[0]) {
                            cube3D.as2DShape(projection).draw(grid.asPointPlotter());
                            sphere3D.as2DShape(projection).draw(grid.asPointPlotter());
                        } else {
                            AbstractShape3D<?> s = shapeChoice.getValue().equals("Cube") ? cube3D : sphere3D;
                            s.as2DShape(projection).draw(grid.asPointPlotter());
                        }
                        tApp.getEventBus().raiseEvent(new RequestUpdateCanvasEvent(vpMainPanel));

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

                    int maxPanX = grid.getGridRectangle().getWidth() / 2;
                    int maxPanY = grid.getGridRectangle().getHeight() / 2;

                    Dimension viewerSize = new Dimension.Simple(
                            grid.getGridRectangle().getHeight(),
                            grid.getGridRectangle().getWidth());

                    Dimension virtualPanSize = new Dimension.Simple(
                            viewerSize.getHeight() + (2 * maxPanY),
                            viewerSize.getWidth() + (2 * maxPanX));

                    Position centerPanOffset = new Position.Simple(maxPanY, maxPanX);

                    ScrollBarHorizontal panXScroll = new ScrollBarHorizontal(grid.getColor(), 0, 0,
                            gridSize.getWidth() - 2);

                    ScrollBarVertical panYScroll = new ScrollBarVertical(grid.getColor(), 0, 0,
                            gridSize.getHeight() - 2);

                    panXScroll.setViewedSize(virtualPanSize, viewerSize);
                    panYScroll.setViewedSize(virtualPanSize, viewerSize);

                    panXScroll.setOffset(centerPanOffset);
                    panYScroll.setOffset(centerPanOffset);

                    panXScroll.onChange((te) -> {
                        Position offset = panXScroll.getValue();

                        float panX = maxPanX - offset.getCol();

                        projection.setPanX(panX);
                        redrawShape.run();
                    });

                    panYScroll.onChange((te) -> {
                        Position offset = panYScroll.getValue();

                        float panY = maxPanY - offset.getRow();

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
                    PathPlotter pp = new CirclePathPlotter(maxPanX, maxPanX, 300);

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

                    RockerButton simulationToggle = new RockerButton(grid.getColor(), "Simulation", "START", "STOP",
                            true);

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

                    redrawShape.run();

                }).start();
    }

    static class Plot2DConfigForm extends AbstractWrappedComponentWithFocusManager<HorizontalPanel>
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
            setCustomExtendedEventHandler(chainedEventHandler.asConditionalEventHandler());

            registerAllFocusables();

        }

        OnValueChangedEventHandler<Plot2D> actionOnChangeHandler;

        public void onChange(EventHandler handler) {

            actionOnChangeHandler = HasValue.UTIL.bindConsumerAsOnValueChangedHandler(this, actionOnChangeHandler,
                    chainedEventHandler, handler);

        }

        @Override
        public Plot2D getValue() {
            return plot2D;
        }

    }

    static class Plot2DConfigFormProjection extends AbstractWrappedComponentWithFocusManager<HorizontalPanel>
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
            setCustomExtendedEventHandler(chainedEventHandler.asConditionalEventHandler());

            registerAllFocusables();
        }

        public void refreshFromProjection() {
            distance.setText(String.valueOf(plot2D.getDistance()));

            anglesInput.setValue(
                    new BaseVec3(
                            plot2D.getThetaX(),
                            plot2D.getThetaY(),
                            plot2D.getThetaZ()));
        }


        OnValueChangedEventHandler<Plot2D> actionOnChangeHandler;

        public void onChange(EventHandler handler) {

            actionOnChangeHandler = HasValue.UTIL.bindConsumerAsOnValueChangedHandler(this, actionOnChangeHandler,
                    chainedEventHandler, handler);

        }

        @Override
        public Plot2D getValue() {
            return plot2D;
        }

    }

    static class Vector3DPanel extends AbstractWrappedComponentWithFocusManager<HorizontalPanel>
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
            setCustomExtendedEventHandler(chainedEventHandler.asConditionalEventHandler());

            registerAllFocusables();

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

        OnValueChangedEventHandler<Vec3<?>> actionOnChangeHandler;

        public void onChange(EventHandler handler) {

            actionOnChangeHandler = HasValue.UTIL.bindConsumerAsOnValueChangedHandler(this, actionOnChangeHandler,
                    chainedEventHandler, handler);

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
