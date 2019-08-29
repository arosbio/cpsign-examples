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
import com.arosbio.modeling.ml.cp.CPRegressionResult;
import com.arosbio.modeling.ml.cp.acp.ACPRegression;
import com.arosbio.modeling.ml.cp.nonconf.NonconfMeasureFactory;
import com.arosbio.modeling.ml.ds_splitting.RandomSampling;

import examples.utils.Config;
import examples.utils.Utils;

public class RegressionChangeNonconformityScore {

	CPSignFactory factory;
	File tempModel, tempModelDifferentNonconfMeasure;


	public static void main(String[] args) throws IllegalAccessException, InvalidLicenseException, IOException, InvalidKeyException, IllegalArgumentException, CDKException {
		RegressionChangeNonconformityScore acp = new RegressionChangeNonconformityScore();
		acp.intialise();
		acp.trainAndSave();
		acp.predict();
		System.out.println("Finished Example ACP-Regression Different Nonconformity scores");
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

		// Init the output files
		try{
			tempModel = File.createTempFile("hergModels.liblinear.", ".cpsign");
			tempModelDifferentNonconfMeasure = File.createTempFile("hergModels.liblinear.", ".cpsign");
			tempModel.deleteOnExit();
			tempModelDifferentNonconfMeasure.deleteOnExit();
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

		// Chose your predictor and scoring algorithm
		ACPRegression predictor = factory.createACPRegression(
				factory.createLibLinearRegression(), 
				new RandomSampling(Config.NUM_OF_AGGREGATED_MODELS, Config.CALIBRATION_RATIO));

		// Wrap the ACP-implementation chosen in Signatures-wrapper
		SignaturesCPRegression signPredictor = factory.createSignaturesCPRegression(predictor, 1, 3);

		// Load data
		signPredictor.fromMolsIterator(new SDFile(Config.REGRESSION_DATASET).getIterator(), 
				Config.REGRESSION_ENDPOINT);

			// Train the aggregated ICPs
			signPredictor.train();

			// Save models to skip train again
			signPredictor.save(tempModel);

			// Change the nonconformity score and train a new model!
			predictor.setNonconformityMeasure(NonconfMeasureFactory.getAbsDiffMeasureRegression());
			// or acpImpl.setNonconformityMeasure(NonconfMeasureFactory.getLogNormalizedMeasureRegression(0.1));

			// Train the aggregated ICPs
			signPredictor.train();

			// Save models to skip train again
			signPredictor.save(tempModelDifferentNonconfMeasure);

	}

	/**
	 * Loads previously created models and use them to predict
	 * @throws IllegalAccessException 
	 * @throws CDKException 
	 * @throws InvalidLicenseException 
	 * @throws IOException 
	 * @throws IllegalArgumentException 
	 * @throws InvalidKeyException 
	 */
	public void predict() throws IllegalAccessException, InvalidLicenseException, CDKException, InvalidKeyException, IllegalArgumentException, IOException {

		// Load models previously trained
		SignaturesCPRegression signPredictor = (SignaturesCPRegression) ModelLoader.loadModel(tempModel.toURI(), null);
		SignaturesCPRegression signPredictor_abs_diff = (SignaturesCPRegression) ModelLoader.loadModel(
				tempModelDifferentNonconfMeasure.toURI(), null); 


		// Predict a new example
			IAtomContainer testMol = new SmilesParser(SilentChemObjectBuilder.getInstance()).parseSmiles(Config.TEST_SMILES);
			List<Double> confidences = Arrays.asList(0.5, 0.7, 0.9);
			List<CPRegressionResult> regResult = signPredictor.predict(testMol, confidences);
			List<CPRegressionResult> regResult_abs_diff = signPredictor_abs_diff.predict(testMol, confidences);
			for (int i=0; i<confidences.size(); i++){
				System.out.println("Confidence: " + regResult.get(i).getConfidence() + ", interval (normal): " + regResult.get(i).getInterval() + ", interval (abs diff): " + regResult_abs_diff.get(i).getInterval());
			}

			//Predict interval specified as distance to predicted value
			List<CPRegressionResult> distanceResult = signPredictor.predictDistances(testMol, Arrays.asList(0.001));
			System.out.println("Distance prediction: " + distanceResult.get(0));

			// Predict the SignificantSignature
			SignificantSignature ss = signPredictor.predictSignificantSignature(testMol);
			System.out.println(ss);
	}



}
