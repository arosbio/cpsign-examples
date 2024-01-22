# CPSign API USAGE GUIDE 

This repo is meant to give some basic examples on how to get up and running with [CPSign](https://github.com/arosbio/cpsign) using the Java API. For more information we also refer to [CPSign readthedocs](https://cpsign.readthedocs.io/en/latest/). It now also includes a basic tutorial on using the CLI found at [CLI demo](cli-api/demo.md).

## Currently supported versions
- 2.0.0-rc7 
- 1.4.1 : Tag v1.4.1, HEAD of Master branch
- 1.2.0 : Tag v1.2.0
- 1.0.0 : Tag v1.0.0

## Requirements
To run these examples on your own computer you need the following:
- Java of version 11 or newer
- Maven for package management

## Examples
All examples can be found in the [examples](java-api/src/test/java/examples/) directory:

- [ApplyDataTransformations](java-api/src/test/java/examples/ApplyDataTransformations.java) : Uses CDK physicochemical descriptors and shows how to apply data transformations.
- [GeneratePredictionImages](java-api/src/test/java/examples/GeneratePredictionImages.java) : Shows how to generate prediction images and how to customize the produced images with custom elements such as titles, boards and legends.
- [ListAvailableServices](java-api/src/test/java/examples/ListAvailableServices.java) : Shows how to list configurable elements that are loaded using the java ServiceLoader class. These also represent classes that users themselves can extend with their own implementations.
- [ManipulatingNumericalDatasets](java-api/src/test/java/examples/ManipulatingNumericalDatasets.java) : Shows basic data-manipulation such as shuffling and splitting data. 
- [ParameterTuning](java-api/src/test/java/examples/ParameterTuning.java) : Show how to preform a grid search over hyper-parameters in order to tune these. 
- [ParsingChemicalFiles](java-api/src/test/java/examples/ParsingChemicalFiles.java) : Shows how to read data from SDF and CSV files.
- [SettingDescriptors](java-api/src/test/java/examples/SettingDescriptors.java) : Shows how to use non-default chemical descriptors, by picking from the `IMolecularDescriptor` implementations from the CDK library.
- [StandardWorkflows](java-api/src/test/java/examples/StandardWorkflows.java) : Shows how to instantiate a predictor model, load data to use for training, train it, predict a test example and how to save the predictor model.
- [StandardWorkflowsNonChem](java-api/src/test/java/examples/StandardWorkflowsNonChem.java) : Does the same workflow as above, but for numerical (non-chemistry) input data. 

Note that this project uses the [pom.xml](java-api/pom.xml) to configure version of CPSign and other test dependencies. 

## Who do I talk to?
Do you have any further issues, refer to the [CPSign Documentation](https://cpsign.readthedocs.io/en/latest/), file an issue on [CPSign GitHub](https://github.com/arosbio/cpsign) or contact Aros Bio info@arosbio.com

