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
package org.alfresco.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.util.Pair;
import org.alfresco.util.ParameterCheck;

/**
 * Basic support for canned query implementations.
 * 
 * @author Derek Hulley
 * @since 4.0
 */
public abstract class AbstractCannedQuery<R> implements CannedQuery<R>
{
    private final CannedQueryParameters parameters;
    private final String queryExecutionId;
    private CannedQueryResults<R> results;
    
    /**
     * Construct the canned query given the original parameters applied
     * 
     * @param parameters            the original query parameters
     * @param queryExecutionId      the unique ID of this query
     */
    protected AbstractCannedQuery(CannedQueryParameters parameters, String queryExecutionId)
    {
        ParameterCheck.mandatory("parameters", parameters);
        ParameterCheck.mandatory("queryExecutionId", queryExecutionId);
        this.parameters = parameters;
        this.queryExecutionId = queryExecutionId;
    }

    @Override
    public CannedQueryParameters getParameters()
    {
        return parameters;
    }
    
    @Override
    public synchronized final CannedQueryResults<R> execute()
    {
        // Check that we are not requerying
        if (results != null)
        {
            throw new IllegalStateException(
                    "This query instance has already by used." +
                    "  It can only be used to query once.");
        }
        
        // Get the raw query results
        List<R> rawResults = queryAndFilter(parameters);
        if (rawResults == null)
        {
            throw new AlfrescoRuntimeException("Execution returned 'null' results");
        }
        
        // Apply sorting
        if (isApplyPostQuerySorting())
        {
            rawResults = applyPostQuerySorting(rawResults, parameters.getSortDetails());
        }
        
        Pair<Integer, Integer> totalResultCount = null;
        
        final boolean permissionsApplied;
        
        // Apply permissions
        String authenticationToken = parameters.getAuthenticationToken();
        if (authenticationToken != null && isApplyPostQueryPermissions())
        {
            // Work out the number of results required
            int requestedCount = parameters.getPageDetails().getResultsRequiredForPaging();
            if (requestedCount != Integer.MAX_VALUE)
            {
                requestedCount++; // add one for "hasMoreItems"
            }
            PagingResults<R> postQueryResults = applyPostQueryPermissions(rawResults, authenticationToken, requestedCount);
            
            rawResults = postQueryResults.getPage();
            totalResultCount = postQueryResults.getTotalResultCount();
            permissionsApplied = postQueryResults.permissionsApplied();
        }
        else
        {
            int totalCount = getTotalResultCount(rawResults);
            totalResultCount = new Pair<Integer, Integer>(totalCount, totalCount);
            
            permissionsApplied = ((rawResults instanceof PermissionedResults) && ((PermissionedResults)rawResults).permissionsApplied());
        }
        
        // Get count
        final Pair<Integer, Integer> finalTotalCount;
        if (parameters.requestTotalResultCountMax() > 0)
        {
            finalTotalCount = totalResultCount;
        }
        else
        {
            finalTotalCount = null;
        }
        
        // Apply paging
        CannedQueryPageDetails pagingDetails = parameters.getPageDetails();
        List<List<R>> pages = Collections.singletonList(rawResults);
        if (isApplyPostQueryPaging())
        {
            pages = applyPostQueryPaging(rawResults, pagingDetails);
        }
        
        // Construct results object
        final List<List<R>> finalPages = pages;
        
        // Has more items beyond requested pages ? ... ie. at least one more page (with at least one result)
        final boolean hasMoreItems = (rawResults.size() > pagingDetails.getResultsRequiredForPaging());
        
        results = new CannedQueryResults<R>()
        {
            @Override
            public CannedQuery<R> getOriginatingQuery()
            {
                return AbstractCannedQuery.this;
            }
            
            @Override
            public String getQueryExecutionId()
            {
                return queryExecutionId;
            }
            
            @Override
            public Pair<Integer, Integer> getTotalResultCount()
            {
                if (parameters.requestTotalResultCountMax() > 0)
                {
                    return finalTotalCount;
                }
                else
                {
                    throw new IllegalStateException("Total results were not requested in parameters.");
                }
            }
            
            @Override
            public int getPagedResultCount()
            {
                int finalPagedCount = 0;
                for (List<R> page : finalPages)
                {
                    finalPagedCount += page.size();
                }
                return finalPagedCount;
            }
            
            @Override
            public int getPageCount()
            {
                return finalPages.size();
            }
            
            @Override
            public R getSingleResult()
            {
                if (finalPages.size() != 1 && finalPages.get(0).size() != 1)
                {
                    throw new IllegalStateException("There must be exactly one page of one result available.");
                }
                return finalPages.get(0).get(0);
            }
            
            @Override
            public List<R> getPage()
            {
                if (finalPages.size() != 1)
                {
                    throw new IllegalStateException("There must be exactly one page of results available.");
                }
                return finalPages.get(0);
            }
            
            @Override
            public List<List<R>> getPages()
            {
                return finalPages;
            }
            
            @Override
            public boolean hasMoreItems()
            {
                return hasMoreItems;
            }
            
            @Override
            public boolean permissionsApplied()
            {
                return permissionsApplied;
            }
        };
        return results;
    }
    
