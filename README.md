# CPSign API USAGE GUIDE #

This repo is meant to serve as a basics for how to get up and running with **CPSign** using the API. 

## Current supported versions ##
- 1.0.0 : Tag v1.0.0, Master branch

## How to set it up ##

### 1. Download CPSign Jar-file
Currently the only way to get CPSign is to contact Aros Bio, a download page will come shortly. The CPSign Jar also comes with a separate jar-file with JavaDoc for getting in-editor help, setting this up in your favorit IDE should be easy (see [Setting up JavaDoc with Eclipse](http://cpsign-docs.genettasoft.com/sections/javadoc_setup.html)).

### 2. Get your license
Contact Aros Bio to get your license, see more information at the [CPSign Documentation](http://cpsign-docs.genettasoft.com/sections/license.html). 

### 3. Clone/fork this repo
If your are using git; clone or fork this repository. In Eclipse, right click and chose "Import..." -> "Git/Projects from Git" -> "Existing local repository" (or "Clone URI" and paste the URI of this repository). 

Otherwise download the project as zip-file from the [downloads page](https://bitbucket.org/genettasoft/cpsign-examples/downloads). Once you have the source, import it in your editor. In Eclipse, right click and chose "Import..." -> "Existing Projects into Workspace" and in the next window chose "Select archive file" and browse to the zip-file you downloaded. Chose the project (should only be one) and press "Finish".

### 4. Add the CPSign-jar and edit 
Once set up in your IDE, there will be complaints about a missing required libraries, which is the CPSign-jar that you should have downloaded in step 1 above. The project `.classpath` refers to lib/cpsign-1.0.0.jar, either resolve it by creating a lib-folder and adding your version of the jar-file to it (and possibly change the classpath if your version differs compared to the one used here) or simply change the classpath to point to your existing jarfile. 
The classpath also refers to JUnit 4.12 and hamcrest 1.3-core that is needed to run the "test class" found under test/RunAll.java. Either remove these references from the class path or add the missing jars for the project to compile correctly. 

Before you can run the project, you will have to point the Config file (src/examples/utils/Config.java) to your license(s), at least a standard license will be needed, (you might need to make changes in case you have a different license type). 

#### 5. Run and enjoy
Now you should be fully set up and everything should be running without any problems. 


## Who do I talk to? ##
Do you have any further issues, refer to the [CPSign Documentation](http://cpsign-docs.genettasoft.com/index.html) or contact Aros Bio info@arosbio.com
