package dtb.terminalIO.examples.plot;

import java.io.IOException;

import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.components.AbstractWrappedComponentWithFocusManager;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.composite.HorizontalPanel;
import dtb.terminalIO.components.composite.VerticalPanel;
import dtb.terminalIO.components.input.FloatInputWithIncrDecrButtons;
import dtb.terminalIO.components.input.HasValue;
import dtb.terminalIO.components.input.TextInputFloat;
import dtb.terminalIO.components.plot.braille.BrailleGrid;
import dtb.terminalIO.components.plot.shape.CompositeShape;
import dtb.terminalIO.components.plot.shape.Line;
import dtb.terminalIO.components.plot.shape.Shape;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.events.ChainedEventHandler;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.focus.DefaultFocusManager;
import dtb.terminalIO.events.focus.FocusManager;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.position.Vec2;
import dtb.terminalIO.geom.position.Vec3;
import dtb.terminalIO.geom.position.Vec3.BaseVec3;
import dtb.terminalIO.geom.position.Vec3.Plot2D;
import dtb.terminalIO.geom.position.Vec3.Plot2DFromProjection;
import dtb.terminalIO.geom.position.Vec3.Plot2DSimple;

public class TestCube {

    public static void main(String[] args) throws IOException {
        
        new TUIApp(args)
               .setEmptyChar(' ')
               .setDefaultMainApplicationPanel()
               .confirmOnExit()
               .setOnReadyStart((tApp)->{
                   
                   Dimension gridSize = new Dimension.Simple(tApp.getMainApplicationPanel().getHeight()-5,
                                                             tApp.getMainApplicationPanel().getWidth()-10);

                   VerticalPanel vpMainPanel = new VerticalPanel().setVerticalSpacer(2);
                   
                   BrailleGrid grid = new BrailleGrid(Colors.BGre, gridSize);
                   
                   vpMainPanel.add(grid);

                   tApp.getMainApplicationPanel().add(vpMainPanel);

                   Cube cube3D = new Cube();

                   Plot2DConfigFormProjection plot2DInputForm = new Plot2DConfigFormProjection(grid.getColor());
                   vpMainPanel.add(plot2DInputForm);
                   
                   plot2DInputForm.onChange((te)-> {
                                                    grid.clearPlots();
                                                    cube3D.as2DShape(plot2DInputForm.getValue()).draw(grid.asPointPlotter());
                                                   } );
                   
                   cube3D.as2DShape(plot2DInputForm.getValue()).draw(grid.asPointPlotter());


               }).start();
    }
        
    
    
    
    public static class Cube {
        int size = 50; 
        Vec3<?> ulf, urf, llf, lrf; // upper left front, upper right front, usw..
        Vec3<?> ulb, urb, llb, lrb; // upper left back ,...
        
        public Cube(int size, Vec3<?> position3D) {
            
            // define back panel
            ulb = new BaseVec3(position3D.getX(), position3D.getY() , position3D.getZ() - size);
            urb = new BaseVec3(position3D.getX() + size, position3D.getY() , position3D.getZ() - size);
            llb = new BaseVec3(position3D.getX(), position3D.getY() + size, position3D.getZ() - size);
            lrb = new BaseVec3(position3D.getX() + size, position3D.getY() + size, position3D.getZ() - size);
            
            // define front panel
            ulf = new BaseVec3(position3D.getX(), position3D.getY() , position3D.getZ());
            urf = new BaseVec3(position3D.getX() + size, position3D.getY() , position3D.getZ());
            llf = new BaseVec3(position3D.getX(), position3D.getY() + size, position3D.getZ());
            lrf = new BaseVec3(position3D.getX() + size, position3D.getY() + size, position3D.getZ());

        }

