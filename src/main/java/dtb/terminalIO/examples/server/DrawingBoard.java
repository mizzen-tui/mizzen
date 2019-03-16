package dtb.terminalIO.examples.server;

import java.io.IOException;

import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Position;

public interface DrawingBoard {

    void clear();
    
    void component(String name , Position pos, Dimension dim, boolean hasBorder);

    void component(String name , Position pos, Dimension dim, String title);
    
    void component(String name , Position pos, Dimension dim);
    
    void title(String name, String title);
    
    void textAt(String name, String position);
    
    void appendText(String name, String text);
    
    void componentClear(String name);

    void setAutoRefresh(boolean auto);

    void refresh();

    void pipeTextFromCommand(String name, String[] cmd) throws IOException;
    
    
    
}
