package examples.utils;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;

public class Config {

	// Licenses - these must match the ones 
	public static final String STANDARD_LICENSE_PATH = "/resources/licenses/cpsign-1.0-standard.license";
	public static final String PRO_LICENSE_PATH = "/resources/licenses/cpsign-1.0-pro.license";
	public static final String PREDICT_LICENSE_PATH = "/resources/licenses/cpsign-1.0-predict.license";

	public static URI STANDARD_LICENSE, PRO_LICENSE, PREDICT_LICENSE;

	// Model parameters
	public static final boolean RUN_FOLDED_SAMPLING = false;
	public static final int NUM_OF_AGGREGATED_MODELS = 10;
	public static final double CALIBRATION_RATIO = 0.2;
	public static final int NUM_FOLDS_CV = 10;
	public static final double CV_CONFIDENCE = 0.75;
	public static final double CV_TOLERANCE = 0.1;
	public static final String TEST_SMILES = "CCCNCC(=O)NC1=CC(=CC=C1)S(=O)(=O)NC2=NCCCCC2";


	// Datafiles
	private static final String NUMERICAL_REGRESSION_DATASET_PATH = "/resources/numerical_data/housing_scale_small.svm";
	private static final String NUMERICAL_CLASSIFICATION_DATASET_PATH = "/resources/numerical_data/spambaseShuffled_small.svm";
	private static final String REGRESSION_DATASET_PATH = "/resources/chemistry_data/glucocorticoid_regression.sdf";
	private static final String CLASSIFICATION_DATASET_PATH = "/resources/chemistry_data/bursi_classification.sdf";
	private static final String CLASSIFICATION_CSV_DATASET_PATH = "/resources/chemistry_data/tabular_data_classification.csv";
	private static final String CLASSIFICATION_CSV_SEMICOLON_DATASET_PATH = "/resources/chemistry_data/tabular_data_csv.csv";
	private static final String TOY_JSON_FORMAT_PATH = "/resources/chemistry_data/toy_json_format_ex.json";

	public static final URI NUMERICAL_REGRESSION_DATASET, NUMERICAL_CLASSIFICATION_DATASET, REGRESSION_DATASET, CLASSIFICATION_DATASET, CLASSIFICATION_CSV, CLASSIFICATION_CSV_SEMICOLON, JSON_EXAMPLE_FORMAT;
	public static final String CLASSIFICATION_ENDPOINT ="Ames test categorisation", REGRESSION_ENDPOINT = "target";
	public static final List<String> CLASSIFICATION_LABELS = Arrays.asList("mutagen", "nonmutagen");

	
	// This is taken to be a relative path to this repo, please make changes according to your preferences
	public static final String IMAGE_BASE_PATH = "output/imgs/";
	
	static {
		NUMERICAL_CLASSIFICATION_DATASET = getURI(NUMERICAL_CLASSIFICATION_DATASET_PATH);
		NUMERICAL_REGRESSION_DATASET = getURI(NUMERICAL_REGRESSION_DATASET_PATH);
		REGRESSION_DATASET = getURI(REGRESSION_DATASET_PATH);
		CLASSIFICATION_DATASET = getURI(CLASSIFICATION_DATASET_PATH);
		CLASSIFICATION_CSV = getURI(CLASSIFICATION_CSV_DATASET_PATH);
		JSON_EXAMPLE_FORMAT = getURI(TOY_JSON_FORMAT_PATH);
		CLASSIFICATION_CSV_SEMICOLON = getURI(CLASSIFICATION_CSV_SEMICOLON_DATASET_PATH);

		STANDARD_LICENSE = getURI(STANDARD_LICENSE_PATH);

		try {
			PRO_LICENSE = getURI(PRO_LICENSE_PATH);
		} catch (Error e) {
			System.err.println("No PRO license supplied");
		}

		try {
			PREDICT_LICENSE = getURI(PREDICT_LICENSE_PATH);
		} catch (Error e) {
			System.err.println("No Predict license supplied");
		}

		// Generate parent folders for output in case not present before
		try{
			File imgDir = new File(new File("").getAbsoluteFile(),IMAGE_BASE_PATH);
			FileUtils.forceMkdir(imgDir);
		} catch (IOException e){
			throw new RuntimeException("Could not create output directory for images, please give permissions to do this or remove image generation");
		}
	}


	public static URI getURI(String relativePath) {
		try {
			return Config.class.getClass().getResource(relativePath).toURI();
		} catch (Exception e) {
			throw new Error("Invalid configuration - please update config in examples.utils.Config.java");
		}
	}
}
