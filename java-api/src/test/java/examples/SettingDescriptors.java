package examples;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.junit.Test;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;

import com.arosbio.chem.io.in.SDFile;
import com.arosbio.cheminf.ChemCPRegressor;
import com.arosbio.cheminf.descriptors.ChemDescriptor;
import com.arosbio.cheminf.descriptors.DescriptorFactory;
import com.arosbio.cheminf.io.ModelSerializer;
import com.arosbio.ml.algorithms.svm.LinearSVR;
import com.arosbio.ml.cp.CPRegressionPrediction;
import com.arosbio.ml.cp.acp.ACPRegressor;
import com.arosbio.ml.cp.nonconf.regression.LogNormalizedNCM;
import com.arosbio.ml.sampling.RandomSampling;

import utils.Config;

public class SettingDescriptors {

	/*
	 * Note: this tests (for this particular data set) takes roughly 8-10 minutes to run
	 * as it requires CDK descriptors that are time consuming to compute. In real world scenarios,
	 * put some effort into picking the appropriate descriptors 
	 */
	@Test
	public void descriptors() throws Exception {

		// Chose your predictor, NCM and scoring algorithm
		ACPRegressor predictor = new ACPRegressor(new LogNormalizedNCM(new LinearSVR()),
				new RandomSampling(Config.getInt("modeling.sampling.num.models",10), Config.getDouble("modeling.sampling.calib.ratio",0.2)));

		// Wrap the predictor in Signatures-wrapper
		ChemCPRegressor chemPredictor = new ChemCPRegressor(predictor);

		// This will by default use the signatures generator with start height 1 and end height 3,
		// This can now be changed!

		// List all available descriptors 
		System.out.println("List of all available chemical descriptors:");
		for (ChemDescriptor d : DescriptorFactory.getInstance().getDescriptorsList())
			System.out.println(d);

		// Use a set of CDK descriptors instead
		List<ChemDescriptor> desc = DescriptorFactory.getCDKDescriptorsNo3D().subList(0, 10);
		chemPredictor.getDataset().setDescriptors(desc);
		
		/* 
		 * Note that when using other descriptors apart from the Signatures Descriptor
		 * it is recommended to both check for missing features (e.g. if cdk failed to compute some descriptors)
		 * and to scale features - and possibly add feature-selection 
		 */

		// Load data, train and save model
		chemPredictor.addRecords(new SDFile(Config.getURI("regression.dataset", null)).getIterator(), 
				Config.getProperty("regression.endpoint"));

		// Train the aggregated ICPs
		chemPredictor.train();

		// Save models to skip train again
		File tmpModel = File.createTempFile("regression-model", ".jar");
		tmpModel.deleteOnExit();
		ModelSerializer.saveModel(chemPredictor, tmpModel, null);

		// Load it back
		ChemCPRegressor loaded = (ChemCPRegressor) ModelSerializer.loadChemPredictor(tmpModel.toURI(), null);

		// Predict a new example
		IAtomContainer testMol = new SmilesParser(SilentChemObjectBuilder.getInstance()).parseSmiles(Config.DEFAULT_SMILES);
		List<Double> confidences = Arrays.asList(0.5, 0.7, 0.9);
		CPRegressionPrediction regResult = loaded.predict(testMol, confidences);

		for (double conf : confidences){
			System.out.printf(Locale.ENGLISH,"Confidence: %.3f, prediction interval: %s, capped prediction interval: %s%n",
				conf, regResult.getIntervals().get(conf).getInterval(), regResult.getInterval(conf).getCappedInterval());
		}

	}

}
