package examples;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;

import com.arosbio.encryption.EncryptionSpecification;
import com.arosbio.modeling.CPSignFactory;
import com.arosbio.modeling.data.DataUtils;
import com.arosbio.modeling.data.Dataset;
import com.arosbio.modeling.data.transform.format.MakeDenseTransformer;

import utils.BaseTest;
import utils.Config;

public class ManipulatingNumericalDatasets extends BaseTest {


	@Test
	public void manipulate() throws IOException {
		////// LOADING
		
		// A Dataset can be loaded easily without the need for the factory
		Dataset dataset = Dataset.fromLIBSVMFormat(Config.getURI("numerical.classification", null).toURL().openStream());
		System.out.println("Original dataset size: " + dataset.size() + ", num features: " + dataset.getNumFeatures());
		
		
		////// MAKING COPIES (CLONES)
		Dataset copiedDataset = dataset.clone();

		
		////// CHECKING FOR EQUALITY 
		// The data is now identical to the first loaded Dataset
		// Normal 'equals' method works - as it uses the same FeatureVector implementation
		Assert.assertEquals(dataset,copiedDataset);
		
		// You can transform the Sparse (default) implementation to a Dense representation
		Dataset copyDense = new MakeDenseTransformer().fitAndTransform(copiedDataset);
		
		// Now the normal equals does not work
		Assert.assertNotEquals(dataset,copyDense);
		// A utility method exists for checking equality 
		Assert.assertTrue(DataUtils.equals(dataset,copiedDataset));

		////// SHUFFLING DATA
		// Shuffle the data around
		copiedDataset.shuffle();
		// Now they do not match!
		Assert.assertNotEquals(dataset,copiedDataset);

		
		////// SPLITTING DATA 
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
		
		
		////// WRITING ENCRYPTED DATA
		URI proLicense = Config.getURI("license.pro", null);
		if (proLicense != null) {
			EncryptionSpecification spec = null;
			try {
				spec = CPSignFactory.getEncryptionSpec(proLicense);
			} catch (IllegalAccessException e) {}
			
			if (spec != null) {
				try {
					
					File encryptedFile = File.createTempFile("encr_problem", ".svm");
					encryptedFile.deleteOnExit();
					
				// Write encrypted file
				dataset.writeRecords(new FileOutputStream(encryptedFile), spec);
				
				// See what is written:
				String contents = FileUtils.readFileToString(encryptedFile, StandardCharsets.UTF_8);
				System.out.println("Encrypted contents:");
				System.out.println(contents.substring(0, Math.min(contents.length(), 100)));

				// Load the Dataset back
				Dataset fromEncryptedFile = Dataset.fromLIBSVMFormat(new FileInputStream(encryptedFile), spec);

				// Make sure that it matches the original problem!
				Assert.assertEquals(dataset, fromEncryptedFile);
//				if (!fromEncryptedFile.equals(dataset))
//					System.err.println("Loaded dataset should equal saved dataset");
				} catch (Exception e) {
					
				}
			}
		}
		
	}

//	public void showEncryption() throws IllegalAccessException, IllegalArgumentException, FileNotFoundException, IOException, InvalidKeyException, InvalidLicenseException{
//		// Check if a license with encryption was given
//		if (encryptionFactory == null || !encryptionFactory.supportEncryption())
//			return;
//
//		// Get the IEncryptionSpec that allows you to encrypt stuff
//		EncryptionSpecification spec = encryptionFactory.getEncryptionSpec();
//
//		// Write encrypted file
//		dataset.writeRecords(new FileOutputStream(encryptedFile), spec);
//
//		// Load the Problem back
//		Dataset fromEncryptedFile = Dataset.fromLIBSVMFormat(new FileInputStream(encryptedFile), spec);
//
//		System.out.println("Dataset re-loaded from encrypted file");
//
//		// Make sure that it matches the original problem!
//		if (!fromEncryptedFile.equals(dataset))
//			System.err.println("Loaded dataset should equal saved dataset");
//	}


}
