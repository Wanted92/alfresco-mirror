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
package org.alfresco.filesys.repo;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.alfresco.filesys.alfresco.ExtendedDiskInterface;
import org.alfresco.filesys.alfresco.RepositoryDiskInterface;
import org.alfresco.filesys.config.ServerConfigurationBean;
import org.alfresco.filesys.repo.rules.Command;
import org.alfresco.filesys.repo.rules.Operation;
import org.alfresco.filesys.repo.rules.RuleEvaluator;
import org.alfresco.jlan.server.SrvSession;
import org.alfresco.jlan.server.core.DeviceContext;
import org.alfresco.jlan.server.core.DeviceContextException;
import org.alfresco.jlan.server.filesys.AccessMode;
import org.alfresco.jlan.server.filesys.FileInfo;
import org.alfresco.jlan.server.filesys.FileName;
import org.alfresco.jlan.server.filesys.FileOpenParams;
import org.alfresco.jlan.server.filesys.FileStatus;
import org.alfresco.jlan.server.filesys.NetworkFile;
import org.alfresco.jlan.server.filesys.SearchContext;
import org.alfresco.jlan.server.filesys.TreeConnection;
import org.alfresco.jlan.server.filesys.cache.FileState;
import org.alfresco.jlan.server.filesys.cache.NetworkFileStateInterface;
import org.alfresco.jlan.smb.SharingMode;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.util.PropertyCheck;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ibatis.annotations.Case;
import org.springframework.extensions.config.ConfigElement;
import org.alfresco.filesys.repo.rules.EvaluatorContext;
import org.alfresco.filesys.repo.rules.commands.CompoundCommand;
import org.alfresco.filesys.repo.rules.commands.CopyContentCommand;
import org.alfresco.filesys.repo.rules.commands.RenameFileCommand;
import org.alfresco.filesys.repo.rules.operations.CloseFileOperation;
import org.alfresco.filesys.repo.rules.operations.CreateFileOperation;
import org.alfresco.filesys.repo.rules.operations.DeleteFileOperation;
import org.alfresco.filesys.repo.rules.operations.MoveFileOperation;
import org.alfresco.filesys.repo.rules.operations.OpenFileOperation;
import org.alfresco.filesys.repo.rules.operations.RenameFileOperation;

/**
 * Non Transactional DiskDriver with rules engine.
 * <p>
 * Provides a DiskInterface that deals with "shuffles". Shuffles are implemented by the Rules Engine.
 * <p>
 * Sits on top of the repository and is non-retryable and non-transactional.  
 * It is, however thread safe and multiple callers may call in parallel. 
 */
public class NonTransactionalRuleContentDiskDriver implements ExtendedDiskInterface 
{
    /**
     * The Driver State. Contained within the JLAN SrvSession.
     */
    private class DriverState
    {
        /**
         * Map of folderName to Evaluator Context.
         */
        Map<String, EvaluatorContext> contextMap = new ConcurrentHashMap<String, EvaluatorContext>();
    }
    
    // Logging
    private static final Log logger = LogFactory.getLog(NonTransactionalRuleContentDiskDriver.class);
    
    private ExtendedDiskInterface diskInterface;
    private RuleEvaluator ruleEvaluator;
    private RepositoryDiskInterface repositoryDiskInterface;
    private CommandExecutor commandExecutor;
    private ContentDiskCallback callbackInterface;
          
    public void init()
    {
        PropertyCheck.mandatory(this, "diskInterface", diskInterface);
        PropertyCheck.mandatory(this, "ruleEvaluator", getRuleEvaluator());
        PropertyCheck.mandatory(this, "repositoryDiskInterface", getRepositoryDiskInterface());        
        PropertyCheck.mandatory(this, "commandExecutor", getCommandExecutor());
    }
    
