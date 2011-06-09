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
package org.alfresco.solr.tracker;

import org.alfresco.solr.AlfrescoCoreAdminHandler;
import org.apache.solr.core.SolrCore;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class CoreWatcherJob implements Job
{
    public CoreWatcherJob()
    {
        super();
    }

    /*
     * (non-Javadoc)
     * @see org.quartz.Job#execute(org.quartz.JobExecutionContext)
     */
    @Override
    public void execute(JobExecutionContext jec) throws JobExecutionException
    {
        AlfrescoCoreAdminHandler adminHandler = (AlfrescoCoreAdminHandler) jec.getJobDetail().getJobDataMap().get("ADMIN_HANDLER");

        for (SolrCore core : adminHandler.getCoreContainer().getCores())
        {

            if (!adminHandler.getTrackers().containsKey(core.getName()))
            {
                if (core.getSolrConfig().getBool("alfresco/track", false))
                {
                    System.out.println("Starting to track " + core.getName());
                    adminHandler.getTrackers().put(core.getName(), new CoreTracker(adminHandler, core));
                }
            }
        }
    }

}