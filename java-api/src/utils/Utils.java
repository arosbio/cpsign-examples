package utils;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import com.arosbio.auth.InvalidLicenseException;
import com.arosbio.io.UriUtils;
import com.arosbio.modeling.CPSignFactory;

public class Utils {

	public static CPSignFactory getFactory() {
		try {
			return getFactory(
					new File(UriUtils.resolvePath(
							Config.getProperties().getProperty("license.standard")
							)).toURI());
		} catch ( Exception e ) {
			System.err.println("Failed validating the license to run test examples");
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
