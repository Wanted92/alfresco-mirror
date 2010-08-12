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
package org.alfresco.repo.action.script;

import java.io.Serializable;
import java.util.Date;

import org.alfresco.repo.jscript.Scopeable;
import org.alfresco.service.cmr.action.ExecutionDetails;
import org.alfresco.service.cmr.action.ExecutionSummary;
import org.alfresco.service.cmr.repository.NodeRef;
import org.mozilla.javascript.Scriptable;

/**
 * ExecutionDetails JavaScript Object. This class is a JavaScript-friendly wrapper for
 *  the {@link ExecutionDetails} (and embeded {@link ExecutionSummary}) class.
 * 
 * @author Nick Burch
 * @see org.alfresco.service.cmr.action.ExecutionDetails
 */
public final class ScriptExecutionDetails implements Serializable, Scopeable
{
    private static final long serialVersionUID = 3182925511891455490L;
    
    /** Root scope for this object */
    private Scriptable scope;
    
    /** The details we wrap */
    private ExecutionDetails details;

    public ScriptExecutionDetails(ExecutionDetails details)
    {
    	 this.details = details;
    }
    
    protected ExecutionDetails getExecutionDetails() 
    {
       return details;
    }
    

    public String getActionType() {
       return details.getActionType();
    }

    public String getActionId() {
       return details.getActionId();
    }

    public int getExecutionInstance() {
       return details.getExecutionInstance();
    }
    
    public NodeRef getPersistedActionRef() {
       return details.getPersistedActionRef();
    }

    public String getRunningOn() {
       return details.getRunningOn();
    }

    public Date getStartedAt() {
       return details.getStartedAt();
    }

    public boolean isCancelRequested() {
       return details.isCancelRequested();
    }

    /**
     * @see org.alfresco.repo.jscript.Scopeable#setScope(org.mozilla.javascript.Scriptable)
     */
    public void setScope(Scriptable scope)
    {
        this.scope = scope;
    }

    @Override
    public String toString() 
    {
       StringBuilder builder = new StringBuilder();
       builder.append("Executing Action: ");
       builder.append(details.getActionType()).append(' ');
       builder.append(details.getActionId()).append(' ');
       builder.append(details.getExecutionInstance()).append(' ');
       if(details.getPersistedActionRef() != null)
       {
          builder.append(details.getPersistedActionRef());
       }
       
       return builder.toString();
    }
}