        public Cube() {
            this(40, Vec3.ZERO);
        }
        
        
        public Shape<?> as2DShape(Plot2D as2D) {

            // get the 2D equivalent points
            Vec2<?> ulb_2D = ulb.plot2D(as2D);
            Vec2<?> urb_2D = urb.plot2D(as2D);
            Vec2<?> llb_2D = llb.plot2D(as2D);
            Vec2<?> lrb_2D = lrb.plot2D(as2D);
            
            Vec2<?> ulf_2D = ulf.plot2D(as2D);
            Vec2<?> urf_2D = urf.plot2D(as2D);
            Vec2<?> llf_2D = llf.plot2D(as2D);
            Vec2<?> lrf_2D = lrf.plot2D(as2D);
            
            // now make the lines add add to shape
            
            Shape<?> cube2D = new CompositeShape().addShape( new Line(ulb_2D, urb_2D) )   // back
                                                  .addShape( new Line(ulb_2D, llb_2D))
                                                  .addShape( new Line(urb_2D, lrb_2D))
                                                  .addShape( new Line(llb_2D, lrb_2D))
                                                  .addShape( new Line(ulf_2D, urf_2D) )   // front
                                                  .addShape( new Line(ulf_2D, llf_2D))
                                                  .addShape( new Line(urf_2D, lrf_2D))
                                                  .addShape( new Line(llf_2D, lrf_2D))
                                                  .addShape( new Line(ulf_2D, ulb_2D))    // connect front to back
                                                  .addShape( new Line(urf_2D, urb_2D))
                                                  .addShape( new Line(llf_2D, llb_2D))
                                                  .addShape( new Line(lrf_2D, lrb_2D));  
            
            
            return cube2D;
        }
        
    }
    
    
    static class Plot2DConfigForm extends AbstractWrappedComponentWithFocusManager<HorizontalPanel> implements HasValue<Plot2D> {

        Plot2DSimple plot2D;

        Vector3DPanel  cameraInput;
        TextInputFloat distance;
        Vector3DPanel  anglesInput;
        
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
            
            cameraInput.onChange((te)->{
                evtHandlerBroadcastValueChanged.handle(te);
            });
            
            distance.onChange((te)->{
                if (distance.getValue() < 0.01) {
                    distance.setText( String.valueOf( "0.01" ) );
                }
                evtHandlerBroadcastValueChanged.handle(te);
            });

            anglesInput.onChange((te)->{
                evtHandlerBroadcastValueChanged.handle(te);
            });
            
            chainedEventHandler = ChainedEventHandler.useChainOfResponsibilityStrategy(this);
            setCustomExtendedEventHandler( chainedEventHandler.asConditionalEventHandler() );

            registerAllFocusables();
            

        }


        OnValueChangedEventHandler<Plot2D> actionOnChangeHandler;

        public void onChange(EventHandler handler) {

            actionOnChangeHandler = HasValue.UTIL.bindConsumerAsOnValueChangedHandler(this, actionOnChangeHandler, chainedEventHandler, handler);

        }
        
