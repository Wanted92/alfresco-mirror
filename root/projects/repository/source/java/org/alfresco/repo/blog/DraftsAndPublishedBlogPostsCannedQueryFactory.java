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
package org.alfresco.repo.blog;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.alfresco.model.ContentModel;
import org.alfresco.query.AbstractCannedQueryFactory;
import org.alfresco.query.CannedQuery;
import org.alfresco.query.CannedQueryFactory;
import org.alfresco.query.CannedQueryPageDetails;
import org.alfresco.query.CannedQueryParameters;
import org.alfresco.query.CannedQuerySortDetails;
import org.alfresco.query.PagingRequest;
import org.alfresco.query.CannedQuerySortDetails.SortOrder;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.permissions.impl.acegi.MethodSecurityInterceptor;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.tagging.TaggingService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.alfresco.util.ParameterCheck;
import org.alfresco.util.PropertyCheck;

/**
 * A {@link CannedQueryFactory} for the creation of {@link DraftsAndPublishedBlogPostsCannedQuery}s.
 * 
 * Currently, this is implemented using calls to lower-level services, notably the {@link NodeService} rather
 * than database queries. This may change in the future.
 * 
 * @since 4.0
 * @author Neil Mc Erlean.
 */
public class DraftsAndPublishedBlogPostsCannedQueryFactory extends AbstractCannedQueryFactory<BlogPostInfo>
{
    private MethodSecurityInterceptor methodSecurityInterceptor;
    private String methodName;
    private Object methodService;
    private NodeService rawNodeService;
    private TaggingService taggingService;
    
    public void setRawNodeService(NodeService nodeService)
    {
        this.rawNodeService = nodeService;
    }
    
    public void setTaggingService(TaggingService taggingService)
    {
        this.taggingService = taggingService;
    }
    
    public void setMethodSecurityInterceptor(MethodSecurityInterceptor methodSecurityInterceptor)
    {
        this.methodSecurityInterceptor = methodSecurityInterceptor;
    }
    
    public void setMethodName(String methodName)
    {
        this.methodName = methodName;
    }
    
    public void setMethodService(Object methodService)
    {
        this.methodService = methodService;
    }
    
    @Override
    public CannedQuery<BlogPostInfo> getCannedQuery(CannedQueryParameters parameters)
    {
        // if not passed in (TODO or not in future cache) then generate a new query execution id
        String queryExecutionId = (parameters.getQueryExecutionId() == null ? super.getQueryExecutionId(parameters) : parameters.getQueryExecutionId());
        
        final DraftsAndPublishedBlogPostsCannedQuery cq = new DraftsAndPublishedBlogPostsCannedQuery(rawNodeService, taggingService,
                                                                                  methodSecurityInterceptor, methodService, methodName,
                                                                                  parameters, queryExecutionId);
        return (CannedQuery<BlogPostInfo>) cq;
    }
    
    public CannedQuery<BlogPostInfo> getCannedQuery(NodeRef blogContainerNode, Date fromDate, Date toDate, String byUser, String tag, PagingRequest pagingReq)
    {
        ParameterCheck.mandatory("blogContainerNode", blogContainerNode);
        ParameterCheck.mandatory("pagingReq", pagingReq);
        
        int requestTotalCountMax = pagingReq.getRequestTotalCountMax();
        
        //FIXME Need tenant service like for GetChildren?
        DraftAndPublishedBlogPostsCannedQueryParams paramBean = new DraftAndPublishedBlogPostsCannedQueryParams(blogContainerNode,
                                                                                    byUser,
                                                                                    fromDate, toDate, tag);
        
        CannedQueryPageDetails cqpd = createCQPageDetails(pagingReq);
        CannedQuerySortDetails cqsd = createCQSortDetails(ContentModel.PROP_PUBLISHED, SortOrder.DESCENDING);
        
        // create query params holder
        CannedQueryParameters params = new CannedQueryParameters(paramBean, cqpd, cqsd, AuthenticationUtil.getRunAsUser(), requestTotalCountMax, pagingReq.getQueryExecutionId());
        
        // return canned query instance
        return getCannedQuery(params);
}

    private CannedQuerySortDetails createCQSortDetails(QName sortProp, SortOrder sortOrder)
    {
        CannedQuerySortDetails cqsd = null;
        List<Pair<? extends Object, SortOrder>> sortPairs = new ArrayList<Pair<? extends Object, SortOrder>>();
        sortPairs.add(new Pair<QName, SortOrder>(sortProp, sortOrder));
        cqsd = new CannedQuerySortDetails(sortPairs);
        return cqsd;
    }

    private CannedQueryPageDetails createCQPageDetails(PagingRequest pagingReq)
    {
        int skipCount = pagingReq.getSkipCount();
        if (skipCount == -1)
        {
            skipCount = CannedQueryPageDetails.DEFAULT_SKIP_RESULTS;
        }
        
        int maxItems = pagingReq.getMaxItems();
        if (maxItems == -1)
        {
            maxItems  = CannedQueryPageDetails.DEFAULT_PAGE_SIZE;
        }
        
        // page details
        CannedQueryPageDetails cqpd = new CannedQueryPageDetails(skipCount, maxItems, CannedQueryPageDetails.DEFAULT_PAGE_NUMBER, CannedQueryPageDetails.DEFAULT_PAGE_COUNT);
        return cqpd;
    }
    
    @Override
    public void afterPropertiesSet() throws Exception
    {
        super.afterPropertiesSet();
        
        PropertyCheck.mandatory(this, "methodSecurityInterceptor", methodSecurityInterceptor);
        PropertyCheck.mandatory(this, "methodService", methodService);
        PropertyCheck.mandatory(this, "methodName", methodName);
    }
}