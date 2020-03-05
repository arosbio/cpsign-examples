package examples.regression;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.security.InvalidKeyException;
import java.util.Arrays;
import java.util.List;

import com.arosbio.auth.InvalidLicenseException;
import com.arosbio.modeling.CPSignFactory;
import com.arosbio.modeling.data.Problem;
import com.arosbio.modeling.data.SparseFeature;
import com.arosbio.modeling.io.ModelInfo;
import com.arosbio.modeling.io.ModelLoader;
import com.arosbio.modeling.ml.cp.CPRegressionPrediction;
import com.arosbio.modeling.ml.cp.CPRegressionPrediction.PredictedInterval;
import com.arosbio.modeling.ml.cp.acp.ACPRegression;
import com.arosbio.modeling.ml.ds_splitting.FoldedSampling;
import com.arosbio.modeling.ml.ds_splitting.RandomSampling;
import com.arosbio.modeling.ml.metrics.Metric;
import com.arosbio.modeling.ml.testing.KFoldCVSplitter;
import com.arosbio.modeling.ml.testing.TestRunner;

import examples.utils.Config;
import examples.utils.Utils;

public class NumericACPRegression {

	CPSignFactory factory;
	File tempModels;

	public static void main(String[] args) throws Exception {
		NumericACPRegression example = new NumericACPRegression();
		example.intialise();
		example.crossvalidate();
		example.trainAndSavePredictor();
		example.predict();

		System.out.println("Finished Example Numeric ACP-Regression");
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
			tempModels = File.createTempFile("acp_regression.models", ".liblinear");
			tempModels.deleteOnExit();
		} catch(IOException ioe){
			Utils.writeErrAndExit("Could not create temporary files for saving models in");
		}
	}

	/**
	 * Loads data, trains models and save the models to disc 
	 * @throws IOException 
	 * @throws InvalidLicenseException 
	 * @throws IllegalAccessException 
	 */
	public void trainAndSavePredictor() throws IllegalAccessException, InvalidLicenseException, IOException {

		// Chose your predictor and scoring algorithm
		ACPRegression predictor = factory.createACPRegression(
				factory.createAbsDifferenceNCM(factory.createLibLinearRegression()),  // or use factory.createLibSvmRegression()
				new FoldedSampling(Config.NUM_OF_AGGREGATED_MODELS)); // Folded -> CCP, Random -> ACP

		// Load sparse data
		Problem data = Problem.fromSparseFile(Config.NUMERICAL_REGRESSION_DATASET.toURL().openStream());

		// Train the aggregated ICPs
		predictor.train(data);

		// Save models - no need to train the same models again
		predictor.setModelInfo(new ModelInfo("ACP Regression")); // Minimum requirement is to set the "model name"
		predictor.save(tempModels);
	}

	/**
	 * Loads previously created models and use them to predict
	 * @throws IOException 
	 * @throws IllegalArgumentException 
	 * @throws InvalidKeyException 
	 * @throws InvalidLicenseException 
	 * @throws IllegalAccessException 
	 */
	public void predict() throws InvalidKeyException, IllegalArgumentException, IOException, IllegalAccessException, InvalidLicenseException {

		// Load models previously trained
		ACPRegression predictor  = (ACPRegression) ModelLoader.loadModel(tempModels.toURI(), null);

		// Predict a new example
		List<SparseFeature> example = CPSignFactory.getSparseVector("1:-1 2:-0.64 3:-0.86437 4:-1 5:-0.37037 6:0.155011 7:0.283213 8:-0.461594 9:-1 10:-0.583969 11:-0.425532 12:1 13:-0.82064");
		// or CPSignFactory.getSparseVector(new double[]{1, 3.5, 4.1, 21.3, 64.4});
		// or CPSignFactory.getSparseVector(new int[]{1, 5, 10, 11}, new double[] {3.4, 12.2, 12.3, 5});

		CPRegressionPrediction regResult = predictor.predict(example, Arrays.asList(0.5, 0.7, 0.9));
		for (PredictedInterval res: regResult.getIntervals().values()) {
			System.out.println("Confidence: " + res.getConfidence() + ", value: " + res.getInterval());
		}

		//Predict interval specified as distance to predicted value
		CPRegressionPrediction distanceResult = predictor.predictConfidence(example, Arrays.asList(1.5));
		System.out.println("Distance prediction: " + distanceResult);

	}

	public void crossvalidate() throws MalformedURLException, IOException {
		// Chose your predictor and scoring algorithm
		ACPRegression predictor = factory.createACPRegression(
				factory.createAbsDifferenceNCM(factory.createLibLinearRegression()),
				new RandomSampling(Config.NUM_OF_AGGREGATED_MODELS, Config.CALIBRATION_RATIO));

		// Load data 
		Problem data = Problem.fromSparseFile(Config.NUMERICAL_REGRESSION_DATASET.toURL().openStream());

		//Do cross-validation with NUM_FOLDS_CV folds
		TestRunner tester = new TestRunner(new KFoldCVSplitter(Config.NUM_FOLDS_CV));
		tester.setEvaluationPoints(Arrays.asList(Config.CV_CONFIDENCE));
		List<Metric> result = tester.evaluate(data,predictor);
		System.out.println("Cross-validation with " + Config.NUM_FOLDS_CV +" folds and confidence "+ Config.CV_CONFIDENCE +": ");
		for (Metric met: result)
			System.out.println(met.toString());
	}

}