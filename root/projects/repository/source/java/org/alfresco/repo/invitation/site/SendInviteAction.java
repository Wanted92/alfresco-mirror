/*
 * Copyright (C) 2005-2009 Alfresco Software Limited.
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

package org.alfresco.repo.invitation.site;

import static org.alfresco.repo.invitation.WorkflowModelNominatedInvitation.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.repo.jscript.ScriptNode;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.workflow.WorkflowModel;
import org.alfresco.repo.workflow.jbpm.JBPMSpringActionHandler;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.namespace.NamespaceService;
import org.jbpm.graph.exe.ExecutionContext;
import org.springframework.beans.factory.BeanFactory;

public class SendInviteAction extends JBPMSpringActionHandler
{
    // TODO Select Version Id.
    private static final long serialVersionUID = 8133039174866049136L;

    private InviteSender inviteSender;
    private NamespaceService namespaceService;

    @Override
    protected void initialiseHandler(BeanFactory factory)
    {
        Repository repository = (Repository) factory.getBean("repositoryHelper");
        ServiceRegistry services = (ServiceRegistry) factory.getBean(ServiceRegistry.SERVICE_REGISTRY);
        inviteSender = new InviteSender(services, repository);
        namespaceService = services.getNamespaceService();
    }

    public void execute(final ExecutionContext context) throws Exception
    {

        Collection<String> propertyNames = Arrays.asList(wfVarInviteeUserName,//
                    wfVarResourceName,//
                    wfVarInviterUserName,//
                    wfVarInviteeUserName,//
                    wfVarRole,//
                    wfVarInviteeGenPassword,//
                    wfVarResourceName,//
                    wfVarInviteTicket,//
                    wfVarServerPath,//
                    wfVarAcceptUrl,//
                    wfVarRejectUrl,
                    InviteSender.WF_INSTANCE_ID);
        Map<String, String> properties = makePropertiesFromContext(context, propertyNames);

        String packageName = WorkflowModel.ASSOC_PACKAGE.toPrefixString(namespaceService).replace(":", "_");
        ScriptNode packageNode = (ScriptNode) context.getVariable(packageName);
        String packageRef = packageNode.getNodeRef().toString();
        properties.put(InviteSender.WF_PACKAGE, packageRef);
        
        String instanceName=WorkflowModel.PROP_WORKFLOW_INSTANCE_ID.toPrefixString(namespaceService).replace(":", "_");
        String instanceId = (String) context.getVariable(instanceName);
        properties.put(InviteSender.WF_INSTANCE_ID, instanceId);
        inviteSender.sendMail(properties);
    }

    private Map<String, String> makePropertiesFromContext(ExecutionContext context, Collection<String> propertyNames)
    {
        Map<String, String> props = new HashMap<String, String>();
        for (String name : propertyNames)
        {
            String value = (String) context.getVariable(name);
            props.put(name, value);
        }
        return props;
    }
}