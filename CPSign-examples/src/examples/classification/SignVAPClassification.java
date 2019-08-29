package examples.classification;

import java.io.File;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.util.List;
import java.util.Map;

import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;
import org.slf4j.LoggerFactory;

import com.arosbio.auth.InvalidLicenseException;
import com.arosbio.chem.io.in.SDFile;
import com.arosbio.chem.io.out.GradientFigureBuilder;
import com.arosbio.chem.io.out.MoleculeFigure;
import com.arosbio.chem.io.out.depictors.MoleculeGradientDepictor;
import com.arosbio.chem.io.out.fields.PValuesField;
import com.arosbio.chem.io.out.fields.TitleField;
import com.arosbio.modeling.CPSignFactory;
import com.arosbio.modeling.cheminf.NamedLabels;
import com.arosbio.modeling.cheminf.SignaturesVAPClassification;
import com.arosbio.modeling.cheminf.SignificantSignature;
import com.arosbio.modeling.io.ModelLoader;
import com.arosbio.modeling.ml.cv.KFoldCV;
import com.arosbio.modeling.ml.ds_splitting.FoldedSampling;
import com.arosbio.modeling.ml.metrics.Metric;
import com.arosbio.modeling.ml.vap.avap.AVAPClassification;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import examples.utils.Config;
import examples.utils.Utils;

public class SignVAPClassification {

	CPSignFactory factory;
	File tempModel;

	boolean generateImages=true;
	private String imageOutputName = "cvap_classification_gradient.png";


	public static void main(String[] args) throws IllegalAccessException, InvalidLicenseException, IOException, InvalidKeyException, IllegalArgumentException, CDKException {
		SignVAPClassification acp = new SignVAPClassification();
		acp.initialise();
		acp.crossvalidate();
		acp.trainAndSave();
		acp.predict();
		System.out.println("Finished Example CVAP-Classification");
	}


	/**
	 * This method just initializes some variables and the CPSignFactory. Please change the 
	 * initialization of CPSignFactory to point to your active license. Also change the 
	 * model and signature-files into a location on your machine so that they can be used 
	 * later on, now temporary files are created for illustrative purposes. 
	 */
	public void initialise() {
		// Start with instantiating CPSignFactory with your license
		factory = Utils.getFactory();

		// Init the output file
		try{
			tempModel = File.createTempFile("bursiModels", ".cpsign");
			tempModel.deleteOnExit();
		} catch(IOException ioe){
			Utils.writeErrAndExit("Could not create temporary files for saving models in");
		}

		// Disable logging from CPSign
		Logger cpsignRoot = (Logger) LoggerFactory.getLogger("com.arosbio");
		cpsignRoot.setLevel(Level.OFF);
	}


	/**
	 * Loads data, trains models and save the models to disc 
	 * @throws IllegalAccessException 
	 * @throws IOException 
	 * @throws InvalidLicenseException 
	 */
	public void trainAndSave() throws IllegalAccessException, InvalidLicenseException, IOException {

		// Chose your implementation of the ICP models (LibLinear or LibSVM)
		AVAPClassification predictor = factory.createVAPClassification(
				factory.createLibLinearClassification(), 
				new FoldedSampling(Config.NUM_OF_AGGREGATED_MODELS)); 

		// Wrap the CVAP-predictor in a Signatures-wrapper
		SignaturesVAPClassification signPredictor = factory.createSignaturesVAPClassification(predictor, 1, 3);

		// Load data from Chemical file
		signPredictor.fromMolsIterator(new SDFile(Config.CLASSIFICATION_DATASET).getIterator(), 
				Config.CLASSIFICATION_ENDPOINT, 
				new NamedLabels(Config.CLASSIFICATION_LABELS));

		// Train the aggregated ICPs
		signPredictor.train();

		// If images should be generated
		if (generateImages){
			signPredictor.computePercentiles(new SDFile(Config.CLASSIFICATION_DATASET).getIterator());
		}

		// Save the trained models
		signPredictor.save(tempModel);

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

		SignaturesVAPClassification signPredictor = null;

		// Load models previously trained
		// when the model isn't encrypted, just send null as EncryptionSpecification
		signPredictor = (SignaturesVAPClassification) ModelLoader.loadModel(tempModel.toURI(), null);

		// Predict a new example
		IAtomContainer testMol = new SmilesParser(SilentChemObjectBuilder.getInstance()).parseSmiles(Config.TEST_SMILES);

		// Get the mapping of label->probability
		Map<String, Double> probabilities = signPredictor.predictProbabilities(testMol);

		System.out.println("Predicted probabilities: " + probabilities);

		// Predict the SignificantSignature
		SignificantSignature ss = signPredictor.predictSignificantSignature(testMol);
		System.out.println("Significant signature="+ss.getSignature() + ", height=" + ss.getHeight() + ", atoms=" + ss.getAtoms());

		if (generateImages){
			GradientFigureBuilder imgBuilder = new GradientFigureBuilder(new MoleculeGradientDepictor());
			imgBuilder.addFieldOverImg(new TitleField(Config.TEST_SMILES));
			imgBuilder.addFieldUnderImg(new PValuesField(probabilities));
			MoleculeFigure img = imgBuilder.build(testMol, ss.getMoleculeGradient());
			File imgFile = new File(new File("").getAbsolutePath(),Config.IMAGE_BASE_PATH+imageOutputName);
			img.saveToFile(imgFile);
			System.out.println("Printed prediction image to: " + imgFile);
		}

	}

	private void crossvalidate() throws IllegalAccessException, IllegalArgumentException, InvalidLicenseException, IOException {
		// Chose your implementation of the ICP models (LibLinear or LibSVM)
		AVAPClassification predictor = factory.createVAPClassification(
				factory.createLibLinearClassification(), 
				new FoldedSampling(Config.NUM_OF_AGGREGATED_MODELS)); 

		// Wrap the CVAP-predictor in a Signatures-wrapper
		SignaturesVAPClassification signPredictor = factory.createSignaturesVAPClassification(predictor, 1, 3);

		// Load data from Chemical file
		signPredictor.fromMolsIterator(new SDFile(Config.CLASSIFICATION_DATASET).getIterator(), 
				Config.CLASSIFICATION_ENDPOINT, 
				new NamedLabels(Config.CLASSIFICATION_LABELS));

		// Run CV
		KFoldCV cv = new KFoldCV(Config.NUM_FOLDS_CV);
		cv.setConfidence(Config.CV_CONFIDENCE);
		List<Metric> result = cv.evaluate(signPredictor);
		System.out.println("Cross-validation with " + Config.NUM_FOLDS_CV +" folds and confidence "+ Config.CV_CONFIDENCE +": ");
		for (Metric met: result)
			System.out.println(met.toString());

	}


}
