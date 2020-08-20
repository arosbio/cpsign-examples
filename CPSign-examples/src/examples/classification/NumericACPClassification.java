package examples.classification;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.security.InvalidKeyException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.arosbio.auth.InvalidLicenseException;
import com.arosbio.modeling.CPSignFactory;
import com.arosbio.modeling.data.FeatureVector;
import com.arosbio.modeling.data.Problem;
import com.arosbio.modeling.data.SparseFeature;
import com.arosbio.modeling.data.SparseVector;
import com.arosbio.modeling.io.ModelInfo;
import com.arosbio.modeling.io.ModelLoader;
import com.arosbio.modeling.ml.cp.acp.ACPClassification;
import com.arosbio.modeling.ml.ds_splitting.RandomSampling;
import com.arosbio.modeling.ml.metrics.Metric;
import com.arosbio.modeling.ml.testing.KFoldCVSplitter;
import com.arosbio.modeling.ml.testing.TestRunner;

import examples.utils.Config;
import examples.utils.Utils;

public class NumericACPClassification {

	CPSignFactory factory;
	File tempModel;

	public static void main(String[] args) throws MalformedURLException, IOException, IllegalAccessException, InvalidLicenseException, InvalidKeyException, IllegalArgumentException {
		NumericACPClassification example = new NumericACPClassification();
		example.intialise();
		example.crossvalidate();
		example.trainAndSavePredictor();
		example.predict();
		System.out.println("Finished Example Numeric ACP-Classification");
	}
	/**
	 * This method just initializes some variables and the CPSignFactory. Please change the 
	 * initialization of CPSignFactory to point to your active license. Also change the 
	 * model and signature-files into a location on your machine so that they can be used 
	 * later on, now temporary files are created for illustrative purposes. 
	 * @throws IOException 
	 * @throws IllegalArgumentException 
	 */
	public void intialise() {
		// Start with instantiating CPSignFactory with your license
		factory = Utils.getFactory();

		// Init the file
		try{
			tempModel = File.createTempFile("acp_classification.models", ".liblinear");
			tempModel.deleteOnExit();
		} catch(IOException ioe){
			Utils.writeErrAndExit("Could not create temporary files for saving models in");
		}
	}

	/**
	 * Loads data, trains models and save the models to disc 
	 * @throws IOException 
	 * @throws MalformedURLException 
	 * @throws InvalidLicenseException 
	 * @throws IllegalAccessException 
	 */
	public void trainAndSavePredictor() throws MalformedURLException, IOException, IllegalAccessException, InvalidLicenseException {

		// Chose your predictor and scoring algorithm
		ACPClassification predictor = factory.createACPClassification(
				factory.createNegativeDistanceToHyperplaneNCM(
				factory.createLibLinearClassification()), 
				new RandomSampling(Config.NUM_OF_AGGREGATED_MODELS, Config.CALIBRATION_RATIO));

		Problem data = Problem.fromLIBSVMFormat(Config.NUMERICAL_CLASSIFICATION_DATASET.toURL().openStream());

		// Train the aggregated 
		predictor.train(data);

		// Save models - no need to train the same models again
		predictor.setModelInfo(new ModelInfo("ACP Classification")); // Minimum info is to set the model name
		predictor.save(tempModel);

	}

	/**
	 * Loads previously created models and use them to predict
	 * @throws InvalidLicenseException 
	 * @throws IllegalAccessException 
	 * @throws IOException 
	 * @throws IllegalArgumentException 
	 * @throws InvalidKeyException 
	 */
	public void predict() throws IllegalAccessException, InvalidLicenseException, InvalidKeyException, IllegalArgumentException, IOException {


		// Load models previously trained
		ACPClassification predictor = (ACPClassification) ModelLoader.loadModel(tempModel.toURI(), null);

		// Predict a new example
		List<SparseFeature> example = CPSignFactory.getSparseVector("1:0.44 3:0.88 5:0.44 6:1.32 18:0.44 19:1.76 21:2.2 23:2.2 49:0.222 52:0.444 53:0.37 55:2.413 56:16 57:140");
		// or CPSignFactory.getSparseVector(new double[]{1, 3.5, 4.1, 21.3, 64.4});
		// or CPSignFactory.getSparseVector(new int[]{1, 5, 10, 11}, new double[] {3.4, 12.2, 12.3, 5});
		FeatureVector v = new SparseVector(example);
		
		Map<Integer, Double> pvals = predictor.predict(v);

		System.out.println("Predicted pvals: "+pvals);

	}

	public void crossvalidate() throws MalformedURLException, IOException {
		// Chose your predictor and scoring algorithm
		ACPClassification predictor = factory.createACPClassification(
				factory.createNegativeDistanceToHyperplaneNCM(
				factory.createLibLinearClassification()), 
				new RandomSampling(Config.NUM_OF_AGGREGATED_MODELS, Config.CALIBRATION_RATIO)); 

		// Load data (do not have to load data separately for cross-validate and train/predict-part!)
		Problem data = Problem.fromLIBSVMFormat(Config.NUMERICAL_CLASSIFICATION_DATASET.toURL().openStream());
		
		// Do CV
		TestRunner tester = new TestRunner(new KFoldCVSplitter(Config.NUM_FOLDS_CV));
//		KFoldCV cv = new KFoldCV(Config.NUM_FOLDS_CV);
		tester.setEvaluationPoints(Arrays.asList(Config.CV_CONFIDENCE));
		List<Metric> result = tester.evaluate(data, predictor);
		System.out.println("Cross-validation with " + Config.NUM_FOLDS_CV + " folds and confidence " + Config.CV_CONFIDENCE +":");
		for (Metric met: result)
			System.out.println(met.toString());
	}

}
