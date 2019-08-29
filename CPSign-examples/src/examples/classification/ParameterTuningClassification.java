package examples.classification;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Arrays;

import javax.naming.CannotProceedException;

import com.arosbio.auth.InvalidLicenseException;
import com.arosbio.chem.io.in.SDFile;
import com.arosbio.modeling.CPSignFactory;
import com.arosbio.modeling.cheminf.NamedLabels;
import com.arosbio.modeling.cheminf.SignaturesCPClassification;
import com.arosbio.modeling.gridsearch.GridSearch;
import com.arosbio.modeling.gridsearch.GridSearch.OptimizationType;
import com.arosbio.modeling.gridsearch.GridSearch.ParameterRange;
import com.arosbio.modeling.gridsearch.GridSearchException;
import com.arosbio.modeling.gridsearch.GridSearchResult;
import com.arosbio.modeling.ml.cp.acp.ACPClassification;
import com.arosbio.modeling.ml.ds_splitting.RandomSampling;

import examples.utils.Config;
import examples.utils.Utils;

public class ParameterTuningClassification {

	CPSignFactory factory;


	public static void main(String[] args) throws CannotProceedException, IllegalArgumentException, IllegalAccessException, IOException, GridSearchException {
		ParameterTuningClassification acp = new ParameterTuningClassification();
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


	public void tuneParameters() throws IllegalArgumentException, IllegalAccessException, IOException, InvalidLicenseException, GridSearchException {
		// Create a GridSearch object 
		GridSearch gs = new GridSearch(Config.NUM_FOLDS_CV, Config.CV_CONFIDENCE, Config.CV_TOLERANCE);
		// Set your custom parameter regions
		gs.setC(new ParameterRange(Arrays.asList(1., 10., 100.))); 

		// Set a Writer to write all output to (otherwise only give you the optimal result)
		Writer writer = new OutputStreamWriter(System.out);
		gs.setWriter(writer);


		// Chose your predictor and scoring algorithm
		ACPClassification predictor = factory.createACPClassification(
				factory.createLibLinearClassification(), 
				new RandomSampling(Config.NUM_OF_AGGREGATED_MODELS, Config.CALIBRATION_RATIO)); 

		// Wrap the predictor in Signatures-wrapper
		SignaturesCPClassification signPredictor = factory.createSignaturesCPClassification(predictor, 1, 3);

		// Load data
		signPredictor.fromMolsIterator(new SDFile(Config.CLASSIFICATION_DATASET).getIterator(),
				Config.CLASSIFICATION_ENDPOINT, 
				new NamedLabels(Config.CLASSIFICATION_LABELS));

		// Start the Grid Search
		GridSearchResult res = gs.classification(signPredictor, OptimizationType.EFFICIENCY);
		gs.classification(signPredictor.getProblem(), predictor, OptimizationType.EFFICIENCY);
		System.out.println(res);
	}



}
