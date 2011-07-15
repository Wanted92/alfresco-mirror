/*
 * Copyright (C) 2005-2010 Alfresco Software Limited.
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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.publishing.EnvironmentImpl;
import org.alfresco.repo.publishing.PublishingModel;
import org.alfresco.repo.publishing.PublishingQueueImpl;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionService;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.publishing.channels.Channel;
import org.alfresco.service.cmr.publishing.channels.ChannelService;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.site.SiteService;
import org.alfresco.service.cmr.site.SiteVisibility;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.BaseSpringTest;
import org.alfresco.util.GUID;
import org.junit.Assert;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * @author Brian
 * 
 */
public class SlideShareTest extends BaseSpringTest
{
    protected ServiceRegistry serviceRegistry;
    protected SiteService siteService;
    protected FileFolderService fileFolderService;
    protected NodeService nodeService;
    protected String siteId;
    protected PublishingQueueImpl queue;
    protected EnvironmentImpl environment;
    protected NodeRef docLib;

    private ChannelService channelService;
    
    private RetryingTransactionHelper transactionHelper;

    public void onSetUp() throws Exception
    {
        serviceRegistry = (ServiceRegistry) getApplicationContext().getBean("ServiceRegistry");
        channelService = (ChannelService) getApplicationContext().getBean("channelService"); 
        AuthenticationUtil.setFullyAuthenticatedUser(AuthenticationUtil.getAdminUserName());
        siteService = serviceRegistry.getSiteService();
        fileFolderService = serviceRegistry.getFileFolderService();
        nodeService = serviceRegistry.getNodeService();
        transactionHelper = serviceRegistry.getRetryingTransactionHelper();

        siteId = GUID.generate();
        siteService.createSite("test", siteId, "Site created by publishing test", "Site created by publishing test",
                SiteVisibility.PUBLIC);
        docLib = siteService.createContainer(siteId, SiteService.DOCUMENT_LIBRARY, ContentModel.TYPE_FOLDER, null);
    }
    
    public void onTearDown()
    {
        siteService.deleteSite(siteId);
    }

    public void testBlank()
    {
        
    }
    
    //Note that this test isn't normally run, as it requires valid YouTube credentials.
    //To run it, remove the initial 'x' from the method name and set the appropriate YouTube credentials where the
    //text "YOUR_USER_NAME" and "YOUR_PASSWORD" appear.
    public void xtestSlideSharePublishAndUnpublishActions() throws Exception
    {
        final NodeRef node = transactionHelper.doInTransaction(new RetryingTransactionCallback<NodeRef>()
        {
            public NodeRef execute() throws Throwable
            {
                Map<QName, Serializable> props = new HashMap<QName, Serializable>();
                props.put(PublishingModel.PROP_CHANNEL_USERNAME, "YOUR_USER_NAME");
                props.put(PublishingModel.PROP_CHANNEL_PASSWORD, "YOUR_PASSWORD");
                Channel channel = channelService.createChannel(siteId, SlideShareChannelType.ID, "SlideShareChannel", props);

                NodeRef channelNode = channel.getNodeRef();
                Resource file = new ClassPathResource("test/alfresco/TestPresentation.pptx");
                Map<QName, Serializable> vidProps = new HashMap<QName, Serializable>();
                vidProps.put(ContentModel.PROP_NAME, "Test Presentation");
                NodeRef node = nodeService.createNode(channelNode, ContentModel.ASSOC_CONTAINS,
                        QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "testPresentation"),
                        ContentModel.TYPE_CONTENT, vidProps).getChildRef();
                ContentService contentService = serviceRegistry.getContentService();
                ContentWriter writer = contentService.getWriter(node, ContentModel.PROP_CONTENT, true);
                writer.setMimetype(MimetypeMap.MIMETYPE_OPENXML_PRESENTATION);
                writer.putContent(file.getFile());
                return node;
            }
        });

        transactionHelper.doInTransaction(new RetryingTransactionCallback<NodeRef>()
        {
            public NodeRef execute() throws Throwable
            {
                ActionService actionService = serviceRegistry.getActionService();
                Action publishAction = actionService.createAction(SlideSharePublishAction.NAME);
                actionService.executeAction(publishAction, node);
                Map<QName, Serializable> props = nodeService.getProperties(node);
                Assert.assertTrue(nodeService.hasAspect(node, SlideSharePublishingModel.ASPECT_ASSET));
                Assert.assertNotNull(props.get(SlideSharePublishingModel.PROP_ASSET_ID));
//                Assert.assertNotNull(props.get(SlideSharePublishingModel.PROP_ASSET_URL));

                System.out.println("SlideShare id: " + props.get(SlideSharePublishingModel.PROP_ASSET_ID));
                
//                Action unpublishAction = actionService.createAction(SlideShareUnpublishAction.NAME);
//                actionService.executeAction(unpublishAction, node);
//                props = nodeService.getProperties(node);
//                Assert.assertFalse(nodeService.hasAspect(node, SlideSharePublishingModel.ASPECT_ASSET));
//                Assert.assertNull(props.get(SlideSharePublishingModel.PROP_ASSET_ID));
//                Assert.assertNull(props.get(SlideSharePublishingModel.PROP_ASSET_URL));
                return null;
            }
        });

        transactionHelper.doInTransaction(new RetryingTransactionCallback<NodeRef>()
        {
            public NodeRef execute() throws Throwable
            {
                nodeService.deleteNode(node);
                return null;
            }
        });

    }

}