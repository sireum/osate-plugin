# temp workaround to build a min jar for arsit using v3.  Need to resolve
# mill+intellij+resource issue.  

# SIREUM_HOME should point to root v3 dir 

PLUGIN_JAR="arsit.jar"

$SIREUM_HOME/bin/sbt-launch.sh "; project aadl-arsit; package; project cli; package; project util-jvm; package"

mkdir tmp
cd tmp

jar xf $SIREUM_HOME/aadl/arsit/jvm/target/scala-2.12/aadl-arsit_2.12-3.jar
jar xf $SIREUM_HOME/cli/jvm/target/scala-2.12/cli_2.12-3.jar
jar xf $SIREUM_HOME/util/jvm/target/scala-2.12/util_2.12-3.jar

jar cvfm ../${PLUGIN_JAR} META-INF/MANIFEST.MF .

cd ..
rm -rf tmp
