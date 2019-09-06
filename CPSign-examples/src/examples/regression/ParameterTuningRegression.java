package examples.regression;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;

import javax.naming.CannotProceedException;

import com.arosbio.chem.io.in.SDFile;
import com.arosbio.modeling.CPSignFactory;
import com.arosbio.modeling.cheminf.SignaturesCPRegression;
import com.arosbio.modeling.gridsearch.GridSearch;
import com.arosbio.modeling.gridsearch.GridSearch.OptimizationType;
import com.arosbio.modeling.gridsearch.GridSearch.ParameterRange;
import com.arosbio.modeling.gridsearch.GridSearchException;
import com.arosbio.modeling.gridsearch.GridSearchResult;
import com.arosbio.modeling.ml.cp.acp.ACPRegression;
import com.arosbio.modeling.ml.cp.nonconf.NonconfMeasureFactory;
import com.arosbio.modeling.ml.cp.nonconf.NonconfMeasureRegression;
import com.arosbio.modeling.ml.ds_splitting.RandomSampling;

import examples.utils.Config;
import examples.utils.Utils;

public class ParameterTuningRegression {

	CPSignFactory factory;

	private NonconfMeasureRegression nonconfMeasure = NonconfMeasureFactory.getLogNormalizedMeasureRegression(0.0);
	private List<Double> betaValuesToTry = Arrays.asList(0.0, 0.25, 0.5);


	public static void main(String[] args) throws CannotProceedException, IllegalArgumentException, IllegalAccessException, IOException, GridSearchException {
		ParameterTuningRegression acp = new ParameterTuningRegression();
		acp.intialise();
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

	
	public void tuneParameters() throws IllegalArgumentException, IllegalAccessException, IOException, GridSearchException {
		// Create a GridSearch object - optionally set nrCV folds, nr ACP models etc..
		GridSearch gs = new GridSearch(Config.NUM_FOLDS_CV, Config.CV_CONFIDENCE, Config.CV_TOLERANCE);
		// Set your custom parameter regions
		ParameterRange costRange = gs.getC();
		costRange.setStop(6);
		gs.setBeta(new ParameterRange(betaValuesToTry)); // if LogNormalizedNonconfMeasureRegression is not set, the grid search will not be done even if this list is set
		// Set a Writer to write all output to (otherwise only give you the optimal result)
		Writer writer = new OutputStreamWriter(System.out);
		gs.setWriter(writer);
		
		// Chose your predictor and scoring algorithm
		ACPRegression predictor = factory.createACPRegression(
				factory.createLibLinearRegression(), 
				new RandomSampling(Config.NUM_OF_AGGREGATED_MODELS, Config.CALIBRATION_RATIO));
		
		// Set the nonconformity measure 
		predictor.setNonconformityMeasure(nonconfMeasure);

		// Wrap the predictor in Signatures-wrapper
		SignaturesCPRegression signPredictor = factory.createSignaturesCPRegression(predictor, 1, 3);

		// Load data
		signPredictor.fromMolsIterator(new SDFile(Config.REGRESSION_DATASET).getIterator(), 
				Config.REGRESSION_ENDPOINT);
		
		// Start the Grid Search
		GridSearchResult res = gs.regression(signPredictor, OptimizationType.EFFICIENCY);
		System.out.println(res);
	}
	
}