package examples;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;

import org.junit.Test;

import com.arosbio.modeling.data.DataRecord;
import com.arosbio.modeling.data.Dataset;
import com.arosbio.modeling.data.Problem;
import com.arosbio.modeling.io.ModelCreator;
import com.arosbio.modeling.io.ModelInfo;
import com.arosbio.modeling.io.ModelLoader;
import com.arosbio.modeling.ml.algorithms.ScoringClassifier;
import com.arosbio.modeling.ml.algorithms.svm.EpsilonSVR;
import com.arosbio.modeling.ml.algorithms.svm.LinearSVC;
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
import com.arosbio.modeling.ml.vap.avap.AVAPClassificationResult;

import utils.BaseTest;
import utils.Config;

/*
 * A key thing to note for using non-signatures descriptor data is that the SVM parameters
 * have been set to the sweet-spot for signatures-based data. This could be far from the ideal
 * parameters for other data, so using parameter tuning could have large impact on the predictive
 * performance!
 */
public class StandardWorkflowsNonSignatures extends BaseTest {

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

		// Loading data can be from LIBSVM data format
		Dataset dataset = null;
		URI uri = Config.getURI("numerical.classification", null);
		try (InputStream stream = uri.toURL().openStream()){
			dataset = Dataset.fromLIBSVMFormat(stream);
		}
		// The Dataset class contains a single design matrix and the corresponding labels
		// The 'base object' for datasets is the Problem class, where data can be earmarked 
		// for using exclusively as calibration or proper training data.
		// The 'normal' dataset is used for both calibration and proper training data - depending on 
		// how it is sampled  
		Problem data = new Problem();
		data.setDataset(dataset);
		// To set some records to only be used for calibration, use:
		// data.setCalibrationExclusiveDataset(calibrationExclusive);
		// To set some records to only be used for fitting the underlying model, use: 
		// data.setModelingExclusiveDataset(modelingExclusive);

		// take out the first record for testing
		DataRecord testRecord = dataset.getRecords().remove(0);

		// Training is then performed using the dataset
		icp.train(data);

		// Predict the record not used in the training
		Map<Integer,Double> prediction = icp.predict(testRecord.getFeatures());

		System.out.println("prediction for test-record of true class {"+(int)testRecord.getLabel()+"}: " + prediction);

		// Saving the model is performed using the ModelCreator class 
		File tmpModel = File.createTempFile("classification-model", "jar");
		tmpModel.deleteOnExit();
		ModelCreator.generateTrainedModel(icp, new ModelInfo("cp-classifier"), tmpModel, null);

		// Loading is done using the ModelLoader class
		ACPClassification loadedPredictor = (ACPClassification) ModelLoader.loadModel(tmpModel.toURI(), null);

		// Predicting with the loaded predictor should yield the exact same result
		// (unless some randomness is part of the predictor)
		Map<Integer,Double> prediction2 = loadedPredictor.predict(testRecord.getFeatures());
		System.out.println("prediction for test-record of true class {"+(int)testRecord.getLabel()+"} (loaded): " + prediction);

	}

	@Test
	public void conformalRegression() throws Exception {
		// Just as for Conformal Classifiers in the example above, instantiation can be 
		// performed using either the CPSignFactory class or by the custructors of the 
		// used classes
		SVR linearSVR = new EpsilonSVR();
		NCMRegression ncm = new NormalizedNCM(linearSVR);

		// Only one Conformal regression class exists, generating 
		// either an ICP, ACP or CCP regression predictor
		ACPRegression icp = new ACPRegression(ncm, new RandomSampling(1, 0.25));

		// Loading data can be from LIBSVM data format
		Dataset dataset = null;
		URI uri = Config.getURI("numerical.regression", null);
		try (InputStream stream = uri.toURL().openStream()){
			dataset = Dataset.fromLIBSVMFormat(stream);
		}
		// The Dataset class contains a single design matrix and the corresponding labels
		// The 'base object' for datasets is the Problem class, where data can be earmarked 
		// for using exclusively as calibration or proper training data.
		// The 'normal' dataset is used for both calibration and proper training data - depending on 
		// how it is sampled  
		Problem data = new Problem();
		data.setDataset(dataset);
		// To set some records to only be used for calibration, use:
		// data.setCalibrationExclusiveDataset(calibrationExclusive);
		// To set some records to only be used for fitting the underlying model, use: 
		// data.setModelingExclusiveDataset(modelingExclusive);

		// take out the first record for testing
		DataRecord testRecord = dataset.getRecords().remove(0);

		// Training is then performed using the dataset
		icp.train(data);

		// Regression requires one or more confidence values for the predictor
		double confidence = Config.getDouble("modeling.conf", .8);
		CPRegressionPrediction prediction = icp.predict(testRecord.getFeatures(), confidence);

		// The prediction output has more information than for the classifier above, containing 
		// The point-prediction (aggregated for all ICP-models) and the prediction 
		// for each confidence level, both the predicted interval and the capped interval based on 
		// mininum and maximum encountered value in the training set. 
		System.out.println("prediction for test-record with true label {"+testRecord.getLabel()+"}:\nmidpoint: " + prediction.getY_hat() +
				"\ninterval: " + prediction.getInterval(confidence).getInterval());

		// Saving and loading the predictor is done in the same way as for the classifier above 
	}

	@Test 
	public void vennABERSclassifier() throws Exception {
		// Venn ABERS predictors work slightly differently - by calibrating the predictions of 
		// a scoring classifier. Further note that only binary classification is possible

		ScoringClassifier classifier = new LinearSVC();
		AVAPClassification cvap = new AVAPClassification(classifier, new FoldedSampling(10));

		Dataset dataset = null;
		URI uri = Config.getURI("numerical.classification", null);
		try (InputStream stream = uri.toURL().openStream()){
			dataset = Dataset.fromLIBSVMFormat(stream);
		}
		// The Dataset class contains a single design matrix and the corresponding labels
		// The 'base object' for datasets is the Problem class, where data can be earmarked 
		// for using exclusively as calibration or proper training data.
		// The 'normal' dataset is used for both calibration and proper training data - depending on 
		// how it is sampled  
		Problem data = new Problem();
		data.setDataset(dataset);
		// To set some records to only be used for calibration, use:
		// data.setCalibrationExclusiveDataset(calibrationExclusive);
		// To set some records to only be used for fitting the underlying model, use: 
		// data.setModelingExclusiveDataset(modelingExclusive);

		// take out the first record for testing
		DataRecord testRecord = dataset.getRecords().remove(0);

		// Training is then performed using the dataset
		cvap.train(data);
		
		// Predictions can either be made for probabilities only
		AVAPClassificationResult prediction = cvap.predict(testRecord.getFeatures());

		System.out.println("probabilities for test-record of true class {"+testRecord.getLabel()+"}: " + prediction.getProbabilities());
		System.out.println("full prediction for test-molecule:" + 
				"\nmean width: " + prediction.getMeanIntervalWidth()+
				"\nmedian width: " + prediction.getMedianIntervalWidth());
		// The 'interval width' carries intrinsic information about how much the prediction can be trusted.
		// The width refers to how much the prediction differed between the two assumed labels

		// Saving and loading the predictor is done in the same way as for the classifier above
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
