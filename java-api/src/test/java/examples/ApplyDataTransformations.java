package examples;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import org.junit.Test;

import com.arosbio.chem.io.in.SDFile;
import com.arosbio.cheminf.ChemCPRegressor;
import com.arosbio.cheminf.descriptors.ChemDescriptor;
import com.arosbio.cheminf.descriptors.DescriptorFactory;
import com.arosbio.commons.Stopwatch;
import com.arosbio.data.transform.feature_selection.DropMissingDataSelector;
import com.arosbio.data.transform.feature_selection.L2_SVR_Selector;
import com.arosbio.data.transform.scale.RobustScaler;
import com.arosbio.ml.algorithms.svm.LinearSVR;
import com.arosbio.ml.cp.acp.ACPRegressor;
import com.arosbio.ml.cp.nonconf.regression.LogNormalizedNCM;
import com.arosbio.ml.metrics.Metric;
import com.arosbio.ml.sampling.RandomSampling;
import com.arosbio.ml.testing.KFoldCV;
import com.arosbio.ml.testing.TestRunner;

import utils.Config;

public class ApplyDataTransformations {

	static int numSamples = Config.getInt("modeling.sampling.num.models", 5);
	static double calibrationRatio = Config.getDouble("modeling.sampling.calib.ratio",0.2);
	static URI dataFile = Config.getURI("regression.dataset", null);
	static String property = Config.getProperty("regression.endpoint");

	/*
	 * Note: this tests (for this particular data set) takes roughly 13-15 minutes to run
	 * as it requires computing CDK descriptors that are time-consuming to compute. Normally
	 * the default descriptor (Signatures) give good performance but they typically do not 
	 * require data transformations so we use physicochemical descriptors that do need standardization.
	 */
	@Test
	public void evaluateTransformations() throws IllegalAccessException, IOException {

		// Chose your predictor and scoring algorithm
		ACPRegressor predictor = new ACPRegressor(
				new LogNormalizedNCM(new LinearSVR()),
				new RandomSampling(numSamples, calibrationRatio));

		// Wrap the predictor in Signatures-wrapper
		ChemCPRegressor chemPredictor = new ChemCPRegressor(predictor);
		
		// Get only the CDK-descriptors, and use only the ones not requiring 3D coordinates 
		// If 3D not present they will be estimated - taking a long time
		List<ChemDescriptor> cdkDescriptorsNo3D = DescriptorFactory.getCDKDescriptorsNo3D();

		// Only take half of them, to save some time
		cdkDescriptorsNo3D = cdkDescriptorsNo3D.subList(0, cdkDescriptorsNo3D.size() / 2);

		// Note that each descriptor can calculate several features
		System.out.println("Initial number of descriptors: " + cdkDescriptorsNo3D.size());

		// Set the new descriptors - overwriting the default SignaturesDescriptor
		chemPredictor.getDataset().setDescriptors(cdkDescriptorsNo3D);

		// Load data
		chemPredictor.addRecords(new SDFile(dataFile).getIterator(), 
				property);

		System.out.println("Total number of features: " + chemPredictor.getDataset().getNumAttributes());

		// Some of them has issues - so check for features with missing values
		chemPredictor.getDataset().apply(new DropMissingDataSelector());

		System.out.println("Num features after initial missing-data-filtration: " + chemPredictor.getDataset().getNumAttributes());

		// Evaluate this data set - note: not scaled or performed any feature-selection
		TestRunner tester = new TestRunner.Builder(new KFoldCV()).build();
		Stopwatch sw = new Stopwatch();
		sw.start();
		List<Metric> metrics = tester.evaluate(chemPredictor.getDataset(),chemPredictor.getPredictor());
		sw.stop();
		System.out.println("\nInitial results using 10-fold CV "+sw+" :");
		for (Metric m : metrics) {
			System.out.println(m);
		}

		// Perform robust scaling of features and then feature-selection
		chemPredictor.getDataset().apply(new RobustScaler(), new L2_SVR_Selector());

		System.out.println("\nNum features after feature-selection: " + chemPredictor.getDataset().getNumAttributes());

		sw.start();
		List<Metric> newMetrics = tester.evaluate(chemPredictor.getDataset(), chemPredictor.getPredictor());
		sw.stop();
		System.out.println("Results after feature-selection and scaling using 10-fold CV " + sw + " :");
		for (Metric m : newMetrics) {
			System.out.println(m);
		}

	}
}
