package examples;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;

import com.arosbio.chem.io.in.SDFile;
import com.arosbio.modeling.cheminf.SignaturesCPRegression;
import com.arosbio.modeling.cheminf.descriptors.Descriptor;
import com.arosbio.modeling.cheminf.descriptors.DescriptorFactory;
import com.arosbio.modeling.io.ModelLoader;
import com.arosbio.modeling.ml.cp.CPRegressionPrediction;
import com.arosbio.modeling.ml.cp.acp.ACPRegression;
import com.arosbio.modeling.ml.ds_splitting.RandomSampling;

import utils.BaseTest;
import utils.Config;

public class SettingDescriptors extends BaseTest {

	@Test
	public void descriptors() throws Exception {

		// Chose your predictor, NCM and scoring algorithm
		ACPRegression predictor = factory.createACPRegression(
				factory.createLogNormalizedNCM(factory.createLinearSVR(), null, 0.1), 
				new RandomSampling(Config.getInt("modeling.sampling.num.models",10), Config.getDouble("modeling.sampling.calib.ratio",0.2)));

		// Wrap the predictor in Signatures-wrapper
		SignaturesCPRegression signPredictor = factory.createSignaturesCPRegression(predictor, 1, 3);

		// This will by default use the signatures generator with start height 1 and end height 3,
		// This can now be changed!

		// List all available descriptors 
		for (Descriptor d : DescriptorFactory.getInstance().getDescriptorsList())
			System.out.println(d);

		// Use a set of CDK descriptors instead
		List<Descriptor> desc = DescriptorFactory.getInstance().getCDKDescriptorsNo3D().subList(0, 10);

		/* 
		 * Note that when using other descriptors apart from the Signatures Descriptor
		 * it is recommended to both check for missing features (e.g. if cdk failed to compute some descriptors)
		 * and to scale features - and possibly add feature-selection 
		 */


		// set this list - or implement your own descriptor if you like!
		signPredictor.getProblem().setDescriptors(desc);

		// Load data, train and save model
		signPredictor.fromMolsIterator(new SDFile(Config.getURI("regression.dataset", null)).getIterator(), 
				Config.getProperty("regression.endpoint"));

		// Train the aggregated ICPs
		signPredictor.train();

		// Save models to skip train again
		File tmpModel = File.createTempFile("hergModels.liblinear.", ".cpsign");
		tmpModel.deleteOnExit();
		signPredictor.save(tmpModel);

		// Load it back
		SignaturesCPRegression loaded = (SignaturesCPRegression) ModelLoader.loadModel(tmpModel.toURI(), null);

		// Predict a new example
		IAtomContainer testMol = new SmilesParser(SilentChemObjectBuilder.getInstance()).parseSmiles(Config.DEFAULT_SMILES);
		List<Double> confidences = Arrays.asList(0.5, 0.7, 0.9);
		CPRegressionPrediction regResult = loaded.predict(testMol, confidences);

		for (double conf : confidences){
			System.out.println("Confidence: " + conf + 
					", interval (normal): " + regResult.getIntervals().get(conf).getInterval());
		}

	}

}
