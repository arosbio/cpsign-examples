package utils;

import java.io.IOException;
import java.io.OutputStreamWriter;

public class SysOutWriter extends OutputStreamWriter {

	public SysOutWriter() {
		super(System.out);
	}
	
	@Override
	public void close() throws IOException {
		// Do nothing - should not close down the system out stream!
	}
	
}
