package utils;

import org.junit.BeforeClass;

import com.arosbio.modeling.CPSignFactory;

public class BaseTest {
	
	public static CPSignFactory factory;
	
	@BeforeClass
	public static void init() {
		// Start with instantiating CPSignFactory with your license
		factory = Utils.getFactory();
	}


}
