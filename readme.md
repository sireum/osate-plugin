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

1. Clone this repository ``git clone -b manifest --recurse-submodules git@github.com:sireum/osate-plugin.git``

1. From the command line

   ```
   cd osate-plugin/org.sireum.aadl.osate/lib
   ln -s $SIREUM_HOME/bin/sireum.jar .
   ```

1. Launch Eclipse and go to *__File__ -> __Import...__*  

1. Expand the *__General__* folder and select *__Existing Project into Workspace__*, then click 
   *__Next__*.

1. Enter the path to the directory where you cloned the repository and then select all the 
   available projects from the project listings and click *__Finish__*.

1. Navigate to *__Window -> Preferences -> Java -> Installed JREs__*.  Select the JRE that is checked (probably '*jre (default)*') 
   and then click *__Edit...__*.  Add the following to *__Default VM arguments__*: ``-ea -Dorg.sireum.home=${env_var:SIREUM_HOME}`` 
   and click *__Finish__* then *__Apply and Close__*
   
1. To resolve access restrictions errors related to ``UiUtil``

   - Navigate to: *__Preferences -> Java -> Compiler -> Errors/Warnings -> Deprecated and restricted API__*, and 

   - Change *__Forbidden reference (access rules)__* from ``Error`` to ``Warning``.

1. __Optional:__ Rebuild tool jars

   * Sireum
     
     ```bash
     git clone --recursive https://github.com/sireum/kekinian
     kekinian/bin/build.cmd
     cp kekinian/bin/sireum.jar <osate-plugin-dir>/org.sireum.aadl.osate/lib/sireum.jar
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
