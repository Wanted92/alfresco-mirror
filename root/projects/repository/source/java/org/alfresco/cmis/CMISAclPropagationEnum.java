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
package org.alfresco.cmis;

/**
 * CMIS ACL propagation
 * 
 * Used to request a particular behaviour or report back behaviour.
 * 
 * @author andyh
 *
 */
public enum CMISAclPropagationEnum  implements EnumLabel
{
    /**
     * The ACL only applies to the object
     * (not yet supported in Alfresco)
     */
    OBJECT_ONLY("objectonly"),
    /**
     * ACLs are applied to all inheriting objects
     * (the default in Alfresco)
     */
    PROPAGATE("propagate"),
    /**
     * Some other mechanism by which ACL changes influence other ACL's non-direct ACEs.
     */
    REPOSITORY_DETERMINED("repositorydetermined");
    
    private String label;

    /**
     * Construct
     * 
     * @param label
     */
    CMISAclPropagationEnum(String label)
    {
        this.label = label;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.alfresco.cmis.EnumLabel#label()
     */
    public String getLabel()
    {
        return label;
    }

    /**
     * Factory for CMISAclPropagationEnum
     */
    public static EnumFactory<CMISAclPropagationEnum> FACTORY = new EnumFactory<CMISAclPropagationEnum>(CMISAclPropagationEnum.class, PROPAGATE, true);

}