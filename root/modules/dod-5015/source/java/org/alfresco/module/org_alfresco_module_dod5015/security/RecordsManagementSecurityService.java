/*
 * Copyright (C) 2005-2008 Alfresco Software Limited.
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
 * http://www.alfresco.com/legal/licensing"
 */
package org.alfresco.module.org_alfresco_module_dod5015.security;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.module.org_alfresco_module_dod5015.capability.Capability;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.AccessStatus;
import org.alfresco.service.namespace.QName;

/**
 * Records management permission service interface
 * 
 * @author Roy Wetherall
 */
public interface RecordsManagementSecurityService
{    
    /**
     * Get a list of the capabilities available
     * 
     * @return  List<Capability>    list of capabilities available
     */
    Set<Capability> getCapabilities();
    
    /**
     * Get the full set of capabilities for the current user.
     * @param nodeRef
     * @return
     */
    Map<Capability, AccessStatus> getCapabilities(NodeRef nodeRef);
    
    /**
     * Get a capability by name
     * @param name
     * @return
     */
    Capability getCapability(String name);    
    
    /**
     * Get the set of aspect QNames which can not be added direct via the public node service;
     * they must be managed via the appropriate actions.
     * @return
     */
    Set<QName> getProtectedAspects();
    
    /**
     * Get the set of property QNames which can not be added, updated or removed direct via the public node service;
     * they must be managed via the appropriate actions.
     * @return
     */
    Set<QName> getProtectedProperties();
    
    /**
     * Creates the initial set of default roles for a root records management node
     * 
     * @param rmRootNode  
     */
    void bootstrapDefaultRoles(NodeRef rmRootNode);
    
    /**
     * Get all the available roles for the given records management root node
     * 
     * @param rmRootNode
     * @return
     */
    Set<Role> getRoles(NodeRef rmRootNode);
    
    /**
     * Gets the roles for a given user
     * 
     * @param rmRootNode
     * @param user
     * @return
     */
    Set<Role> getRolesByUser(NodeRef rmRootNode, String user);
    
    /**
     * Get a role by name
     * 
     * @param rmRootNode
     * @param role
     * @return
     */
    Role getRole(NodeRef rmRootNode, String role);    
    
    /**
     * Indicate whether a role exists for a given records management root node
     * @param rmRootNode
     * @param role
     * @return
     */
    boolean existsRole(NodeRef rmRootNode, String role);
    
    /**
     * Create a new role
     * 
     * @param rmRootNode
     * @param role
     * @param roleDisplayLabel
     * @param capabilities
     * @return
     */
    Role createRole(NodeRef rmRootNode, String role, String roleDisplayLabel, Set<Capability> capabilities);
    
    /**
     * Update an existing role
     * 
     * @param rmRootNode
     * @param role
     * @param roleDisplayLabel
     * @param capabilities
     * @return
     */
    Role updateRole(NodeRef rmRootNode, String role, String roleDisplayLabel, Set<Capability> capabilities);
    
    /**
     * Delete a role
     * 
     * @param rmRootNode
     * @param role
     */
    void deleteRole(NodeRef rmRootNode, String role);
    
    /**
     * Assign a role to an authority
     * 
     * @param authorityName
     * @param rmRootNode
     * @param role
     */
    void assignRoleToAuthority(NodeRef rmRootNode, String role, String authorityName);
}
