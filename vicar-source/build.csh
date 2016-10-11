#!/bin/csh

mkdir ./target/classes/

# External packages and jars.  Edit these to reflect your own pathnames

setenv XERCES /usr/local/vicar/external/xerces/v2.4.0/
setenv X1 $XERCES/xmlParserAPIs.jar
setenv X2 $XERCES/xercesImpl.jar
setenv XALAN /usr/local/vicar/external/xalan/v2.1.0/
setenv X3 $XALAN/bin/xalan.jar
setenv X4 $XALAN/bin/xalanj1compat.jar
setenv JAKARTA_ORO /usr/local/vicar/external/jakarta_oro/jakarta-oro-2.0.4/
setenv X5 $JAKARTA_ORO/jakarta-oro-2.0.4.jar

# Compile the code

setenv CP .:${X1}:${X2}:${X3}:${X4}:${X5}

echo javac -d ./target/classes/ -classpath $CP jpl/mipl/io/codec/*.java jpl/mipl/io/plugins/*.java jpl/mipl/io/streams/*.java jpl/mipl/io/util/*.java jpl/mipl/io/vicar/*.java jpl/mipl/io/xsl/*.java jpl/mipl/io/SimpleConvert.java
javac -d ./target/classes/ -classpath $CP jpl/mipl/io/codec/*.java jpl/mipl/io/plugins/*.java jpl/mipl/io/streams/*.java jpl/mipl/io/util/*.java jpl/mipl/io/vicar/*.java jpl/mipl/io/xsl/*.java jpl/mipl/io/SimpleConvert.java

# Copy over services files, for operation registries etc.

mkdir -p ./target/classes/META-INF/services
echo cp jpl/mipl/io/codec/javax.* ./target/classes/META-INF/services/
cp jpl/mipl/io/codec/javax.* ./target/classes/META-INF/services/
echo cp jpl/mipl/io/plugins/javax.* ./target/classes/META-INF/services/
cp jpl/mipl/io/plugins/javax.* ./target/classes/META-INF/services/

# Copy over the XSL files, for the transcoders

mkdir -p ./target/classes/jpl/mipl/io/xsl/
echo cp jpl/mipl/io/xsl/*.xsl ./target/classes/jpl/mipl/io/xsl/
cp jpl/mipl/io/xsl/*.xsl ./target/classes/jpl/mipl/io/xsl/

# Make the jar

cd ./target/classes
echo jar cvf ../vicario.jar .
jar cvf ../vicario.jar .

