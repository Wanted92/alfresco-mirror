/*
 * Copyright (C) 2005-2010 Alfresco Software Limited.
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
package org.alfresco.repo.domain.qname.ibatis;

import org.alfresco.repo.domain.qname.AbstractQNameDAOImpl;
import org.alfresco.repo.domain.qname.NamespaceEntity;
import org.alfresco.repo.domain.qname.QNameEntity;
import org.springframework.orm.ibatis.SqlMapClientTemplate;

/**
 * iBatis-specific extension of the QName and Namespace abstract DAO 
 * 
 * @author Derek Hulley
 * @since 3.3
 */
public class QNameDAOImpl extends AbstractQNameDAOImpl
{
    private static final String SELECT_NS_BY_ID = "alfresco.qname.select_NamespaceById";
    private static final String SELECT_NS_BY_URI = "alfresco.qname.select_NamespaceByUri";
    private static final String INSERT_NS = "alfresco.qname.insert_Namespace";
    private static final String UPDATE_NS = "alfresco.qname.update_Namespace";
    private static final String SELECT_QNAME_BY_ID = "alfresco.qname.select_QNameById";
    private static final String SELECT_QNAME_BY_NS_AND_LOCALNAME = "alfresco.qname.select_QNameByNsAndLocalName";
    private static final String INSERT_QNAME = "alfresco.qname.insert_QName";
    private static final String UPDATE_QNAME = "alfresco.qname.update_QName";

    private SqlMapClientTemplate template;

    public void setSqlMapClientTemplate(SqlMapClientTemplate sqlMapClientTemplate)
    {
        this.template = sqlMapClientTemplate;
    }

    @Override
    protected NamespaceEntity findNamespaceEntityById(Long id)
    {
        NamespaceEntity entity = new NamespaceEntity();
        entity.setId(id);
        entity = (NamespaceEntity) template.queryForObject(SELECT_NS_BY_ID, entity);
        return entity;
    }

    @Override
    protected NamespaceEntity findNamespaceEntityByUri(String uri)
    {
        NamespaceEntity entity = new NamespaceEntity();
        entity.setUriSafe(uri);
        entity = (NamespaceEntity) template.queryForObject(SELECT_NS_BY_URI, entity);
        return entity;
    }

    @Override
    protected NamespaceEntity createNamespaceEntity(String uri)
    {
        NamespaceEntity entity = new NamespaceEntity();
        entity.setVersion(NamespaceEntity.CONST_LONG_ZERO);
        entity.setUriSafe(uri);
        template.insert(INSERT_NS, entity);
        return entity;
    }
    
    @Override
    protected int updateNamespaceEntity(NamespaceEntity entity, String uri)
    {
        entity.setUriSafe(uri);
        entity.incrementVersion();
        return template.update(UPDATE_NS, entity);
    }

    @Override
    protected QNameEntity findQNameEntityById(Long id)
    {
        QNameEntity entity = new QNameEntity();
        entity.setId(id);
        entity = (QNameEntity) template.queryForObject(SELECT_QNAME_BY_ID, entity);
        return entity;
    }
    
    @Override
    protected QNameEntity findQNameEntityByNamespaceAndLocalName(Long nsId, String localName)
    {
        QNameEntity entity = new QNameEntity();
        entity.setNamespaceId(nsId);
        entity.setLocalNameSafe(localName);
        entity = (QNameEntity) template.queryForObject(SELECT_QNAME_BY_NS_AND_LOCALNAME, entity);
        return entity;
    }

    @Override
    protected QNameEntity createQNameEntity(Long nsId, String localName)
    {
        QNameEntity entity = new QNameEntity();
        entity.setVersion(QNameEntity.CONST_LONG_ZERO);
        entity.setNamespaceId(nsId);
        entity.setLocalNameSafe(localName);
        template.insert(INSERT_QNAME, entity);
        return entity;
    }
    
    @Override
    protected int updateQNameEntity(QNameEntity entity, Long nsId, String localName)
    {
        entity.setNamespaceId(nsId);
        entity.setLocalNameSafe(localName);
        entity.incrementVersion();
        return template.update(UPDATE_QNAME, entity);
    }
}