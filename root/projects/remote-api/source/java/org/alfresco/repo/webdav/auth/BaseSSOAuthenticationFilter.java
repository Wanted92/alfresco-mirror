/*
 * Copyright (C) 2005-2007 Alfresco Software Limited.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.

 * As a special exception to the terms and conditions of version 2.0 of 
 * the GPL, you may redistribute this Program in connection with Free/Libre 
 * and Open Source Software ("FLOSS") applications as described in Alfresco's 
 * FLOSS exception.  You should have recieved a copy of the text describing 
 * the FLOSS exception, and it is also available here: 
 * http://www.alfresco.com/legal/licensing
 */
package org.alfresco.repo.webdav.auth;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.transaction.UserTransaction;

import org.alfresco.filesys.ServerConfigurationBean;
import org.alfresco.jlan.server.auth.ntlm.NTLM;
import org.alfresco.jlan.server.auth.passthru.DomainMapping;
import org.alfresco.jlan.server.config.SecurityConfigSection;
import org.alfresco.jlan.util.IPAddress;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.SessionUser;
import org.alfresco.repo.security.authentication.AuthenticationComponent;
import org.alfresco.repo.security.authentication.AuthenticationException;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.transaction.TransactionService;
import org.apache.commons.logging.Log;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * Base class with common code and initialisation for single signon authentication filters.
 * 
 * @author gkspencer
 * @author kroast
 */
public abstract class BaseSSOAuthenticationFilter implements Filter
{
	// Constants
	//
	// Session value names
	//
	// Note: These values are copied from the AuthenticationHelper and LoginBean classes to avoid project dependencies
	
    protected static final String AUTHENTICATION_USER = "_alfAuthTicket";
    protected static final String LOGIN_EXTERNAL_AUTH = "_alfExternalAuth";
	
    // Request level marker to indicate that authentication should not be processed
    //
    // Note: copied from the AbstractAuthenticationFilter to avoid project dependencies
    
    protected static final String NO_AUTH_REQUIRED = "alfNoAuthRequired"; 

    // Attribute used by WebDAV filters for storing the WebDAV user details
    
    protected static final String WEBDAV_AUTH_USER    = "_alfDAVAuthTicket";
    
    // Allow an authenitcation ticket to be passed as part of a request to bypass authentication

    private static final String ARG_TICKET = "ticket";
    
    // Servlet context, required to get authentication service

	protected ServletContext m_context;
    
    // File server configuration

	private ServerConfigurationBean m_srvConfig;
    
    // Security configuration section, for domain mappings

	private SecurityConfigSection m_secConfig;
    
    // Various services required by NTLM authenticator

	protected AuthenticationService m_authService;
    protected AuthenticationComponent m_authComponent;
    protected PersonService m_personService;
    protected NodeService m_nodeService;
    protected TransactionService m_transactionService;
    
    // Local server name, from either the file servers config or DNS host name

    protected String m_srvName;
    
    // Login page relative address, if null then login will loop until a valid login is received
    
    private String m_loginPage;
    
    // Indicate whether ticket based logons are supported
    
    private boolean m_ticketLogons;
    
    // User object attribute name
    
    private String m_userAttributeName = AUTHENTICATION_USER;
    
