package examples;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;

import examples.authentication.AuthenticateWithLicense;
import examples.classification.NumericACPClassification;
import examples.classification.NumericTCPClassification;
import examples.classification.NumericVAPClassification;
import examples.classification.ParameterTuningClassification;
import examples.classification.SignACPClassification;
import examples.classification.SignTCPClassification;
import examples.classification.SignVAPClassification;
import examples.customization.ExtendCLI;
import examples.customization.ManipulatingNumericalDatasets;
import examples.customization.SettingDescriptors;
import examples.customization.SettingSVMParameters;
import examples.io.GeneratePredictionImages;
import examples.regression.NumericACPRegression;
import examples.regression.ParameterTuningRegression;
import examples.regression.SignACPRegression;

public class RunAll {

	
	List<Class<? extends Object>> classesToRun = Arrays.asList(
			// Auth
			AuthenticateWithLicense.class,
			
			// Classification
			NumericACPClassification.class,
			NumericTCPClassification.class,
			NumericVAPClassification.class,
			ParameterTuningClassification.class,
			SignACPClassification.class,
			SignTCPClassification.class,
			SignVAPClassification.class,
			
			// Custom
//			ExtendCLI.class,
			ManipulatingNumericalDatasets.class,
			SettingDescriptors.class,
			SettingSVMParameters.class,
			
			// IO
			GeneratePredictionImages.class,
			
			
			// Regression
			NumericACPRegression.class,
			ParameterTuningRegression.class,
			SignACPRegression.class
			
			);
	
	@Test
	public void RunAllExamples() throws Exception{
		
		for(Class<?> clazz : classesToRun){
			try{
				runClass(clazz);
				System.out.println("\n--------------------------------------------------------------------------------------------\n");
			} catch(Exception e){
				System.err.println("Class " + clazz + " failed with exception stacktrace:");
				e.printStackTrace();
			} catch(Error e){
				System.err.println("Class " + clazz + " failed with error stacktrace:");
				e.printStackTrace();
			}
		}
	}
	
	public static void runClass(Class<?> clazz) throws Exception {
		Method mainMethod = clazz.getMethod("main", String[].class);
		mainMethod.invoke(clazz.newInstance(), (Object)null);
	}
	
	@Rule
	public final ExpectedSystemExit exit = ExpectedSystemExit.none();
	
	@Test
	public void runExtendCLI() {
		exit.expectSystemExitWithStatus(0); // should call exit with a successful exit status
		
		ExtendCLI.main(new String[] {});
	}
}
