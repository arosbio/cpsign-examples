package examples.regression;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.CannotProceedException;

import com.arosbio.chem.io.in.SDFile;
import com.arosbio.modeling.CPSignFactory;
import com.arosbio.modeling.cheminf.SignaturesCPRegression;
import com.arosbio.modeling.ml.cp.acp.ACPRegression;
import com.arosbio.modeling.ml.ds_splitting.RandomSampling;
import com.arosbio.modeling.ml.gridsearch.GridSearch;
import com.arosbio.modeling.ml.gridsearch.GridSearch.GSResult;
import com.arosbio.modeling.ml.gridsearch.GridSearchException;
import com.arosbio.modeling.ml.gridsearch.GridSearchResult;
import com.arosbio.modeling.ml.interfaces.Tunable.TunableParameter;
import com.arosbio.modeling.ml.metrics.MedianPredictionIntervalWidth;
import com.arosbio.modeling.ml.testing.KFoldCVSplitter;

import examples.utils.Config;
import examples.utils.SysOutWriter;
import examples.utils.Utils;

public class ParameterTuningRegression {

	CPSignFactory factory;

	private List<Double> betaValuesToTry = Arrays.asList(0.0, 0.25, 0.5);


	public static void main(String[] args) throws CannotProceedException, IllegalArgumentException, IllegalAccessException, IOException, GridSearchException {
		ParameterTuningRegression acp = new ParameterTuningRegression();
		acp.intialise();
		acp.listAvailableParameters();
		acp.tuneParameters();
		System.out.println("Finished Example Tune Parameters");
	}


	/**
	 * This method just initializes some variables and the CPSignFactory. Please change the 
	 * initialization of CPSignFactory to point to your active license. Also change the 
	 * model and signature-files into a location on your machine so that they can be used 
	 * later on, now temporary files are created for illustrative purposes. 
	 */
	public void intialise() {
		// Start with instantiating CPSignFactory with your license
		factory = Utils.getFactory();
	}

	public void listAvailableParameters() {
		// Chose your predictor, NCM and scoring algorithm
		ACPRegression predictor = factory.createACPRegression(
				factory.createAbsDifferenceNCM(factory.createLibLinearRegression()), 
				new RandomSampling(Config.NUM_OF_AGGREGATED_MODELS, Config.CALIBRATION_RATIO));
		List<TunableParameter> params = predictor.getTunableParameters();
		System.out.println("Possible parameters to gridsearch for ACP Regression");
		for (TunableParameter p : params) {
			System.out.println(p);
		}
	}


	public void tuneParameters() throws IllegalArgumentException, IllegalAccessException, IOException, GridSearchException {
		// Create a GridSearch object - optionally set nrCV folds, nr ACP models etc..
		GridSearch gs = new GridSearch(new KFoldCVSplitter(Config.NUM_FOLDS_CV), Config.CV_CONFIDENCE, Config.CV_TOLERANCE);

		// Set your custom parameter regions
		Map<String,List<Object>> paramGrid = new HashMap<>();
		paramGrid.put("COST", Arrays.asList(1, 10, 100));
		// Grid search beta only if NCM LogNormalizedNCM is set! 
		paramGrid.put("NCM_BETA", new ArrayList<>(betaValuesToTry)); // if LogNormalizedNonconfMeasureRegression is not set, the grid search will not be done even if this list is set
		// Set a Writer to write all output to (otherwise only give you the 'n' optimal results)
		// Here simply write to system out but would likely be to a file
		gs.setLoggingWriter(new SysOutWriter());

		// Chose your predictor and scoring algorithm
		ACPRegression predictor = factory.createACPRegression(
				factory.createLogNormalizedNCM(factory.createLibLinearRegression(), null, 0), 
				new RandomSampling(Config.NUM_OF_AGGREGATED_MODELS, Config.CALIBRATION_RATIO));

		// Wrap the predictor in Signatures-wrapper
		SignaturesCPRegression signPredictor = factory.createSignaturesCPRegression(predictor, 1, 3);

		// Load data
		signPredictor.fromMolsIterator(new SDFile(Config.REGRESSION_DATASET).getIterator(), 
				Config.REGRESSION_ENDPOINT);

		// Start the Grid Search
		GridSearchResult res = gs.search(signPredictor, 
				new MedianPredictionIntervalWidth(Config.CV_CONFIDENCE),
				paramGrid);
		System.out.println(res);

		// The best "n" (definable) results are available to check afterwards - sorted with the best one first
		for (GSResult r : res.getBestParameters()) {
			System.out.println(r + " params: " + r.getParams());
		}
	}

}