    /**
     * Implement the basic query, returning either filtered or all results.
     * <p/>
     * The implementation may optimally select, filter, sort and apply permissions.
     * If not, however, the subsequent post-query methods
     * ({@link #applyPostQuerySorting(List, CannedQuerySortDetails)},
     *  {@link #applyPostQueryPermissions(List, String)} and
     *  {@link #applyPostQueryPaging(List, CannedQueryPageDetails)}) can
     * be used to trim the results as required.
     * 
     * @param parameters            the full parameters to be used for execution
     */
    protected abstract List<R> queryAndFilter(CannedQueryParameters parameters);
    
    /**
     * Override to get post-query calls to do sorting.
     * 
     * @return              <tt>true</tt> to get a post-query call to sort (default <tt>false</tt>)
     */
    protected boolean isApplyPostQuerySorting()
    {
        return false;
    }
    
    /**
     * Called before {@link #applyPostQueryPermissions(List)} to allow the results to be sorted prior to permission checks.
     * Note that the {@link #query()} implementation may optimally sort results during retrieval, in which case this method does not need to be implemented.
     * 
     * @param results               the results to sort
     * @param sortDetails           details of the sorting requirements
     * @return                      the results according to the new sort order
     */
    protected List<R> applyPostQuerySorting(List<R> results, CannedQuerySortDetails sortDetails)
    {
        throw new UnsupportedOperationException("Override this method if post-query sorting is required.");
    }
    
    /**
     * Override to get post-query calls to apply permission filters.
     * 
     * @return              <tt>true</tt> to get a post-query call to apply permissions (default <tt>false</tt>)
     */
    protected boolean isApplyPostQueryPermissions()
    {
        return false;
    }
    
    /**
     * Called after the {@link #query() query} to filter out results based on permissions.
     * Note that the {@link #query()} implementation may optimally only select results
     * based on available privileges, in which case this method does not need to be implemented.
     * <p/>
     * Permission evaluations should continue until the requested number of results are retrieved
     * or all available results have been examined.
     * 
     * @param results               the results to apply permissions to
     * @param authenticationToken   the authentication token provided in the parameters
     * @param requestedCount        the minimum number of results to pass the permission checks
     *                              in order to fully satisfy the paging requirements
     * @return                      the remaining results (as a single "page") after permissions have been applied
     */
    protected PagingResults<R> applyPostQueryPermissions(List<R> results, String authenticationToken, int requestedCount)
    {
        throw new UnsupportedOperationException("Override this method if post-query filtering is required.");
    }
    
    /**
     * Get the total number of available results after sorting and filtering.
     * <p/>
     * The default implementation assumes that the given results are the final total possible.
     * 
     * @param results               the results after filtering and sorting, but before paging
     * @return                      the total number of results before paging
     * @throws UnsupportedOperationException if the implementation does not support  retrieval
     *                              of the total query result count.
     */
    protected int getTotalResultCount(List<R> results)
    {
        return results.size();
    }
    
    /**
     * Override to get post-query calls to do pull out paged results.
     * 
     * @return              <tt>true</tt> to get a post-query call to page (default <tt>true</tt>)
     */
    protected boolean isApplyPostQueryPaging()
    {
        return true;
    }
    
    /**
     * Called after the {@link #applyPostQuerySorting(List) sorting phase} to pull out results specific
     * to the required pages.  Note that the {@link #query()} implementation may optimally
     * create page-specific results, in which case this method does not need to be implemented.
     * <p/>
     * The base implementation assumes that results are not paged and that the current results
     * are all the available results i.e. that paging still needs to be applied.
     * 
     * @param results               full results (all or excess pages)
     * @param pageDetails           details of the paging requirements
     * @return                      the specific page of results as per the query parameters
     */
    protected List<List<R>> applyPostQueryPaging(List<R> results, CannedQueryPageDetails pageDetails)
    {
        int skipResults = pageDetails.getSkipResults();
        int pageSize = pageDetails.getPageSize();
        int pageCount = pageDetails.getPageCount();
        int pageNumber = pageDetails.getPageNumber();
        
        int availableResults = results.size();
        int totalResults = pageSize * pageCount;
        int firstResult = skipResults + ((pageNumber-1) * pageSize);            // first of window
        
        List<List<R>> pages = new ArrayList<List<R>>(pageCount);
        
        // First some shortcuts
        if (skipResults == 0 && pageSize > availableResults)
        {
            return Collections.singletonList(results);  // Requesting more results in one page than are available
        }
        else if (firstResult > availableResults)
        {
            return pages;                               // Start of first page is after all results
        }
        
        // Build results
        Iterator<R> iterator = results.listIterator(firstResult);
        int countTotal = 0;
        List<R> page = new ArrayList<R>(Math.min(results.size(), pageSize));    // Prevent memory blow-out
        pages.add(page);
        while (iterator.hasNext() && countTotal < totalResults)
        {
            if (page.size() == pageSize)
            {
                // Create a page and add it to the results
                page = new ArrayList<R>(pageSize);
                pages.add(page);
            }
            R next = iterator.next();
            page.add(next);
            
            countTotal++;
        }
        
        // Done
        return pages;
    }
}
