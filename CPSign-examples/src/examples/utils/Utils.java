package examples.utils;

import java.io.IOException;
import java.net.URI;

import com.arosbio.auth.InvalidLicenseException;
import com.arosbio.modeling.CPSignFactory;

public class Utils {

	public static CPSignFactory getFactory() {
		try {
			return getFactory(Config.STANDARD_LICENSE);
		} catch ( InvalidLicenseException e) {
			Utils.writeErrAndExit(e.getMessage());
			return null; // never happening
		}
	}

	public static CPSignFactory getFactory(URI license) {
		// Instantiating CPSignFactory with your license
		try{
			return new CPSignFactory(license);
		} catch (IOException | InvalidLicenseException e){
			// Could not load the license (or it's incorrect/invalid)
			Utils.writeErrAndExit(e.getMessage());
			return null; // never happening
		}
	}

	public static void writeErrAndExit(String errMsg){
		System.err.println(errMsg);
		System.exit(1);
	}
}
