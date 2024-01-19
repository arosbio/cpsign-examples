package examples;

import java.util.Iterator;
import java.util.ServiceLoader;

import org.junit.Test;

import com.arosbio.ml.cp.nonconf.NCM;

public class ListAvailableServices {
    
    /*
	 * Note that CPSign uses ServiceLoader functionality which allows the user to 
	 * both list all available implementations and to extend CPSign with the things you need.
	 * This can also be applied to the CLI as long as you implement and adhere the 
	 * requirements for Java services. Currently these interfaces can be extended:
	 * - Descriptor
	 * - Transformer
	 * - MLAlgorithm
	 * - PValueCalculator
	 * - NCM
	 * - SamplingStrategy
	 * - Metric
	 * - TestingStrategy
	 */
	@Test
	public void listAvailableImplementations() throws Exception {
		ServiceLoader<NCM> ncmLoader = ServiceLoader.load(NCM.class); 

		System.out.println("All available NCMs:");
		Iterator<NCM> iter = ncmLoader.iterator();
		while (iter.hasNext()) {
			System.out.println(iter.next().getName());
		}

	}

}
