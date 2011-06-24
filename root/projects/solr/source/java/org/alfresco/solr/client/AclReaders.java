/*
 * Copyright (C) 2005-2011 Alfresco Software Limited.
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
package org.alfresco.solr.client;

import java.util.List;

/**
 * SOLR-side representation of ACL Readers information.
 * 
 * @since 4.0
 */
public class AclReaders
{
    private final long id;
    private final List<String> readers;

    public AclReaders(long id, List<String> readers)
    {
        this.id = id;
        this.readers = readers;
    }

    @Override
    public String toString()
    {
        return "AclReaders [id=" + id + ", readers=" + readers + "]";
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (id ^ (id >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        AclReaders other = (AclReaders) obj;
        if (id != other.id) return false;
        return true;
    }

    public long getId()
    {
        return id;
    }

    public List<String> getReaders()
    {
        return readers;
    }
}