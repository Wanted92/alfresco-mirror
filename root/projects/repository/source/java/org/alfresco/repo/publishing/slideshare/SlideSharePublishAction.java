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
package org.alfresco.repo.publishing.slideshare;

import java.io.File;
import java.util.List;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.repo.content.filestore.FileContentReader;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.tagging.TaggingService;
import org.alfresco.util.Pair;
import org.alfresco.util.TempFileProvider;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.benfante.jslideshare.SlideShareAPI;

public class SlideSharePublishAction extends ActionExecuterAbstractBase
{
    private final static Log log = LogFactory.getLog(SlideSharePublishAction.class);

    public static final String NAME = "publish_slideshare";

    private NodeService nodeService;
    private ContentService contentService;
    private TaggingService taggingService;
    private SlideSharePublishingHelper slideShareHelper;

    public void setSlideShareHelper(SlideSharePublishingHelper slideShareHelper)
    {
        this.slideShareHelper = slideShareHelper;
    }

    public void setNodeService(NodeService nodeService)
    {
        this.nodeService = nodeService;
    }

    public void setContentService(ContentService contentService)
    {
        this.contentService = contentService;
    }

    public void setTaggingService(TaggingService taggingService)
    {
        this.taggingService = taggingService;
    }

    @Override
    protected void executeImpl(Action action, NodeRef nodeRef)
    {
        SlideShareAPI api = slideShareHelper.getSlideShareApi();
        Pair<String,String> usernamePassword = slideShareHelper.getSlideShareCredentialsForNode(nodeRef);
        if (api == null || usernamePassword == null)
        {
            throw new AlfrescoRuntimeException("publish.failed.unable_to_connect_to_service_provider");
        }
        
        ContentReader reader = contentService.getReader(nodeRef, ContentModel.PROP_CONTENT);
        if (reader.exists())
        {
            File contentFile;
            boolean deleteContentFileOnCompletion = false;
            if (FileContentReader.class.isAssignableFrom(reader.getClass()))
            {
                //Grab the content straight from the content store if we can...
                contentFile = ((FileContentReader)reader).getFile();
            }
            else
            {
                //...otherwise copy it to a temp file and use the copy... 
                File tempDir = TempFileProvider.getLongLifeTempDir("slideshare");
                contentFile = TempFileProvider.createTempFile("slideshare", "", tempDir);
                reader.getContent(contentFile);
                deleteContentFileOnCompletion = true;
            }

            String name = (String) nodeService.getProperty(nodeRef, ContentModel.PROP_NAME);
            String title = (String) nodeService.getProperty(nodeRef, ContentModel.PROP_TITLE);
            if (title == null || title.length() == 0)
            {
                title = name;
            }
            String description = (String) nodeService.getProperty(nodeRef, ContentModel.PROP_DESCRIPTION);
            if (description == null || description.length() == 0)
            {
                description = title;
            }

            List<String> tagList = taggingService.getTags(nodeRef);
            StringBuilder tags = new StringBuilder();
            for (String tag : tagList)
            {
                tags.append(tag);
                tags.append(' ');
            }

            String assetId = api.uploadSlideshow(usernamePassword.getFirst(), usernamePassword.getSecond(), title, 
                    contentFile, description, tags.toString(), false, false, false, false, false);
            nodeService.setProperty(nodeRef, SlideSharePublishingModel.PROP_ASSET_ID, assetId);
            
            if (deleteContentFileOnCompletion)
            {
                contentFile.delete();
            }
        }
    }


    @Override
    protected void addParameterDefinitions(List<ParameterDefinition> paramList)
    {
    }
}