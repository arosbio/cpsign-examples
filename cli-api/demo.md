# CPSIGN CLI DEMO

Here is a short demo of how to run CPSign using the command line interface (CLI) in an efficient manner. The demo is written for MacOS and linux-based systems and test-run in bash, minor adjustments might be required to run on a different system. To run the examples, it is assumed that the commands are runned from the `cli-api` directory, as all paths are relative to this directory. It is also required that you aquire a valid standard or pro license and put it in the `resources` directory, or update the paths in the parameter files accordingly.


## Setting up an environment
A suggestion to minimize typing is to make an alias for running cpsign (put it your `.bash_profile` or similar file that is run every time you open a new termial tab/window):
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
From running the `cpsign` only command, we saw the available programs that can be run, let's start with the first one: `precompute`. First check the parameters:
```
> cpsign precompute
```

That produces a lot of output, fairly hard to grasp. Try this instead:
```
> cpsign precompute --help --short
```
Try to compose the script, go through section by section, e.g.:
```
> cpsign precompute --help input
```
Let's say we wish to pick specific descriptors, use the `explain` program to list the available descriptors and their arguments:
```
> cpsign explain descriptors
```

There is a file `precompute.args` that is already filled in with the parameters necessary for a rudementary default precomputed model. Run this using:
```
> cpsign @precompute.args
```

## Evalute the default parameters
Check what parameters that we can use
```
> cpsign cv --help --short
```

Notice that we have the `--model-in` parameter, which takes a precomputed "model" - no need to add labels, endpoint, transformers or descriptors again. Check out the file `cv.args` and run it using:

```
> cpsign @cv.args
```

## Training a final model
The `train` program has similar parameters as the `crossvalidate`, but here we need to define where the model should be saved and give it a name. List the output section of parameters:
```
> cpsign train --help output
```
Run `train` using the `train.args` file.
```
> cpsign @train.args
```

## Predict using the model
The `predict` program requires a trained model to be given and only have parameters for what should be predicted and the type of output that should be given. 
```
> cpsign predict --help --short
```
There is also the 'gradient image' and 'significant signature' output 
