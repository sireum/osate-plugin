# Osate-Plugin

This repository contains the Sireum Osate-Plugin that translates AADL instance
models to [AIR](https://github.com/sireum/air).

## Installation for Sireum Developers 

The plugin can be developed using an OSATE release (following the installation
directions [here](http://osate.org/download-and-install.html)) or an OSATE
development environment (following the installation directions
[here](http://osate.org/setup-development.html)).  Eclipse is the underlying IDE
being used in either case so the rest of these instructions will refer to the
IDE as 'Eclipse'.

1. Launch Eclipse and then follow the directions to install the [Scala IDE for
   Eclipse](http://scala-ide.org).  After installing the Scala IDE you will need to
   restart Eclipse.

2. Clone this repository ``git clone git@github.com:sireum/osate-plugin.git osate-plugin``

3. Use the following to assemble the Sireum jar

    ```bash
    git clone --recursive -b master git@github.com:sireum/v3.git sireum-v3
    git clone git@github.com:sireum/air.git sireum-v3/aadl/ir
    git clone git@github.com:sireum/v3-awas.git sireum-v3/awas
    git clone git@github.com:santoslab/arsit.git sireum-v3/aadl/arsit
    ./sireum-v3/bin/sbt-launch assembly
    ```
    The jar will be located at ``sireum-v3/bin/sireum.jar``.  Copy it into the plugins 
    ``osate-plugin/org.sireum/lib`` directory

4. In Eclipse go to __File__ -> __Import...__  

5. Expand the __General__ folder and select __Existing Project into Workspace__, then click 
   __Next__.

6. Enter the path to the directory where you cloned the repository and then select __org.sireum__ 
   from the project listings and click __Finish__.

7. Switch to the plug-in development perspective: __Window__ -> __Perspective__ -> 
   __Open Perspective__ -> __Other...__ -> __Plug-in Development__.

    
<!---
## Installation for Users

1. Download and install the latest [OSATE release](http://osate.org/download-and-install.html)

2. TBD
--->
