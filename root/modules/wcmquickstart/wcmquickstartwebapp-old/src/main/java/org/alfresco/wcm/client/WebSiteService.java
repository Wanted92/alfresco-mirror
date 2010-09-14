/*
 * Copyright (C) 2005-2010 Alfresco Software Limited.
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
package org.alfresco.wcm.client;

import java.util.Collection;

/**
 * Web Site Service Interface
 * 
 * @author Roy Wetherall
 */
public interface WebSiteService 
{
	/**
	 * Gets all the web sites hosted on the repository
	 * 
	 * @return	Collection<WebSite>		web sites
	 */
	Collection<WebSite> getWebSites();
	
	/**
	 * Gets the web site that relates to the host name and port.
	 * 
	 * @param hostName	host name
	 * @param hostPort  port number
	 * @return WebSite	web site, null if non found
	 */
	WebSite getWebSite(String hostName, int hostPort);	
}