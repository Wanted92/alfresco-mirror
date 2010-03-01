package org.alfresco.filesys.alfresco;

import net.sf.acegisecurity.Authentication;

import org.alfresco.jlan.server.auth.ClientInfo;
import org.alfresco.service.cmr.repository.NodeRef;

/*
 * Copyright (C) 2007-2010 Alfresco Software Limited.
 *
 * This file is part of Alfresco
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 */

/**
 * Alfresco Client Information Class
 * 
 * <p>Contains additional fields used by the Alfresco filesystem drivers.
 */
public class AlfrescoClientInfo extends ClientInfo {

  // Authentication token
  
  private Authentication m_authToken;
  
  // Authentication ticket, used for web access without having to re-authenticate
  
  private String m_authTicket;
  
  // Home folder node
  
  private NodeRef m_homeNode;

  /**
   * Default constructor
   */
  public AlfrescoClientInfo()
  {
      super("", null);
  }
  
  /**
   * Class constructor
   * 
   * @param user User name
   * @param pwd Password
   */
  public AlfrescoClientInfo(String user, byte[] pwd)
  {
    super(user, pwd);
  }

  /**
   * Check if the client has an authentication token
   * 
   * @return boolean
   */
  public final boolean hasAuthenticationToken()
  {
      return m_authToken != null ? true : false;
  }
  
  /**
   * Return the authentication token
   * 
   * @return Authentication
   */
  public final Authentication getAuthenticationToken()
  {
      return m_authToken;
  }

  /**
   * Check if the client has an authentication ticket
   * 
   * @return boolean
   */
  public final boolean hasAuthenticationTicket()
  {
    return m_authTicket != null ? true : false;
  }
  
  /**
   * Return the authentication ticket
   * 
   * @return String
   */
  public final String getAuthenticationTicket()
  {
    return m_authTicket;
  }
  
  /**
   * Check if the client has a home folder node
   * 
   * @return boolean
   */
  public final boolean hasHomeFolder()
  {
      return m_homeNode != null ? true : false;
  }
  
  /**
   * Return the home folder node
   * 
   * @return NodeRef
   */
  public final NodeRef getHomeFolder()
  {
      return m_homeNode;
  }

  /**
   * Set the authentication toekn
   * 
   * @param token Authentication
   */
  public final void setAuthenticationToken(Authentication token)
  {
      m_authToken = token;
  }

  /**
   * Set the authentication ticket
   * 
   * @param ticket String
   */
  public final void setAuthenticationTicket(String ticket)
  {
    m_authTicket = ticket;
  }
  
  /**
   * Set the home folder node
   * 
   * @param homeNode NodeRef
   */
  public final void setHomeFolder(NodeRef homeNode)
  {
      m_homeNode = homeNode;
  }
  
}
