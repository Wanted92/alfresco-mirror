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
package org.alfresco.opencmis.mapping;

import java.io.Serializable;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.chemistry.opencmis.commons.PropertyIds;

/**
 * Get the CMIS version series checked out property
 * 
 * @author dward
 */
public class IsVersionSeriesCheckedOutProperty extends AbstractVersioningProperty
{
    /**
     * Construct
     */
    public IsVersionSeriesCheckedOutProperty(ServiceRegistry serviceRegistry)
    {
        super(serviceRegistry, PropertyIds.IS_VERSION_SERIES_CHECKED_OUT);
    }

    @Override
    public Serializable getValue(NodeRef nodeRef)
    {
        return isWorkingCopy(nodeRef) || hasWorkingCopy(getVersionSeries(nodeRef));
    }
}
