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
package org.alfresco.cmis.mapping;

import java.io.Serializable;
import java.util.Collection;

import org.alfresco.repo.search.impl.lucene.LuceneQueryParser;
import org.alfresco.repo.search.impl.querymodel.PredicateMode;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.namespace.QName;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.BooleanClause.Occur;

/**
 * Common support for lucene query building.
 * 
 * @author andyh
 *
 */
public abstract class AbstractSimpleProperty extends AbstractProperty
{

    protected AbstractSimpleProperty(ServiceRegistry serviceRegistry, String propertyName)
    {
        super(serviceRegistry, propertyName);
    }

    protected abstract String getValueAsString(Serializable value);
    
    protected String getRangeMax()
    {
        return "\uFFFF";
    }
    
    protected String getRangeMin()
    {
        return "\u0000";
    }
    
    protected abstract DataTypeDefinition getInDataType();
    
    protected abstract QName getQNameForExists();
    
    /*
     * (non-Javadoc)
     * @see org.alfresco.cmis.property.PropertyLuceneBuilder#buildLuceneEquality(org.alfresco.repo.search.impl.lucene.LuceneQueryParser, java.io.Serializable, org.alfresco.repo.search.impl.querymodel.PredicateMode)
     */
    public Query buildLuceneEquality(LuceneQueryParser lqp, Serializable value, PredicateMode mode) throws ParseException
    {
        return lqp.getFieldQuery(getLuceneFieldName(), getValueAsString(value));
    }
    
    /*
     * (non-Javadoc)
     * @see org.alfresco.cmis.property.PropertyLuceneBuilder#buildLuceneExists(org.alfresco.repo.search.impl.lucene.LuceneQueryParser, java.lang.Boolean)
     */
    public Query buildLuceneExists(LuceneQueryParser lqp, Boolean not) throws ParseException
    {
        if (not)
        {
            return lqp.getFieldQuery("ISNULL", getQNameForExists().toString());
        }
        else
        {
            return lqp.getFieldQuery("ISNOTNULL", getQNameForExists().toString());
        }
    }

    /*
     * (non-Javadoc)
     * @see org.alfresco.cmis.property.PropertyLuceneBuilder#buildLuceneGreaterThan(org.alfresco.repo.search.impl.lucene.LuceneQueryParser, java.io.Serializable, org.alfresco.repo.search.impl.querymodel.PredicateMode)
     */
    public Query buildLuceneGreaterThan(LuceneQueryParser lqp, Serializable value, PredicateMode mode) throws ParseException
    {
        String field = getLuceneFieldName();
        String stringValue = getValueAsString(value);
        return lqp.getRangeQuery(field, stringValue, getRangeMax(), false, true);
    }

    /*
     * (non-Javadoc)
     * @see org.alfresco.cmis.property.PropertyLuceneBuilder#buildLuceneGreaterThanOrEquals(org.alfresco.repo.search.impl.lucene.LuceneQueryParser, java.io.Serializable, org.alfresco.repo.search.impl.querymodel.PredicateMode)
     */
    public Query buildLuceneGreaterThanOrEquals(LuceneQueryParser lqp, Serializable value, PredicateMode mode) throws ParseException
    {
        String field = getLuceneFieldName();
        String stringValue = getValueAsString(value);
        return lqp.getRangeQuery(field, stringValue, getRangeMax(), true, true);
    }

