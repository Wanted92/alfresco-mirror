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
package org.alfresco.repo.dictionary;

import java.util.Map;

import org.alfresco.service.cmr.dictionary.AssociationDefinition;
import org.alfresco.service.cmr.dictionary.ModelDefinition;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.dictionary.TypeDefinition;
import org.alfresco.service.namespace.NamespacePrefixResolver;
import org.alfresco.service.namespace.QName;


/**
 * Compiled Type Definition
 * 
 * @author David Caruana
 */
/*package*/ class M2TypeDefinition extends M2ClassDefinition
    implements TypeDefinition
{
    /*package*/ M2TypeDefinition(ModelDefinition model, M2Type m2Type, NamespacePrefixResolver resolver, Map<QName, PropertyDefinition> modelProperties, Map<QName, AssociationDefinition> modelAssociations)
    {
        super(model, m2Type, resolver, modelProperties, modelAssociations); 
    }
    
    @Override
    public String getDescription()
    {
        String value = M2Label.getLabel(model, "type", name, "description");
        
        // if we don't have a description call the super class
        if (value == null)
        {
           value = super.getDescription();
        }
        
        return value;
    }

    @Override
    public String getTitle()
    {
        String value = M2Label.getLabel(model, "type", name, "title");
        
        // if we don't have a title call the super class
        if (value == null)
        {
           value = super.getTitle();
        }
        
        return value;
   }
}