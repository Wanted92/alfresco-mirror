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
package org.alfresco.repo.model.filefolder;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import junit.framework.TestCase;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.transform.AbstractContentTransformerTest;
import org.alfresco.repo.security.authentication.AuthenticationComponent;
import org.alfresco.repo.transaction.TransactionUtil;
import org.alfresco.repo.transaction.TransactionUtil.TransactionWork;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.util.ApplicationContextHelper;
import org.alfresco.util.GUID;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;

/**
 * Tests around some of the data structures that lead to performance
 * degradation.  We use the {@link org.alfresco.service.cmr.model.FileFolderService FileFolderService}
 * as it provides the most convenient and most common test scenarios.
 * <p>
 * Note that this test is not designed to validate performance figures, but is
 * rather a handy tool for doing benchmarking.  It is therefore not named <i>*Test</i> as is the
 * pattern for getting tests run by the continuous build.
 * 
 * @author Derek Hulley
 */
public class FileFolderPerformanceTester extends TestCase
{
    private static Log logger = LogFactory.getLog(FileFolderPerformanceTester.class);
    
    private static ApplicationContext ctx = ApplicationContextHelper.getApplicationContext();
    
    private TransactionService transactionService;
    private AuthenticationComponent authenticationComponent;
    private NodeService nodeService;
    private FileFolderService fileFolderService;
    private StoreRef storeRef;
    private NodeRef rootFolderRef;
    private File dataFile;
    
    @Override
    public void setUp() throws Exception
    {
        ServiceRegistry serviceRegistry = (ServiceRegistry) ctx.getBean(ServiceRegistry.SERVICE_REGISTRY);
        transactionService = serviceRegistry.getTransactionService();
        authenticationComponent = (AuthenticationComponent) ctx.getBean("authenticationComponent");
        nodeService = serviceRegistry.getNodeService();
        fileFolderService = serviceRegistry.getFileFolderService();
        
        // authenticate
        authenticationComponent.setSystemUserAsCurrentUser();
        
        // create a folder root to work in
        storeRef = nodeService.createStore(StoreRef.PROTOCOL_WORKSPACE, getName() + "_" + System.currentTimeMillis());
        NodeRef rootNodeRef = nodeService.getRootNode(storeRef);
        rootFolderRef = nodeService.createNode(
                rootNodeRef,
                ContentModel.ASSOC_CHILDREN,
                QName.createQName(NamespaceService.ALFRESCO_URI, getName()),
                ContentModel.TYPE_FOLDER).getChildRef();
        dataFile = AbstractContentTransformerTest.loadQuickTestFile("txt");
    }
    
    public void testSetUp() throws Exception
    {
        assertNotNull(dataFile);
    }
    