    /**
     * Initialize the filter
     * 
     * @param args FilterConfig
     * 
     * @exception ServletException
     */
    public void init(FilterConfig args) throws ServletException
    {
        // Save the servlet context, needed to get hold of the authentication service

    	m_context = args.getServletContext();
        
        // Setup the authentication context

    	WebApplicationContext ctx = WebApplicationContextUtils.getRequiredWebApplicationContext(m_context);
        
        ServiceRegistry serviceRegistry = (ServiceRegistry) ctx.getBean(ServiceRegistry.SERVICE_REGISTRY);
        m_nodeService = serviceRegistry.getNodeService();
        m_transactionService = serviceRegistry.getTransactionService();
        m_authService = serviceRegistry.getAuthenticationService();
        
        m_authComponent = (AuthenticationComponent) ctx.getBean("AuthenticationComponent");
        m_personService = (PersonService) ctx.getBean("personService");
        
        m_srvConfig = (ServerConfigurationBean) ctx.getBean(ServerConfigurationBean.SERVER_CONFIGURATION);
        
        // Get the local server name, try the file server config first

        if (m_srvConfig != null)
        {
            m_srvName = m_srvConfig.getServerName();
            
            if (m_srvName != null)
            {
                try
                {
                    InetAddress resolved = InetAddress.getByName(m_srvName);
                    if (resolved == null)
                    {
                        // Failed to resolve the configured name

                    	m_srvName = m_srvConfig.getLocalServerName(true);
                    }
                }
                catch (UnknownHostException ex)
                {
                    if (getLogger().isErrorEnabled())
                        getLogger().error("NTLM filter, error resolving CIFS host name", ex);
                }
            }
            
            // If we still do not have a name use the DNS name of the server, with the domain part removed
            
            if ( m_srvName == null)
            {
                m_srvName = m_srvConfig.getLocalServerName(true);
            }
            
            // Find the security configuration section

            m_secConfig = (SecurityConfigSection)m_srvConfig.getConfigSection(SecurityConfigSection.SectionName);
        }
        else
        {
            // Get the host name

        	try
            {
                // Get the local host name

        		m_srvName = InetAddress.getLocalHost().getHostName();
                
                // Strip any domain name

        		int pos = m_srvName.indexOf(".");
                if (pos != -1)
                {
                    m_srvName = m_srvName.substring(0, pos - 1);
                }
            }
            catch (UnknownHostException ex)
            {
                if (getLogger().isErrorEnabled())
                    getLogger().error("NTLM filter, error getting local host name", ex);
            }
        }
        
        // Check if the server name is valid

        if (m_srvName == null || m_srvName.length() == 0)
        {
            throw new ServletException("Failed to get local server name");
        }
    }
    
    /**
     * Delete the servlet filter
     */
    public void destroy()
    {
    }

    /**
     * Create the user object that will be stored in the session
     * 
     * @param userName String
     * @param ticket String
     * @param personNode NodeRef
     * @param homeSpace String
     * @return SessionUser 
     */
    protected abstract SessionUser createUserObject( String userName, String ticket, NodeRef personNode, String homeSpace);
    
    /**
     * Callback to get the specific impl of the Session User for a filter
     * 
     * @return User from the session
     */
    protected SessionUser getSessionUser(HttpSession session)
    {
        return (SessionUser)session.getAttribute( getUserAttributeName());
    }
    
    /**
     * Return the user object session attribute name
     * 
     * @return String
     */
    protected final String getUserAttributeName()
    {
    	return m_userAttributeName;
    }

    /**
     * Set the user object attribute name
     * 
     * @param userAttr String
     */
    protected final void setUserAttributeName( String userAttr)
    {
    	m_userAttributeName = userAttr;
    }
    