    @Override
    public FileInfo getFileInformation(SrvSession sess, TreeConnection tree,
            String path) throws IOException
    {
        FileInfo info = diskInterface.getFileInformation(sess, tree, path);
        
        if(getCallbackInterface() != null)
        {
            getCallbackInterface().getFileInformation(sess, tree, path, info);
        }
        return info;
    }
    
    @Override
    public int fileExists(SrvSession sess, TreeConnection tree, String path)
    {
        int fileExists = diskInterface.fileExists(sess, tree, path);
        if(getCallbackInterface() != null)
        {
            getCallbackInterface().fileExists(sess, tree, path, fileExists);
        }
        return fileExists;
    }
  
    @Override
    public DeviceContext createContext(String shareName, ConfigElement args)
            throws DeviceContextException
    {
        return diskInterface.createContext(shareName, args);
    }

    @Override
    public void treeOpened(SrvSession sess, TreeConnection tree)
    {
        diskInterface.treeOpened(sess, tree);
        if(getCallbackInterface() != null)
        {
            getCallbackInterface().treeOpened(sess, tree);
        }
    }

    @Override
    public void treeClosed(SrvSession sess, TreeConnection tree)
    {
        diskInterface.treeClosed(sess, tree);
        
        if(getCallbackInterface() != null)
        {
            getCallbackInterface().treeClosed(sess, tree);
        }
    }

    @Override
    public void closeFile(SrvSession sess, TreeConnection tree,
            NetworkFile param) throws IOException
    {
        if(logger.isDebugEnabled())
        {
            logger.debug("closeFile");
        }
        
        ContentContext tctx = (ContentContext) tree.getContext();
        NodeRef rootNode = tctx.getRootNode();
        
        DriverState driverState = getDriverState(sess);
             
        String[] paths = FileName.splitPath(param.getFullName());
        String folder = paths[0];
        String file = paths[1];
        
        EvaluatorContext ctx = driverState.contextMap.get(folder);
        if(ctx == null)
        {
            ctx =  ruleEvaluator.createContext();
            driverState.contextMap.put(folder, ctx);
            if(logger.isDebugEnabled())
            {
                logger.debug("new driver context: " + folder);
            }
        }

        Operation o = new CloseFileOperation(file, param, rootNode, param.getFullName(), param.hasDeleteOnClose());
        Command c = ruleEvaluator.evaluate(ctx, o);
        
        commandExecutor.execute(sess, tree, c);

        if(getCallbackInterface() != null)
        {
            getCallbackInterface().closeFile(sess, tree, param);
        }
    }

    @Override
    public void createDirectory(SrvSession sess, TreeConnection tree,
            FileOpenParams params) throws IOException
    {
        diskInterface.createDirectory(sess, tree, params);
        
        if(getCallbackInterface() != null)
        {
            getCallbackInterface().createDirectory(sess, tree, params);
        }
    }

    @Override
    public NetworkFile createFile(SrvSession sess, TreeConnection tree,
            FileOpenParams params) throws IOException
    {
        if(logger.isDebugEnabled())
        {
            logger.debug("createFile");
        }
        
        ContentContext tctx = (ContentContext) tree.getContext();
        NodeRef rootNode = tctx.getRootNode();
        
        String[] paths = FileName.splitPath(params.getPath());
        String folder = paths[0];
        String file = paths[1];
        
        DriverState driverState = getDriverState(sess);
        EvaluatorContext ctx = getEvaluatorContext(driverState, folder);

        Operation o = new CreateFileOperation(file, rootNode, params.getPath());
        Command c = ruleEvaluator.evaluate(ctx, o);
        
        Object ret = commandExecutor.execute(sess, tree, c);
        
        if(ret != null && ret instanceof NetworkFile)
        {   
            if(getCallbackInterface() != null)
            {
                getCallbackInterface().createFile(sess, tree, params, (NetworkFile)ret);
            }
            
            return (NetworkFile)ret;
        }
        else
        {
            // Error - contact broken
            logger.error("contract broken - NetworkFile not returned");
            return null;
        }
    }

