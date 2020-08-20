package examples.classification;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.security.InvalidKeyException;
import java.util.List;

import com.arosbio.auth.InvalidLicenseException;
import com.arosbio.modeling.CPSignFactory;
import com.arosbio.modeling.data.FeatureVector;
import com.arosbio.modeling.data.Problem;
import com.arosbio.modeling.data.SparseFeature;
import com.arosbio.modeling.data.SparseVector;
import com.arosbio.modeling.io.ModelInfo;
import com.arosbio.modeling.io.ModelLoader;
import com.arosbio.modeling.ml.algorithms.LibLinear;
import com.arosbio.modeling.ml.algorithms.params.LibLinearParameters;
import com.arosbio.modeling.ml.ds_splitting.RandomSampling;
import com.arosbio.modeling.ml.metrics.Metric;
import com.arosbio.modeling.ml.testing.KFoldCVSplitter;
import com.arosbio.modeling.ml.testing.TestRunner;
import com.arosbio.modeling.ml.vap.avap.AVAPClassification;
import com.arosbio.modeling.ml.vap.avap.AVAPClassificationResult;

import examples.utils.Config;
import examples.utils.Utils;

public class NumericVAPClassification {

	CPSignFactory factory;
	File tempModel;


	public static void main(String[] args) throws MalformedURLException, IOException, InvalidKeyException, IllegalAccessException, IllegalArgumentException {
		NumericVAPClassification example = new NumericVAPClassification();
		example.intialise();
		example.crossvalidate();
		example.trainAndSavePredictor();
		example.predict();
		System.out.println("Finished Example Numeric CVAP-Classification");
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
			tempModel = File.createTempFile("cvap_classification.models", ".liblinear");
			tempModel.deleteOnExit();
		} catch(IOException ioe){
			Utils.writeErrAndExit("Could not create temporary files for saving predictor in");
		}
	}

	/**
	 * Loads data, trains models and save the models to disc 
	 */
	public void trainAndSavePredictor() {

		// Chose your predictor and scoring algorithm
		AVAPClassification predictor = new AVAPClassification(
				new LibLinear(LibLinearParameters.defaultClassification()), 
				new RandomSampling(Config.NUM_OF_AGGREGATED_MODELS, Config.CALIBRATION_RATIO));

		// Load sparse data
		try{
			Problem data = Problem.fromLIBSVMFormat(Config.NUMERICAL_CLASSIFICATION_DATASET.toURL().openStream());

			// Train the aggregated 
			predictor.train(data);

			// Save models - no need to train the same models again
			predictor.setModelInfo(new ModelInfo("CVAP Classification")); // Minimum info is to set the model name
			predictor.save(tempModel);

		} catch(IllegalStateException e){
			// License not supporting training functionality 
			Utils.writeErrAndExit(e.getMessage());
		} catch (IOException e){
			Utils.writeErrAndExit("Problem loading data or saving models");
		} catch (InvalidLicenseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Loads previously created models and use them to predict
	 * @throws IllegalAccessException 
	 * @throws IOException 
	 * @throws IllegalArgumentException 
	 * @throws InvalidKeyException 
	 */
	public void predict() throws IllegalAccessException, InvalidKeyException, IllegalArgumentException, IOException {

		// Load models previously trained
		AVAPClassification predictor = (AVAPClassification) ModelLoader.loadModel(tempModel.toURI(), null);

		// Predict a new example
		List<SparseFeature> example = CPSignFactory.getSparseVector("1:0.44 3:0.88 5:0.44 6:1.32 18:0.44 19:1.76 21:2.2 23:2.2 49:0.222 52:0.444 53:0.37 55:2.413 56:16 57:140");
		// or CPSignFactory.getSparseVector(new double[]{1, 3.5, 4.1, 21.3, 64.4});
		// or CPSignFactory.getSparseVector(new int[]{1, 5, 10, 11}, new double[] {3.4, 12.2, 12.3, 5});
		FeatureVector v = new SparseVector(example);

		AVAPClassificationResult result = predictor.predict(v);
		System.out.println("Prediction: "+result);

	}

	public void crossvalidate() throws MalformedURLException, IOException {
		// Init predictor
		AVAPClassification predictor = factory.createVAPClassification(
				factory.createLibLinearClassification(), 
				new RandomSampling(Config.NUM_OF_AGGREGATED_MODELS, Config.CALIBRATION_RATIO)); 

		// Load data (do not have to load data separately for cross-validate and train/predict-part!)
		Problem data = Problem.fromLIBSVMFormat(Config.NUMERICAL_CLASSIFICATION_DATASET.toURL().openStream());

		// Do CV
		TestRunner tester = new TestRunner(new KFoldCVSplitter(Config.NUM_FOLDS_CV));
		List<Metric> result = tester.evaluate(data, predictor);
		System.out.println("Cross-validation with " + Config.NUM_FOLDS_CV +" folds: " + result);
	}

}
