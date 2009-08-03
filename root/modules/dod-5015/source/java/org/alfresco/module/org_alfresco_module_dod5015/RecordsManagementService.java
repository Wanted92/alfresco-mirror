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
package org.alfresco.module.org_alfresco_module_dod5015;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.module.org_alfresco_module_dod5015.capability.Capability;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.AccessStatus;
import org.alfresco.service.namespace.QName;

/**
 * Records management service interface
 * 
 * @author Roy Wetherall
 */
public interface RecordsManagementService
{
    /**
     * Gets the records management root node for the file plan component specified
     * 
     * @return  NodeRef records management root
     */
    NodeRef getRecordsManagementRoot(NodeRef nodeRef);
    
    /**
     * Indicates whether the given node is a record or not.
     * 
     * @param nodeRef   node reference
     * @return boolean  true if record, false otherwise
     */
    boolean isRecord(NodeRef nodeRef);
    
    /**
     * Indicates whether the record is declared
     * 
     * @param nodeRef   node reference (record)
     * @return boolean  true if record is declared, false otherwise
     */
    boolean isRecordDeclared(NodeRef nodeRef);
    
    /**
     * Indicates whether the contents of a record folder are all declared.
     * 
     * @param nodeRef   node reference (record folder)
     * @return boolean  true if record folder contents are declared, false otherwise
     */
    boolean isRecordFolderDeclared(NodeRef nodeRef);
    
    /**
     * Indicates whether the given node is a record folder or not.
     * 
     * @param nodeRef   node reference
     * @return boolean  true if record folder, false otherwise
     */
    boolean isRecordFolder(NodeRef nodeRef);
    
    /**
     * Indicates whether the given node is a record management container of not.
     * 
     * @param nodeRef   node reference
     * @return boolean  true if records management container
     */
    boolean isRecordsManagementContainer(NodeRef nodeRef);
    
    /**
     * Get all the record folders that a record is filed into.
     * 
     * @param record            the record node reference
     * @return List<NodeRef>    list of folder record node references
     */
    List<NodeRef> getRecordFolders(NodeRef record);
    
    /**
     * 
     * @param recordFolder
     * @return
     */
    List<NodeRef> getRecords(NodeRef recordFolder);
    
    /**
     * Get the disposition schedule for a given record management node.
     * 
     * @param nodeRef                   node reference to rm container, record folder or record
     * @return DispositionInstructions  disposition instructions
     */
    DispositionSchedule getDispositionSchedule(NodeRef nodeRef);
    
    /**
     * Adds a new disposition action definition to the given disposition schedule.
     * 
     * @param schedule The DispositionSchedule to add to
     * @param actionDefinitionParams Map of parameters to use to create the action definition
     */
    DispositionActionDefinition addDispositionActionDefinition(DispositionSchedule schedule, 
                Map<QName, Serializable> actionDefinitionParams);
    
    /**
     * Removes the given disposition action definition from the given disposition
     * schedule.
     * 
     * @param schedule The DispositionSchedule to remove from
     * @param actionDefinition The DispositionActionDefinition to remove
     */
    void removeDispositionActionDefinition(DispositionSchedule schedule, 
                DispositionActionDefinition actionDefinition);
    
    /**
     * Updates the given disposition action definition belonging to the given disposition
     * schedule.
     * 
     * @param schedule The DispositionSchedule the action belongs to
     * @param actionDefinition The DispositionActionDefinition to update
     * @param actionDefinitionParams Map of parameters to use to update the action definition
     * @return The updated DispositionActionDefinition
     */
    DispositionActionDefinition updateDispositionActionDefinition(DispositionSchedule schedule, 
                DispositionActionDefinition actionDefinition,
                Map<QName, Serializable> actionDefinitionParams);
    
    
    /**
     * TODO MOVE THIS FROM THIS API
     * 
     * @param nodeRef
     * @return
     */
    boolean isNextDispositionActionEligible(NodeRef nodeRef);
    
    /**
     * 
     * @param nodeRef
     * @return
     */
    DispositionAction getNextDispositionAction(NodeRef nodeRef);
    
    // TODO List<DispositionAction> getDispositionActionHistory(NodeRef nodeRef);
    // TODO boolean isDispositionActionEligible(DispositionAction nodeRef); ???  (move to disposition action object?)  
    
    /**
     * Get the vital record definition for a given node reference within the file plan
     * 
     * @param nodeRef               node reference to a container, record folder or record
     * @return VitalRecordDetails   vital record details, null if none
     */
    VitalRecordDefinition getVitalRecordDefinition(NodeRef nodeRef);  
    
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
}
