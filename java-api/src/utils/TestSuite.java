package utils;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import examples.ApplyDataTransformations;
import examples.AuthenticateLicenseToCPSign;
import examples.GeneratePredictionImages;
import examples.ManipulatingNumericalDatasets;
import examples.ParameterTuning;
import examples.SettingDescriptors;

@RunWith(Suite.class)
@SuiteClasses({
	ApplyDataTransformations.class,
	AuthenticateLicenseToCPSign.class,
	GeneratePredictionImages.class,
	ManipulatingNumericalDatasets.class,
	ParameterTuning.class,
	SettingDescriptors.class
})
public class TestSuite {

}
