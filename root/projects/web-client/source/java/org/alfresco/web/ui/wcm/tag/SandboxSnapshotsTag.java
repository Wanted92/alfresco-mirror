/*
 * Copyright (C) 2005 Alfresco, Inc.
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
package org.alfresco.web.ui.wcm.tag;

import javax.faces.component.UIComponent;

import org.alfresco.web.ui.common.tag.BaseComponentTag;

/**
 * @author Kevin Roast
 */
public class SandboxSnapshotsTag extends BaseComponentTag
{
   /**
    * @see javax.faces.webapp.UIComponentTag#getComponentType()
    */
   public String getComponentType()
   {
      return "org.alfresco.faces.SandboxSnapshots";
   }

   /**
    * @see javax.faces.webapp.UIComponentTag#getRendererType()
    */
   public String getRendererType()
   {
      return null;
   }
   
   /**
    * @see javax.faces.webapp.UIComponentTag#setProperties(javax.faces.component.UIComponent)
    */
   protected void setProperties(UIComponent component)
   {
      super.setProperties(component);
      
      setStringProperty(component, "value", this.value);
      setStringProperty(component, "dateFilter", this.dateFilter);
   }
   
   /**
    * @see org.alfresco.web.ui.common.tag.HtmlComponentTag#release()
    */
   public void release()
   {
      super.release();
      this.value = null;
      this.dateFilter = null;
   }
   
   /**
    * Set the value (sandbox to display snapshots for)
    *
    * @param value     the value
    */
   public void setValue(String value)
   {
      this.value = value;
   }
   
   /**
    * Set the dateFilter
    *
    * @param dateFilter     the dateFilter
    */
   public void setDateFilter(String dateFilter)
   {
      this.dateFilter = dateFilter;
   }


   /** the dateFilter */
   private String dateFilter;
   
   /** the value (sandbox to display snapshots for) */
   private String value;
}
