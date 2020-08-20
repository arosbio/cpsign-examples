package examples.customization;

import java.util.ArrayList;
import java.util.List;

import com.arosbio.modeling.app.CLI;
import com.arosbio.modeling.app.CLIUsageTool;
import com.arosbio.modeling.app.cli.ArgumentType;
import com.arosbio.modeling.app.cli.ParameterDefaultValue;
import com.arosbio.modeling.app.impl.CLIParams.CLIParamsSection;
import com.arosbio.modeling.app.impl.CLITrain.TrainParams;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

public class ExtendCLI {

	public static void main(String[] args) {
		ExtendedParameters customParams = new ExtendedParameters();
		JCommander jc = JCommander.newBuilder().
				addObject(new TrainParams()). // here's cpsigns own params (for the "train" program)
				addObject(customParams). // add your custom params
				build();

		// JCommanders default - simple but ugly printing of the parameters
		jc.usage();

		// CPSign has some custom printing for pretty output
		// Print first cpsigns parameters
		List<CLIParamsSection> sections = new TrainParams().getSections();
		StringBuilder sb = new StringBuilder();
		for (CLIParamsSection section: sections) {
			sb.append(CLIUsageTool.getSectionUsage(section, null, false, false));
			sb.delete(sb.length()-1, sb.length()); // remove a newline in the end for denser output
		}

		// Print the custom section of parameters
		sb.append(CLIUsageTool.getSectionUsage(customParams, null, false, false));
		System.out.println(sb.toString());

		// For parsing only the custom parameters
		JCommander jcCustom = JCommander.newBuilder().
				addObject(customParams).
				acceptUnknownOptions(true). // Important! or you have to add all CPSigns parameters as well, which would be slower!
				build();

		// Full set of arguments passed to the main
		String[] arguments = new String[]{"train", "-h", "--short", "--extra"};
		jcCustom.parse(arguments);

		if (customParams.addedExtra)
			System.out.println("Custom parameter was sent");
		else
			throw new RuntimeException("Custom parameter was not sent");

		// CPSign will fail in case you send non-recognized arguments 
		// so you need to strip away all extra things you send

		List<String> filteredArgs = new ArrayList<>();
		for (String arg: arguments)
			if (!arg.equals("--extra"))
				filteredArgs.add(arg);

		// Finally you can call CPSign CLI with your arguments
		// Note that the call should *not* be done to the CLITrain 
		// class itself, as logging and some generic functionality
		// is performed in the CLI-class
		System.out.println("Now calling CPSign:");
		CLI.main(filteredArgs.toArray(new String[] {}));

	}

	public static class ExtendedParameters implements CLIParamsSection {
		@Parameter(names = {"--extra"}, description = "Your CUSTOM parameter")
		boolean addedExtra = false;

		@ParameterDefaultValue(defaultValue="Option 1")
		@ArgumentType(arg="[id] or [text]")
		@Parameter(names = {"--numbers"}, description = "In case you need multiple options, "
				+ "setting the newlines yourself can be nice:\n\t(1) Option 1 (default)\n\t(2) Option 2")
		Integer num = 1;

		@Override
		public String getSectionName() {
			return "Custom";
		}

	}

}
