package examples;

import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;

import org.junit.Test;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;

import com.arosbio.chem.io.in.SDFile;
import com.arosbio.modeling.cheminf.NamedLabels;
import com.arosbio.modeling.cheminf.SignaturesCPClassification;
import com.arosbio.modeling.cheminf.SignaturesCPRegression;
import com.arosbio.modeling.cheminf.SignaturesVAPClassification;
import com.arosbio.modeling.cheminf.SignaturesVAPClassification.SignaturesCVAPResult;
import com.arosbio.modeling.ml.algorithms.ScoringClassifier;
import com.arosbio.modeling.ml.algorithms.svm.LinearSVC;
import com.arosbio.modeling.ml.algorithms.svm.LinearSVR;
import com.arosbio.modeling.ml.algorithms.svm.PlattScaledC_SVC;
import com.arosbio.modeling.ml.algorithms.svm.SVC;
import com.arosbio.modeling.ml.algorithms.svm.SVR;
import com.arosbio.modeling.ml.cp.CPRegressionPrediction;
import com.arosbio.modeling.ml.cp.acp.ACPClassification;
import com.arosbio.modeling.ml.cp.acp.ACPRegression;
import com.arosbio.modeling.ml.cp.nonconf.NCM;
import com.arosbio.modeling.ml.cp.nonconf.classification.InverseProbabilityNCM;
import com.arosbio.modeling.ml.cp.nonconf.classification.NCMMondrianClassification;
import com.arosbio.modeling.ml.cp.nonconf.regression.NCMRegression;
import com.arosbio.modeling.ml.cp.nonconf.regression.NormalizedNCM;
import com.arosbio.modeling.ml.cp.tcp.TCPClassification;
import com.arosbio.modeling.ml.ds_splitting.FoldedSampling;
import com.arosbio.modeling.ml.ds_splitting.RandomSampling;
import com.arosbio.modeling.ml.vap.avap.AVAPClassification;

import utils.BaseTest;
import utils.Config;

public class StandardWorkflows extends BaseTest {

	@SuppressWarnings("unused")
	@Test
	public void conformalClassification() throws Exception {

		///// INSTANTIATION OF PREDICTOR OBJECT

		// Conformal predictors are based on a non-conformity measure (NCM)
		// Which require a particular underlying scoring algorithm

		// The 'negaative distance to hyperplane' NCM requires a Support Vector Classifier
		SVC svc = factory.createLinearSVC();
		NCMMondrianClassification ncm = factory.createNegativeDistanceToHyperplaneNCM(svc);

		// the NCM can then be used in any of the Conformal classifier implementations:
		// ICP - using a single random split of calibration and proper training set (being a special case of an ACP)
		ACPClassification icp = factory.createACPClassification(ncm, new RandomSampling(1, .2));
		// ACP - using several aggregated ICPs is created using several random splits
		ACPClassification acp = factory.createACPClassification(ncm, new RandomSampling(10, .2));
		// CCP - using a folded sampling strategy to yield cross-conformal predictors
		ACPClassification ccp = factory.createACPClassification(ncm, new FoldedSampling(5));
		// TCP 
		TCPClassification tcp = factory.createTCPClassification(ncm);

		// Instantiation can also be done directly using the constructors of the classes
		// Here using the InverseProbability NCM 
		PlattScaledC_SVC probabilityScorer = new PlattScaledC_SVC();
		NCMMondrianClassification ncmProbability = new InverseProbabilityNCM(probabilityScorer);
		ACPClassification icpProbability = new ACPClassification(ncmProbability, new RandomSampling(1, .2));

		// The ACP classifier can now be used for training / predicting plain records - with no molecular data
		// e.g. icp.train(problem); icp.predict(example);

		// But CPSign is intended mainly for molecular data records
		// The predictor is then wrapped in a SignaturesXX implementation of the correct class, e.g.:
		SignaturesCPClassification moleculePredictor = factory.createSignaturesCPClassification(icp, 1, 3);
		// This creates a wrapper that directly deals with molecular data, by computing descriptors etc.
		// This default instantiation uses the signatures descriptor with height 1-3
		// How other descriptors are used is exemplified in SettingDescriptors.java 


		///// LOADING DATA AND COMPUTING DESCRIPTORS

		// Loading data can be used with e.g. CDK classes or any of the convenience stuff supplied with CPSign
		// e.g. IteratingSDFReader in CDK, or with CPSign wrapper class:
		SDFile sdf = new SDFile(Config.getURI("classification.dataset", null));

		// The fromMolsIterator takes molecules from an iterator and computes the descriptors
		// Gradually building up the data set
		moleculePredictor.fromMolsIterator(
				sdf.getIterator(), 
				Config.getProperty("classification.endpoint"),
				new NamedLabels(Config.getProperty("classification.labels").split("[\\s,]")));

		// Then train the predictor, using the computed records. Note that the records are stored
		// in the SignatureXX-wrapper object
		moleculePredictor.train();
		// internally this is the same thing as the following:
		// acp.train(acpClassification.getProblem());

		// Predictions are made directly on IAtomContainers, applying any potential data-transformations
		// prior to predictions
		IAtomContainer testMolecule = new SmilesParser(SilentChemObjectBuilder.getInstance()).parseSmiles(Config.DEFAULT_SMILES);
		Map<String,Double> prediction = moleculePredictor.predictMondrian(testMolecule);

		System.out.println("prediction for test-molecule: " + prediction);
	}

