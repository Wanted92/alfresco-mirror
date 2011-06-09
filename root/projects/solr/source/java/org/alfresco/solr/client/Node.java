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

public class Node
{
    public static enum STATUS
    {
        UPDATED, DELETED, UNKNOWN;
    };

    private long id;
    private long txnId;
    private STATUS status;
    public long getId()
    {
        return id;
    }
    public void setId(long id)
    {
        this.id = id;
    }
    public long getTxnId()
    {
        return txnId;
    }
    public void setTxnId(long txnId)
    {
        this.txnId = txnId;
    }
    public STATUS getStatus()
    {
        return status;
    }
    public void setStatus(STATUS status)
    {
        this.status = status;
    }
    @Override
    public String toString()
    {
        return "NodeInfo [id=" + id + ", txnId=" + txnId + ", status=" + status + "]";
    }
}