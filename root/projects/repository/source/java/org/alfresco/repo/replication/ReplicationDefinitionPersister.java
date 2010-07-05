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

package org.alfresco.repo.replication;

import java.util.List;

import org.alfresco.service.cmr.replication.ReplicationDefinition;
import org.alfresco.service.namespace.QName;

/**
 * This class provides the implementation of ReplicationDefinition persistence.
 * 
 * @author Nick Burch
 * @since 3.4
 */
public interface ReplicationDefinitionPersister
{
    /**
     * This method serializes the {@link ReplicationDefinition} and stores it in
     * the repository. {@link ReplicationDefinition}s saved in this way may be
     * retrieved using the <code>load()</code> method.
     * 
     * @param replicationDefinition The {@link ReplicationDefinition} to be
     *            persisted.
     */
    void saveReplicationDefinition(ReplicationDefinition replicationDefinition);

    /**
     * This method retrieves a {@link ReplicationDefinition} that has been stored
     * in the repository using the <code>save()</code> method. If no
     * {@link ReplicationDefinition} exists in the repository with the specified
     * rendition name then this method returns null.
     * 
     * @param replicationName The unique identifier used to specify the
     *            {@link ReplicationDefinition} to retrieve.
     * @return The specified {@link ReplicationDefinition} or null.
     */
    ReplicationDefinition loadReplicationDefinition(QName replicationName);

    /**
     * This method retrieves the {@link ReplicationDefinition}s that have been
     * stored in the repository using the <code>save()</code> method.
     * <P/>
     * If there are no such {@link ReplicationDefinition}s, an empty list is
     * returned.
     * 
     * @return The {@link ReplicationDefinition}s.
     */
    List<ReplicationDefinition> loadReplicationDefinitions();

    /**
     * This method retrieves the stored {@link ReplicationDefinition}s that have
     * been registered for the specified transfer target name.
     * <P/>
     * If there are no such rendering {@link ReplicationDefinition}s, an empty
     * list is returned.
     * 
     * @param targetName the name of a target.
     * @return The {@link ReplicationDefinition}s.
     * @throws NullPointerException if the target is null.
     * @see #saveReplicationDefinition(ReplicationDefinition)
     */
    List<ReplicationDefinition> loadReplicationDefinitions(String targetName);
}