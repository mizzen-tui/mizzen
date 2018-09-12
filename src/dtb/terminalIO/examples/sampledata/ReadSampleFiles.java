package dtb.terminalIO.examples.sampledata;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import dtb.terminalIO.ControlCode;

public class ReadSampleFiles {
    
    public static String readSampleFile(String relativeFileName) throws IOException {
	InputStream stream = ReadSampleFiles.class.getResourceAsStream(relativeFileName);
	BufferedInputStream in = new BufferedInputStream(stream);
	StringBuffer sb = new StringBuffer();
	while (in.available()>0) {
	    int ch = in.read();
	    sb.append( (char) ch);
	}
	return sb.toString();
    }
    
    public static String readSampleFile(String relativeFileName, boolean stripColors) throws IOException {
	String contents = readSampleFile(relativeFileName);
	if (stripColors) {
	    contents = ControlCode.removeColorCodes(contents);
	}
	return contents;
    }
    
    public static void main(String args[]) throws Exception {
	System.out.println( readSampleFile("acme-white-200.txt", false ));
    }

}
