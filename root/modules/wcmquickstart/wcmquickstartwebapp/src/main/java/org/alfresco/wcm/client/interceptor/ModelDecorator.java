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
package org.alfresco.wcm.client.interceptor;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.alfresco.wcm.client.directive.TemplateConstants;
import org.springframework.extensions.surf.RequestContext;
import org.springframework.extensions.surf.support.ThreadLocalRequestContext;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import freemarker.template.TemplateDirectiveModel;

/**
 * Load data into the Spring Model
 * @author Chris Lack
 */
public class ModelDecorator
{
	private Map<String, TemplateDirectiveModel> freemarkerDirectives;
	private boolean editorialSite;

    public void populate(HttpServletRequest request, ModelAndView modelAndView)
    {		 
	    if (modelAndView != null && 
	    		( ! modelAndView.hasView() || ! (modelAndView.getView() instanceof RedirectView))) // Don't bother if redirect
	    {
		    Map<String,Object> model = modelAndView.getModel();
			
			// Store custom Freemarker directives in the spring model
		    model.putAll(freemarkerDirectives);
		    
		    // Store website, section and asset on spring model too for use in page meta data
			RequestContext requestContext = ThreadLocalRequestContext.getRequestContext();	    
		    model.put("webSite", requestContext.getValue("webSite"));
		    model.put("section", requestContext.getValue("section"));
		    model.put("asset", requestContext.getValue("asset"));
		    
		    // If spring freemarker macros are used then this line is needed as the Surf freemarker view
		    // resolver doesn't put the spring request context into the model.
		    model.put("springMacroRequestContext", 
		    			new org.springframework.web.servlet.support.RequestContext(request, model));
		    
		    // Put the pre-filtered/modified URI into the model
		    model.put("uri", requestContext.getAttribute("javax.servlet.forward.request_uri"));	
		    
		    // Flag that the site is the editorial version so that editorial-only features can
		    // be enabled
		    model.put("editorialSite", editorialSite);
		    
		    // Enable the web editor if this is the editorial site.
		    request.setAttribute(TemplateConstants.REQUEST_ATTR_KEY_WEF_ENABLED, editorialSite);
	    }
    }

    public void setFreemarkerDirectives(Map<String,TemplateDirectiveModel> directives) 
    {
		this.freemarkerDirectives = directives;
	}
    
    
    public void setEditorialSite(boolean editorialFeaturesEnabled)
    {
        this.editorialSite = editorialFeaturesEnabled;
    }      
}

