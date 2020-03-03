package examples.classification;

import java.io.File;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.util.Arrays;
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
import com.arosbio.modeling.cheminf.SignaturesCPClassification;
import com.arosbio.modeling.cheminf.SignificantSignature;
import com.arosbio.modeling.io.ModelLoader;
import com.arosbio.modeling.ml.cp.acp.ACPClassification;
import com.arosbio.modeling.ml.ds_splitting.FoldedSampling;
import com.arosbio.modeling.ml.ds_splitting.RandomSampling;
import com.arosbio.modeling.ml.metrics.Metric;
import com.arosbio.modeling.ml.testing.KFoldCVSplitter;
import com.arosbio.modeling.ml.testing.TestRunner;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import examples.utils.Config;
import examples.utils.Utils;

public class SignACPClassification {

	CPSignFactory factory;
	File tempModel;

	boolean generateImages=true;

	private static final String IMAGE_OUTPUT = "acp_classification_gradient.png";


	public static void main(String[] args) throws IllegalAccessException, IllegalArgumentException, InvalidLicenseException, IOException, InvalidKeyException, CDKException {
		SignACPClassification acp = new SignACPClassification();
		acp.intialise();
		acp.crossvalidate();
		acp.trainAndSave();
		acp.predict();
		System.out.println("Finished Example ACP-Classification");
	}


	/**
	 * This method just initializes some variables and the CPSignFactory. Please change the 
	 * initialization of CPSignFactory to point to your active license. Also change the 
	 * model and signature-files into a location on your machine so that they can be used 
	 * later on, now temporary files are created for illustrative purposes. 
	 */
	public void intialise() {
		// Start with instantiating CPSignFactory with your license
		//		Configuration.init();
		factory = Utils.getFactory();

		// Init the files
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
	 * @throws IllegalArgumentException 
	 */
	public void trainAndSave() throws IllegalAccessException, IllegalArgumentException, InvalidLicenseException, IOException {

		// Chose your implementation of the ICP models (LibLinear or LibSVM)
		ACPClassification predictor = factory.createACPClassification(
				factory.createNegativeDistanceToHyperplaneNCM(
						factory.createLibLinearClassification()), 
				(Config.RUN_FOLDED_SAMPLING? 
						new FoldedSampling(Config.NUM_OF_AGGREGATED_MODELS) : 
							new RandomSampling(Config.NUM_OF_AGGREGATED_MODELS, Config.CALIBRATION_RATIO))); 

		// Wrap the ACP-implementation chosen in Signatures-wrapper
		SignaturesCPClassification signPredictor = factory.createSignaturesCPClassification(predictor, 1, 3);

		// Load data
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
	 * @throws IOException 
	 * @throws IllegalArgumentException 
	 * @throws InvalidKeyException 
	 * @throws CDKException 
	 * @throws InvalidLicenseException 
	 */
	public void predict() throws IllegalAccessException, InvalidKeyException, IllegalArgumentException, IOException, InvalidLicenseException, CDKException {

		// Load models previously trained
		// when the model isn't encrypted, just send null as EncryptionSpecification
		SignaturesCPClassification signACP = (SignaturesCPClassification) ModelLoader.loadModel(tempModel.toURI(), null);

		// Predict a new example
		IAtomContainer testMol = new SmilesParser(SilentChemObjectBuilder.getInstance()).parseSmiles(Config.TEST_SMILES);

		// Get the mapping of label->p-value
		Map<String, Double> pvals = signACP.predictMondrian(testMol);

		System.out.println("Predicted pvals: " + pvals);

		// Predict the SignificantSignature
		SignificantSignature ss = signACP.predictSignificantSignature(testMol);
		System.out.println("Significant signautre="+ss.getSignature() + ", height=" + ss.getHeight() + ", atoms=" + ss.getAtoms());

		if (generateImages){
			GradientFigureBuilder imgBuilder = new GradientFigureBuilder(new MoleculeGradientDepictor());
			imgBuilder.addFieldOverImg(new TitleField(Config.TEST_SMILES));
			imgBuilder.addFieldUnderImg(new PValuesField(pvals));
			MoleculeFigure img = imgBuilder.build(testMol, ss.getMoleculeGradient());
			File imgFile = new File(new File("").getAbsolutePath(),Config.IMAGE_BASE_PATH+IMAGE_OUTPUT);
			img.saveToFile(imgFile);
			System.out.println("Printed prediction image to: " + imgFile);
		}


	}

	public void crossvalidate() throws IllegalAccessException, IllegalArgumentException, InvalidLicenseException, IOException {
		// Chose your implementation of the ICP models (LibLinear or LibSVM)
		ACPClassification predictor = factory.createACPClassification(
				factory.createNegativeDistanceToHyperplaneNCM(factory.createLibLinearClassification()), 
				(Config.RUN_FOLDED_SAMPLING? 
						new FoldedSampling(Config.NUM_OF_AGGREGATED_MODELS) : 
							new RandomSampling(Config.NUM_OF_AGGREGATED_MODELS, Config.CALIBRATION_RATIO))); 

		// Wrap the ACP-predictor chosen in Signatures-wrapper
		SignaturesCPClassification signACP = factory.createSignaturesCPClassification(predictor, 1, 3);

		// Load data
		signACP.fromMolsIterator(new SDFile(Config.CLASSIFICATION_DATASET).getIterator(), 
				Config.CLASSIFICATION_ENDPOINT, 
				new NamedLabels(Config.CLASSIFICATION_LABELS));

		//Do cross-validation with NUM_FOLDS_CV folds
		TestRunner tester = new TestRunner(new KFoldCVSplitter(Config.NUM_FOLDS_CV));
		tester.setEvaluationPoints(Arrays.asList(Config.CV_CONFIDENCE));
		List<Metric> result = tester.evaluate(signACP);
		System.out.println("Cross-validation with " + Config.NUM_FOLDS_CV +" folds and confidence "+ Config.CV_CONFIDENCE +": ");
		for (Metric met: result)
			System.out.println(met.toString());			

	}


}
