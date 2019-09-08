package dtb.terminalIO.examples;

import java.io.IOException;
import java.util.Arrays;
import java.util.Random;
import java.util.function.Function;

import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.components.ButtonBar;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.Composite;
import dtb.terminalIO.components.HorizontalPanel;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.VerticalPanel;
import dtb.terminalIO.components.chart.SparkLineComponent;
import dtb.terminalIO.components.input.Button;
import dtb.terminalIO.controlcodes.Colors;
import dtb.terminalIO.controlcodes.ControlCode;
import dtb.terminalIO.events.ConditionalEventHandler;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.OnIdleEventEventHandler;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.TerminalEvent.IS_Event_TYPE;
import dtb.terminalIO.events.TerminalEvent.TYPE;
import dtb.terminalIO.events.focus.FocusManager;
import dtb.terminalIO.events.focus.Focusable;
import dtb.terminalIO.events.focus.RequestFocusEvent;
import dtb.terminalIO.events.focus.ToFocusable;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.events.mouse.ApplicationMouseEvent;
import dtb.terminalIO.events.mouse.GetComponentAtOffset;
import dtb.terminalIO.events.mouse.MOUSE_ACTION;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.util.Logging;

public class SparklineTest {
    
    public static void main(String[] args) throws IOException {

        Logging.setEnabled(true);
        
        new TUIApp(args)
                .setCanvasSize(24, 100)
                .setEmptyChar(' ')
                .setMainApplicationPanel(new Composite().setBounds(20, 100).setEmptyChar(' ').setBorder(true))
                .confirmOnExit()
                .setOnReadyStart((tApp)-> {
                    
                    Function<Integer,String> hiliteSparklineHeight = (height)-> ((height==1||height==20)? Colors.IRed.asString() : Colors.IGre.asString()) + height + ControlCode.RCol.asString();
                    
                    //Function<String,String> hiliteHotKey = (s)-> Colors.IGre.asString() + s + ControlCode.RCol.asString();

                    SparkLineComponent sparkLine1 = new SparkLineComponent(Colors.BCya);
                    SparkLineComponent sparkLine2 = new SparkLineComponent(Colors.BIGre);
                    TextComponent sparkLine1Label = new TextComponent("Sparkline : \nheight="+hiliteSparklineHeight.apply(sparkLine1.getHeight()));
                    TextComponent sparkLine2Label = new TextComponent("Sparkline : \nheight="+sparkLine2.getHeight());
                    
                    ButtonBar bannerPanel = new ButtonBar().setHorizontalSpacer(0);
                    bannerPanel.setBorder(false);
                    bannerPanel.setNoBorderChars();
                    bannerPanel.add(new TextComponent("Demonstrate Sparkline Component:    "));
                    bannerPanel.addButton(Colors.IGre,"CTRL-C",Keys.CTRL_C_KEYSTROKE, (te) -> tApp.getTerminateAppHotKeyEventHandler().handle(te)  );
                    bannerPanel.add(new TextComponent(" to quit "));
                    bannerPanel.addButton(Colors.IGre,"UP", Keys.UP_KEYSTROKE, (te)->{
                        if (sparkLine1.getHeight()<20) {
                            sparkLine1.resize( new Dimension.Simple(sparkLine1.getHeight()+1, sparkLine1.getWidth()) );
                            sparkLine1Label.setText("Sparkline : \nheight="+hiliteSparklineHeight.apply(sparkLine1.getHeight()));
                        }
                    } ); 
                    // need to bind the events below - chicken and egg situation ( forward declaration issue with sparkline1)
                    bannerPanel.add(new TextComponent("/"));
                    bannerPanel.addButton(Colors.IGre,"DOWN", Keys.DOWN_KEYSTROKE, (te)->{
                        if (sparkLine1.getHeight()>1) {
                            sparkLine1.resize( new Dimension.Simple(sparkLine1.getHeight()-1, sparkLine1.getWidth()) );
                            sparkLine1Label.setText("Sparkline : \nheight="+ hiliteSparklineHeight.apply(sparkLine1.getHeight()));
                        }
                    });
                    bannerPanel.add(new TextComponent(" increase/decrease sparkline height"));
                    tApp.getMainApplicationPanel().add(1, 1,bannerPanel);

                    sparkLine1.resize(new Dimension.Simple(5, 40));
                    sparkLine2.resize(new Dimension.Simple(1, 40));
                    
                    VerticalPanel vp = new VerticalPanel();
                    TextComponent dataPoints = new TextComponent(" .. ");
                    
                    vp.add(new HorizontalPanel().add(new TextComponent("Data      : ")).add(dataPoints));
                    vp.add(new HorizontalPanel().add(sparkLine1Label).add(sparkLine1));
                    vp.add(new HorizontalPanel().add(sparkLine2Label).add(sparkLine2));
                    vp.setVerticalSpacer(1);
                    
                    
                    tApp.getMainApplicationPanel().add(3, 1, vp);
                    
                    Random rint = new Random();
                    
                    // traverse main app panel find focusable's and build a focusManager and attach to Eventbus
                    
                    FocusManager mainFocusManager = FocusManager.UTIL.buildFocusManagerFromComponent(tApp.getMainApplicationPanel(),tApp.getEventBus());
                    
                    tApp.getEventBus().appendEventHandler(  mainFocusManager );
                    
//                    tApp.getEventBus().appendEventHandler( new ConditionalEventHandler() {
//
//                        IS_Event_TYPE appMouseType = new IS_Event_TYPE(TYPE.APPLICATION_MOUSE);
//                        
//                        @Override
//                        public EventHandler canHandle(TerminalEvent<?> te) {
//                            if (appMouseType.test(te)) {
//                                return this;
//                            }
//                            return null;
//                        }
//
//                        @Override
//                        public void handle(TerminalEvent<?> te) {
//                            ApplicationMouseEvent appMouseEvent = (ApplicationMouseEvent) te;
//
//                            if (appMouseEvent.getMouseAction() != MOUSE_ACTION.DOWN_LEFT) {
//                                return;
//                            }
//
//                            Position canvasPosition = appMouseEvent.getEventData();
//                            
//                            Component<?> componentClicked = GetComponentAtOffset.UTIL.getComponentAtOffset( (Component<?>)tApp.getMainApplicationPanel(), canvasPosition );
//                            
//                            // componentClicked - might not be a Focusable .. so go up its ancestors
//                            // and find an ancestor that is focusable
//                            
//                            while ( componentClicked!= null && 
//                                    !(componentClicked instanceof Focusable) &&
//                                    !(componentClicked instanceof ToFocusable)) {
//                                componentClicked = (Component<?>)componentClicked.getParentContainer();
//                            }
//                            
//                            if (componentClicked == null) {
//                                return;
//                            }
//
//                            Focusable focusComponent = null;
//                            
//                            if (componentClicked instanceof Focusable) {
//                                focusComponent = (Focusable)componentClicked;
//                            } else if (componentClicked instanceof ToFocusable) {
//                                focusComponent = ((ToFocusable) componentClicked).asFocusable();
//                            }
//                            
//                            if (focusComponent != null) {
//                                tApp.getEventBus().raiseEvent( new RequestFocusEvent(focusComponent));
//                                if (focusComponent instanceof Button) {
//                                    Button clickable = (Button)focusComponent;
//                                    tApp.getEventBus().appendEventHandler( new OnIdleEventEventHandler.OneTime((te2)-> {
//                                        if (clickable.hasFocus()) {
//                                            clickable.onClick( te2.getEventBus() );
//                                        }
//                                    })); 
//                                }
//                            }
//                            
//
//                            
//                        }
//
//                        
//                    });
                    
                    
                    tApp.getEventBus().appendEventHandler( new OnIdleEventEventHandler.RepeatingInterval(100, (te)-> {
                        
                        Integer[] data = new Integer[30];

                        for (int i = 0; i < data.length; i++) {
                            data[i] = rint.nextInt(100);
                        }

                        sparkLine1.renderPoints(data);
                        sparkLine2.renderPoints(data);
                        
                        dataPoints.setTextAndResize( Arrays.deepToString(data) );
                        
                        tApp.doRefreshCanvasIfModified();
                        
                    } ));
                    

                }).start();
    }
    
    

}
