#!/bin/sh
# -------
# Script for apply AMPs to installed WAR
# -------
export ALF_HOME=@@BITROCK_INSTALLDIR@@
export CATALINA_HOME=@@BITROCK_TOMCAT_DIRNAME@@
. $ALF_HOME/scripts/setenv.sh
echo "This script will apply all the AMPs in amps and amps-share to the alfresco.war and share.war files in $CATALINA_HOME/webapps"
echo "Press control-c to stop this script . . ."
echo "Press any other key to continue . . ."
read RESP
java -jar $ALF_HOME/bin/alfresco-mmt.jar install $ALF_HOME/amps $CATALINA_HOME/webapps/alfresco.war -directory
java -jar $ALF_HOME/bin/alfresco-mmt.jar list $CATALINA_HOME/webapps/alfresco.war
java -jar $ALF_HOME/bin/alfresco-mmt.jar install $ALF_HOME/amps_share $CATALINA_HOME/webapps/share.war -directory
java -jar $ALF_HOME/bin/alfresco-mmt.jar list $CATALINA_HOME/webapps/share.war
echo "About to clean out tomcat/webapps/alfresco and share directories and temporary files..."
echo "Press control-c to stop this script . . ."
echo "Press any other key to continue . . ."
read DUMMY
rm -rf $CATALINA_HOME/webapps/alfresco
rm -rf $CATALINA_HOME/webapps/share
. $ALF_HOME/bin/clean_tomcat.sh
