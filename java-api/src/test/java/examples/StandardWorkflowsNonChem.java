package examples;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.util.Locale;
import java.util.Map;

import org.junit.Test;

import com.arosbio.cheminf.io.ModelSerializer;
import com.arosbio.data.DataRecord;
import com.arosbio.data.Dataset;
import com.arosbio.ml.algorithms.ScoringClassifier;
import com.arosbio.ml.algorithms.svm.EpsilonSVR;
import com.arosbio.ml.algorithms.svm.LinearSVC;
import com.arosbio.ml.algorithms.svm.PlattScaledC_SVC;
import com.arosbio.ml.algorithms.svm.SVC;
import com.arosbio.ml.algorithms.svm.SVR;
import com.arosbio.ml.cp.CPRegressionPrediction;
import com.arosbio.ml.cp.acp.ACPClassifier;
import com.arosbio.ml.cp.acp.ACPRegressor;
import com.arosbio.ml.cp.nonconf.classification.InverseProbabilityNCM;
import com.arosbio.ml.cp.nonconf.classification.NCMMondrianClassification;
import com.arosbio.ml.cp.nonconf.classification.NegativeDistanceToHyperplaneNCM;
import com.arosbio.ml.cp.nonconf.regression.NCMRegression;
import com.arosbio.ml.cp.nonconf.regression.NormalizedNCM;
import com.arosbio.ml.cp.tcp.TCPClassifier;
import com.arosbio.ml.io.ModelInfo;
import com.arosbio.ml.sampling.FoldedSampling;
import com.arosbio.ml.sampling.FoldedStratifiedSampling;
import com.arosbio.ml.sampling.RandomSampling;
import com.arosbio.ml.vap.avap.AVAPClassifier;
import com.arosbio.ml.vap.avap.CVAPPrediction;

import utils.Config;

/*
 * A key thing to note for using non-chemical descriptor data is that the SVM hyper-parameters
 * have been set to the sweet-spot for signatures-based data. This could be far from the ideal
 * hyper-parameters for other data, so using parameter tuning could have large impact on the 
 * predictive performance!
 */
public class StandardWorkflowsNonChem {

	@SuppressWarnings("unused")
	@Test
	public void conformalClassification() throws Exception {

		///// INSTANTIATION OF PREDICTOR OBJECT

		// Conformal predictors are based on a non-conformity measure (NCM)
		// Which require a particular underlying scoring algorithm

		// The 'negative distance to hyperplane' NCM requires a Support Vector Classifier
		SVC svc = new LinearSVC();
		NCMMondrianClassification ncm = new NegativeDistanceToHyperplaneNCM(svc);

		// the NCM can then be used in any of the Conformal classifier implementations:
		// ICP - using a single random split of calibration and proper training set (being a special case of an ACP)
		ACPClassifier icp = new ACPClassifier(ncm.clone(), new RandomSampling(1, .2));
		// ACP - using several aggregated ICPs is created using several random splits
		ACPClassifier acp = new ACPClassifier(ncm.clone(), new RandomSampling(10, .2));
		// CCP - using a folded sampling strategy to yield cross-conformal predictors
		ACPClassifier ccp = new ACPClassifier(ncm.clone(), new FoldedSampling(5));
		// TCP 
		TCPClassifier tcp = new TCPClassifier(ncm.clone());

		// Instantiation can also be done directly using the constructors of the classes
		// Here using the InverseProbability NCM 
		PlattScaledC_SVC probabilityScorer = new PlattScaledC_SVC();
		NCMMondrianClassification ncmProbability = new InverseProbabilityNCM(probabilityScorer);
		ACPClassifier icpProbability = new ACPClassifier(ncmProbability, new RandomSampling(1, .2));

		// The ACP classifier can now be used for training / predicting plain records - with no molecular data

		// Loading data can be from LIBSVM data format
		Dataset dataset = null;
		URI uri = Config.getURI("numerical.classification", null);
		try (InputStream stream = uri.toURL().openStream()){
			dataset = Dataset.fromLIBSVMFormat(stream);
		}
		// The Dataset.SubSet class contains a single design matrix and the corresponding labels
		// The 'base object' for datasets is the Dataset class, where data can be earmarked 
		// for using exclusively for model calibration or training of underlying scoring models.
		// The 'normal' dataset is used for both calibration and proper training data - depending on 
		// how it is sampled  
		// Dataset data = new Dataset();
		// data.setDataset(dataset);
		// To set some records to only be used for calibration, use:
		// data.withCalibrationExclusiveDataset(calibrationExclusive);
		// To set some records to only be used for fitting the underlying model, use: 
		// data.withModelingExclusiveDataset(modelingExclusive);

		// take out the first record for testing
		DataRecord testRecord = dataset.getDataset().remove(0);

		// Training is then performed using the dataset
		icp.train(dataset);

		// Predict the record not used in the training
		Map<Integer,Double> prediction = icp.predict(testRecord.getFeatures());

		System.out.printf("prediction for test-record of true class {%d}: %s%n",(int)testRecord.getLabel(), prediction);

		// Saving the model is performed using the ModelCreator class 
		File tmpModel = File.createTempFile("classification-model", "jar");
		tmpModel.deleteOnExit();
		icp.setModelInfo(new ModelInfo("cp-classifier"));
		ModelSerializer.saveModel(icp, tmpModel, null);

		// Loading is done using the ModelLoader class
		ACPClassifier loadedPredictor = (ACPClassifier) ModelSerializer.loadPredictor(tmpModel.toURI(), null);

		// Predicting with the loaded predictor should yield the exact same result
		// (unless some randomness is part of the predictor)
		Map<Integer,Double> prediction2 = loadedPredictor.predict(testRecord.getFeatures());
		System.out.printf("prediction for test-record of true class {%d}: %s%n",(int)testRecord.getLabel(), prediction);

	}

