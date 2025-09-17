# Sireum OSATE Plugin

This repository contains the Sireum [OSATE](http://osate.org) Plugin that
translates AADL instance models to [AIR](https://github.com/sireum/air) and then
to downstream Sireum tools such as [AWAS](https://github.com/sireum/v3-awas).

## Installation for Developers 

1. Install an [OSATE development
   environment](http://osate.org/setup-development.html) -- the rest of these
   instructions will refer to this as Eclipse

   You'll probably get plugin version errors which usually have to be resolved
   before you can build the OSATE source.  A workaround is to navigate to
   ``Preferences`` > ``Plug-in Development`` > ``API Errors/Warnings`` >
   ``Version Management`` and set all the severity levels to ``Ignore``.

1. Install [Sireum Kekinian](https://github.com/sireum/kekinian#installing)

1. Clone the repository ``git clone --recurse-submodules git@github.com:sireum/osate-plugin.git``

1. From the command line

   ```
   mkdir osate-plugin/org.sireum.aadl.osate/lib
   ln -s $SIREUM_HOME/bin/sireum.jar osate-plugin/org.sireum.aadl.osate/lib/sireum.jar
   ```

1. Launch Eclipse and go to *__File__ -> __Import...__*  

1. Expand the *__General__* folder and select *__Existing Project into
   Workspace__*, then click *__Next__*.

1. Enter the path to the directory where you cloned the repository and then
   select all the available projects from the project listings and click
   *__Finish__*.

1. Navigate to *__Preferences -> Java -> Installed JREs__*.  Select the JRE that
   is checked (probably '*jre (default)*') and then click *__Edit...__*.  If you
   launch Eclipse from the command line (ie. it will inherit your environment
   variables) then add 
   ``-ea -Dorg.sireum.home=${env_var:SIREUM_HOME}`` to
   *__Default VM arguments__*, otherwise add 
   ``-ea -Dorg.sireum.home=<path-to-sireum-home>``. Click *__Finish__* then *__Apply
   and Close__*.
   
1. To resolve access restrictions errors related to ``UiUtil``

   - Navigate to: *__Preferences -> Java -> Compiler -> Errors/Warnings -> Deprecated and restricted API__*, and 

   - Change *__Forbidden reference (access rules)__* from ``Error`` to ``Warning``.

1. __Optional:__ Rebuild tool jars
     
   * Awas
   
     ```bash
     git clone --recursive -b master git@github.com:sireum/v3.git sireum-v3
     git clone git@github.com:sireum/air.git sireum-v3/aadl/ir
     git clone git@github.com:sireum/v3-awas.git sireum-v3/awas
     export SIREUM_HOME=`pwd`/sireum-v3
     ./sireum-v3/bin/sbt-launch.sh "project awasJar" assembly
     cp ./sireum-v3/awasJar/target/scala-2.12/awasJar-assembly-0.1.0-SNAPSHOT.jar <ostate-plugin-dir>/org.sireum.aadl.osate.awas/lib/awasJar-assembly-0.1.0-SNAPSHOT.jar
     ```

## macOS 14 SWT Issue

If the OSATE runtime instance looks like [this screenshot](https://github.com/eclipse-platform/eclipse.platform.swt/issues/1012#issuecomment-1914320902)
then you need to use a JRE that was not built under macOS 14.  For example,
[Zulu
17.0.10+7](https://www.azul.com/core-post-download/?endpoint=zulu&uuid=a8d7a419-71b6-4833-b155-0cf4b4936312)
does not exhibit the issue and doing ``vtool -show java`` on it shows it was
built under macOS 11.1 

## 2025-03 hamcrest issue

If it takes a long time for the splash screen to appear when launching an OSATE
runtime instance, remove the hamcrest entries from the OSATE2 launcher
[here](https://github.com/osate/osate2/blob/1fc6a025404f24f9a363d744a0f8599dd1dc2773/releng/osate.releng/OSATE2.launch#L633-L634).

If the slow startup occurs for a plugin unit test, open the unit test's runtime
configuration, select the ``Plugins`` tab, change ``Launch with:`` to 
``Plug-ins selected below``, search for ``hamrcrest`` and uncheck the
``org.hamrcrest.library`` entry