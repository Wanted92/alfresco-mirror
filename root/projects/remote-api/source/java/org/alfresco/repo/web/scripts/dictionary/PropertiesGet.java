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
package org.alfresco.repo.web.scripts.dictionary;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.namespace.QName;
import org.apache.commons.collections.comparators.ComparatorChain;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;

/**
 * 
 * Webscript to get the Propertydefinitions for a given classname eg. =>cm_person
 * 
 * @author Saravanan Sellathurai
 */

public class PropertiesGet extends DictionaryWebServiceBase
{
	private static final String MODEL_PROP_KEY_PROPERTY_DETAILS = "propertydefs";
	private static final String DICTIONARY_CLASS_NAME = "classname";
	private static final String PARAM_NAME = "name";
	private static final String REQ_URL_TEMPL_VAR_NAMESPACE_PREFIX = "nsp";
	
	/**
     * @Override  method from DeclarativeWebScript 
     */
    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache)
    {
        QName classQName = null;
        String className = req.getServiceMatch().getTemplateVars().get(DICTIONARY_CLASS_NAME);
        if (className != null && className.length() != 0)
        {            
            classQName = createClassQName(className);
            if (classQName == null)
            {
                // Error 
                throw new WebScriptException(Status.STATUS_NOT_FOUND, "Check the className - " + className + " - parameter in the URL");
            }
        }
        
        String[] names = req.getParameterValues(PARAM_NAME);
        
        String namespacePrefix = req.getParameter(REQ_URL_TEMPL_VAR_NAMESPACE_PREFIX);        
        String namespaceURI = null;
        if (namespacePrefix != null)
        {
            namespaceURI = this.namespaceService.getNamespaceURI(namespacePrefix);
        }
        
        Map<QName, PropertyDefinition> propMap = null;
        if (classQName == null)
        {
            if (names != null)
            {
                propMap = new HashMap<QName, PropertyDefinition>(names.length);
                for (String name : names)
                {
                    QName propQName = QName.createQName(name, namespaceService);
                    PropertyDefinition propDef = dictionaryservice.getProperty(propQName);
                    if (propDef != null)
                    {
                        propMap.put(propQName, propDef);
                    }
                }
            }
            else
            {
                Collection<QName> propQNames = dictionaryservice.getAllProperties(null);
                propMap = new HashMap<QName, PropertyDefinition>(propQNames.size());
                for (QName propQName : propQNames)
                {
                    propMap.put(propQName, dictionaryservice.getProperty(propQName));
                }
            }
            
        }
        else
        {
            // Get all the property definitions for the class
            propMap = dictionaryservice.getClass(classQName).getProperties();            
        }
        
        // Filter the properties by URI
        List<PropertyDefinition> props = new ArrayList<PropertyDefinition>(propMap.size());
        for (Map.Entry<QName, PropertyDefinition> entry : propMap.entrySet())
        {
            if ((namespaceURI != null && 
                 namespaceURI.equals(entry.getKey().getNamespaceURI()) == true) ||
                namespaceURI == null)
            {
                props.add(entry.getValue());   
            }
        }
        
        // Order property definitions by title
        Collections.sort(props, new PropertyDefinitionComparator());
        
        // Pass list of property definitions to template
        Map<String, Object> model = new HashMap<String, Object>();
        model.put(MODEL_PROP_KEY_PROPERTY_DETAILS, props);
        return model;
         
    }
    
    /**
     * Property definition comparator.
     * 
     * Used to order property definitions by title.
     */
    private class PropertyDefinitionComparator implements Comparator<PropertyDefinition>
    {
        public int compare(PropertyDefinition arg0, PropertyDefinition arg1)
        {
            int result = 0;
            
            String title0 = arg0.getTitle();
            String title1 = arg1.getTitle();
            
            if (title0 == null && title1 != null)
            {
                result = 1;
            }
            else if (title0 != null && title1 == null)
            {
                result = -1;
            }
            else if (title0 != null && title1 != null)
            {
                result = String.CASE_INSENSITIVE_ORDER.compare(arg0.getTitle(), arg1.getTitle());
            }
            
            return result;
        }        
    }
   
}