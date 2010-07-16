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

package org.alfresco.repo.forms.processor.workflow;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;

import static org.alfresco.repo.forms.processor.node.FormFieldConstants.*;

/**
 * @author Nick Smith
 *
 */
public class DataKeyMatcher
{
    /**
     * A regular expression which can be used to match property names. These
     * names will look like <code>"prop_cm_name"</code>. The pattern can also be
     * used to extract the "cm" and the "name" parts.
     */
    private final static Pattern propertyNamePattern = Pattern.compile("(^[a-zA-Z0-9]+)_([a-zA-Z0-9_]+$)");

    public DataKeyMatcher(NamespaceService namespaceService)
    {
        this.namespaceService = namespaceService;
    }

    /**
     * A regular expression which can be used to match association names. These
     * names will look like <code>"assoc_cm_references_added"</code>. The
     * pattern can also be used to extract the "cm", the "name" and the suffix
     * parts.
     */
    private final static Pattern associationNamePattern = Pattern.compile("(^[a-zA-Z0-9]+)_([a-zA-Z0-9_]+)(_[a-zA-Z]+$)");

    private final NamespaceService namespaceService;
    
    /**
     * 
     * @param dataKey
     * @return
     */
    public DataKeyInfo match(String dataKey)
    {
        if (dataKey.startsWith(PROP_DATA_PREFIX))
        {
            return matchProperty(dataKey);
        }
        else if(dataKey.startsWith(ASSOC_DATA_PREFIX))
        {
            return matchAssociation(dataKey);
        }
        
        // No match found.
        return null;
    }

    private DataKeyInfo matchAssociation(String dataKey)
    {
        String keyName = dataKey.substring(ASSOC_DATA_PREFIX.length());
        Matcher matcher = associationNamePattern.matcher(keyName);
        if (!matcher.matches())
        {
            return null;
        }
        QName qName = getQName(matcher);
        String suffix = matcher.group(3);
        boolean isAdd = !(ASSOC_DATA_REMOVED_SUFFIX.equals(suffix));
        return DataKeyInfo.makeAssociationDataKeyInfo(keyName, qName, isAdd);
    }

    private DataKeyInfo matchProperty(String dataKey)
    {
        String keyName = dataKey.substring(PROP_DATA_PREFIX.length());
        Matcher matcher = propertyNamePattern.matcher(keyName);
        if (matcher.matches())
        {
            QName qName = getQName(matcher);
            return DataKeyInfo.makePropertyDataKeyInfo(keyName, qName);
        }
        return DataKeyInfo.makeTransientDataKeyInfo(keyName);
    }

    private QName getQName(Matcher matcher)
    {
        String prefix = matcher.group(1);
        String localName = matcher.group(2);
        QName qName = QName.createQName(prefix, localName, namespaceService);
        return qName;
    }
}