	@Test
	public void conformalRegression() throws Exception {
		// Instantiate the underlying scoring model, here a SVR with RBF kernel
		SVR rbfSVR = new EpsilonSVR();
		// Pick the nonconformity measure to use
		NCMRegression ncm = new NormalizedNCM(rbfSVR);

		// Only one Conformal regression class exists, generating 
		// either an ICP, ACP or CCP regression predictor depending on the SamplingStrategy and settings thereof that are used
		ACPRegressor icp = new ACPRegressor(ncm, new RandomSampling(1, 0.25));

		// Loading data can be from LIBSVM data format
		Dataset dataset = null;
		URI uri = Config.getURI("numerical.regression", null);
		try (InputStream stream = uri.toURL().openStream()){
			dataset = Dataset.fromLIBSVMFormat(stream);
		}
		// The Dataset.SubSet class contains a single design matrix and the corresponding labels
		// The 'base object' for datasets is the Dataset class, where data can be earmarked 
		// for using exclusively as calibration or proper training data.
		// The 'normal' dataset is used for both calibration and proper training data - depending on 
		// how it is sampled. To set some records to only be used for calibration, use:
		// data.setCalibrationExclusiveDataset(calibrationExclusive);
		// To set some records to only be used for fitting the underlying model, use: 
		// data.setModelingExclusiveDataset(modelingExclusive);

		// take out the first record for testing
		DataRecord testRecord = dataset.getDataset().remove(0);

		// Training is then performed using the dataset
		icp.train(dataset);

		// Regression requires one or more confidence values for the predictor
		double confidence = Config.getDouble("modeling.conf", .8);
		CPRegressionPrediction prediction = icp.predict(testRecord.getFeatures(), confidence);

		// The prediction output has more information than for the classifier above, containing 
		// The point-prediction (aggregated for all ICP-models) and the prediction 
		// for each confidence level, both the predicted interval and the capped interval based on 
		// minimum and maximum encountered value in the training set. 
		System.out.printf(Locale.ENGLISH,"prediction for test-record with true label {%.3f}%nmidpoint: %.3f%ninterval: %s%n",
			testRecord.getLabel(),prediction.getY_hat(), prediction.getInterval(confidence).getInterval());

		// Saving and loading the predictor is done in the same way as for the classifier above 
	}

	@Test 
	public void vennABERSclassifier() throws Exception {
		// Venn-ABERS predictors work slightly differently - by calibrating the predictions of 
		// a scoring classifier. Further note that only binary classification is possible

		ScoringClassifier classifier = new LinearSVC();
		AVAPClassifier cvap = new AVAPClassifier(classifier, new FoldedStratifiedSampling(10));

		Dataset dataset = null;
		URI uri = Config.getURI("numerical.classification", null);
		try (InputStream stream = uri.toURL().openStream()){
			dataset = Dataset.fromLIBSVMFormat(stream);
		}
		// The Dataset.SubSet class contains a single design matrix and the corresponding labels
		// The 'base object' for datasets is the Dataset class, where data can be earmarked 
		// for using exclusively as calibration or proper training data.
		// The 'normal' dataset is used for both calibration and proper training data - depending on 
		// how it is sampled. To set some records to only be used for calibration, use:
		// data.setCalibrationExclusiveDataset(calibrationExclusive);
		// To set some records to only be used for fitting the underlying model, use: 
		// data.setModelingExclusiveDataset(modelingExclusive);

		// take out the first record for testing
		DataRecord testRecord = dataset.getDataset().remove(0);

		// Training is then performed using the dataset
		cvap.train(dataset);
		
		// Predictions can either be made for probabilities only
		CVAPPrediction<Integer>  prediction = cvap.predict(testRecord.getFeatures());

		System.out.printf("probabilities for test-record of true class {%d}: %s%n",
			(int)testRecord.getLabel(), prediction.getProbabilities());
		System.out.printf(Locale.ENGLISH,"full prediction for test-molecule:%nmean width: %.4f%nmedian width: %.4f%n", 
			prediction.getMeanP0P1Width(), prediction.getMedianP0P1Width());
		// The 'interval width' carries intrinsic information about how much the prediction can be trusted.
		// The width refers to how much the prediction differed between the two assumed labels

		// Saving and loading the predictor is done in the same way as for the classifier above
	}

}
