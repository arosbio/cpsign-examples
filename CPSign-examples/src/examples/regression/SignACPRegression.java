package examples.regression;

import java.io.File;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.util.Arrays;
import java.util.List;

import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;

import com.arosbio.auth.InvalidLicenseException;
import com.arosbio.chem.io.in.SDFile;
import com.arosbio.modeling.CPSignFactory;
import com.arosbio.modeling.cheminf.SignaturesCPRegression;
import com.arosbio.modeling.cheminf.SignificantSignature;
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

public class SignACPRegression {

	CPSignFactory factory;
	File tempModel;


	public static void main(String[] args) throws IllegalAccessException, IllegalArgumentException, InvalidLicenseException, IOException, InvalidKeyException, CDKException {
		SignACPRegression acp = new SignACPRegression();
		acp.intialise();
		acp.crossvalidate();
		acp.trainAndSave();
		acp.predict();
		System.out.println("Finished Example ACP-Regression");
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

		// Init the output file
		try{
			tempModel = File.createTempFile("hergModels.liblinear.", ".cpsign");
			tempModel.deleteOnExit();
		} catch(IOException ioe){
			Utils.writeErrAndExit("Could not create temporary files for saving models in");
		}
	}


	/**
	 * Loads data, trains models and save the models to disc 
	 * @throws IllegalAccessException 
	 * @throws IOException 
	 * @throws InvalidLicenseException 
	 */
	public void trainAndSave() throws IllegalAccessException, InvalidLicenseException, IOException {

		// Chose your implementation of the ICP models (LibLinear or LibSVM)
		ACPRegression predictor = factory.createACPRegression(
				factory.createAbsDifferenceNCM(factory.createLibLinearRegression()), 
				(Config.RUN_FOLDED_SAMPLING? 
						new FoldedSampling(Config.NUM_OF_AGGREGATED_MODELS) : 
							new RandomSampling(Config.NUM_OF_AGGREGATED_MODELS, Config.CALIBRATION_RATIO)));

		// Wrap the ACP-predictor chosen in Signatures-wrapper
		SignaturesCPRegression signPredictor = factory.createSignaturesCPRegression(predictor, 1, 3);

		// Load data
		signPredictor.fromMolsIterator(new SDFile(Config.REGRESSION_DATASET).getIterator(), 
				Config.REGRESSION_ENDPOINT); 

		// Train the aggregated predictor
		signPredictor.train();

		// Save models to skip train again
		signPredictor.save(tempModel);
		//			signPredictor.saveEncrypted(jar, spec); // or as encrypted

	}

	/**
	 * Loads previously created models and use them to predict
	 * @throws IllegalAccessException 
	 * @throws IOException 
	 * @throws IllegalArgumentException 
	 * @throws InvalidKeyException 
	 * @throws CDKException 
	 * @throws InvalidLicenseException 
	 */
	public void predict() throws IllegalAccessException, InvalidKeyException, IllegalArgumentException, IOException, InvalidLicenseException, CDKException {

		// Load models previously trained
		SignaturesCPRegression signACP = (SignaturesCPRegression) ModelLoader.loadModel(tempModel.toURI(), null);

		// Predict a new example
		IAtomContainer testMol = new SmilesParser(SilentChemObjectBuilder.getInstance()).parseSmiles(Config.TEST_SMILES);
		CPRegressionPrediction regResult= signACP.predict(testMol, Arrays.asList(0.5, 0.7, 0.9));
		for(PredictedInterval res: regResult.getIntervals().values()){
			System.out.println("Confidence: " + res.getConfidence() + ", value: " + res.getInterval());
		}

		//Predict interval specified as distance to predicted value
		CPRegressionPrediction distanceResult = signACP.predictDistances(testMol, Arrays.asList(0.001));
		System.out.println("Distance prediction: " + distanceResult.getDistanceBasedIntervals().values().iterator().next());

		// Predict the SignificantSignature
		SignificantSignature ss = signACP.predictSignificantSignature(testMol);
		System.out.println(ss);
	}

	public void crossvalidate() throws IllegalAccessException, IllegalArgumentException, InvalidLicenseException, IOException {
		// Chose your implementation of the ICP models (LibLinear or LibSVM)
		ACPRegression predictor = factory.createACPRegression(
				factory.createAbsDifferenceNCM(factory.createLibLinearRegression()), 
				(Config.RUN_FOLDED_SAMPLING? 
						new FoldedSampling(Config.NUM_OF_AGGREGATED_MODELS) : 
							new RandomSampling(Config.NUM_OF_AGGREGATED_MODELS, Config.CALIBRATION_RATIO)));

		// Wrap the ACP-implementation chosen in Signatures-wrapper
		SignaturesCPRegression signPredictor = factory.createSignaturesCPRegression(predictor, 1, 3);


		// Load data
		signPredictor.fromMolsIterator(new SDFile(Config.REGRESSION_DATASET).getIterator(), 
				Config.REGRESSION_ENDPOINT);

		//Do cross-validation with NUM_FOLDS_CV folds
		TestRunner tester = new TestRunner(new KFoldCVSplitter(Config.NUM_FOLDS_CV),Arrays.asList(Config.CV_CONFIDENCE));
		List<Metric> result = tester.evaluate(signPredictor);
		System.out.println("Cross-validation with " + Config.NUM_FOLDS_CV +" folds and confidence "+ Config.CV_CONFIDENCE +": ");
		for (Metric met: result)
			System.out.println(met.toString());

	}



}