	@Test
	public void conformalRegression() throws Exception {
		// Just as for Conformal Classifiers in the example above, instantiation can be 
		// performed using either the CPSignFactory class or by the custructors of the 
		// used classes
		SVR linearSVR = new LinearSVR();
		NCMRegression ncm = new NormalizedNCM(linearSVR);

		// Only one Conformal regression class exists, generating 
		// either an ICP, ACP or CCP regression predictor
		ACPRegression icp = new ACPRegression(ncm, new RandomSampling(1, 0.25));

		// Wrap in the Signatures wrapper class 
		SignaturesCPRegression moleculePredictor = new SignaturesCPRegression(icp);

		// Load data
		SDFile sdf = new SDFile(Config.getURI("regression.dataset", null));
		moleculePredictor.fromMolsIterator(
				sdf.getIterator(), 
				Config.getProperty("regression.endpoint"));

		moleculePredictor.train();

		// Predict the test molecule - requiring one or more confidences in the prediction(s)
		IAtomContainer testMolecule = new SmilesParser(SilentChemObjectBuilder.getInstance()).parseSmiles(Config.DEFAULT_SMILES);
		double confidence = Config.getDouble("modeling.conf", .8);
		CPRegressionPrediction prediction = moleculePredictor.predict(testMolecule, confidence);

		// The prediction output has more information than for the classifier above, containing 
		// The point-prediction (aggregated for all ICP-models) and the prediction 
		// for each confidence level, both the predicted interval and the capped interval based on 
		// mininum and maximum encountered value in the training set. 
		System.out.println("prediction for test-molecule:\nmidpoint: " + prediction.getY_hat() +
				"\ninterval: " + prediction.getInterval(confidence).getInterval());

	}

	@Test 
	public void vennABERSclassifier() throws Exception {
		// Venn ABERS predictors work slightly differently - by calibrating the predictions of 
		// a scoring classifier. Further note that only binary classification is possible

		ScoringClassifier classifier = new LinearSVC();
		AVAPClassification cvap = new AVAPClassification(classifier, new FoldedSampling(10));

		SignaturesVAPClassification moleculePredictor = new SignaturesVAPClassification(cvap);

		// Loading data and training is identical to the conformal predictor wrappers above

		SDFile sdf = new SDFile(Config.getURI("classification.dataset", null));

		moleculePredictor.fromMolsIterator(
				sdf.getIterator(), 
				Config.getProperty("classification.endpoint"),
				new NamedLabels(Config.getProperty("classification.labels").split("[\\s,]")));

		moleculePredictor.train();

		IAtomContainer testMolecule = new SmilesParser(SilentChemObjectBuilder.getInstance()).parseSmiles(Config.DEFAULT_SMILES);
		// Predictions can either be made for probabilities only
		Map<String,Double> prediction = moleculePredictor.predictProbabilities(testMolecule);
		// Or to get all available info - i.e. also the 
		SignaturesCVAPResult fullPrediction = moleculePredictor.predict(testMolecule);

		System.out.println("probabilities for test-molecule: " + prediction);
		System.out.println("full prediction for test-molecule:\nprobabilities: " + fullPrediction.getProbabilties() + 
				"\nmean width: " + fullPrediction.getMeanIntervalWidth()+
				"\nmedian width: " + fullPrediction.getMedianIntervalWidth());
		// The 'interval width' carries intrinsic information about how much the prediction can be trusted.
		// The width refers to how much the prediction differed between the two assumed labels
	}

	/*
	 * Note that CPSign uses ServiceLoader functionality which allows the user to 
	 * both list all available implementations and to extend CPSign with the things you need.
	 * This can also be applied to the CLI as long as you implement and adhere the 
	 * requirements for Java services. Currently these interfaces can be extended:
	 * - Descriptors
	 * - Transformer
	 * - MLAlgorithm
	 * - PValueCalculator
	 * - NCM
	 * - SamplingStrategy
	 * - Metric
	 * - TestingStrategy
	 */
	@Test
	public void listAvailableImplementations() throws Exception {
		ServiceLoader<NCM> ncmLoader = ServiceLoader.load(NCM.class); 

		System.out.println("All available NCMs:");
		Iterator<NCM> iter = ncmLoader.iterator();
		while (iter.hasNext()) {
			System.out.println(iter.next().getNames().get(0));
		}

	}

}
