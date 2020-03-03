package examples.classification;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.CannotProceedException;

import com.arosbio.auth.InvalidLicenseException;
import com.arosbio.chem.io.in.SDFile;
import com.arosbio.modeling.CPSignFactory;
import com.arosbio.modeling.cheminf.NamedLabels;
import com.arosbio.modeling.cheminf.SignaturesCPClassification;
import com.arosbio.modeling.ml.cp.acp.ACPClassification;
import com.arosbio.modeling.ml.ds_splitting.RandomSampling;
import com.arosbio.modeling.ml.gridsearch.GridSearch;
import com.arosbio.modeling.ml.gridsearch.GridSearch.GSResult;
import com.arosbio.modeling.ml.gridsearch.GridSearchException;
import com.arosbio.modeling.ml.gridsearch.GridSearchResult;
import com.arosbio.modeling.ml.interfaces.Tunable.TunableParameter;
import com.arosbio.modeling.ml.metrics.ObservedFuzziness;
import com.arosbio.modeling.ml.testing.KFoldCVSplitter;

import examples.utils.Config;
import examples.utils.SysOutWriter;
import examples.utils.Utils;

public class ParameterTuningClassification {

	CPSignFactory factory;


	public static void main(String[] args) throws CannotProceedException, IllegalArgumentException, IllegalAccessException, IOException, GridSearchException {
		ParameterTuningClassification acp = new ParameterTuningClassification();
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
		ACPClassification predictor = factory.createACPClassification(
				factory.createNegativeDistanceToHyperplaneNCM(factory.createLibLinearClassification()), 
				new RandomSampling(Config.NUM_OF_AGGREGATED_MODELS, Config.CALIBRATION_RATIO));
		List<TunableParameter> params = predictor.getTunableParameters();
		System.out.println("Possible parameters to gridsearch for ACP Classification");
		for (TunableParameter p : params) {
			System.out.println(p);
		}
	}


	public void tuneParameters() throws IllegalArgumentException, IllegalAccessException, IOException, InvalidLicenseException, GridSearchException {
		// Create a GridSearch object 
		GridSearch gs = new GridSearch(new KFoldCVSplitter(Config.NUM_FOLDS_CV), Config.CV_CONFIDENCE, Config.CV_TOLERANCE);
		// Set your custom parameter regions
		Map<String,List<Object>> paramGrid = new HashMap<>();
		paramGrid.put("COST", Arrays.asList(1, 10, 100)); 

		// Set a Writer to write all output to (otherwise only give you the optimal result)
		// Here simply write to system out
		gs.setLoggingWriter(new SysOutWriter());


		// Chose your predictor, NCM and scoring algorithm
		ACPClassification predictor = factory.createACPClassification(
				factory.createNegativeDistanceToHyperplaneNCM(factory.createLibLinearClassification()), 
				new RandomSampling(Config.NUM_OF_AGGREGATED_MODELS, Config.CALIBRATION_RATIO)); 

		// Wrap the predictor in Signatures-wrapper
		SignaturesCPClassification signPredictor = factory.createSignaturesCPClassification(predictor, 1, 3);

		// Load data
		signPredictor.fromMolsIterator(new SDFile(Config.CLASSIFICATION_DATASET).getIterator(),
				Config.CLASSIFICATION_ENDPOINT, 
				new NamedLabels(Config.CLASSIFICATION_LABELS));

		// Start the Grid Search, use the proportion of multi-label prediction 
		// sets as optimization metric (at a given confidence)
		GridSearchResult res = gs.search(signPredictor,
				new ObservedFuzziness(),
//				new ProportionMultiLabelPredictions(Config.CV_CONFIDENCE),
				paramGrid); 
		
		System.out.println(res);
		
		// The best "n" (definable) results are available to check afterwards - sorted with the best one first
		for (GSResult r : res.getBestParameters()) {
			System.out.println(r + " params: " + r.getParams());
		}
	}



}
