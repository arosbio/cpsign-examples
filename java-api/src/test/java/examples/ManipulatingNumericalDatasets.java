package examples;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;

import com.arosbio.commons.GlobalConfig;
import com.arosbio.data.DataUtils;
import com.arosbio.data.Dataset;
import com.arosbio.data.DenseFloatVector;
import com.arosbio.data.DenseVector;
import com.arosbio.data.Dataset.SubSet;
import com.arosbio.data.transform.format.MakeDenseTransformer;
import com.arosbio.encryption.EncryptionSpecification;

import utils.Config;

public class ManipulatingNumericalDatasets {


	@SuppressWarnings("unused")	
	@Test
	public void manipulate() throws IOException {
		////// LOADING
		
		// A Dataset can be loaded easily
		SubSet dataset = SubSet.fromLIBSVMFormat(Config.getURI("numerical.classification", null).toURL().openStream());
		System.out.println("Original dataset: " + dataset);
		
		
		////// MAKING COPIES (CLONES)
		SubSet copiedDataset = dataset.clone();

		
		////// CHECKING FOR EQUALITY 
		// The data is now identical to the first loaded Dataset
		// Normal 'equals' method works - as it uses the same FeatureVector implementation
		Assert.assertEquals(dataset,copiedDataset);
		
		// You can transform the Sparse (default) implementation to a Dense representation
		SubSet copyDense = new MakeDenseTransformer().fit(copiedDataset).transform(copiedDataset);
		
		// Depending on if you're trying to save memory you get either single or double precision floating point representation of the attributes
		if (GlobalConfig.getInstance().isMemSaveMode())
			Assert.assertTrue(copyDense.get(0).getFeatures() instanceof DenseFloatVector);
		else
			Assert.assertTrue(copyDense.get(0).getFeatures() instanceof DenseVector);
		// This can be altered using the transformer.useDoublePrecision(boolean) method instead of using the global setting
		
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
		SubSet[] staticPartitions = dataset.splitStatic(indexToSplitAt);
		System.out.println("Static partition 0: " + staticPartitions[0].size());
		System.out.println("Static partition 1: " + staticPartitions[1].size());

		// Or static by what fraction to split at
		double firstFraction = 0.3; // first set should include 30% 
		SubSet[] staticPartitionsByFraction = dataset.splitStatic(firstFraction);
		System.out.println("Static partition by fraction 0: " + staticPartitionsByFraction[0].size());
		System.out.println("Static partition by fraction 1: " + staticPartitionsByFraction[1].size());

		// Or use a Random partitioning 
		SubSet[] randomPartitions = dataset.splitRandom(firstFraction);
		System.out.println("Random partition 0: " + randomPartitions[0].size());
		System.out.println("Random partition 1: " + randomPartitions[1].size());
		
		
		////// WRITING ENCRYPTED DATA
		EncryptionSpecification spec = null; // Fill in if you have an Encryption spec
		
		if (spec != null) {
			File encryptedFile = File.createTempFile("encr_data", ".svm");
			encryptedFile.deleteOnExit();

			try (OutputStream out = new FileOutputStream(encryptedFile)){
				
				// Write encrypted file
				dataset.writeRecords(out, spec);
			} catch (InvalidKeyException e){
				System.err.println("Could not encrypt data - invalid encryption specification!");
				return;
			}
				
			// See what is written:
			String contents = FileUtils.readFileToString(encryptedFile, StandardCharsets.UTF_8);
			System.out.println("Encrypted contents:");
			System.out.println(contents.substring(0, Math.min(contents.length(), 100)));

			try (InputStream input = new FileInputStream(encryptedFile)){
				// Load the Dataset back
				Dataset fromEncryptedFile = Dataset.fromLIBSVMFormat(input, spec);

				// Make sure that it matches the original problem!
				Assert.assertEquals(dataset, fromEncryptedFile);

			} catch (Exception e) {
				
			}
		}
		
	}



}
