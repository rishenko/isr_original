#!/usr/local/bin/tcsh

mkdir -p ./javadoc/

# External packages and jars.  Edit these to reflect your own pathnames

setenv XERCES /usr/local/vicar/external/xerces/v2.4.0/
setenv X1 $XERCES/xmlParserAPIs.jar
setenv X2 $XERCES/xercesImpl.jar
setenv XALAN /usr/local/vicar/external/xalan/v2.1.0/
setenv X3 $XALAN/bin/xalan.jar
setenv X4 $XALAN/bin/xalanj1compat.jar
setenv JAKARTA_ORO /usr/local/vicar/external/jakarta_oro/jakarta-oro-2.0.4/
setenv X5 $JAKARTA_ORO/jakarta-oro-2.0.4.jar

setenv CP ${X1}:${X2}:${X3}:${X4}:${X5}

javadoc -J-mx128m -classpath ./src/main/java/:$CP -sourcepath ./src/main/java/ -d ./javadoc -author -version -link http://java.sun.com/j2se/1.4/docs/api -link http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs @packagelist |& tee make_javadoc.log

