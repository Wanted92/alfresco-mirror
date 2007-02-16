/*
 * Copyright (C) 2005 Alfresco, Inc.
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
package org.alfresco.repo.content.filestore;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.CRC32;

import junit.framework.TestCase;

/**
 * Some tests to check out the </code>java.lang.nio</code> functionality
 * 
 * @author Derek Hulley
 */
public class FileIOTest extends TestCase
{
    private static final String TEST_CONTENT = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private File file;
    
    public FileIOTest(String name)
    {
        super(name);
    }
    
    public void setUp() throws Exception
    {
        file = File.createTempFile(getName(), ".txt");
        OutputStream os = new FileOutputStream(file);
        os.write(TEST_CONTENT.getBytes());
        os.flush();
        os.close();
    }
    
    /**
     * Attempt to read the same file using multiple channels concurrently
     */
    public void testConcurrentFileReads() throws Exception
    {
        // open the file for a read
        FileInputStream isA = new FileInputStream(file);
        FileInputStream isB = new FileInputStream(file);
        
        // get the channels
        FileChannel channelA = isA.getChannel();
        FileChannel channelB = isB.getChannel();
        
        // buffers for reading
        ByteBuffer bufferA = ByteBuffer.allocate(10);
        ByteBuffer bufferB = ByteBuffer.allocate(10);
        
        // read file into both buffers
        int countA = 0;
        int countB = 0;
        do
        {
            countA = channelA.read((ByteBuffer)bufferA.clear());
            countB = channelB.read((ByteBuffer)bufferB.clear());
            assertEquals("Should read same number of bytes", countA, countB);
        } while (countA > 6);
        
        // both buffers should be at the same marker 6
        assertEquals("BufferA marker incorrect", 6, bufferA.position());
        assertEquals("BufferB marker incorrect", 6, bufferB.position());
    }
    
    public void testConcurrentReadWrite() throws Exception
    {
        // open file for a read
        FileInputStream isRead = new FileInputStream(file);
        // open file for write
        FileOutputStream osWrite = new FileOutputStream(file);
        
        // get channels
        FileChannel channelRead = isRead.getChannel();
        FileChannel channelWrite = osWrite.getChannel();
        
        // buffers
        ByteBuffer bufferRead = ByteBuffer.allocate(26);
        ByteBuffer bufferWrite = ByteBuffer.wrap(TEST_CONTENT.getBytes());
        
        // read - nothing will be read
        int countRead = channelRead.read(bufferRead);
        assertEquals("Expected nothing to be read", -1, countRead);
        // write
        int countWrite = channelWrite.write(bufferWrite);
        assertEquals("Not all characters written", 26, countWrite);
        
        // close the write side
        channelWrite.close();
        
        // reread
        countRead = channelRead.read(bufferRead);
        assertEquals("Expected full read", 26, countRead);
    }
    
    public void testCrcPerformance() throws Exception
    {
        long before = System.nanoTime();
        int count = 1000000;
        Set<Long> results = new HashSet<Long>(count);
        boolean negatives = false;
        for (int i = 0; i < count; i++)
        {
            CRC32 crc = new CRC32();
            crc.update(Integer.toString(i).getBytes());
            long value = crc.getValue();
            if (value < 0)
            {
                negatives = true;
            }
            if (!results.add(value))
            {
                System.out.println("Duplicate on " + i);
            }
        }
        long after = System.nanoTime();
        long delta = after - before;
        double aveNs = (double)delta / (double)count;
        System.out.println(String.format("CRC32: %10.2f ns per item.  Negatives=" + negatives, aveNs));
    }
}
