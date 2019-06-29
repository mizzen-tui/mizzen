package dtb.terminalIO.util.formatters;

import dtb.terminalIO.controlcodes.Colors;
import dtb.terminalIO.controlcodes.ControlCode;
import dtb.terminalIO.controlcodes.ControlCode.Color;

public class ClippToLengthTextFormatter implements TextLineFormatter
{

    int clipLength;
    Color hiliteColor;
    
    public ClippToLengthTextFormatter(int clipLength, Color color)
    {
        super();
        if ( clipLength < 1)
        {
            throw new IllegalArgumentException("clipLength " + clipLength + " too small" );
        }
        this.clipLength = clipLength;
        this.hiliteColor = color;
    }

    public ClippToLengthTextFormatter(int clipLength)
    {
        this(clipLength, Colors.BBlu);
    }


    @Override
    public String getFormattedText(String line)
    {
        if (line == null)
        {
            return line;
        }
        
        if (line.length() > this.clipLength)
        {
            line = line.substring(0, Math.max(this.clipLength - 5,1)) + hiliteColor.asString() + " ... " + ControlCode.RCol.asString();
        }
        return line;
    }

}

