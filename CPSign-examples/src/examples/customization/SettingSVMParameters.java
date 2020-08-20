package examples.customization;

import com.arosbio.modeling.CPSignFactory;
import com.arosbio.modeling.ml.algorithms.LibLinear;
import com.arosbio.modeling.ml.algorithms.LibSvm;
import com.arosbio.modeling.ml.algorithms.params.LibLinearParameters;
import com.arosbio.modeling.ml.algorithms.params.LibSvmParameters;

import de.bwaldvogel.liblinear.Parameter;
import de.bwaldvogel.liblinear.SolverType;
import examples.utils.Utils;
import libsvm.svm_parameter;

/**
 * CPSign uses default parameters that previously been found to produce good results
 * using these signatures: 
 * Alvarsson J, Eklund M, Andersson C, Carlsson L, Spjuth O, Wikberg JES. 
 * Benchmarking study of parameter variation when using signature fingerprints together 
 * with support vector machines. J Chem Inf Model. 2014;54: 3211–3217
 * 
 * 
 * However, advanced users might want to set their own parameters for the support vector machines 
 * and can now do so since the 0.3.14 version of cpsign. For instance if numerical data are
 * used from different source than signatures, the SVM parameters might need some tweaking
 *  
 * @author staffan
 *
 */
public class SettingSVMParameters {

	CPSignFactory factory;

	public static void main(String[] args) throws IllegalAccessException {
		SettingSVMParameters setup = new SettingSVMParameters();
		setup.intialise();
		setup.setLibLinearParameters();
		setup.setLibSVMParameters();
	}

	public void intialise() {
		// Start with instantiating CPSignFactory with your license
		factory = Utils.getFactory();
	}

	public void setLibLinearParameters() throws IllegalAccessException{
		LibLinear liblin = factory.createLibLinearClassification();
		LibLinearParameters params = liblin.getParameters();

		// you can set individual parameter values one at a time
		params.setC(10); 
		params.setEpsilon(0.1);
		if (params.getC() != 10 || params.getEpsilon() != 0.1)
			throw new RuntimeException();

		// Or change all parameters at the same time
		Parameter liblinParams = new Parameter(SolverType.L1R_LR, 100, 0.5);
		liblin.setParameters(new LibLinearParameters(liblinParams));
		if (liblin.getParameters().getC() != 100
				|| liblin.getParameters().getEpsilon() != 0.5)
			throw new RuntimeException();

	}

	public void setLibSVMParameters() throws IllegalAccessException {
		LibSvm impl = factory.createLibSvmClassification();
		LibSvmParameters params = impl.getParameters();

		// you can set individual parameter values one at a time
		params.setC(10); 
		params.setEpsilon(0.1);
		params.setGamma(0.001);
		if (params.getC() != 10 || 
				params.getEpsilon() != 0.1 || 
				params.getGamma() != 0.001)
			throw new RuntimeException();

		// Or change all parameters at the same time
		svm_parameter svmParams = new svm_parameter();
		svmParams.C = 100;
		svmParams.eps = 0.5;
		impl.setParameters(new LibSvmParameters(svmParams));
		if (impl.getParameters().getC() != 100 || 
				impl.getParameters().getEpsilon() != 0.5)
			throw new RuntimeException();

	}

}
