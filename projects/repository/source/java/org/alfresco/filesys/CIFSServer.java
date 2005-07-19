/*
 * Copyright (C) 2005 Alfresco, Inc.
 *
 * Licensed under the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version
 * 2.1 of the License, or (at your option) any later version.
 * You may obtain a copy of the License at
 *
 *     http://www.gnu.org/licenses/lgpl.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.alfresco.filesys;

import java.io.IOException;
import java.io.PrintStream;
import java.net.SocketException;

import org.alfresco.config.source.ClassPathConfigSource;
import org.alfresco.config.xml.XMLConfigService;
import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.filesys.netbios.server.NetBIOSNameServer;
import org.alfresco.filesys.server.NetworkServer;
import org.alfresco.filesys.server.config.ServerConfiguration;
import org.alfresco.filesys.server.filesys.DiskInterface;
import org.alfresco.filesys.smb.server.SMBServer;
import org.alfresco.service.ServiceRegistry;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * CIFS Server Class
 * <p>
 * Create and start the various server components required to run the CIFS server.
 * 
 * @author GKSpencer
 */
public class CIFSServer
{
    private static final Log logger = LogFactory.getLog("org.alfresco.smb.server");

    private ServiceRegistry serviceRegistry;
    private String configLocation;
    private DiskInterface diskInterface;
    private ServerConfiguration filesysConfig;

    /**
     * @param serviceRegistry connects to the repository
     * @param configService
     * @param diskInterface
     */
    public CIFSServer(ServiceRegistry serviceRegistry, String configLocation, DiskInterface diskInterface)
    {
        this.serviceRegistry = serviceRegistry;
        this.configLocation = configLocation;
        this.diskInterface = diskInterface;
    }

    /**
     * Return the server configuration
     * 
     * @return ServerConfiguration
     */
    public final ServerConfiguration getConfiguration() {
        return filesysConfig;
    }
    
    /**
     * Start the CIFS server components
     * 
     * @exception SocketException If a network error occurs
     * @exception IOException If an I/O error occurs
     */
    public final void startServer() throws SocketException, IOException
    {
        try
        {
            ClassPathConfigSource classPathConfigSource = new ClassPathConfigSource(configLocation);
            XMLConfigService xmlConfigService = new XMLConfigService(classPathConfigSource);
            xmlConfigService.init();
            filesysConfig = new ServerConfiguration(serviceRegistry, xmlConfigService, diskInterface);
            filesysConfig.init();

            // Create the SMB server and NetBIOS name server, if enabled
            if (filesysConfig.isSMBServerEnabled())
            {
                // Create the NetBIOS name server if NetBIOS SMB is enabled
                if (filesysConfig.hasNetBIOSSMB())
                    filesysConfig.addServer(new NetBIOSNameServer(serviceRegistry, filesysConfig));

                // Create the SMB server
                filesysConfig.addServer(new SMBServer(serviceRegistry, filesysConfig));
            }

            // Start the configured servers
            for (int i = 0; i < filesysConfig.numberOfServers(); i++)
            {
                // Get the current server
                NetworkServer server = filesysConfig.getServer(i);

                if (logger.isInfoEnabled())
                    logger.info("Starting server " + server.getProtocolName() + " ...");

                // Start the server
                String serverName = server.getConfiguration().getServerName();
                server.startServer();
            }
        }
        catch (Throwable e)
        {
            filesysConfig = null;
            throw new AlfrescoRuntimeException("Failed to start CIFS Server", e);
        }
    }

    /**
     * Stop the CIFS server components
     */
    public final void stopServer()
    {
        if (filesysConfig == null)
        {
            // initialisation failed
            return;
        }
        // Shutdown the servers
        for (int i = 0; i < filesysConfig.numberOfServers(); i++)
        {
            // Get the current server
            NetworkServer server = filesysConfig.getServer(i);

            if (logger.isInfoEnabled())
                logger.info("Shutting server " + server.getProtocolName() + " ...");

            // Start the server
            server.shutdownServer(false);
        }
    }

    /**
     * Runs the CIFS server directly
     * 
     * @param args String[]
     */
    public static void main(String[] args)
    {
        PrintStream out = System.out;

        out.println("CIFS Server Test");
        out.println("----------------");

        try
        {
            // Create the configuration service in the same way that Spring creates it
            ApplicationContext ctx = new ClassPathXmlApplicationContext("alfresco/application-context.xml");

            // get the CIFS server bean
            CIFSServer server = (CIFSServer) ctx.getBean("cifsServer");
            if (server == null)
            {
                throw new AlfrescoRuntimeException("Server bean 'cifsServer' not defined");
            }

            // Only wait for shutdown if the SMB/CIFS server is enabled
            
            if ( server.getConfiguration().isSMBServerEnabled())
            {
                
                // SMB/CIFS server should have automatically started
                // Wait for shutdown via the console
                
                out.println("Enter 'x' to shutdown ...");
                boolean shutdown = false;
    
                // Wait while the server runs, user may stop the server by typing a key
                
                while (shutdown == false)
                {
    
                    // Wait for the user to enter the shutdown key
    
                    int ch = System.in.read();
    
                    if (ch == 'x' || ch == 'X')
                        shutdown = true;
    
                    synchronized (server)
                    {
                        server.wait(20);
                    }
                }
    
                // Stop the server
                
                server.stopServer();
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
        System.exit(1);
    }
}
