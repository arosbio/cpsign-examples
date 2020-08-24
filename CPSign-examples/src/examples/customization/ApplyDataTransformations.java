package examples.customization;

import java.io.File;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.openscience.cdk.exception.CDKException;

import com.arosbio.auth.InvalidLicenseException;
import com.arosbio.chem.io.in.SDFile;
import com.arosbio.commons.Stopwatch;
import com.arosbio.modeling.CPSignFactory;
import com.arosbio.modeling.cheminf.SignaturesCPRegression;
import com.arosbio.modeling.cheminf.descriptors.CDKDescriptorWrapper;
import com.arosbio.modeling.cheminf.descriptors.Descriptor;
import com.arosbio.modeling.cheminf.descriptors.DescriptorFactory;
import com.arosbio.modeling.data.transform.feature_selection.DropMissingDataSelecter;
import com.arosbio.modeling.data.transform.feature_selection.L2_SVR_Selecter;
import com.arosbio.modeling.data.transform.scale.RobustScaler;
import com.arosbio.modeling.ml.cp.acp.ACPRegression;
import com.arosbio.modeling.ml.ds_splitting.RandomSampling;
import com.arosbio.modeling.ml.metrics.Metric;
import com.arosbio.modeling.ml.testing.KFoldCVSplitter;
import com.arosbio.modeling.ml.testing.TestRunner;

import examples.utils.Config;
import examples.utils.Utils;

public class ApplyDataTransformations {

	CPSignFactory factory;
	File tempModel;

	public static void main(String[] args) throws IllegalAccessException, InvalidLicenseException, IOException, InvalidKeyException, IllegalArgumentException, CDKException {
		ApplyDataTransformations acp = new ApplyDataTransformations();
		acp.intialise();
		acp.evaluateTransformations();
		System.out.println("Finished Example Setting SignaturesGenerator");
	}

	/**
	 * This method just initializes some variables and the CPSignFactory. Please change the 
	 * initialization of CPSignFactory to point to your active license. Also change the 
	 * model and signature-files into a location on your machine so that they can be used 
	 * later on, now temporary files are created for illustrative purposes. 
	 */
	public void intialise() {
		// Start with instantiating CPSignFactory with your license
		factory = Utils.getFactory();

		// Init the files
		try{
			tempModel = File.createTempFile("hergModels.liblinear.", ".cpsign");
			tempModel.deleteOnExit();
		} catch(IOException ioe){
			Utils.writeErrAndExit("Could not create temporary file for saving model in");
		}
	}


	/**
	 * Loads data, trains models and save the models to disc 
	 * @throws IllegalAccessException 
	 * @throws IOException 
	 * @throws InvalidLicenseException 
	 */
	public void evaluateTransformations() throws IllegalAccessException, InvalidLicenseException, IOException {

		// Chose your predictor and scoring algorithm
		ACPRegression predictor = factory.createACPRegression(
				factory.createLogNormalizedNCM(factory.createLibLinearRegression(), null, 0.1), 
				new RandomSampling(Config.NUM_OF_AGGREGATED_MODELS, Config.CALIBRATION_RATIO));

		// Wrap the predictor in Signatures-wrapper
		SignaturesCPRegression signPredictor = factory.createSignaturesCPRegression(predictor, 1, 3);

		List<Descriptor> allDescriptors = DescriptorFactory.getInstance().getDescriptorsList();
		List<Descriptor> cdkDescriptors = new ArrayList<>();

		// Get only the CDK-descriptors, and use only the ones not requiring 3D coordinates (takes a long time to compute and estimate of those)
		for (Descriptor d : allDescriptors) {
			if (d instanceof CDKDescriptorWrapper && !((CDKDescriptorWrapper) d).requires3DCoordinates()) {
				cdkDescriptors.add(d);
			}
		}

		// Only take half of them, they 
		cdkDescriptors = cdkDescriptors.subList(0, cdkDescriptors.size() / 2);

		// Note that each descriptor can calculate several features
		System.out.println("Initial number of descriptors: " + cdkDescriptors.size());

		// Set the new descriptors - overwriting the default SignaturesDescriptor
		signPredictor.getProblem().setDescriptors(cdkDescriptors);

		// Load data
		signPredictor.fromMolsIterator(new SDFile(Config.REGRESSION_DATASET).getIterator(), 
				Config.REGRESSION_ENDPOINT);

		System.out.println("Total number of features: " + signPredictor.getFeatureNames(false).size());

		// Some of them has issues with e.g. calculating 3D coordinates if input data does not contain it - so check for features with missing values
		signPredictor.getProblem().apply(new DropMissingDataSelecter());

		System.out.println("Num features after inital filtration: " + signPredictor.getFeatureNames(false).size());

		// Evaluate this dataset - note: not scaled or performed any feature-selection
		TestRunner tester = new TestRunner(new KFoldCVSplitter());
		Stopwatch sw = new Stopwatch();
		sw.start();
		List<Metric> metrics = tester.evaluate(signPredictor);
		sw.stop();
		System.out.println("\nInitial results using 10-fold CV "+sw+" :");
		for (Metric m : metrics) {
			System.out.println(m);
		}

		// Perform robust scaling of features and then feature-selection
		signPredictor.getProblem().apply(Arrays.asList(new RobustScaler(), new L2_SVR_Selecter()));

		System.out.println("\nNum features after feature-selection: " + signPredictor.getFeatureNames(false).size());

		sw.start();
		List<Metric> newMetrics = tester.evaluate(signPredictor);
		sw.stop();
		System.out.println("Results after feature-selection and scaling using 10-fold CV " + sw + " :");
		for (Metric m : newMetrics) {
			System.out.println(m);
		}

	}
}
