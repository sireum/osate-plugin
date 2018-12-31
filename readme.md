# Sireum OSATE Plugin

This repository contains the Sireum [OSATE](http://osate.org) Plugin that translates AADL instance
models to [AIR](https://github.com/sireum/air) and then to downstream Sireum tools such as [AWAS](https://github.com/sireum/v3-awas).

## Installation for Developers 

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

3. In Eclipse go to __File__ -> __Import...__  

4. Expand the __General__ folder and select __Existing Project into Workspace__, then click 
   __Next__.

5. Enter the path to the directory where you cloned the repository and then select all the 
   available projects from the project listings and click __Finish__.

6. Switch to the plug-in development perspective: __Window__ -> __Perspective__ -> 
   __Open Perspective__ -> __Other...__ -> __Plug-in Development__.

7. __Optional:__ Rebuild tool jars

   * AIR
     
     ```bash
     git clone https://github.com/sireum/air.git
     cd air
     ./prelude.sh
     ./mill-standalone air.jvm.assembly
     cp out/air/jvm/assembly/dest/out.jar <osate-plugin-dir>/org.sireum.aadl.osate.air/air.jar
     ```

   * ACT
   
     ```bash
     git clone --recursive https://github.com/sireum/act-plugin.git
     cd act-plugin
     ./bin/prelude.sh
     ./bin/mill act.jvm.jar
     cp out/act/jvm/jar/dest/out.jar <osate-plugin-dir>/org.sireum.aadl.osate.act/lib/act.jar
     ```
     
   * Arsit
   
     ```bash
     git clone --recursive -b master git@github.com:sireum/v3.git sireum-v3
     git clone git@github.com:sireum/air.git sireum-v3/aadl/ir
     git clone --recursive git@github.com:santoslab/arsit.git sireum-v3/aadl/arsit
     git clone git@github.com:sireum/v3-awas.git sireum-v3/awas
     export SIREUM_HOME=`pwd`/sireum-v3
     cd <osate-plugin-dir>/org.sireum.aadl.osate.arsit/lib
     ./build.sh
     ```
     
   * AWAS
   
     __TODO__
