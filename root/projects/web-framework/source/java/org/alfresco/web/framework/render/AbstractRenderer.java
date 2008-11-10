/*
 * Copyright (C) 2005-2008 Alfresco Software Limited.
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
package org.alfresco.web.framework.render;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.web.framework.exception.RendererExecutionException;
import org.alfresco.web.framework.exception.RendererInitializationException;

/**
 * An abstract implementation of the Renderer interface that can be
 * extended by application developers for quick implementation.
 * 
 * @author muzquiano
 */
public abstract class AbstractRenderer implements Renderer
{	
	protected Processor processor = null;
	
	public void setProcessor(Processor processor)
	{
		this.processor = processor;
	}
	
	public Processor getProcessor()
	{
		return this.processor;
	}
	
	/* (non-Javadoc)
	 * @see org.alfresco.web.framework.render.Renderer#init()
	 */
	public void init()
		throws RendererInitializationException
	{
		if(this.processor == null)
		{
			throw new RendererInitializationException("Unable to init renderer: null processor");
		}
	}
	
	/* (non-Javadoc)
	 * @see org.alfresco.web.framework.render.Renderer#render(org.alfresco.web.framework.render.RendererContext, org.alfresco.web.framework.render.RenderFocus)
	 */
	public void render(RenderContext context, RenderFocus focus)
		throws RendererExecutionException
	{
		if(focus== null || focus == RenderFocus.BODY)
		{
			body(context);
		}
		else if(focus == RenderFocus.ALL)
		{
			all(context);
		}
		else if(focus == RenderFocus.HEADER)
		{
			header(context);
		}
		else if(focus == RenderFocus.FOOTER)
		{
			footer(context);
		}
	}

	/* (non-Javadoc)
	 * @see org.alfresco.web.framework.render.Renderer#all(org.alfresco.web.framework.render.RendererContext)
	 */
	public void all(RenderContext context)
		throws RendererExecutionException
	{
		header(context);
		body(context);
		footer(context);
	}
	
    /* (non-Javadoc)
     * @see org.alfresco.web.framework.render.Renderer#head(org.alfresco.web.framework.render.RendererContext)
     */
    public void header(RenderContext context)
        throws RendererExecutionException
    {
    	// default implementation which simply returns a stamp
    	
        String stamp = "<!-- Generated by " + getClass().getName() + " -->";
        if(context != null && context.getObject() != null)
        {
            stamp = "<!-- Generated by " + getClass().getName() + " for " + context.getObject().getId() + " -->";
        }
        stamp += "\r\n";
        
        print(context, stamp);
        
        // call through to processor header() function
    }

    /* (non-Javadoc)
     * @see org.alfresco.web.framework.render.Renderer#body(org.alfresco.web.framework.render.RendererContext)
     */
    public abstract void body(RenderContext context)
    	throws RendererExecutionException;

    /* (non-Javadoc)
     * @see org.alfresco.web.framework.render.Renderer#footer(org.alfresco.web.framework.render.RendererContext)
     */
    public void footer(RenderContext context)
    	throws RendererExecutionException
	{    	
    	// TODO: call through to processor footer() function
    	// Not currently implemented for processors
	}
    
    /**
     * Commits the given string to the response output stream
     * 
     * @param response the response
     * @param str the string
     * 
     * @throws RendererExecutionException
     */
    protected void print(HttpServletResponse response, String str)
        throws RendererExecutionException
    {
        try
        {
            response.getWriter().print(str);
        }
        catch(IOException ex)
        {
            throw new RendererExecutionException("Unable to print string to response: " + str, ex);
        }
    }

    /**
     * Commits the given string to the response output stream
     * 
     * @param context the render context
     * @param str the string
     * 
     * @throws RendererExecutionException
     */
    protected void print(RenderContext context, String str)
        throws RendererExecutionException
    {
        try
        {
        	context.getResponse().getWriter().print(str);
        }
        catch(IOException ex)
        {
            throw new RendererExecutionException("Unable to print string to response: " + str, ex);
        }
    }
    
}
