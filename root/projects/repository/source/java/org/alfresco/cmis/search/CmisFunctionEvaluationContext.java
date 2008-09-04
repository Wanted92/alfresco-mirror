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
 * http://www.alfresco.com/legal/licensing"
 */
package org.alfresco.cmis.search;

import java.io.Serializable;
import java.util.Map;

import org.alfresco.cmis.dictionary.CMISDictionaryService;
import org.alfresco.cmis.property.CMISPropertyService;
import org.alfresco.repo.search.impl.querymodel.FunctionEvaluationContext;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;

/**
 * @author andyh
 */
public class CmisFunctionEvaluationContext implements FunctionEvaluationContext
{
    private Map<String, NodeRef> nodeRefs;

    private Map<String, Float> scores;

    private NodeService nodeService;

    private CMISPropertyService cmisPropertyService;

    private CMISDictionaryService cmisDictionaryService;

    
    
    /**
     * @param nodeRefs the nodeRefs to set
     */
    protected void setNodeRefs(Map<String, NodeRef> nodeRefs)
    {
        this.nodeRefs = nodeRefs;
    }

    /**
     * @param scores the scores to set
     */
    protected void setScores(Map<String, Float> scores)
    {
        this.scores = scores;
    }

    /**
     * @param nodeService the nodeService to set
     */
    protected void setNodeService(NodeService nodeService)
    {
        this.nodeService = nodeService;
    }

    /**
     * @param cmisPropertyService the cmisPropertyService to set
     */
    protected void setCmisPropertyService(CMISPropertyService cmisPropertyService)
    {
        this.cmisPropertyService = cmisPropertyService;
    }

    /**
     * @param cmisDictionaryService the cmisDictionaryService to set
     */
    protected void setCmisDictionaryService(CMISDictionaryService cmisDictionaryService)
    {
        this.cmisDictionaryService = cmisDictionaryService;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.alfresco.repo.search.impl.querymodel.FunctionEvaluationContext#getNodeRefs()
     */
    public Map<String, NodeRef> getNodeRefs()
    {
        return nodeRefs;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.alfresco.repo.search.impl.querymodel.FunctionEvaluationContext#getNodeService()
     */
    public NodeService getNodeService()
    {
        return nodeService;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.alfresco.repo.search.impl.querymodel.FunctionEvaluationContext#getProperty(org.alfresco.service.cmr.repository.NodeRef,
     *      org.alfresco.service.namespace.QName)
     */
    public Serializable getProperty(NodeRef nodeRef, QName propertyQName)
    {
        String propertyName = cmisDictionaryService.getCMISMapping().getCmisPropertyName(propertyQName);
        return cmisPropertyService.getProperty(nodeRef, propertyName);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.alfresco.repo.search.impl.querymodel.FunctionEvaluationContext#getScores()
     */
    public Map<String, Float> getScores()
    {
        return scores;
    }

}
