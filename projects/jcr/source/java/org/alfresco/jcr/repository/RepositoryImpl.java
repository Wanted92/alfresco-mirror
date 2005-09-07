/*
 * Copyright (C) 2005 Alfresco, Inc.
 *
 * Licensed under the Mozilla Public License version 1.1 
 * with a permitted attribution clause. You may obtain a
 * copy of the License at
 *
 *   http://www.alfresco.org/legal/license.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.alfresco.jcr.repository;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.jcr.session.SessionContextProxyFactory;
import org.alfresco.jcr.session.SessionImpl;
import org.alfresco.repo.security.authentication.AuthenticationException;
import org.alfresco.repo.security.authentication.AuthenticationService;
import org.alfresco.service.ServiceRegistry;


/**
 * Alfresco implementation of a JCR Repository
 * 
 * @author David Caruana
 */
public class RepositoryImpl implements Repository
{
    /** Empty Password, if not supplied */
    private final static char[] EMPTY_PASSWORD = "".toCharArray();
    
    // TODO: Redirect to Alfresco "About" Service
    /** Repository Descriptors */
    private static final Map<String, String> descriptors = new HashMap<String, String>();
    static
    {
        descriptors.put(Repository.REP_NAME_DESC, "Alfresco Content Repository");
        descriptors.put(Repository.REP_VENDOR_DESC, "Alfresco");
        descriptors.put(Repository.REP_VENDOR_URL_DESC, "http://www.alfresco.org");
        descriptors.put(Repository.REP_VERSION_DESC, "0.6.0");
        descriptors.put(Repository.SPEC_NAME_DESC, "Content Repository API for Java(TM) Technology Specification");
        descriptors.put(Repository.SPEC_VERSION_DESC, "1.0");
        descriptors.put(Repository.LEVEL_1_SUPPORTED, "true");
        descriptors.put(Repository.OPTION_TRANSACTIONS_SUPPORTED, "true");
    }

    // Service dependencies
    private AuthenticationService authenticationService;  // TODO: remove when moved to service registry
    private ServiceRegistry serviceRegistry;

    
    /**
     * Set the authentication service
     * 
     * @param authenticationService
     */
    public void setAuthenticationService(AuthenticationService authenticationService)
    {
        this.authenticationService = authenticationService;
    }

    /**
     * Get the authentication service
     * 
     * @return  the authentication service
     */
    public AuthenticationService getAuthenticationService()
    {
        return authenticationService;
    }
    
    /**
     * Set the service registry
     * 
     * @param serviceRegistry
     */
    public void setServiceRegistry(ServiceRegistry serviceRegistry)
    {
        this.serviceRegistry = serviceRegistry;
    }

    /**
     * Get the service registry
     * 
     * @return  the service registry
     */
    public ServiceRegistry getServiceRegistry()
    {
        return serviceRegistry;
    }

    /* (non-Javadoc)
     * @see javax.jcr.Repository#getDescriptorKeys()
     */
    public String[] getDescriptorKeys()
    {
        String[] keys = (String[]) descriptors.keySet().toArray(new String[descriptors.keySet().size()]);
        return keys;
    }

    /* (non-Javadoc)
     * @see javax.jcr.Repository#getDescriptor(java.lang.String)
     */
    public String getDescriptor(String key)
    {
        return descriptors.get(key); 
    }

    /* (non-Javadoc)
     * @see javax.jcr.Repository#login(javax.jcr.Credentials, java.lang.String)
     */
    public Session login(Credentials credentials, String workspaceName)
        throws LoginException, NoSuchWorkspaceException, RepositoryException
    {
        // extract username and password
        // TODO: determine support for general Credentials
        String username = null;
        char[] password = EMPTY_PASSWORD;
        if (credentials != null && credentials instanceof SimpleCredentials)
        {
            username = ((SimpleCredentials)credentials).getUserID();
            password = ((SimpleCredentials)credentials).getPassword();
        }

        // authenticate user
        try
        {
            authenticationService.authenticate(username, password);
        }
        catch(AuthenticationException e)
        {
            throw new LoginException("Alfresco Repository failed to authenticate credentials", e);
        }

        try
        {
            // construct the session
            String ticket = authenticationService.getCurrentTicket();
            SessionImpl sessionImpl = new SessionImpl(this, ticket, workspaceName, getAttributes(credentials));
            Session session = (Session)SessionContextProxyFactory.create(sessionImpl, Session.class, sessionImpl);
    
            // clear the security context for this thread
            authenticationService.clearCurrentSecurityContext();

            // the session is ready
            return session;
        }
        catch(AlfrescoRuntimeException e)
        {
            throw new RepositoryException(e);
        }
    }

    /* (non-Javadoc)
     * @see javax.jcr.Repository#login(javax.jcr.Credentials)
     */
    public Session login(Credentials credentials)
        throws LoginException, RepositoryException
    {
        return login(credentials, null);
    }

    /* (non-Javadoc)
     * @see javax.jcr.Repository#login(java.lang.String)
     */
    public Session login(String workspaceName)
        throws LoginException, NoSuchWorkspaceException, RepositoryException
    {
        return login(null, workspaceName);
    }

    /* (non-Javadoc)
     * @see javax.jcr.Repository#login()
     */
    public Session login()
        throws LoginException, RepositoryException
    {
        return login(null, null);
    }

    /**
     * Get attributes from passed Credentials
     * 
     * @param credentials  the credentials to extract attribute from
     * @return  the attributes
     */
    private Map<String, Object> getAttributes(Credentials credentials)
    {
        Map<String, Object> attributes = null;
        if (credentials != null && credentials instanceof SimpleCredentials)
        {
            SimpleCredentials simpleCredentials = (SimpleCredentials)credentials;
            String[] names = simpleCredentials.getAttributeNames();
            attributes = new HashMap<String, Object>(names.length);
            for (String name : names)
            {
                attributes.put(name, simpleCredentials.getAttribute(name));
            }
        }
        return attributes;
    }
    
}
