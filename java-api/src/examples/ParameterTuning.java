package examples;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.arosbio.auth.InvalidLicenseException;
import com.arosbio.chem.io.in.SDFile;
import com.arosbio.modeling.cheminf.SignaturesCPRegression;
import com.arosbio.modeling.ml.cp.acp.ACPRegression;
import com.arosbio.modeling.ml.ds_splitting.RandomSampling;
import com.arosbio.modeling.ml.gridsearch.GridSearch;
import com.arosbio.modeling.ml.gridsearch.GridSearch.GSResult;
import com.arosbio.modeling.ml.gridsearch.GridSearchException;
import com.arosbio.modeling.ml.gridsearch.GridSearchResult;
import com.arosbio.modeling.ml.interfaces.Configurable.ConfigParameter;
import com.arosbio.modeling.ml.metrics.cp.regression.MedianPredictionIntervalWidth;
import com.arosbio.modeling.ml.testing.KFoldCV;
import com.arosbio.modeling.ml.testing.LOOCV;
import com.arosbio.modeling.ml.testing.RandomSplit;

import utils.BaseTest;
import utils.Config;
import utils.SysOutWriter;

public class ParameterTuning extends BaseTest {


	/*
	 * Note: this test is rather slow as it computes an exhaustive grid search of parameters. 
	 */
	@Test
	public void regression() throws IllegalArgumentException, InvalidLicenseException, IOException, GridSearchException {
		
		
		// Initialize your predictor, non-conformity measure (NCM) and scoring algorithm(s)
		ACPRegression predictor = factory.createACPRegression(
				factory.createLogNormalizedNCM(factory.createLinearSVR(),0.01), 
				new RandomSampling(Config.getInt("modeling.sampling.num.models", 10), Config.getDouble("modeling.sampling.calib.ratio",0.2)));
		
		// You can list all available parameters that can be tuned
		List<ConfigParameter> params = predictor.getConfigParameters();
		System.out.println("Possible parameters to gridsearch for ACP Regression");
		for (ConfigParameter p : params) {
			System.out.println(p);
		}
		
		
		// Create a GridSearch object 
		GridSearch gs = new GridSearch(
				new KFoldCV(Config.getInt("modeling.k",5)), 
				Config.getDouble("modeling.conf",0.8), 
				Config.getDouble("modeling.tol", 0.1));
		// Note that there are several other testing strategies that can be used:
		new LOOCV(); // Leave-one-out cross-validation
		new RandomSplit(0.3); // Split randomly with 30% in the test set 

		// Set your custom parameter regions
		Map<String,List<Object>> paramGrid = new HashMap<>();
		paramGrid.put("cost", Arrays.asList(1, 10, 100));
		// Grid search beta only if NCM uses it! 
		paramGrid.put("ncmBeta", Arrays.asList(0.0, 0.25, 0.5)); // if LogNormalizedNonconfMeasureRegression is not set, the grid search will not be done even if this list is set
		// Set a Writer to write all output to (otherwise only give you the 'n' optimal results)
		// Here simply write to system out but would likely be to a file
		gs.setLoggingWriter(new SysOutWriter());

		// Wrap the predictor in Signatures-wrapper or create a 
		SignaturesCPRegression signPredictor = factory.createSignaturesCPRegression(predictor, 1, 3);

		// Load data
		signPredictor.fromMolsIterator(new SDFile(Config.getURI("regression.dataset",null)).getIterator(), 
				Config.getProperty("regression.endpoint"));

		// Start the Grid Search
		GridSearchResult res = gs.search(signPredictor, 
				new MedianPredictionIntervalWidth(Config.getDouble("modeling.conf", 0.8)),
				paramGrid);
		System.out.println(res);

		// The best "n" (definable) results are available to check afterwards - sorted with the best one first
		for (GSResult r : res.getBestParameters()) {
			System.out.println(r + " params: " + r.getParams());
		}
	}

}
