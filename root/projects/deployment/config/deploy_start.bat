@echo off
setlocalif "%JAVA_HOME%"=="" goto NoJavaHomeif not exist %JAVA_HOME%\bin\java.exe goto InvalidJavaHomegoto StartServer:NoJavaHomeecho.echo Error: JAVA_HOME environment variable is not set.goto End:InvalidJavaHomeecho.echo Error: JAVA_HOME '%JAVA_HOME%' does not contain a valid Java installation.goto End:StartServerrem Set RMI_LISTEN_HOSTNAME to the hostname you wish the deployment server to listen on.rem See http://www.springframework.org/docs/api/org/springframework/remoting/rmi/RmiServiceExporter.htmlrem for more details.set RMI_LISTEN_HOSTNAME=echo .
echo ==============================
echo = Alfresco Deployment Server =
echo =   use control-c to stop    =
echo ==============================
echo .
if "%RMI_LISTEN_HOSTNAME%"=="" goto StartServerWithoutRMIHostnamegoto StartServerWithRMIHostname:StartServerWithoutRMIHostnamestart /min "Deployment Server" "%JAVA_HOME%\bin\java" -server -Djava.ext.dirs=. org.alfresco.deployment.Main application-context.xml >>deployment.log 2>&1goto End:StartServerWithRMIHostnamestart /min "Deployment Server" "%JAVA_HOME%\bin\java" -server -Djava.ext.dirs=. -Djava.rmi.server.hostname=%RMI_LISTEN_HOSTNAME% org.alfresco.deployment.Main application-context.xml >>deployment.log 2>&1goto End:Endendlocal