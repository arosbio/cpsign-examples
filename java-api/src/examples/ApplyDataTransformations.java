package examples;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.arosbio.auth.InvalidLicenseException;
import com.arosbio.chem.io.in.SDFile;
import com.arosbio.commons.Stopwatch;
import com.arosbio.modeling.cheminf.SignaturesCPRegression;
import com.arosbio.modeling.cheminf.descriptors.Descriptor;
import com.arosbio.modeling.cheminf.descriptors.DescriptorFactory;
import com.arosbio.modeling.data.transform.feature_selection.DropMissingDataSelecter;
import com.arosbio.modeling.data.transform.feature_selection.L2_SVR_Selecter;
import com.arosbio.modeling.data.transform.scale.RobustScaler;
import com.arosbio.modeling.ml.cp.acp.ACPRegression;
import com.arosbio.modeling.ml.ds_splitting.RandomSampling;
import com.arosbio.modeling.ml.metrics.Metric;
import com.arosbio.modeling.ml.testing.KFoldCV;
import com.arosbio.modeling.ml.testing.TestRunner;

import utils.BaseTest;
import utils.Config;

public class ApplyDataTransformations extends BaseTest {


	/*
	 * Note: this tests (for this particular data set) takes roughly 13-15 minutes to run
	 * as it requires CDK descriptors that are time consuming to compute. In real world scenarios,
	 * put some effort into picking the appropriate descriptors 
	 */
	@Test
	public void evaluateTransformations() throws IllegalAccessException, InvalidLicenseException, IOException {

		// Chose your predictor and scoring algorithm
		ACPRegression predictor = factory.createACPRegression(
				factory.createLogNormalizedNCM(factory.createLinearSVR(), null, 0.1), 
				new RandomSampling(Config.getInt("modeling.sampling.num.models", 5), Config.getDouble("modeling.sampling.calib.ratio",0.2)));

		// Wrap the predictor in Signatures-wrapper
		SignaturesCPRegression signPredictor = factory.createSignaturesCPRegression(predictor, 1, 3);
		
		// Get only the CDK-descriptors, and use only the ones not requiring 3D coordinates 
		// If 3D not present they will be estimated - taking a long time
		List<Descriptor> cdkDescriptorsNo3D = DescriptorFactory.getInstance().getCDKDescriptorsNo3D();

		// Only take half of them, save some time
		cdkDescriptorsNo3D = cdkDescriptorsNo3D.subList(0, cdkDescriptorsNo3D.size() / 2);

		// Note that each descriptor can calculate several features
		System.out.println("Initial number of descriptors: " + cdkDescriptorsNo3D.size());

		// Set the new descriptors - overwriting the default SignaturesDescriptor
		signPredictor.getProblem().setDescriptors(cdkDescriptorsNo3D);

		// Load data
		signPredictor.fromMolsIterator(new SDFile(Config.getURI("regression.dataset", null)).getIterator(), 
				Config.getProperty("regression.endpoint"));

		System.out.println("Total number of features: " + signPredictor.getFeatureNames(false).size());

		// Some of them has issues - so check for features with missing values
		signPredictor.getProblem().apply(new DropMissingDataSelecter());

		System.out.println("Num features after inital missing-data-filtration: " + signPredictor.getFeatureNames(false).size());

		// Evaluate this data set - note: not scaled or performed any feature-selection
		TestRunner tester = new TestRunner(new KFoldCV());
		Stopwatch sw = new Stopwatch();
		sw.start();
		List<Metric> metrics = tester.evaluate(signPredictor);
		sw.stop();
		System.out.println("\nInitial results using 10-fold CV "+sw+" :");
		for (Metric m : metrics) {
			System.out.println(m);
		}

		// Perform robust scaling of features and then feature-selection
		signPredictor.getProblem().apply(Arrays.asList(new RobustScaler(), new L2_SVR_Selecter()));

		System.out.println("\nNum features after feature-selection: " + signPredictor.getFeatureNames(false).size());

		sw.start();
		List<Metric> newMetrics = tester.evaluate(signPredictor);
		sw.stop();
		System.out.println("Results after feature-selection and scaling using 10-fold CV " + sw + " :");
		for (Metric m : newMetrics) {
			System.out.println(m);
		}

	}
}
