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

package org.alfresco.repo.publishing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.alfresco.repo.transfer.manifest.TransferManifestNode;
import org.alfresco.repo.transfer.manifest.TransferManifestNodeFactory;
import org.alfresco.repo.transfer.manifest.TransferManifestNormalNode;
import org.alfresco.service.cmr.publishing.MutablePublishingPackage;
import org.alfresco.service.cmr.publishing.PublishingPackageEntry;
import org.alfresco.service.cmr.repository.NodeRef;

/**
 * @author Brian
 * @author Nick Smith
 * 
 */
public class MutablePublishingPackageImpl implements MutablePublishingPackage
{
    private final TransferManifestNodeFactory transferManifestNodeFactory;
    private final List<PublishingPackageEntry> entries = new ArrayList<PublishingPackageEntry>();
    private final Set<NodeRef> nodesToPublish = new HashSet<NodeRef>();
    private final Set<NodeRef> nodesToUnpublish= new HashSet<NodeRef>();
    
    /**
     * @param transferManifestNodeFactory
     */
    public MutablePublishingPackageImpl(TransferManifestNodeFactory transferManifestNodeFactory)
    {
        this.transferManifestNodeFactory = transferManifestNodeFactory;
    }

    /**
    * {@inheritDoc}
     */
    public void addNodesToPublish(NodeRef... nodesToAdd)
    {
        addNodesToPublish(Arrays.asList(nodesToAdd));
    }

    /**
    * {@inheritDoc}
     */
    public void addNodesToPublish(Collection<NodeRef> nodesToAdd)
    {
        for (NodeRef nodeRef : nodesToAdd)
        {
            TransferManifestNode payload = transferManifestNodeFactory.createTransferManifestNode(nodeRef, null);
            if (TransferManifestNormalNode.class.isAssignableFrom(payload.getClass()))
            {
                entries.add(new PublishingPackageEntryImpl(true, nodeRef, (TransferManifestNormalNode) payload));
            }
        }
        nodesToPublish.addAll(nodesToAdd);
    }

    /**
    * {@inheritDoc}
     */
    public void addNodesToUnpublish(NodeRef... nodesToRemove)
    {
        addNodesToUnpublish(Arrays.asList(nodesToRemove));
    }

    /**
    * {@inheritDoc}
     */
    public void addNodesToUnpublish(Collection<NodeRef> nodesToRemove)
    {
        for (NodeRef nodeRef : nodesToRemove)
        {
            entries.add(new PublishingPackageEntryImpl(false, nodeRef, null));
        }
        nodesToUnpublish.addAll(nodesToRemove);
    }

    /**
    * {@inheritDoc}
    */
    @Override
    public Collection<PublishingPackageEntry> getEntries()
    {
        return entries;
    }

    /**
    * {@inheritDoc}
    */
    @Override
    public Set<NodeRef> getNodesToPublish()
    {
        return nodesToPublish;
    }

    /**
    * {@inheritDoc}
    */
    @Override
    public Set<NodeRef> getNodesToUnpublish()
    {
        return nodesToUnpublish;
    }
}