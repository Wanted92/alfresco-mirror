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
package org.alfresco.web.scripts;

import java.io.Writer;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.alfresco.web.config.ServerProperties;
import org.alfresco.web.site.WebFrameworkConstants;
import org.alfresco.web.uri.UriUtils;

/**
 * WebScript Runtime for rendering as Web Framework components.
 * 
 * @author kevinr
 * @author muzquiano
 */
public class LocalWebScriptRuntime extends AbstractRuntime
{
    private ServerProperties serverProperties;
    private LocalWebScriptContext context;
    private Writer out;
    private String method;

    public LocalWebScriptRuntime(
            Writer out, RuntimeContainer container, ServerProperties serverProps, LocalWebScriptContext context) 
    {
        super(container);
        
        this.out = out;
        this.serverProperties = serverProps;
        this.context = context;
        this.method = "GET";
    }
    
    public LocalWebScriptContext getLocalContext()
    {
    	return context;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.alfresco.web.scripts.Runtime#getName()
     */
    public String getName()
    {
        return "Web Framework Runtime";
    }

    @Override
    protected String getScriptUrl()
    {
        return context.ScriptUrl;
    }

    @Override
    protected WebScriptRequest createRequest(Match match)
    {
        // this includes all elements of the xml
        Map properties = context.Object.getProperties();
        String scriptUrl = context.ExecuteUrl;

        // component ID is always available to the component
        properties.put("id", context.Object.getId());

        // add/replace the "well known" context tokens in component properties
        for (String arg : context.Object.getCustomProperties().keySet())
        {
            properties.put(arg, UriUtils.replaceUriTokens((String)context.Object.getCustomProperties().get(arg), context.Tokens));
        }

        // add the html binding id
        String htmlBindingId = (String) context.RenderContext.getValue(WebFrameworkConstants.RENDER_DATA_HTMLID);
        if (htmlBindingId != null)
        {
            properties.put(ProcessorModelHelper.PROP_HTMLID, htmlBindingId);
        }
        
        // try to determine the http servlet request and bind it in if we can
        HttpServletRequest request = context.RenderContext.getRequest();
        
        return new LocalWebScriptRequest(this, scriptUrl, match, properties, serverProperties, request, context);
    }

    @Override
    protected LocalWebScriptResponse createResponse()
    {
        return new LocalWebScriptResponse(this, context, out);
    }

    @Override
    protected String getScriptMethod()
    {
        return method;
    }

    @Override
    protected Authenticator createAuthenticator()
    {
        return null;
    }
    
    public void setScriptMethod(String method)
    {
    	this.method = method;
    }
}