    /**
     * Callback to create the User environment as appropriate for a filter impl
     * 
     * @param session HttpSession
     * @param userName String
     * @return SessionUser
     * @throws IOException
     * @throws ServletException
     */
    protected SessionUser createUserEnvironment(HttpSession session, String userName)
        throws IOException, ServletException
    {
        SessionUser user = null;
        
        UserTransaction tx = m_transactionService.getUserTransaction();
        
        try
        {
            tx.begin();
            
            // Setup User object and Home space ID etc.
            
            final NodeRef personNodeRef = m_personService.getPerson(userName);
            
            // Use the system user context to do the user lookup
            RunAsWork<String> getUserNameRunAsWork = new RunAsWork<String>()
            {
                public String doWork() throws Exception
                {
                    return (String) m_nodeService.getProperty(personNodeRef, ContentModel.PROP_USERNAME);
                }
            };
            userName = AuthenticationUtil.runAs(getUserNameRunAsWork, AuthenticationUtil.SYSTEM_USER_NAME);
            
            m_authComponent.setCurrentUser(userName);
            String currentTicket = m_authService.getCurrentTicket();
            
            NodeRef homeSpaceRef = (NodeRef) m_nodeService.getProperty(personNodeRef, ContentModel.PROP_HOMEFOLDER);
            
            // Create the user object to be stored in the session
            
            user = createUserObject( userName, currentTicket, personNodeRef, homeSpaceRef.getId());
            
            tx.commit();
        }
        catch (Throwable ex)
        {
            try
            {
                tx.rollback();
            }
            catch (Exception err)
            {
                getLogger().error("Failed to rollback transaction", err);
            }
            if (ex instanceof RuntimeException)
            {
                throw (RuntimeException)ex;
            }
            else if (ex instanceof IOException)
            {
                throw (IOException)ex;
            }
            else if (ex instanceof ServletException)
            {
                throw (ServletException)ex;
            }
            else
            {
                throw new RuntimeException("Authentication setup failed", ex);
            }
        }
        
        // Store the user on the session
        
        session.setAttribute( getUserAttributeName(), user);
        session.setAttribute( LOGIN_EXTERNAL_AUTH, Boolean.TRUE);
        
        return user;
    }
    
    /**
     * Callback executed on successful ticket validation during Type3 Message processing
     * 
     * @param req HttpServletReqeust
     * @param session HttpSession
     */
    protected void onValidate(HttpServletRequest req, HttpSession session)
    {
    }
    
    /**
     * Callback executed on failed authentication of a user ticket during Type3 Message processing
     * 
     *  @param req HttpServletRequest
     *  @param res HttpServletResponse
     *  @param session HttpSession
     */
    protected void onValidateFailed(HttpServletRequest req, HttpServletResponse res, HttpSession session)
        throws IOException
    {
    }
    
    /**
     * Callback executed on completion of NTLM login
     * 
     * @param req HttpServletRequest
     * @param res HttpServletResponse
     * @return true to continue filter chaining, false otherwise
     */
    protected boolean onLoginComplete(HttpServletRequest req, HttpServletResponse res)
        throws IOException
    {
    	return true;
    }
    
    /**
     * Map a client IP address to a domain
     * 
     * @param clientIP String
     * @return String
     */
    protected final String mapClientAddressToDomain(String clientIP)
    {
        // Check if there are any domain mappings
    	
        if (m_secConfig != null && m_secConfig.hasDomainMappings() == false)
        {
            return null;
        }
        
        if (m_secConfig != null)
        {
            // Convert the client IP address to an integer value
        	
            int clientAddr = IPAddress.parseNumericAddress(clientIP);
            for (DomainMapping domainMap : m_secConfig.getDomainMappings())
            {
                if (domainMap.isMemberOfDomain(clientAddr))
                {
                    if (getLogger().isDebugEnabled())
                        getLogger().debug("Mapped client IP " + clientIP + " to domain " + domainMap.getDomain());
                
                    return domainMap.getDomain();
                }
            }
        }
        
        if (getLogger().isDebugEnabled())
            getLogger().debug("Failed to map client IP " + clientIP + " to a domain");
        
        // No domain mapping for the client address
        
        return null;
    }
    