    /*
     * (non-Javadoc)
     * @see org.alfresco.cmis.property.PropertyLuceneBuilder#buildLuceneIn(org.alfresco.repo.search.impl.lucene.LuceneQueryParser, java.util.Collection, java.lang.Boolean, org.alfresco.repo.search.impl.querymodel.PredicateMode)
     */
    public Query buildLuceneIn(LuceneQueryParser lqp, Collection<Serializable> values, Boolean not, PredicateMode mode) throws ParseException
    {
        String field = getLuceneFieldName();
        
        // Check type conversion

        @SuppressWarnings("unused")
        Object converted = DefaultTypeConverter.INSTANCE.convert(getInDataType(), values);
        Collection<String> asStrings = DefaultTypeConverter.INSTANCE.convert(String.class, values);

        if (asStrings.size() == 0)
        {
            if (not)
            {
                return new MatchAllDocsQuery();
            }
            else
            {
                return new TermQuery(new Term("NO_TOKENS", "__"));
            }
        }
        else if (asStrings.size() == 1)
        {
            String value = asStrings.iterator().next();
            if (not)
            {
                return lqp.getDoesNotMatchFieldQuery(field, value);
            }
            else
            {
                return lqp.getFieldQuery(field, value);
            }
        }
        else
        {
            BooleanQuery booleanQuery = new BooleanQuery();
            if (not)
            {
                booleanQuery.add(new MatchAllDocsQuery(), Occur.MUST);
            }
            for (String value : asStrings)
            {
                Query any = lqp.getFieldQuery(field, value);
                if (not)
                {
                    booleanQuery.add(any, Occur.MUST_NOT);
                }
                else
                {
                    booleanQuery.add(any, Occur.SHOULD);
                }
            }
            return booleanQuery;
        }
    }

    /*
     * (non-Javadoc)
     * @see org.alfresco.cmis.property.PropertyLuceneBuilder#buildLuceneInequality(org.alfresco.repo.search.impl.lucene.LuceneQueryParser, java.io.Serializable, org.alfresco.repo.search.impl.querymodel.PredicateMode)
     */
    public Query buildLuceneInequality(LuceneQueryParser lqp, Serializable value, PredicateMode mode) throws ParseException
    {
        String field = getLuceneFieldName();
        String stringValue = getValueAsString(value);
        return lqp.getDoesNotMatchFieldQuery(field, stringValue);
    }

    /*
     * (non-Javadoc)
     * @see org.alfresco.cmis.property.PropertyLuceneBuilder#buildLuceneLessThan(org.alfresco.repo.search.impl.lucene.LuceneQueryParser, java.io.Serializable, org.alfresco.repo.search.impl.querymodel.PredicateMode)
     */
    public Query buildLuceneLessThan(LuceneQueryParser lqp, Serializable value, PredicateMode mode) throws ParseException
    {
        String field = getLuceneFieldName();
        String stringValue = getValueAsString(value);
        return lqp.getRangeQuery(field, getRangeMin(), stringValue, true, false);
    }

    /*
     * (non-Javadoc)
     * @see org.alfresco.cmis.property.PropertyLuceneBuilder#buildLuceneLessThanOrEquals(org.alfresco.repo.search.impl.lucene.LuceneQueryParser, java.io.Serializable, org.alfresco.repo.search.impl.querymodel.PredicateMode)
     */
    public Query buildLuceneLessThanOrEquals(LuceneQueryParser lqp, Serializable value, PredicateMode mode) throws ParseException
    {
        String field = getLuceneFieldName();
        String stringValue = getValueAsString(value);
        return lqp.getRangeQuery(field, getRangeMin(), stringValue, true, true);
    }

    /*
     * (non-Javadoc)
     * @see org.alfresco.cmis.property.PropertyLuceneBuilder#buildLuceneLike(org.alfresco.repo.search.impl.lucene.LuceneQueryParser, java.io.Serializable, java.lang.Boolean)
     */
    public Query buildLuceneLike(LuceneQueryParser lqp, Serializable value, Boolean not) throws ParseException
    {
        String field = getLuceneFieldName();
        String stringValue = getValueAsString(value);

        if (not)
        {
            BooleanQuery booleanQuery = new BooleanQuery();
            booleanQuery.add(new MatchAllDocsQuery(), Occur.MUST);
            booleanQuery.add(lqp.getLikeQuery(field, stringValue), Occur.MUST_NOT);
            return booleanQuery;
        }
        else
        {
            return lqp.getLikeQuery(field, stringValue);
        }
    }

    /*
     * (non-Javadoc)
     * @see org.alfresco.cmis.property.PropertyLuceneBuilder#getLuceneSortField()
     */
    public String getLuceneSortField()
    {
        return getLuceneFieldName();
    }
}