    @Override
    public void deleteDirectory(SrvSession sess, TreeConnection tree, String dir)
            throws IOException
    {
              
        diskInterface.deleteDirectory(sess, tree, dir);
        
        if(getCallbackInterface() != null)
        {
            getCallbackInterface().deleteDirectory(sess, tree, dir);
        }
    
        
    }

    @Override
    public void deleteFile(SrvSession sess, TreeConnection tree, String name)
            throws IOException
    {
        if(logger.isDebugEnabled())
        {
            logger.debug("deleteFile name:" + name);
        }
        
        ContentContext tctx = (ContentContext) tree.getContext();
        NodeRef rootNode = tctx.getRootNode();
        
        DriverState driverState = getDriverState(sess);
             
        String[] paths = FileName.splitPath(name);
        String folder = paths[0];
        String file = paths[1];
        
        EvaluatorContext ctx = getEvaluatorContext(driverState, folder);
    
        Operation o = new DeleteFileOperation(file, rootNode, name);
        Command c = ruleEvaluator.evaluate(ctx, o);
        commandExecutor.execute(sess, tree, c);
        
        if(getCallbackInterface() != null)
        {
            getCallbackInterface().deleteFile(sess, tree, name);
        }
    
    } // End of deleteFile

    @Override
    public void flushFile(SrvSession sess, TreeConnection tree, NetworkFile file)
            throws IOException
    {
        diskInterface.flushFile(sess, tree, file);   
        if(getCallbackInterface() != null)
        {
            getCallbackInterface().flushFile(sess, tree, file);
        }
    }

    @Override
    public boolean isReadOnly(SrvSession sess, DeviceContext ctx)
            throws IOException
    {
        boolean isReadOnly = diskInterface.isReadOnly(sess, ctx);
        if(getCallbackInterface() != null)
        {
            getCallbackInterface().isReadOnly(sess, ctx, isReadOnly);
        }
        
        return isReadOnly;
    }

    @Override
    public NetworkFile openFile(SrvSession sess, TreeConnection tree,
            FileOpenParams param) throws IOException
    {
        String path = param.getPath();
        
        if(logger.isDebugEnabled())
        {
            int sharedAccess = param.getSharedAccess();
            String strSharedAccess = "none"; 
            
            switch(sharedAccess)
            {
                case SharingMode.NOSHARING:
                    strSharedAccess = "nosharing"; 
                    break;
                case SharingMode.READ: 
                    strSharedAccess = "read"; 
                    break;
                case SharingMode.WRITE:
                    strSharedAccess = "write"; 
                    break;
                case SharingMode.READWRITE: 
                    strSharedAccess = "read-write"; 
                    break;
                case SharingMode.DELETE: 
                    strSharedAccess = "delete"; 
                    break;
            }
            
            logger.debug("openFile:" + path 
            + ", isDirectory: " + param.isDirectory()
            + ", isStream: " + param.isStream()
            + ", readOnlyAccess: " + param.isReadOnlyAccess()
            + ", readWriteAccess: " + param.isReadWriteAccess()
            + ", writeOnlyAccess:" +param.isWriteOnlyAccess()
            + ", attributesOnlyAccess:" +param.isAttributesOnlyAccess()
            + ", sequentialAccessOnly:" + param.isSequentialAccessOnly()
            + ", requestBatchOpLock:" +param.requestBatchOpLock()
            + ", requestExclusiveOpLock:" +param.requestExclusiveOpLock()  
            + ", isDeleteOnClose:" +param.isDeleteOnClose()
            + ", sharedAccess: " + strSharedAccess

            );
        }
        
        ContentContext tctx = (ContentContext) tree.getContext();
        NodeRef rootNode = tctx.getRootNode();
        
        DriverState driverState = getDriverState(sess);
             
        String[] paths = FileName.splitPath(path);
        String folder = paths[0];
        String file = paths[1];
        
        EvaluatorContext ctx = getEvaluatorContext(driverState, folder);
              
        boolean writeAccess = param.isReadWriteAccess();
        boolean truncate = param.isOverwrite();

        Operation o = new OpenFileOperation(file, writeAccess, truncate, rootNode, path);
        Command c = ruleEvaluator.evaluate(ctx, o);
        Object ret = commandExecutor.execute(sess, tree, c);

        if(ret != null && ret instanceof NetworkFile)
        {
            if(getCallbackInterface() != null)
            {
                getCallbackInterface().openFile(sess, tree, param, (NetworkFile)ret);
            }
             
            if(logger.isDebugEnabled())
            {
                logger.debug("returning open file: for path:" + path +", ret:" + ret);
            }
            return (NetworkFile)ret;
        }
        else
        {
            // Error - contact broken
            logger.error("contract broken - NetworkFile not returned");
            return null;
        }

        //return diskInterface.openFile(sess, tree, params);
    } // End of OpenFile

