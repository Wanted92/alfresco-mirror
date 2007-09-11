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
package org.alfresco.web.bean.wcm;

import javax.faces.context.FacesContext;

import org.alfresco.model.ApplicationModel;
import org.alfresco.model.ContentModel;
import org.alfresco.model.WCMAppModel;
import org.alfresco.repo.avm.AVMNodeConverter;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.repo.domain.PropertyValue;

/**
 * Bean implementation for the AVM "Create Webapp Folder" dialog.
 * 
 * @author Kevin Roast
 */
public class CreateWebappDialog extends CreateFolderDialog
{
   // ------------------------------------------------------------------------------
   // Dialog implementation
   
   /**
    * @see org.alfresco.web.bean.dialog.BaseDialogBean#finishImpl(javax.faces.context.FacesContext, java.lang.String)
    */
   @Override
   protected String finishImpl(FacesContext context, String outcome) throws Exception
   {
      final String stagingStore = this.avmBrowseBean.getStagingStore();
      final String parent = AVMUtil.buildSandboxRootPath( stagingStore );
      this.avmService.createDirectory(parent, this.name);
      
      final String path = AVMNodeConverter.ExtendAVMPath(parent, this.name);
      this.avmService.addAspect(path, ApplicationModel.ASPECT_UIFACETS);
      this.avmService.addAspect(path, WCMAppModel.ASPECT_WEBAPP);
      if (this.description != null && this.description.length() != 0)
      {
         this.avmService.setNodeProperty(path, 
                                         ContentModel.PROP_DESCRIPTION, 
                                         new PropertyValue(DataTypeDefinition.TEXT,
                                                           this.description));
      }

      // Snapshot the store with the empty webapp
      this.avmService.createSnapshot( stagingStore, null, null);
      
      return outcome;
   }
}
