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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.ContentService;
import org.apache.commons.httpclient.methods.multipart.PartBase;
import org.apache.commons.httpclient.util.EncodingUtil;

/**
 * @author brian
 *
 */
public class ContentDataPart extends PartBase
{
    /** Attachment's file name */
    protected static final String FILE_NAME = "; filename=";

    /** Attachment's file name as a byte array */
    private static final byte[] FILE_NAME_BYTES = 
        EncodingUtil.getAsciiBytes(FILE_NAME);

    private ContentService contentService;
    private ContentData data;
    private String filename;

    /**
     * ContentDataPart 
     * @param contentService
     * @param partName
     * @param data
     */
    public ContentDataPart(ContentService contentService, String partName, ContentData data) {
        super(partName, data.getMimetype(), data.getEncoding(), null);
        this.contentService = contentService;
        this.data = data;
        this.filename = partName;
    }

    /**
     * Write the disposition header to the output stream
     * @param out The output stream
     * @throws IOException If an IO problem occurs
     * @see Part#sendDispositionHeader(OutputStream)
     */
    protected void sendDispositionHeader(OutputStream out) 
    throws IOException {
        super.sendDispositionHeader(out);
        if (filename != null) {
            out.write(FILE_NAME_BYTES);
            out.write(QUOTE_BYTES);
            out.write(EncodingUtil.getAsciiBytes(filename));
            out.write(QUOTE_BYTES);
        }
    }

    /* (non-Javadoc)
     * @see org.apache.commons.httpclient.methods.multipart.Part#lengthOfData()
     */
    @Override
    protected long lengthOfData() throws IOException
    {
        return data.getSize();
    }

    /* (non-Javadoc)
     * @see org.apache.commons.httpclient.methods.multipart.Part#sendData(java.io.OutputStream)
     */
    @Override
    protected void sendData(OutputStream out) throws IOException
    {

        // Get the content from the content URL and write it to out
        
        // Can't use the following code since it closes 'out', which needs to remain open.
        // AbstractContentReader.
        // contentService.getRawReader(data.getContentUrl()).getContent(out);
        
        InputStream is = contentService.getRawReader(data.getContentUrl()).getContentInputStream();
        
//        ReadableByteChannel rbc = Channels.newChannel(is);
//        WritableByteChannel wbc = Channels.newChannel(out);   
        
        try 
        {
            byte[] tmp = new byte[4096];
            int len;
            while ((len = is.read(tmp)) >= 0) 
            {
                out.write(tmp, 0, len);
            }
        } 
        finally 
        {
            // we're done with the input stream, close it
            is.close();
        }

        
        
    }
}