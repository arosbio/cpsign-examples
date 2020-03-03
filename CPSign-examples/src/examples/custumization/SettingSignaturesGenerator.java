package examples.custumization;

import java.io.File;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;

import com.arosbio.auth.InvalidLicenseException;
import com.arosbio.chem.io.in.SDFile;
import com.arosbio.modeling.CPSignFactory;
import com.arosbio.modeling.cheminf.SignaturesCPRegression;
import com.arosbio.modeling.cheminf.signatures.SignaturesGenerator;
import com.arosbio.modeling.cheminf.signatures.SignaturesGeneratorStandard;
import com.arosbio.modeling.io.ModelLoader;
import com.arosbio.modeling.ml.cp.CPRegressionPrediction;
import com.arosbio.modeling.ml.cp.acp.ACPRegression;
import com.arosbio.modeling.ml.ds_splitting.RandomSampling;

import examples.utils.Config;
import examples.utils.Utils;

public class SettingSignaturesGenerator {

	CPSignFactory factory;
	File tempModel;

	public static void main(String[] args) throws IllegalAccessException, InvalidLicenseException, IOException, InvalidKeyException, IllegalArgumentException, CDKException {
		SettingSignaturesGenerator acp = new SettingSignaturesGenerator();
		acp.intialise();
		acp.trainAndSave();
		acp.predict();
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
	public void trainAndSave() throws IllegalAccessException, InvalidLicenseException, IOException {

		// Chose your predictor and scoring algorithm
		ACPRegression predictor = factory.createACPRegression(
				factory.createLogNormalizedNCM(factory.createLibLinearRegression(), null, 0.1), 
				new RandomSampling(Config.NUM_OF_AGGREGATED_MODELS, Config.CALIBRATION_RATIO));

		// Wrap the predictor in Signatures-wrapper
		SignaturesCPRegression signPredictor = factory.createSignaturesCPRegression(predictor, 1, 3);

		// set signatures-generator!
		// signPredictor.setSignaturesGenerator(new SignaturesGeneratorStandard()); // standard (already set by default)
		// signPredictor.setSignaturesGenerator(new SignaturesGeneratorStereo()); // stereo-signatures
		// implement your own!
		signPredictor.setSignaturesGenerator(new MyGenerator());

		// Load data, train and save model
		signPredictor.fromMolsIterator(new SDFile(Config.REGRESSION_DATASET).getIterator(), 
				Config.REGRESSION_ENDPOINT);

			// Train the aggregated ICPs
			signPredictor.train();

			// Save models to skip train again
			signPredictor.save(tempModel);

	}

	public class MyGenerator implements SignaturesGenerator {

		@Override
		public String getName() {
			return "my own generator";
		}

		@Override
		public Map<String, Double> generateSignatures(IAtomContainer molecule, int startheight, int endheight)
				throws CDKException {
			// just wrap the standard one
			return new SignaturesGeneratorStandard().generateSignatures(molecule, startheight, endheight);
		}

		@Override
		public Map<IAtom, String> generateSignatures(IAtomContainer molecule, int height) throws CDKException {
			// just wrap the standard one
			return new SignaturesGeneratorStandard().generateSignatures(molecule, height);
		}
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


		// Load the trained predictor
		SignaturesCPRegression predictor = (SignaturesCPRegression) ModelLoader.loadModel(tempModel.toURI(), null);


		// Here you must set the signatures generator so that the same signatures can be generated for new examples
		predictor.setSignaturesGenerator(new MyGenerator());

		// Predict a new example
			IAtomContainer testMol = new SmilesParser(SilentChemObjectBuilder.getInstance()).parseSmiles(Config.TEST_SMILES);
			List<Double> confidences = Arrays.asList(0.5, 0.7, 0.9);
			CPRegressionPrediction regResult = predictor.predict(testMol, confidences);

			for (double conf : confidences){
				System.out.println("Confidence: " + conf + 
						", interval (normal): " + regResult.getIntervals().get(conf).getInterval());
			}

	}

}
