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
package org.alfresco.web.app;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import javax.transaction.UserTransaction;

import net.sf.acegisecurity.providers.UsernamePasswordAuthenticationToken;
import net.sf.acegisecurity.providers.dao.UsernameNotFoundException;

import org.alfresco.config.ConfigElement;
import org.alfresco.config.ConfigService;
import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.importer.ImporterBootstrap;
import org.alfresco.repo.security.authentication.AuthenticationService;
import org.alfresco.repo.security.authentication.RepositoryAuthenticationDao;
import org.alfresco.repo.security.authentication.StoreContextHolder;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.app.servlet.AuthenticationFilter;
import org.alfresco.web.bean.repository.Repository;
import org.alfresco.web.bean.repository.User;
import org.apache.log4j.Logger;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * ServletContextListener implementation that initialises the application.
 * 
 * NOTE: This class must appear after the Spring context loader listener
 * 
 * @author gavinc
 */
public class ContextListener implements ServletContextListener, HttpSessionListener
{
   private static Logger logger = Logger.getLogger(ContextListener.class);
   private static final String ADMIN = "admin";
   private ServletContext servletContext;
   
   /**
    * @see javax.servlet.ServletContextListener#contextInitialized(javax.servlet.ServletContextEvent)
    */
   public void contextInitialized(ServletContextEvent event)
   {
      // make sure that the spaces store in the repository exists
      this.servletContext = event.getServletContext();
      WebApplicationContext ctx = WebApplicationContextUtils.getRequiredWebApplicationContext(servletContext);
      ServiceRegistry registry = (ServiceRegistry)ctx.getBean(ServiceRegistry.SERVICE_REGISTRY);
      NodeService nodeService = registry.getNodeService();
      SearchService searchService = registry.getSearchService();
      NamespaceService namespaceService = registry.getNamespaceService();

      String repoStoreName = Application.getRepositoryStoreName(servletContext);
      if (repoStoreName == null)
      {
         throw new Error("Repository store name has not been configured, is 'store-name' element missing?");
      }      

      // repo bootstrap code for our client
      UserTransaction tx = null;
      String companySpaceId = null;
      try
      {
         tx = registry.getUserTransaction();
         tx.begin();
         
         // get and setup the initial store ref from config
         StoreRef storeRef = Repository.getStoreRef(servletContext);
         
         // check the repository exists, create if it doesn't
         if (nodeService.exists(storeRef) == false)
         {
            storeRef = nodeService.createStore(StoreRef.PROTOCOL_WORKSPACE, repoStoreName);
            
            if (logger.isDebugEnabled())
               logger.debug("Created store with name: " + repoStoreName);
         }
         
         NodeRef rootNodeRef = nodeService.getRootNode(storeRef);
         
         // see if the company home space is present
         String companySpaceName = Application.getCompanyRootName(servletContext);
         String companyXPath = NamespaceService.ALFRESCO_PREFIX + ":" + QName.createValidLocalName(companySpaceName);
         List<NodeRef> nodes = nodeService.selectNodes(rootNodeRef, companyXPath, null, namespaceService, false);
         if (nodes.size() == 0)
         {
            if (logger.isDebugEnabled())
                logger.debug("Importing bootstrap data into store: " + repoStoreName + "...");
             
            // Construct binding values for import
            String companySpaceDescription = Application.getCompanyRootDescription(servletContext);
            String glossaryFolderName = Application.getGlossaryFolderName(servletContext);
            String templatesFolderName = Application.getTemplatesFolderName(servletContext);
            if (companySpaceName == null)
            {
               throw new Error("Company root name has not been configured, is 'company-root-name' element missing?");
            }
            if (glossaryFolderName == null)
            {
               throw new Error("Glossary folder name has not been configured, is 'glossary-folder-name' element missing?");
            }
            if (templatesFolderName == null)
            {
               throw new Error("Templates folder name has not been configured, is 'templates-folder-name' element missing?");
            }
            Properties configuration = new Properties();
            configuration.put("companySpaceName", companySpaceName);
            configuration.put("companySpaceDescription", companySpaceDescription);
            configuration.put("glossaryName", glossaryFolderName);
            configuration.put("templatesName", templatesFolderName);

            // Import bootstrap set of data
            ImporterBootstrap bootstrap = (ImporterBootstrap)ctx.getBean("importerBootstrap");
            bootstrap.setConfiguration(configuration);
            bootstrap.setStoreId(repoStoreName);
            bootstrap.bootstrap();

            // Find company root after import
            nodes = nodeService.selectNodes(rootNodeRef, companyXPath, null, namespaceService, false);
            
            if (logger.isDebugEnabled())
                logger.debug("Bootstrap data imported into store: " + repoStoreName);
         }
         // Extract company space id and store it in the Application object
         companySpaceId = nodes.get(0).getId();
         Application.setCompanyRootId(companySpaceId);
         
         // check the admin user exists, create if it doesn't
         RepositoryAuthenticationDao dao = (RepositoryAuthenticationDao)ctx.getBean("alfDaoImpl");
         // this is required to setup the ACEGI context before we can check for the user
         StoreContextHolder.setContext(storeRef);
         try
         {
            dao.loadUserByUsername(ADMIN);
         }
         catch (UsernameNotFoundException ne)
         {
            ConfigService configService = (ConfigService)ctx.getBean(Application.BEAN_CONFIG_SERVICE);
            // default to password of "admin" if we don't find config for it
            String password = ADMIN;
            ConfigElement adminConfig = configService.getGlobalConfig().getConfigElement("admin");
            if (adminConfig != null)
            {
               List<ConfigElement> children = adminConfig.getChildren();
               if (children.size() != 0)
               {
                  // try to find the config element for the initial password
                  ConfigElement passElement = children.get(0);
                  if (passElement.getName().equals("initial-password"))
                  {
                     password = passElement.getValue();
                  }
               }
            }
            
            // create the Authentication instance for the "admin" user
            UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(ADMIN, password);
            AuthenticationService authService = (AuthenticationService)ctx.getBean("authenticationService");
            authService.createAuthentication(storeRef, token);
            
            // create the node to represent the Person instance for the admin user
            Map<QName, Serializable> props = new HashMap<QName, Serializable>(7, 1.0f);
            props.put(ContentModel.PROP_USERNAME, ADMIN);
            props.put(ContentModel.PROP_FIRSTNAME, ADMIN);
            props.put(ContentModel.PROP_LASTNAME, ADMIN);
            props.put(ContentModel.PROP_HOMEFOLDER, companySpaceId);
            props.put(ContentModel.PROP_EMAIL, "");
            props.put(ContentModel.PROP_ORGID, "");
            
            // Create the person under the special people system folder
            // This is required to allow authenticate() to succeed during login
            List<NodeRef> results = nodeService.selectNodes(
                  rootNodeRef, RepositoryAuthenticationDao.PEOPLE_FOLDER, null, namespaceService, false);
            if (results.size() != 1)
            {
                throw new Exception("Unable to find system types folder path: " + RepositoryAuthenticationDao.PEOPLE_FOLDER);
            }

            nodeService.createNode(
                  results.get(0),
                  ContentModel.ASSOC_CHILDREN,
                  ContentModel.TYPE_PERSON,  // expecting this qname path in the authentication methods
                  ContentModel.TYPE_PERSON,
                  props);
         }

         // commit the transaction
         tx.commit();
      }
      catch (Exception e)
      {
         // rollback the transaction
         try { if (tx != null) {tx.rollback();} } catch (Exception ex) {}
         System.out.println(e.toString());
         throw new AlfrescoRuntimeException("Failed to initialise ", e);
      }
      
      if (logger.isDebugEnabled())
         logger.debug("Server is running in portal server: " + Application.inPortalServer(servletContext));
   }

   /**
    * @see javax.servlet.ServletContextListener#contextDestroyed(javax.servlet.ServletContextEvent)
    */
   public void contextDestroyed(ServletContextEvent event)
   {
      // nothing to do
   }

   /**
    * Session created listener
    */
   public void sessionCreated(HttpSessionEvent event)
   {
      if (logger.isDebugEnabled())
         logger.debug("HTTP session created: " + event.getSession().getId());
   }

   /**
    * Session destroyed listener
    */
   public void sessionDestroyed(HttpSessionEvent event)
   {
      if (logger.isDebugEnabled())
         logger.debug("HTTP session destroyed: " + event.getSession().getId());
      
      User user = (User)event.getSession().getAttribute(AuthenticationFilter.AUTHENTICATION_USER);
      if (user != null)
      {
         // invalidate ticket
         WebApplicationContext ctx = WebApplicationContextUtils.getRequiredWebApplicationContext(servletContext);
         AuthenticationService authService = (AuthenticationService)ctx.getBean("authenticationService");
         authService.invalidate(user.getTicket());
         event.getSession().removeAttribute(AuthenticationFilter.AUTHENTICATION_USER);
      }
   }
}
