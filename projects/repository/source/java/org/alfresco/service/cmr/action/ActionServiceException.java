/*
 * Copyright (C) 2005 Alfresco, Inc.
 *
 * Licensed under the Mozilla Public License version 1.1 
 * with a permitted attribution clause. You may obtain a
 * copy of the License at
 *
 *   http://www.alfresco.org/legal/license.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.alfresco.service.cmr.action;

import org.alfresco.error.AlfrescoRuntimeException;

/**
 * Rule Service Exception Class
 * 
 * @author Roy Wetherall
 */
public class ActionServiceException extends AlfrescoRuntimeException 
{
	/**
	 * Serial version UID 
	 */
	private static final long serialVersionUID = 3257571685241467958L;

    public ActionServiceException(String msgId)
    {
        super(msgId);
    }

    public ActionServiceException(String msgId, Object[] msgParams)
    {
        super(msgId, msgParams);
    }

    public ActionServiceException(String msgId, Object[] msgParams, Throwable cause)
    {
        super(msgId, msgParams, cause);
    }

    public ActionServiceException(String msgId, Throwable cause)
    {
        super(msgId, cause);
    }
}
