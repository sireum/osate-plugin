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

1. Clone this repository ``git clone git@github.com:sireum/osate-plugin.git osate-plugin``

1. Launch Eclipse and go to __File__ -> __Import...__  

1. Expand the __General__ folder and select __Existing Project into Workspace__, then click 
   __Next__.

1. Enter the path to the directory where you cloned the repository and then select all the 
   available projects from the project listings and click __Finish__.

1. Switch to the plug-in development perspective: __Window__ -> __Perspective__ -> 
   __Open Perspective__ -> __Other...__ -> __Plug-in Development__.

1. __Optional:__ Rebuild tool jars

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
     git clone --recursive https://github.com/sireum/act.git
     cd act
     ./bin/build.cmd min-jar
     cp ./out/act/jvm/jar/dest/out.jar <osate-plugin-dir>/org.sireum.aadl.osate.act/lib/act.jar
     ```
     
   * Arsit
   
     ```bash
     git clone --recursive git@github.com:santoslab/arsit.git
     cd arsit
     ./bin/prelude.sh
     ./bin/mill arsit.jvm.jar
     cp out/arsit/jvm/jar/dest/out.jar <ostate-plugin-dir>/org.sireum.aadl.osate.arsit/lib/arsit.jar
     ```
     
   * Awas
   
     ```bash
     git clone --recursive -b master git@github.com:sireum/v3.git sireum-v3
     git clone git@github.com:sireum/air.git sireum-v3/aadl/ir
     git clone git@github.com:sireum/v3-awas.git sireum-v3/awas
     export SIREUM_HOME=`pwd`/sireum-v3
     ./sireum-v3/bin/sbt-launch.sh "project awasJar" assembly
     cp ./sireum-v3/awasJar/target/scala-2.12/awasJar-assembly-0.1.0-SNAPSHOT.jar <ostate-plugin-dir>/org.sireum.aadl.osate.awas/lib/awasJar-assembly-0.1.0-SNAPSHOT.jar
     ```
