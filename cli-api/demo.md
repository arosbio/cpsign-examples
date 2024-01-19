# CPSIGN CLI DEMO

Here is a short demo of how to run CPSign using the command line interface (CLI) in an efficient manner. The demo is written for Mac and linux-based platforms and test-run in bash, minor adjustments might be required to run on a different platform. To run the examples, it is assumed that the commands are runned from the `cli-api` directory, as all paths are relative to this directory. It is also required that you have a recent version of cpsign fatjar which can be downloaded from [CPSign releases page](https://github.com/arosbio/cpsign/releases).


## Setting up an environment
When downloading cpsign from GitHub the execution file privileges are stripped, so here we recommend/assume that you create an alias for running cpsign:
```
alias cpsign='java -jar <path-to-cpsign>'
```

This will make the following commands identical:
```
> java -jar <cpsign-jar> train @args
> cpsign train @args
```

Run some basic commands:
```
> cpsign --version
> cpsign
```

## Precompute
From running `cpsign` without any extra parameters, we saw the top-most usage text which lists the available "programs" that can be run. The first step is typically to start with `precompute` - in order to compute descriptors. First check the available parameters for `precompute`:
```
> cpsign precompute
```

That shows a lot of parameters which might be daunting at first. Note that only the parameters following a star (`*`) are required parameters and that there are default parameters for the remaining ones. Some arguments might be more complex due to syntax or require further information, these you can typically get more information about using the `explain` program, e.g. to see more information about the available descriptors and sub-arguments run:
```
> cpsign explain descriptors
```

There is a file `precompute.args` that is already filled in with the parameters necessary for a rudimentary default precomputed model. Run this using:
```
> cpsign @precompute.args
```

## Evaluate the default parameters
Check what parameters that we can use
```
> cpsign cv
```

Notice that we have the `--data-set` parameter, which takes a precomputed dataset - no need to add labels, property, transformers or descriptors again. Check out the file `cv.args` and run it using:

```
> cpsign @cv.args
```

## Training a final model
The `train` program has similar parameters as the `crossvalidate`, but here we need to define where the model should be saved and give it a name. Again we can print the usage help text to see the available parameters:

```
> cpsign train 
```

Run `train` using the `train.args` file.
```
> cpsign @train.args
```

## Predict using the model
The `predict` program requires a trained model to be given and only have parameters for what should be predicted and the type of output that should be generated. There are quite a few tweakable parameters, again to display the usage manual run:

```
> cpsign predict
```

Note that there are many parameters prefixed with `-si:`/`--si:` or `-gi:`/`--gi:` which are for image generation of either significant signature or atom gradients. Look in the [predict.args](predict.args) file to see how some of these are used, to predict a single SMILES you can run the file using:

```
> cpsign @predict.args
```

## Concluding remarks
This concludes this demo, for more information we refer to the [CPSign readthedocs](https://cpsign.readthedocs.io/en/latest/) page and the CLI usage help texts that should explain the most important parameters - often more information is available using the `explain` program - run `cpsign explain` in order to list the available extra help texts.


