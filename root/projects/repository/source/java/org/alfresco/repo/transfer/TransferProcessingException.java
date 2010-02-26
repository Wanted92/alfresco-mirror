/*
 * Copyright (C) 2009-2010 Alfresco Software Limited.
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
 * FLOSS exception.  You should have received a copy of the text describing 
 * the FLOSS exception, and it is also available here: 
 * http://www.alfresco.com/legal/licensing"
 */

package org.alfresco.repo.transfer;

import org.alfresco.service.cmr.transfer.TransferException;

/**
 * @author brian
 *
 */
public class TransferProcessingException extends TransferException
{
    /**
     * 
     */
    private static final long serialVersionUID = 2547803698674661069L;

    /**
     * @param msgId
     * @param msgParams
     * @param cause
     */
    public TransferProcessingException(String msgId, Object[] msgParams, Throwable cause)
    {
        super(msgId, msgParams, cause);
    }

    /**
     * @param msgId
     * @param msgParams
     */
    public TransferProcessingException(String msgId, Object[] msgParams)
    {
        super(msgId, msgParams);
    }

    /**
     * @param msgId
     * @param cause
     */
    public TransferProcessingException(String msgId, Throwable cause)
    {
        super(msgId, cause);
    }

    /**
     * @param msgId
     */
    public TransferProcessingException(String msgId)
    {
        super(msgId);
    }
}