    /**
     * Creates <code>folderCount</code> folders below the given parent and populates each folder with
     * <code>fileCount</code> files.  The folders will be created as siblings in one go, but the files
     * are added one to each folder until each folder has the presribed number of files within it.
     * This can therefore be used to test the performance when the L2 cache sizes are exceeded.
     * <p>
     * Each creation (file or folder) uses the <b>PROPAGATION REQUIRED</b> transaction declaration.
     * 
     * @param parentNodeRef the level zero parent
     * @param randomOrder true if each thread must put the children into the folders in a random order
     * @return Returns the average time (ms) to create the <b>files only</b>
     */
    private void buildStructure(
            final NodeRef parentNodeRef,
            final int threadCount,
            final boolean randomOrder,
            final int folderCount,
            final int fileCount,
            final double[] dumpPoints)
    {
        TransactionWork<NodeRef[]> createFoldersWork = new TransactionWork<NodeRef[]>()
        {
            public NodeRef[] doWork() throws Exception
            {
                NodeRef[] folders = new NodeRef[folderCount];
                for (int i = 0; i < folderCount; i++)
                {
                    FileInfo folderInfo = fileFolderService.create(
                            parentNodeRef,
                            GUID.generate(),
                            ContentModel.TYPE_FOLDER);
                    // keep the reference
                    folders[i] = folderInfo.getNodeRef();
                }
                return folders;
            }
        };
        final NodeRef[] folders = TransactionUtil.executeInUserTransaction(
                transactionService,
                createFoldersWork);
        // the worker that will load the files into the folders
        Runnable runnable = new Runnable()
        {
            private long start;
            public void run()
            {
                // authenticate
                authenticationComponent.setSystemUserAsCurrentUser();
                
                // progress around the folders until they have been populated
                start = System.currentTimeMillis();
                int nextDumpNumber = 0;
                for (int i = 0; i < fileCount; i++)
                {
                    // must we dump results
                    double completedCount = (double) i;
                    double nextDumpCount = (dumpPoints == null || dumpPoints.length == 0 || nextDumpNumber >= dumpPoints.length)
                                           ? -1.0
                                           : (double) fileCount * dumpPoints[nextDumpNumber];
                    if ((nextDumpCount - 0.5) < completedCount && completedCount < (nextDumpCount + 0.5))
                    {
                        dumpResults(i);
                        nextDumpNumber++;
                    }
                    // shuffle folders if required
                    List<NodeRef> foldersList = Arrays.asList(folders);
                    if (randomOrder)
                    {
                        // shuffle folder list
                        Collections.shuffle(foldersList);
                    }
                    for (int j = 0; j < folders.length; j++)
                    {
                        if (logger.isDebugEnabled())
                        {
                            String msg = String.format(
                                    "Thread %s loading file %4d into folder %4d",
                                    Thread.currentThread().getName(),
                                    i, j);
                            logger.debug(msg);
                        }
                        final NodeRef folderRef = folders[j];
                        TransactionWork<FileInfo> createFileWork = new TransactionWork<FileInfo>()
                        {
                            public FileInfo doWork() throws Exception
                            {
                                FileInfo fileInfo = fileFolderService.create(
                                        folderRef,
                                        GUID.generate(),
                                        ContentModel.TYPE_CONTENT);
                                NodeRef nodeRef = fileInfo.getNodeRef();
                                // write the content
                                ContentWriter writer = fileFolderService.getWriter(nodeRef);
                                writer.putContent(dataFile);
                                // done
                                return fileInfo;
                            }
                        };
                        TransactionUtil.executeInUserTransaction(transactionService, createFileWork);
                    }
                }
                dumpResults(fileCount);
            }
            private void dumpResults(int currentFileCount)
            {
                long end = System.currentTimeMillis();
                long time = (end - start);
                double average = (double) time / (double) (folderCount * currentFileCount);
                double percentComplete = (double) currentFileCount / (double) fileCount * 100.0;
                System.out.println(
                        "[" + Thread.currentThread().getName() + "] \n" +
                        "   Created " + currentFileCount + " files in each of " + folderCount + " folders: \n" +
                        "   Progress: " + String.format("%9.2f", percentComplete) +  " percent complete \n" +
                        "   Average: " + String.format("%10.2f", average) + " ms per file \n" +
                        "   Average: " + String.format("%10.2f", 1000.0/average) + " files per second");
            }
        };

        // kick off the required number of threads
        System.out.println(
                "Starting " + threadCount +
                " threads loading " + fileCount +
                " files in each of " + folderCount +
                " folders (" +
                (randomOrder ? "shuffled" : "in order") + ").");
        ThreadGroup threadGroup = new ThreadGroup(getName());
        Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++)
        {
            threads[i] = new Thread(threadGroup, runnable, String.format("FileLoader-%02d", i));
            threads[i].start();
        }
        // join each thread so that we wait for them all to finish
        for (int i = 0; i < threads.length; i++)
        {
            try
            {
                threads[i].join();
            }
            catch (InterruptedException e)
            {
                // not too serious - the worker threads are non-daemon
            }
        }
    }
    
    public void test1Folder10Children() throws Exception
    {
        buildStructure(rootFolderRef, 2, false, 1, 10, null);
    }
    
    public void test10Folders100ChildrenMultiTxn() throws Exception
    {
        buildStructure(rootFolderRef, 2, false, 10, 100, new double[] {0.50});
    }
//    
//    public void test10Folders100ChildrenMultiTxnMultiThread() throws Exception
//    {
//        buildStructure(4, rootFolderRef, 10, 100);
//    }
//
//    public void test1000Folders1000ChildrenMultiTxnMultiThread() throws Exception
//    {
//        buildStructure(rootFolderRef, 4, true, 1000, 1000);
//    }
//    
//    public void test100Folders1Child() throws Exception
//    {
//        timeBuildStructure(rootFolderRef, 100, 1);
//    }
//    
//    public void test1000Folders10Children() throws Exception
//    {
//        timeBuildStructure(rootFolderRef, 1000, 10);
//    }
//    
//    public void test1000Folders100Children() throws Exception
//    {
//        timeBuildStructure(rootFolderRef, 5, 100);
//    }
//    
//    public void test1000Folders1000Children() throws Exception
//    {
//        timeBuildStructure(rootFolderRef, 1000, 1000);
//    }
}
