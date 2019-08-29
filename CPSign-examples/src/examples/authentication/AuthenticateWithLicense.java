package examples.authentication;

import java.io.IOException;

import com.arosbio.auth.InvalidLicenseException;
import com.arosbio.commons.auth.AuthenticationFactory;
import com.arosbio.commons.auth.CPSignLicense;
import com.arosbio.modeling.CPSignFactory;

import examples.utils.Config;

public class AuthenticateWithLicense {

	public static void main(String[] args) throws InvalidLicenseException, IOException {
		// Authentication is done by initializing the CPSignFactory. This can be done either directly: 
		@SuppressWarnings("unused")
		CPSignFactory factory = new CPSignFactory(Config.STANDARD_LICENSE);
		
		// Or you can do it by manually loading the license and then passing the license to CPSignFactory
		
		// Licenses can be gotten from the AuthenticationFactory
		CPSignLicense license = AuthenticationFactory.getLicense(Config.STANDARD_LICENSE);
		System.out.println("License info: " + license.toString());

		// Pass the license to the Factory
		factory = new CPSignFactory(license);

	}

}
