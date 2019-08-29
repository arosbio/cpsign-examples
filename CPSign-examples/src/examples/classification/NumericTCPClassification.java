package examples.classification;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Map;

import com.arosbio.auth.InvalidLicenseException;
import com.arosbio.modeling.CPSignFactory;
import com.arosbio.modeling.data.Problem;
import com.arosbio.modeling.data.SparseFeature;
import com.arosbio.modeling.ml.cp.tcp.TCPClassification;

import examples.utils.Config;
import examples.utils.Utils;

public class NumericTCPClassification {

	CPSignFactory factory;


	public static void main(String[] args) throws IllegalArgumentException, InvalidLicenseException, MalformedURLException, IllegalAccessException, IOException {
		NumericTCPClassification acp = new NumericTCPClassification();
		acp.intialise();
		acp.predictWithTCPClassification();
		System.out.println("Finished Example Sparse TCP-Classification");
	}
	
	/**
	 * This method just initializes some variables and the CPSignFactory. Please change the 
	 * initialization of CPSignFactory to point to your active license. Also change the 
	 * model and signature-files into a location on your machine so that they can be used 
	 * later on, now temporary files are created for illustrative purposes. 
	 */
	public void intialise(){
		// Start with instantiating CPSignFactory with your license
		factory = Utils.getFactory();
	}


	/**
	 * Loads previously created models and use them to predict
	 * @throws IOException 
	 * @throws MalformedURLException 
	 * @throws InvalidLicenseException 
	 * @throws IllegalArgumentException 
	 * @throws IllegalAccessException 
	 */
	public void predictWithTCPClassification() throws IllegalArgumentException, InvalidLicenseException, MalformedURLException, IOException, IllegalAccessException {

		// Init TCP and chose the scoring implementation
		TCPClassification tcpImpl = factory.createTCPClassification(factory.createLibLinearClassification()); 

		// Load data from Sparse file (.svm format) and "train" the TCP 
		tcpImpl.train(Problem.fromSparseFile(Config.NUMERICAL_CLASSIFICATION_DATASET.toURL().openStream()));

		// Predict a new example
		List<SparseFeature> example = CPSignFactory.getSparseVector("1:0.44 3:0.88 5:0.44 6:1.32 18:0.44 19:1.76 21:2.2 23:2.2 49:0.222 52:0.444 53:0.37 55:2.413 56:16 57:140");
		// or CPSignFactory.getSparseVector(new double[]{1, 3.5, 4.1, 21.3, 64.4});
		// or CPSignFactory.getSparseVector(new int[]{1, 5, 10, 11}, new double[] {3.4, 12.2, 12.3, 5});
		Map<Integer, Double> pvals = tcpImpl.predict(example);

		System.out.println("Predicted pvals: " + pvals);

	}

}
