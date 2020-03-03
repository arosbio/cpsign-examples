package examples.classification;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;

import com.arosbio.auth.InvalidLicenseException;
import com.arosbio.chem.io.in.SDFile;
import com.arosbio.modeling.CPSignFactory;
import com.arosbio.modeling.cheminf.NamedLabels;
import com.arosbio.modeling.cheminf.SignaturesCPClassification;
import com.arosbio.modeling.cheminf.SignificantSignature;
import com.arosbio.modeling.ml.cp.tcp.TCPClassification;

import examples.utils.Config;
import examples.utils.Utils;

public class SignTCPClassification {

	CPSignFactory factory;
	File tempTCPData;


	public static void main(String[] args) throws IllegalAccessException, IllegalArgumentException, InvalidLicenseException, IOException, CDKException {
		SignTCPClassification example = new SignTCPClassification();
		example.intialise();
		example.predict();
		System.out.println("Finished Example TCP-Classification");
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

		// Init the output model file
		try{
			tempTCPData = File.createTempFile("bursiTCP", ".csr");
			tempTCPData.deleteOnExit();
		} catch(Exception ioe){
			Utils.writeErrAndExit("Could not create temporary files for saving models to");
		}
	}


	/**
	 * Loads previously created models and use them to predict
	 * @throws IllegalAccessException 
	 * @throws IOException 
	 * @throws InvalidLicenseException 
	 * @throws IllegalArgumentException 
	 * @throws CDKException 
	 */
	public void predict() throws IllegalAccessException, IllegalArgumentException, InvalidLicenseException, IOException, CDKException {

		// Init TCP and chose the scoring implementation
		TCPClassification predictor = factory.createTCPClassification(
				factory.createNegativeDistanceToHyperplaneNCM(factory.createLibLinearClassification()));

		// Wrap the predictor in a Signatures-wrapper
		SignaturesCPClassification signPredictor = factory.createSignaturesCPClassification(predictor, 1, 3);

		// Load data from Chemical file
		signPredictor.fromMolsIterator(new SDFile(Config.CLASSIFICATION_DATASET).getIterator(), 
				Config.CLASSIFICATION_ENDPOINT, 
				new NamedLabels(Config.CLASSIFICATION_LABELS));

		// Train!
		signPredictor.train();

		// Predict a new example
		IAtomContainer testMol = new SmilesParser(SilentChemObjectBuilder.getInstance()).parseSmiles(Config.TEST_SMILES);
		// Get the mapping of label->p-value
		Map<String, Double> pvals = signPredictor.predictMondrian(testMol);

		System.out.println("Predicted pvals: "+pvals);

		// Predict the SignificantSignature
		SignificantSignature ss = signPredictor.predictSignificantSignature(testMol);
		System.out.println(ss);

		// Save the generated records and signatures to file
		signPredictor.save(tempTCPData);

	}

}