    @Override
    public int readFile(SrvSession sess, TreeConnection tree, NetworkFile file,
            byte[] buf, int bufPos, int siz, long filePos) throws IOException
    {
        int readSize = diskInterface.readFile(sess, tree, file, buf, bufPos, siz, filePos);
        
        if(getCallbackInterface() != null)
        {
            getCallbackInterface().readFile(sess, tree, file, buf, bufPos, siz, filePos, readSize);
        }
        
        return readSize;
    }

    @Override
    public void renameFile(SrvSession sess, TreeConnection tree,
            String oldPath, String newPath) throws IOException
    {
        ContentContext tctx = (ContentContext) tree.getContext();
        NodeRef rootNode = tctx.getRootNode();
        
        if(logger.isDebugEnabled())
        {
            logger.debug("renameFile oldPath:" + oldPath + ", newPath:" + newPath);
        }
        
        DriverState driverState = getDriverState(sess);
        
        // Is this a rename within the same folder or a move between folders?
        
        String[] paths = FileName.splitPath(oldPath);
        String oldFolder = paths[0];
        String oldFile = paths[1];
        
        paths = FileName.splitPath(newPath);
        String newFolder = paths[0];
        String newFile = paths[1];
                
        if(oldFolder.equalsIgnoreCase(newFolder))
        {
            logger.debug("renameFileCommand - is a rename within the same folder");
            
            EvaluatorContext ctx = getEvaluatorContext(driverState, oldFolder);
            
            Operation o = new RenameFileOperation(oldFile, newFile, oldPath, newPath, rootNode);
            Command c = ruleEvaluator.evaluate(ctx, o); 
            commandExecutor.execute(sess, tree, c);
        }
        else    
        {
            logger.debug("move - call renameFile directly");
//            // TODO Use old interface for rename/move until think 
//            // through move operation and how it applies to the evaluator contexts
//            // plural since there will be two contexts.
//            logger.debug("move");
//            Operation o = new MoveFileOperation(oldFile, newFile);
//            Command c = ruleEvaluator.evaluate(ctx, o);
//            
//            commandExecutor.execute(sess, tree, c);
            
              diskInterface.renameFile(sess, tree, oldPath, newPath);

        }
        if(getCallbackInterface() != null)
        {
            getCallbackInterface().renameFile(sess, tree, oldPath, newPath);
        }
    }

    @Override
    public long seekFile(SrvSession sess, TreeConnection tree,
            NetworkFile file, long pos, int typ) throws IOException
    {
        long ret = diskInterface.seekFile(sess, tree, file, pos, typ);
        if(getCallbackInterface() != null)
        {
            getCallbackInterface().seekFile(sess, tree, file, pos, typ);
        }
        
        return ret;
    }

    @Override
    public void setFileInformation(SrvSession sess, TreeConnection tree,
            String name, FileInfo info) throws IOException
    {
        diskInterface.setFileInformation(sess, tree, name, info);
        if(getCallbackInterface() != null)
        {
            getCallbackInterface().setFileInformation(sess, tree, name, info);
        }
    }

