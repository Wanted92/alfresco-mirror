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
package org.alfresco.web.bean.workflow;

import java.util.List;

import javax.faces.context.FacesContext;

import org.alfresco.web.app.Application;
import org.alfresco.web.config.DialogsConfigElement.DialogButtonConfig;

/**
 * Bean implementation for the "View Completed Task" dialog.
 * 
 * @author gavinc
 */
public class ViewCompletedTaskDialog extends ManageTaskDialog
{
   // ------------------------------------------------------------------------------
   // Dialog implementation

   private static final long serialVersionUID = 1568710712589201055L;

   @Override
   protected String finishImpl(FacesContext context, String outcome)
         throws Exception
   {
      // nothing to do as the finish button is not shown and the dialog is read only
      
      return outcome;
   }

   @Override
   public String getCancelButtonLabel()
   {
      return Application.getMessage(FacesContext.getCurrentInstance(), "close");
   }
   
   @Override
   public List<DialogButtonConfig> getAdditionalButtons()
   {
      return null;
   }
   
   @Override
   public String getContainerTitle()
   {
      String titleStart = Application.getMessage(FacesContext.getCurrentInstance(), "view_completed_task_title");
         
      return titleStart + ": " + this.getWorkflowTask().title;
   }
}
