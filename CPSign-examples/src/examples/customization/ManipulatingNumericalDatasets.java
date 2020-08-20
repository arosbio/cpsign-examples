package examples.custumization;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.util.List;
import java.util.Map;

import com.arosbio.auth.InvalidLicenseException;
import com.arosbio.chem.io.in.SDFile;
import com.arosbio.encryption.EncryptionSpecification;
import com.arosbio.modeling.CPSignFactory;
import com.arosbio.modeling.cheminf.NamedLabels;
import com.arosbio.modeling.cheminf.SignaturesCPClassification;
import com.arosbio.modeling.cheminf.SignaturesCPRegression;
import com.arosbio.modeling.data.Dataset;
import com.arosbio.modeling.data.Problem;
import com.arosbio.modeling.ml.cp.acp.ACPClassification;
import com.arosbio.modeling.ml.ds_splitting.RandomSampling;

import examples.utils.Config;
import examples.utils.Utils;

public class ManipulatingNumericalDatasets {

	CPSignFactory factory, encryptionFactory;
	Dataset dataset;

	File encryptedFile;

	public static void main(String[] args) throws Exception {
		ManipulatingNumericalDatasets msp = new ManipulatingNumericalDatasets();
		msp.initialise();
		msp.loadProblem();
		msp.copyAndShuffle();
		msp.splitIt();
		msp.showEncryption();
		msp.testWorkflow();
	}

	public void initialise() throws IOException {
		// Start with instantiating CPSignFactory with your license
		factory = Utils.getFactory();

		// If you have a license that supports encryption
		if (Config.PRO_LICENSE != null) {
			try{

				encryptionFactory = new CPSignFactory(Config.PRO_LICENSE);
				encryptedFile = File.createTempFile("encr_problem", ".svm");
				encryptedFile.deleteOnExit();
			} catch (IOException | InvalidLicenseException e){
				// No license that supports encryption found
			}
		}

	}

	public void loadProblem() throws IOException {
		// A Problem can be loaded easily without the need for the factory
		dataset = Dataset.fromSparseFile(Config.NUMERICAL_CLASSIFICATION_DATASET.toURL().openStream());

		System.out.println("Original dataset size: " + dataset.size());
	}

	public void copyAndShuffle() throws IOException{

		// If you want to make a copy:
		Dataset copiedDataset = dataset.clone();

		// The data is now identical to the first loaded Dataset
		if (!dataset.equals(copiedDataset))
			System.err.println("Datasets not matching");

		// Shuffle the data around
		copiedDataset.shuffle();

		// Now they do not match!
		if (dataset.equals(copiedDataset))
			System.err.println("Shuffled dataset should not match");

		// Print it to make sure!
//		System.out.println("Shuffled data:\n" + copiedDataset);
	}

	public void splitIt() {
		// Splitting can be done by a static partitioning
		int indexToSplitAt = 2;
		Dataset[] staticPartitions = dataset.splitStatic(indexToSplitAt);
		System.out.println("Static partition 0: " + staticPartitions[0].size());
		System.out.println("Static partition 1: " + staticPartitions[1].size());

		// Or static by what fraction to split at
		double firstFraction = 0.3; // first set should include 30% 
		Dataset[] staticPartitionsByFraction = dataset.splitStatic(firstFraction);
		System.out.println("Static partition by fraction 0: " + staticPartitionsByFraction[0].size());
		System.out.println("Static partition by fraction 1: " + staticPartitionsByFraction[1].size());

		// Or use a Random partitioning 
		Dataset[] randomPartitions = dataset.splitRandom(firstFraction);
		System.out.println("Random partition 0: " + randomPartitions[0].size());
		System.out.println("Random partition 1: " + randomPartitions[1].size());
	}

	public void showEncryption() throws IllegalAccessException, IllegalArgumentException, FileNotFoundException, IOException, InvalidKeyException, InvalidLicenseException{
		// Check if a license with encryption was given
		if (encryptionFactory == null || !encryptionFactory.supportEncryption())
			return;

		// Get the IEncryptionSpec that allows you to encrypt stuff
		EncryptionSpecification spec = encryptionFactory.getEncryptionSpec();

		// Write encrypted file
		dataset.writeRecordsToStream(new FileOutputStream(encryptedFile), spec);

		// Load the Problem back
		Dataset fromEncryptedFile = Dataset.fromSparseFile(new FileInputStream(encryptedFile), spec);

		System.out.println("Dataset re-loaded from encrypted file");

		// Make sure that it matches the original problem!
		if (!fromEncryptedFile.equals(dataset))
			System.err.println("Loaded dataset should equal saved dataset");
	}

	/**
	 * Here's an example of how to use the Sparse Predictor, we use the Signature-wrapper to compute Problems using
	 * Signatures Generation. Use the signatures generated in the first file to get the same indexes!
	 */
	public void testWorkflow() throws IllegalArgumentException, IllegalAccessException, IOException{
		// Do the signature generation step for training data
		SignaturesCPClassification signACP = factory.createSignaturesCPClassification(null, 1, 3);
		signACP.fromMolsIterator(new SDFile(Config.CLASSIFICATION_DATASET).getIterator(), 
				Config.CLASSIFICATION_ENDPOINT, 
				new NamedLabels(Config.CLASSIFICATION_LABELS));

		// Get the (Numerical) Problem
		Problem generatedProblem = signACP.getProblem().clone();
		if (generatedProblem.getDataset().isEmpty())
			throw new RuntimeException("generated dataset must not be empty");
		System.out.println("Generated problem of size: " + generatedProblem.getNumRecords());

		// Now we need some data to test with! Generate it from the other ChemFile

		List<String> signatures = signACP.getProblem().getSignatures(); 

		SignaturesCPRegression acpReg = factory.createSignaturesCPRegression(null, 1, 3); // we dont need the ACP-implementation!
		acpReg.getProblem().setSignatures(signatures); // use the previous signatures

		acpReg.fromMolsIterator(new SDFile(Config.REGRESSION_DATASET).getIterator(), 
				Config.REGRESSION_ENDPOINT);

		Problem testProblem = acpReg.getProblem().clone();

		// now we have two Problems, one for training and one for testing
		ACPClassification impl = factory.createACPClassification(
				factory.createNegativeDistanceToHyperplaneNCM(factory.createLibLinearClassification()), 
				new RandomSampling(10, 0.2));
		impl.train(generatedProblem);

		// predict the first 10 molecules 
		for(int i=0; i< 10; i++){
			Map<Integer, Double> pvals = impl.predict(testProblem.getDataset().getRecords().get(i).getFeatures());
			System.out.println("prediction: "+pvals);
		}

	}

}