    @Override
    public SearchContext startSearch(SrvSession sess, TreeConnection tree,
            String searchPath, int attrib) throws FileNotFoundException
    {
        SearchContext context = diskInterface.startSearch(sess, tree, searchPath, attrib);
        if(getCallbackInterface() != null)
        {
            getCallbackInterface().startSearch(sess, tree, searchPath, attrib, context);
        }
        return context;
    }

    @Override
    public void truncateFile(SrvSession sess, TreeConnection tree,
            NetworkFile file, long siz) throws IOException
    {
        diskInterface.truncateFile(sess, tree, file, siz);
        if(getCallbackInterface() != null)
        {
            getCallbackInterface().truncateFile(sess, tree, file, siz);
        }
    }

    @Override
    public int writeFile(SrvSession sess, TreeConnection tree,
            NetworkFile file, byte[] buf, int bufoff, int siz, long fileoff)
            throws IOException
    {
        int writeSize = diskInterface.writeFile(sess, tree, file, buf, bufoff, siz, fileoff);
        if(getCallbackInterface() != null)
        {
            getCallbackInterface().writeFile(sess, tree, file, buf, bufoff, siz, fileoff, writeSize);
        }
        
        return writeSize;
    }

    public void setDiskInterface(ExtendedDiskInterface diskInterface)
    {
        this.diskInterface = diskInterface;
    }

    public ExtendedDiskInterface getDiskInterface()
    {
        return diskInterface;
    }

    public void setRuleEvaluator(RuleEvaluator ruleEvaluator)
    {
        this.ruleEvaluator = ruleEvaluator;
    }

    public RuleEvaluator getRuleEvaluator()
    {
        return ruleEvaluator;
    }

    @Override
    public void registerContext(DeviceContext ctx, ServerConfigurationBean serverConfig)
            throws DeviceContextException
    {
        diskInterface.registerContext(ctx, serverConfig); 
    }

    public void setRepositoryDiskInterface(RepositoryDiskInterface repositoryDiskInterface)
    {
        this.repositoryDiskInterface = repositoryDiskInterface;
    }

    public RepositoryDiskInterface getRepositoryDiskInterface()
    {
        return repositoryDiskInterface;
    }

    public void setCommandExecutor(CommandExecutor commandExecutor)
    {
        this.commandExecutor = commandExecutor;
    }

    public CommandExecutor getCommandExecutor()
    {
        return commandExecutor;
    } 
    

    /**
     * Get the driver state from the session.
     * @param sess
     * @return the driver state.
     */
    private DriverState getDriverState(SrvSession sess)
    {
    
        synchronized (sess)
        {
            // Get the driver state
            Object state = sess.getDriverState();
            if(state == null)
            {
                state = new DriverState();
                sess.setDriverState(state);
                if(logger.isDebugEnabled())
                {
                    logger.debug("new driver state created");
                }

            }
            DriverState driverState = (DriverState)state;
            return driverState;
        }
    }

    /**
     * Get the evaluator context from the state and the folder.
     * @param driverState
     * @param folder
     * @return
     */
    private EvaluatorContext getEvaluatorContext(DriverState driverState, String folder)
    {
        synchronized(driverState.contextMap)
        {
            EvaluatorContext ctx = driverState.contextMap.get(folder);
            if(ctx == null)
            {
                ctx =  ruleEvaluator.createContext();
                driverState.contextMap.put(folder, ctx);
                if(logger.isDebugEnabled())
                {
                    logger.debug("new driver context: " + folder);
                }
            }
            return ctx;
        }
    }

    public void setCallbackInterface(ContentDiskCallback callbackInterface)
    {
        this.callbackInterface = callbackInterface;
    }

    public ContentDiskCallback getCallbackInterface()
    {
        return callbackInterface;
    }


}
  