    /**
     * Check if the request has specified a ticket parameter to bypass the standard authentication
     * 
     * @param req HttpServletRequest
     * @param sess HttpSession
     * @return boolean
     */
    protected boolean checkForTicketParameter( HttpServletRequest req, HttpSession sess)
    {
    	// Check if the request includes an authentication ticket

    	boolean ticketValid = false;
    	String ticket = req.getParameter(ARG_TICKET);
    	
    	if (ticket != null && ticket.length() != 0)
    	{
            if (getLogger().isDebugEnabled())
                getLogger().debug("Logon via ticket from " + req.getRemoteHost() + " (" +
                        req.getRemoteAddr() + ":" + req.getRemotePort() + ")" + " ticket=" + ticket);
            
    		UserTransaction tx = null;
    		try
    		{
    		    // Validate the ticket
    			
    		    m_authService.validate(ticket);

    		    SessionUser user = getSessionUser( sess);
    		    
                if ( user == null)
                {
        		    // Start a transaction
                	
        		    tx = m_transactionService.getUserTransaction();
        		    tx.begin();
        		    
                    // Need to create the User instance if not already available
        		    
                    String currentUsername = m_authService.getCurrentUserName();
                    
        		    NodeRef personRef = m_personService.getPerson(currentUsername);
        		    user = createUserObject( currentUsername, m_authService.getCurrentTicket(), personRef, null);
        		    
        		    tx.commit();
        		    tx = null; 
        		    
        		    // Store the User object in the Session - the authentication servlet will then proceed
        		    
        		    req.getSession().setAttribute( getUserAttributeName(), user);
                }
    		    
    		    // Indicate the ticket parameter was specified, and valid
                
                ticketValid = true;
    		}
    		catch (AuthenticationException authErr)
        	{
        		if (getLogger().isDebugEnabled())
        		    getLogger().debug("Failed to authenticate user ticket: " + authErr.getMessage(), authErr);
        	}
        	catch (Throwable e)
        	{
        		if (getLogger().isDebugEnabled())
                    getLogger().debug("Error during ticket validation and user creation: " + e.getMessage(), e);
        	}
        	finally
        	{
        		try
        	    {
        			if (tx != null)
        	        {
        				tx.rollback();
       	        	}
        	    }
        	    catch (Exception tex)
        	    {
        	    }
        	}
    	}
    	
    	// Return the ticket parameter status
    	
    	return ticketValid;
    }
    
    /**
     * Redirect to the login page
     * 
     * @param req HttpServletRequest
     * @param req HttpServletResponse
     * @exception IOException
     */
    protected void redirectToLoginPage(HttpServletRequest req, HttpServletResponse res)
    	throws IOException
    {
    	if ( hasLoginPage())
    		res.sendRedirect(req.getContextPath() + "/faces" + getLoginPage());
    }
    
    /**
     * Return the logger
     * 
     * @return Log
     */
    protected abstract Log getLogger();
    
    /**
     * Determine if the login page is available
     * 
     * @return boolean
     */
    protected final boolean hasLoginPage()
    {
    	return m_loginPage != null ? true : false;
    }
    
    /**
     * Return the login page address
     * 
     * @return String
     */
    protected final String getLoginPage()
    {
    	return m_loginPage;
    }
    
    /**
     * Set the login page address
     * 
     * @param loginPage String
     */
    protected final void setLoginPage( String loginPage)
    {
    	m_loginPage = loginPage;
    }
    
    /**
     * Check if ticket based logons are allowed
     * 
     * @return boolean
     */
    protected final boolean allowsTicketLogons()
    {
    	return m_ticketLogons;
    }
    
    /**
     * Set the ticket based logons allowed flag
     * 
     * @param ticketsAllowed boolean
     */
    protected final void setTicketLogons( boolean ticketsAllowed)
    {
    	m_ticketLogons = ticketsAllowed;
    }

    /**
     * Check if a security blob starts with the NTLMSSP signature
     * 
     * @param byts byte[]
     * @param offset int
     * @return boolean
     */
    protected final boolean isNTLMSSPBlob( byte[] byts, int offset)
    {
	    // Check if the blob has the NTLMSSP signature

    	boolean isNTLMSSP = false;
    	
	    if (( byts.length - offset) >= NTLM.Signature.length) {
	      
	      // Check for the NTLMSSP signature
	      
	      int idx = 0;
	      while ( idx < NTLM.Signature.length && byts[offset + idx] == NTLM.Signature[ idx])
	        idx++;
	      
	      if ( idx == NTLM.Signature.length)
	        isNTLMSSP = true;
	    }
	    
	    return isNTLMSSP;
    }    
}