        @Override
        public Plot2D getValue() {
            return plot2D;
        }

    }

    static class Plot2DConfigFormProjection extends AbstractWrappedComponentWithFocusManager<HorizontalPanel> implements HasValue<Plot2D> {

        Plot2DFromProjection plot2D;

        TextInputFloat distance;
        Vector3DPanel  anglesInput;
        
        ChainedEventHandler chainedEventHandler;

        public Plot2DConfigFormProjection(Color color) {
            super(color, new HorizontalPanel());

            HorizontalPanel controlsInGrid = getWrappedComponent();

            distance = new TextInputFloat(5);
            distance.setText(String.valueOf(50f));
            anglesInput = new Vector3DPanel(color, "Angles (degrees)", true);

            controlsInGrid.add(new TextComponent("Distance"));
            controlsInGrid.add(distance);
            
            controlsInGrid.add(anglesInput);
            
            plot2D = new Plot2DFromProjection();

            float[] distanceValue = new float[]{50.0f};
            
            EventHandler evtHandlerBroadcastValueChanged = (te) -> {
                plot2D.setDistance(distanceValue[0]);
                plot2D.setThetaX(anglesInput.getValue().getX());
                plot2D.setThetaY(anglesInput.getValue().getY());
                plot2D.setThetaZ(anglesInput.getValue().getZ());
                te.getEventBus().raiseEvent(new HasValue.ValueChangedEvent<Plot2D>(Plot2DConfigFormProjection.this));
            };
            
            distance.onChange((te)->{
                distanceValue[0] = distance.getValue(); 
                if (distanceValue[0] < 0.01) {
                    distanceValue[0] = 0.01f;
                }
                evtHandlerBroadcastValueChanged.handle(te);
            });

            anglesInput.onChange((te)->{
                evtHandlerBroadcastValueChanged.handle(te);
            });
            
            chainedEventHandler = ChainedEventHandler.useChainOfResponsibilityStrategy(this);
            setCustomExtendedEventHandler( chainedEventHandler.asConditionalEventHandler() );

            registerAllFocusables();
            

        }


        OnValueChangedEventHandler<Plot2D> actionOnChangeHandler;

        public void onChange(EventHandler handler) {

            actionOnChangeHandler = HasValue.UTIL.bindConsumerAsOnValueChangedHandler(this, actionOnChangeHandler, chainedEventHandler, handler);

        }
        
        @Override
        public Plot2D getValue() {
            return plot2D;
        }

    }
    
    
    static class Vector3DPanel  extends AbstractWrappedComponentWithFocusManager<HorizontalPanel> implements HasValue<Vec3<?>> {

        Vec3<?>             vector;
        FloatInputWithIncrDecrButtons      xInput;
        FloatInputWithIncrDecrButtons      yInput;
        FloatInputWithIncrDecrButtons      zInput;

        ChainedEventHandler chainedEventHandler;

        public Vector3DPanel(Color color, String label, String x_label, String y_label, String z_label, boolean useUpDown) {
            super(color, new HorizontalPanel());

            HorizontalPanel controlsInGrid = getWrappedComponent().setHorizontalSpacer(2);
            
            vector = new BaseVec3(0f, 0f, 0f);
            xInput = new FloatInputWithIncrDecrButtons(color, x_label,"-","+");
            yInput = new FloatInputWithIncrDecrButtons(color, y_label,"-","+");
            zInput = new FloatInputWithIncrDecrButtons(color, z_label,"-","+");

            setValue(vector);

            controlsInGrid.add(new TextComponent(label+" : "));

            controlsInGrid.add(xInput);

            controlsInGrid.add(yInput);

            controlsInGrid.add(zInput);

            // emit valueChanged events when the above controls are used
            EventHandler evtHandlerBroadcastValueChanged = (te) -> {
                vector = new BaseVec3(xInput.getValue(), yInput.getValue(), zInput.getValue());
                te.getEventBus().raiseEvent(new HasValue.ValueChangedEvent<Vec3<?>>(Vector3DPanel.this));
            };

            // handle incr/decr requests
            xInput.onChange((te)->{
                evtHandlerBroadcastValueChanged.handle(te);
            });

            yInput.onChange((te)->{
                evtHandlerBroadcastValueChanged.handle(te);
            });

            zInput.onChange((te)->{
                evtHandlerBroadcastValueChanged.handle(te);
            });

            
            chainedEventHandler = ChainedEventHandler.useChainOfResponsibilityStrategy(this);
            setCustomExtendedEventHandler( chainedEventHandler.asConditionalEventHandler() );

            registerAllFocusables();

        }
        
        public Vector3DPanel(Color color, String label, boolean useUpDown) {
            this(color, label , "X", "Y", "Z", useUpDown);
        }

        public Vector3DPanel(Color color, String label) {
            this(color, label , false);
        }

        public Vector3DPanel(Color color) {
            this(color, "Point3D");
        }


        OnValueChangedEventHandler<Vec3<?>> actionOnChangeHandler;

        public void onChange(EventHandler handler) {

            actionOnChangeHandler = HasValue.UTIL.bindConsumerAsOnValueChangedHandler(this, actionOnChangeHandler, chainedEventHandler, handler);

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
