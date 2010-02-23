/*
 * Copyright (C) 2005-2009 Alfresco Software Limited.
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
package org.alfresco.cmis.acl;

import org.alfresco.cmis.CMISAccessControlEntry;

/**
 * @author andyh
 *
 */
public class CMISAccessControlEntryImpl implements CMISAccessControlEntry
{
    private String principalId;
    
    private String permission;
   
    private int position;
    
    /*package*/ CMISAccessControlEntryImpl(String principalId, String permission, int position)
    {
        this.principalId = principalId;
        this.permission = permission;
        this.position = position;
    }
    
    /**
     * Can be used for external calls to add or delete aces.
     * (All  must be at position 0 === directly set on the object)
     * 
     * @param principalId
     * @param permission
     */
    public CMISAccessControlEntryImpl(String principalId, String permission)
    {
        this(principalId, permission, 0);
    }

    /* (non-Javadoc)
     * @see org.alfresco.cmis.CMISAccessControlEntry#getDirect()
     */
    public boolean getDirect()
    {
        return position == 0;
    }

    /* (non-Javadoc)
     * @see org.alfresco.cmis.CMISAccessControlEntry#getPermission()
     */
    public String getPermission()
    {
        return permission;
    }

    /* (non-Javadoc)
     * @see org.alfresco.cmis.CMISAccessControlEntry#getPrincipalId()
     */
    public String getPrincipalId()
    {
       return principalId;
    }

    /**
     * @return the position
     */
    public int getPosition()
    {
        return position;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((permission == null) ? 0 : permission.hashCode());
        result = prime * result + position;
        result = prime * result + ((principalId == null) ? 0 : principalId.hashCode());
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final CMISAccessControlEntryImpl other = (CMISAccessControlEntryImpl) obj;
        if (permission == null)
        {
            if (other.permission != null)
                return false;
        }
        else if (!permission.equals(other.permission))
            return false;
        if (position != other.position)
            return false;
        if (principalId == null)
        {
            if (other.principalId != null)
                return false;
        }
        else if (!principalId.equals(other.principalId))
            return false;
        return true;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        builder.append(getPrincipalId()).append(", ");
        builder.append(getPermission()).append(", ");
        builder.append(getPosition()).append(", ");
        builder.append(getDirect()).append("]");
        return builder.toString();
    }

    

    
}
