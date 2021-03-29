package examples;

import java.io.IOException;
import java.net.URI;

import org.junit.Test;

import com.arosbio.auth.InvalidLicenseException;
import com.arosbio.commons.auth.AuthenticationFactory;
import com.arosbio.commons.auth.CPSignLicense;
import com.arosbio.modeling.CPSignFactory;

import utils.Config;
import utils.Utils;

public class AuthenticateLicenseToCPSign {

	@Test
	public void authenticate() throws InvalidLicenseException, IOException {
		// Pick the license that you have, according to the config.properties file
		URI licenseURI = Utils.getURI(Config.getProperties().getProperty("license.standard"));
		
		// Authentication is done by initializing the CPSignFactory. This can be done either directly: 
		@SuppressWarnings("unused")
		CPSignFactory factory = new CPSignFactory(licenseURI);
		
		// Or you can do it by manually loading the license and then passing the license to CPSignFactory
		
		// Licenses can be gotten from the AuthenticationFactory
		CPSignLicense license = AuthenticationFactory.getLicense(licenseURI);
		System.out.println("License info: " + license);

		// Pass the license to the Factory
		factory = new CPSignFactory(license);
		
	}

}
