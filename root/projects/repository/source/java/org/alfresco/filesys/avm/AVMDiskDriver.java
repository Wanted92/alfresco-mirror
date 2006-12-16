/*
 * Copyright (C) 2006 Alfresco, Inc.
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

package org.alfresco.filesys.avm;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.SortedMap;
import java.util.StringTokenizer;

import javax.transaction.UserTransaction;

import org.alfresco.config.ConfigElement;
import org.alfresco.filesys.alfresco.AlfrescoDiskDriver;
import org.alfresco.filesys.server.SrvSession;
import org.alfresco.filesys.server.core.DeviceContext;
import org.alfresco.filesys.server.core.DeviceContextException;
import org.alfresco.filesys.server.core.DeviceInterface;
import org.alfresco.filesys.server.filesys.AccessDeniedException;
import org.alfresco.filesys.server.filesys.DirectoryNotEmptyException;
import org.alfresco.filesys.server.filesys.DiskInterface;
import org.alfresco.filesys.server.filesys.FileAttribute;
import org.alfresco.filesys.server.filesys.FileExistsException;
import org.alfresco.filesys.server.filesys.FileInfo;
import org.alfresco.filesys.server.filesys.FileName;
import org.alfresco.filesys.server.filesys.FileOpenParams;
import org.alfresco.filesys.server.filesys.FileStatus;
import org.alfresco.filesys.server.filesys.NetworkFile;
import org.alfresco.filesys.server.filesys.PathNotFoundException;
import org.alfresco.filesys.server.filesys.SearchContext;
import org.alfresco.filesys.server.filesys.TreeConnection;
import org.alfresco.filesys.server.pseudo.PseudoFile;
import org.alfresco.filesys.server.pseudo.PseudoFileList;
import org.alfresco.filesys.server.pseudo.PseudoFolderNetworkFile;
import org.alfresco.filesys.server.state.FileState;
import org.alfresco.filesys.util.StringList;
import org.alfresco.filesys.util.WildCard;
import org.alfresco.repo.avm.CreateStoreTxnListener;
import org.alfresco.repo.avm.CreateVersionTxnListener;
import org.alfresco.repo.avm.PurgeStoreTxnListener;
import org.alfresco.repo.avm.PurgeVersionTxnListener;
import org.alfresco.repo.security.authentication.AuthenticationComponent;
import org.alfresco.service.cmr.avm.AVMExistsException;
import org.alfresco.service.cmr.avm.AVMNodeDescriptor;
import org.alfresco.service.cmr.avm.AVMNotFoundException;
import org.alfresco.service.cmr.avm.AVMService;
import org.alfresco.service.cmr.avm.AVMStoreDescriptor;
import org.alfresco.service.cmr.avm.AVMWrongTypeException;
import org.alfresco.service.cmr.avm.VersionDescriptor;
import org.alfresco.service.cmr.repository.MimetypeService;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.transaction.TransactionService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * AVM Repository Filesystem Driver Class
 * 
 * <p>Provides a filesystem interface for various protocols such as SMB/CIFS and FTP.
 * 
 * @author GKSpencer
 */
public class AVMDiskDriver extends AlfrescoDiskDriver implements DiskInterface {

    // Logging
    
    private static final Log logger = LogFactory.getLog(AVMDiskDriver.class);
    
    // Configuration key names
    
    private static final String KEY_STORE 		= "storePath";
    private static final String KEY_VERSION		= "version";
    private static final String KEY_CREATE		= "createStore";

    // AVM path seperator
    
    public static final char AVM_SEPERATOR			= '/';
    public static final String AVM_SEPERATOR_STR	= "/";
    
    // Services and helpers
    
    private AVMService m_avmService;
    private TransactionService m_transactionService;
    private MimetypeService m_mimetypeService;
    
    private AuthenticationComponent m_authComponent;
    private AuthenticationService m_authService;
    
    // AVM listeners
    
    private CreateStoreTxnListener m_createStoreListener;
    private PurgeStoreTxnListener m_purgeStoreListener;
    
    private CreateVersionTxnListener m_createVerListener;
    private PurgeVersionTxnListener m_purgeVerListener;
    
    /**
     * Default constructor
     */
    public AVMDiskDriver()
    {
    }
    
    /**
     * Return the AVM service
     * 
     * @return AVMService
     */
    public final AVMService getAvmService()
    {
    	return m_avmService;
    }
    
    /**
     * Return the authentication service
     * 
     * @return AuthenticationService
     */
    public final AuthenticationService getAuthenticationService()
    {
    	return m_authService;
    }
    
    /**
     * Return the transaction service
     * 
     * @return TransactionService
     */
    public final TransactionService getTransactionService()
    {
    	return m_transactionService;
    }
    
    /**
     * Set the AVM service
     * 
     * @param avmService AVMService
     */
    public void setAvmService(AVMService avmService)
    {
    	m_avmService = avmService;
    }
    
    /**
     * Set the transaction service
     * 
     * @param transactionService the transaction service
     */
    public void setTransactionService(TransactionService transactionService)
    {
        m_transactionService = transactionService;
    }

    /**
     * Set the authentication component
     * 
     * @param authComponent AuthenticationComponent
     */
    public void setAuthenticationComponent(AuthenticationComponent authComponent)
    {
        m_authComponent = authComponent;
    }

    /**
     * Set the authentication service
     * 
     * @param authService AuthenticationService
     */
    public void setAuthenticationService(AuthenticationService authService)
    {
    	m_authService = authService;
    }
    
    /**
     * Set the mimetype service
     * 
     * @param mimetypeService MimetypeService
     */
    public void setMimetypeService(MimetypeService mimetypeService)
    {
        m_mimetypeService = mimetypeService;
    }
    
    /**
     * Set the create store listener
     * 
     * @param createStoreListener CreateStoreTxnListener
     */
    public void setCreateStoreListener(CreateStoreTxnListener createStoreListener)
    {
    	m_createStoreListener = createStoreListener;
    }
    
    /**
     * Set the purge store listener
     * 
     * @param purgeStoreListener PurgeStoreTxnListener
     */
    public void setPurgeStoreListener(PurgeStoreTxnListener purgeStoreListener)
    {
    	m_purgeStoreListener = purgeStoreListener;
    }
    
    /**
     * Set the create version listener
     * 
     * @param createVersionListener CreateVersionTxnListener
     */
    public void setCreateVersionListener(CreateVersionTxnListener createVersionListener)
    {
    	m_createVerListener = createVersionListener;
    }
    
    /**
     * Set the purge version listener
     * 
     * @param purgeVersionListener PurgeVersionTxnListener
     */
    public void setPurgeVersionListener(PurgeVersionTxnListener purgeVersionListener)
    {
    	m_purgeVerListener = purgeVersionListener;
    }
    
    /**
     * Parse and validate the parameter string and create a device context object for this instance
     * of the shared device.
     * 
     * @param devIface DeviceInterface
     * @param name String
     * @param cfg ConfigElement
     * @return DeviceContext
     * @exception DeviceContextException
     */
    public DeviceContext createContext(DeviceInterface devIface, String name, ConfigElement cfg)
    	throws DeviceContextException
    {
        // Use the system user as the authenticated context for the filesystem initialization
        
        m_authComponent.setCurrentUser( m_authComponent.getSystemUserName());
        
        // Wrap the initialization in a transaction
        
        UserTransaction tx = m_transactionService.getUserTransaction(false);

        AVMContext context = null;
        
        try
        {
            // Start the transaction
            
            if ( tx != null)
                tx.begin();
            
            // Check if the share is a virtualization view
            
            ConfigElement virtElem = cfg.getChild( "virtualView");
            if ( virtElem != null)
            {
            	// Create the context
            	
            	context = new AVMContext( name);
            	
	            // Enable file state caching
	            
	            context.enableStateTable( true, getStateReaper());
	            
	            // Get a list of the available AVM stores
	            
	            List<AVMStoreDescriptor> storeList = m_avmService.getStores();
	            
	            if ( storeList != null && storeList.size() > 0)
	            {
	            	// Create a file state for the root folder that does not expire, and add pseudo files
	            	// for the stores

	            	FileState rootState = context.getStateTable().findFileState( FileName.DOS_SEPERATOR_STR, true, true);
	            	rootState.setExpiryTime( FileState.NoTimeout);
	            	
	            	// Add pseudo files for the stores
	            	
	            	for ( AVMStoreDescriptor storeDesc : storeList)
	            	{
	            		// Add a pseudo file for the current store
	            		
	            		rootState.addPseudoFile( new StorePseudoFile( storeDesc));
	            	}
	            }
	            
	            // Plug the virtualization view context into the various store/version call back listeners
	            // so that store/version pseudo folders can be kept in sync with AVM
	            
	            m_createStoreListener.addCallback( context);
	            m_purgeStoreListener.addCallback( context);
	            
	            m_createVerListener.addCallback( context);
	            m_purgeVerListener.addCallback( context);
            }
            else
            {
	            // Get the store path
	            
	            ConfigElement storeElement = cfg.getChild(KEY_STORE);
	            if (storeElement == null || storeElement.getValue() == null || storeElement.getValue().length() == 0)
	                throw new DeviceContextException("Device missing init value: " + KEY_STORE);
	            
	            String storePath = storeElement.getValue();
	
	            // Get the version if specified, or default to the head version
	            
	            int version = AVMContext.VERSION_HEAD;
	            
	            ConfigElement versionElem = cfg.getChild(KEY_VERSION);
	            if ( versionElem != null)
	            {
	            	// Check if the version is valid
	            	
	            	if ( versionElem.getValue() == null || versionElem.getValue().length() == 0)
	            		throw new DeviceContextException("Store version not specified");
	            	
	            	// Validate the version id
	            	
	            	try
	            	{
	            		version = Integer.parseInt( versionElem.getValue());
	            	}
	            	catch ( NumberFormatException ex)
	            	{
	            		throw new DeviceContextException("Invalid store version specified, " + versionElem.getValue());
	            	}
	            	
	            	// Range check the version id
	            	
	            	if ( version < 0 && version != AVMContext.VERSION_HEAD)
	            		throw new DeviceContextException("Invalid store version id specified, " + version);
	            }
	            
	            // Check if the create flag is enabled
	            
	            ConfigElement createStore = cfg.getChild( KEY_CREATE);

	            // Validate the store path
	            
	            AVMNodeDescriptor rootNode = m_avmService.lookup( version, storePath);
	            if ( rootNode == null)
	            {
	            	// Check if the store should be created
	            	
	            	if ( createStore == null || version != AVMContext.VERSION_HEAD)
	            		throw new DeviceContextException("Invalid store path/version, " + storePath + " (" + version + ")");
	            	
	            	// Parse the store path
	            	
	            	String storeName = null;
	            	String path = null;
	            	
	            	int pos = storePath.indexOf(":/");
	            	if ( pos != -1)
	            	{
	            		storeName = storePath.substring(0, pos);
	            		if ( storePath.length() > pos)
	            			path = storePath.substring(pos + 2);
	            	}
	            	else
	            		storeName = storePath;
	            	
	            	// Check if the store exists
	            	
	            	AVMStoreDescriptor storeDesc = null;
	            	
	            	try
	            	{
	            		storeDesc = m_avmService.getStore( storeName);
	            	}
	            	catch (AVMNotFoundException ex)
	            	{
	            	}
	            	
	            	// Create a new store if it does not exist
	            	
	            	if ( storeDesc == null)
	            		m_avmService.createStore( storeName);
	            	
	            	// Check if there is an optional path
	            	
	            	if ( path != null)
	            	{
	            		// Split the path
	            		
	            		StringTokenizer tokens = new StringTokenizer( path, AVMPath.AVM_SEPERATOR_STR);
	            		StringList paths = new StringList();
	            		
	            		while ( tokens.hasMoreTokens())
	            			paths.addString( tokens.nextToken());
	            		
	            		// Create the path, or folders that do not exist
	            		
	            		AVMPath curPath = new AVMPath( storeName, version, FileName.DOS_SEPERATOR_STR);
	            		AVMNodeDescriptor curDesc = m_avmService.lookup( curPath.getVersion(), curPath.getAVMPath());

            			// Walk the path checking creating each folder as required
	            			
	            		for ( int i = 0; i < paths.numberOfStrings(); i++)
	            		{
	            			AVMNodeDescriptor nextDesc = null;
	            			
	            			try
	            			{
	            				// Check if the child folder exists
		            			
		            			nextDesc = m_avmService.lookup( curDesc, paths.getStringAt( i));
		            		}
		            		catch ( AVMNotFoundException ex)
		            		{
		            		}
		            		
		            		// Check if the folder exists
		            		
		            		if ( nextDesc == null)
		            		{
		            			// Create the new folder
		            			
		            			m_avmService.createDirectory( curPath.getAVMPath(), paths.getStringAt( i));
		            			
		            			// Get the details of the new folder
		            			
		            			nextDesc = m_avmService.lookup( curDesc, paths.getStringAt( i));
		            		}
		            		else if ( nextDesc.isFile())
		            			throw new DeviceContextException("Path element error, not a folder, " + paths.getStringAt( i));
		            		
		            		// Step to the next level
		            		
		            		curPath.parsePath( storeName, version, curPath.getRelativePath() + paths.getStringAt( i) + FileName.DOS_SEPERATOR_STR);
		            		curDesc = nextDesc;
	            		}
	            	}
	            	
	            	// Validate the store path again
	            	
	            	rootNode = m_avmService.lookup( version, storePath);
	            	if ( rootNode == null)
	            		throw new DeviceContextException("Failed to create new store " + storePath);
	            }
	            
	            // Create the context
	            
	            context = new AVMContext( name, storePath, version);

	            // Enable file state caching
	            
	            context.enableStateTable( true, getStateReaper());
            }

            // Commit the transaction
            
            tx.commit();
            tx = null;
        }
        catch (Exception ex)
        {
            logger.error("Error during create context", ex);
            
            // Rethrow the exception
            
            throw new DeviceContextException("Driver setup error, " + ex.getMessage());
        }
        finally
        {
            // If there is an active transaction then roll it back
            
            if ( tx != null)
            {
                try
                {
                    tx.rollback();
                }
                catch (Exception ex)
                {
                    logger.warn("Failed to rollback transaction", ex);
                }
            }
        }

        // Return the context for this shared filesystem
        
        return context;
    }

    /**
     * Return a list of the available AVM store names
     * 
     * @return StringList
     */
    public final StringList getAVMStoreNames()
    {
        // Use the system user as the authenticated context to get the AVM store list
        
        m_authComponent.setCurrentUser( m_authComponent.getSystemUserName());
        
        // Wrap the service request in a transaction
        
        UserTransaction tx = m_transactionService.getUserTransaction(false);

        StringList storeNames = new StringList();
        
        try
        {
            // Start the transaction
            
            if ( tx != null)
                tx.begin();

            // Get the list of AVM stores
            
            List<AVMStoreDescriptor> storeList = m_avmService.getStores();
            
            if ( storeList != null)
            {
            	for ( AVMStoreDescriptor storeDesc : storeList)
            		storeNames.addString( storeDesc.getName());
            }

            // Commit the transaction
            
            tx.commit();
            tx = null;
        }
        catch (Exception ex)
        {
	        logger.error("Error during create context", ex);
	    }
	    finally
	    {
	        // If there is an active transaction then roll it back
	        
	        if ( tx != null)
	        {
	            try
	            {
	                tx.rollback();
	            }
	            catch (Exception ex)
	            {
	                logger.warn("Failed to rollback transaction", ex);
	            }
	        }
	    }
	    
	    // Return the list of AVM store names
	    
	    return storeNames;
    }
    
    /**
     * Build the full store path for a file/folder using the share relative path
     * 
     * @param ctx AVMContext
     * @param path String
     * @return AVMPath
     */
    protected final AVMPath buildStorePath( AVMContext ctx, String path)
    {
    	// Check if the AVM filesystem is a normal or virtualization view

    	AVMPath avmPath = null;
    	
    	if ( ctx.isVirtualizationView())
    	{
    		// Create a path for the virtualization view
    		
    		avmPath = new AVMPath( path);
    	}
    	else
    	{
    		// Create a path to a single store/version
    		
    		avmPath = new AVMPath( ctx.getStorePath(), ctx.isVersion(), path);
    	}
    	
    	// Return the path
    	
    	return avmPath;
    }
    
	/**
     * Close the file.
     * 
     * @param sess Server session
     * @param tree Tree connection.
     * @param file Network file context.
     * @exception java.io.IOException If an error occurs.
     */
    public void closeFile(SrvSession sess, TreeConnection tree, NetworkFile file)
    	throws java.io.IOException
    {
    	// DEBUG
    	
    	if ( logger.isDebugEnabled())
    		logger.debug("Close file " + file.getFullName());
    	
        //	Close the file
        
        file.closeFile();

        //	Check if the file/directory is marked for delete
      	
      	if ( file.hasDeleteOnClose()) {
      		
      		//	Check for a file or directory
      		
      		if ( file.isDirectory())
      			deleteDirectory(sess, tree, file.getFullName());
      		else
      			deleteFile(sess, tree, file.getFullName());
      	}
    }

    /**
     * Create a new directory on this file system.
     * 
     * @param sess Server session
     * @param tree Tree connection.
     * @param params Directory create parameters
     * @exception java.io.IOException If an error occurs.
     */
    public void createDirectory(SrvSession sess, TreeConnection tree, FileOpenParams params)
    	throws java.io.IOException
    {
    	// Check if the filesystem is writable
    	
    	AVMContext ctx = (AVMContext) tree.getContext();
    	if ( ctx.isVersion() != AVMContext.VERSION_HEAD)
    		throw new AccessDeniedException("Cannot create " + params.getPath() + ", filesys not writable");
    	
    	// Split the path to get the new folder name and relative path
    	
    	String[] paths = FileName.splitPath( params.getPath());
    	
    	// Convert the relative path to a store path
    	
    	AVMPath storePath = buildStorePath( ctx, paths[0]);
    	
    	// DEBUG
    	
    	if ( logger.isDebugEnabled())
    		logger.debug("Create directory params=" + params + ", storePath=" + storePath + ", name=" + paths[1]);
    	
    	// Check if the filesystem is the virtualization view
    	
    	if ( ctx.isVirtualizationView() && storePath.isReadOnlyPseudoPath())
    	{
    		throw new AccessDeniedException( "Cannot create folder in store/version layer, " + params.getPath());
    	}
    	
    	// Create a new file
    	
    	sess.beginWriteTransaction( m_transactionService);
    	
    	try
    	{
    		// Create the new file entry

    		m_avmService.createDirectory( storePath.getAVMPath(), paths[1]);
    	}
    	catch ( AVMExistsException ex)
    	{
    		throw new FileExistsException( params.getPath());
    	}
    	catch ( AVMNotFoundException ex)
    	{
    		throw new FileNotFoundException( params.getPath());
    	}
    	catch ( AVMWrongTypeException ex)
    	{
    		throw new FileNotFoundException( params.getPath());
    	}
    }
    
    /**
     * Create a new file on the file system.
     * 
     * @param sess Server session
     * @param tree Tree connection
     * @param params File create parameters
     * @return NetworkFile
     * @exception java.io.IOException If an error occurs.
     */
    public NetworkFile createFile(SrvSession sess, TreeConnection tree, FileOpenParams params)
    	throws java.io.IOException
    {
    	// Check if the filesystem is writable
    	
    	AVMContext ctx = (AVMContext) tree.getContext();
    	
    	// Split the path to get the file name and relative path
    	
    	String[] paths = FileName.splitPath( params.getPath());
    	
    	// Convert the relative path to a store path
    	
    	AVMPath storePath = buildStorePath( ctx, paths[0]);
    	
    	// DEBUG
    	
    	if ( logger.isDebugEnabled())
    		logger.debug("Create file params=" + params + ", storePath=" + storePath + ", name=" + paths[1]);
    	
    	// Check if the filesystem is the virtualization view
    	
    	if ( ctx.isVirtualizationView() && storePath.isReadOnlyPseudoPath())
    	{
    		throw new AccessDeniedException( "Cannot create file in store/version layer, " + params.getPath());
    	}
    	else if ( storePath.getVersion() != AVMContext.VERSION_HEAD)
    	{
    		throw new AccessDeniedException("Cannot create " + params.getPath() + ", filesys not writable");
    	}
    	
    	// Create a new file
    	
    	sess.beginWriteTransaction( m_transactionService);
    	
    	AVMNetworkFile netFile = null;
    	
    	try
    	{
    		// Create the new file entry

    		m_avmService.createFile( storePath.getAVMPath(), paths[1]).close();

    		// Get the new file details
    		
    		AVMPath fileStorePath = buildStorePath( ctx, params.getPath());
    		AVMNodeDescriptor nodeDesc = m_avmService.lookup( fileStorePath.getVersion(), fileStorePath.getAVMPath());

    		if ( nodeDesc != null)
	    	{
	    	    //	Create the network file object for the new file
	    	    
	    	    netFile = new AVMNetworkFile( nodeDesc, fileStorePath.getAVMPath(), fileStorePath.getVersion(), m_avmService);
    	    	netFile.setGrantedAccess(NetworkFile.READWRITE);
	    	    netFile.setFullName(params.getPath());
	    	    
	    	    // Set the mime-type for the new file
	    	    
	    	    netFile.setMimeType( m_mimetypeService.guessMimetype( paths[1]));
	    	}
    	}
    	catch ( AVMExistsException ex)
    	{
    		throw new FileExistsException( params.getPath());
    	}
    	catch ( AVMNotFoundException ex)
    	{
    		throw new FileNotFoundException( params.getPath());
    	}
    	catch ( AVMWrongTypeException ex)
    	{
    		throw new FileNotFoundException( params.getPath());
    	}
    	
    	// Return the file
    	
    	return netFile;
    }
    
    /**
     * Delete the directory from the filesystem.
     * 
     * @param sess Server session
     * @param tree Tree connection
     * @param dir Directory name.
     * @exception java.io.IOException The exception description.
     */
    public void deleteDirectory(SrvSession sess, TreeConnection tree, String dir)
    	throws java.io.IOException
    {
    	// Convert the relative path to a store path
    	
    	AVMContext ctx = (AVMContext) tree.getContext();
    	AVMPath storePath = buildStorePath( ctx, dir);
    	
    	// DEBUG
    	
    	if ( logger.isDebugEnabled())
    		logger.debug("Delete directory, path=" + dir + ", storePath=" + storePath);
    	
    	// Check if the filesystem is the virtualization view
    	
    	if ( ctx.isVirtualizationView() && storePath.isPseudoPath())
    	{
    		throw new AccessDeniedException( "Cannot delete pseudo folder, " + dir);
    	}
    	
    	// Make sure the path is to a folder before deleting it
    	
    	sess.beginWriteTransaction( m_transactionService);
    	
    	try
    	{
    		AVMNodeDescriptor nodeDesc = m_avmService.lookup( storePath.getVersion(), storePath.getAVMPath());
	    	if ( nodeDesc != null)
	    	{
	    		// Check that we are deleting a folder
	    		
	    		if ( nodeDesc.isDirectory())
	    		{
	    			// Make sure the directory is empty
	    			
	    			SortedMap<String, AVMNodeDescriptor> fileList = m_avmService.getDirectoryListing( nodeDesc);
	    			if ( fileList != null && fileList.size() > 0)
	    				throw new DirectoryNotEmptyException( dir);
	    			
	    			// Delete the folder
	    			
	    			m_avmService.removeNode( storePath.getAVMPath());
	    		}
	    		else
	    			throw new IOException( "Delete directory path is not a directory, " + dir);
	    	}
    	}
    	catch ( AVMNotFoundException ex)
    	{
    		throw new IOException( "Directory not found, " + dir);
    	}
    	catch ( AVMWrongTypeException ex)
    	{
    		throw new IOException( "Invalid path, " + dir);
    	}
    }

    /**
     * Delete the specified file.
     * 
     * @param sess Server session
     * @param tree Tree connection
     * @param file NetworkFile
     * @exception java.io.IOException The exception description.
     */
    public void deleteFile(SrvSession sess, TreeConnection tree, String name)
    	throws java.io.IOException
    {
    	// Convert the relative path to a store path
    	
    	AVMContext ctx = (AVMContext) tree.getContext();
    	AVMPath storePath = buildStorePath( ctx, name);
    	
    	// DEBUG
    	
    	if ( logger.isDebugEnabled())
    		logger.debug("Delete file, path=" + name + ", storePath=" + storePath);
    	
    	// Check if the filesystem is the virtualization view
    	
    	if ( ctx.isVirtualizationView() && storePath.isPseudoPath())
    	{
    		throw new AccessDeniedException( "Cannot delete pseudo file, " + name);
    	}
    	
    	// Make sure the path is to a file before deleting it
    	
    	sess.beginWriteTransaction( m_transactionService);
    	
    	try
    	{
    		AVMNodeDescriptor nodeDesc = m_avmService.lookup( storePath.getVersion(), storePath.getAVMPath());
	    	if ( nodeDesc != null)
	    	{
	    		// Check that we are deleting a file
	    		
	    		if ( nodeDesc.isFile())
	    		{
	    			// Delete the file
	    			
	    			m_avmService.removeNode( storePath.getAVMPath());
	    		}
	    		else
	    			throw new IOException( "Delete file path is not a file, " + name);
	    	}
    	}
    	catch ( AVMNotFoundException ex)
    	{
    		throw new IOException( "File not found, " + name);
    	}
    	catch ( AVMWrongTypeException ex)
    	{
    		throw new IOException( "Invalid path, " + name);
    	}
    }

    /**
     * Check if the specified file exists, and whether it is a file or directory.
     * 
     * @param sess Server session
     * @param tree Tree connection
     * @param name java.lang.String
     * @return int
     * @see FileStatus
     */
    public int fileExists(SrvSession sess, TreeConnection tree, String name)
    {
    	// Convert the relative path to a store path
    	
    	AVMContext ctx = (AVMContext) tree.getContext();
    	AVMPath storePath = buildStorePath( ctx, name);
    	
    	// DEBUG
    	
    	if ( logger.isDebugEnabled())
    		logger.debug("File exists check, path=" + name + ", storePath=" + storePath);
    	
    	// Check if the path is valid
    	
    	int status = FileStatus.NotExist;
    	
    	if ( storePath.isValid() == false)
    		return status;
    	
    	// Check if the filesystem is the virtualization view
    	
    	if ( ctx.isVirtualizationView() && storePath.isReadOnlyPseudoPath())
    	{
    		// Find the file state for the pseudo folder
    		
    		FileState fstate = findPseudoState( storePath, ctx);
    		
    		if ( fstate != null)
    		{
				// DEBUG
				
				if ( logger.isDebugEnabled())
					logger.debug( "  Found pseudo file " + fstate);
				
    			// Check if the pseudo file is a file or folder
    			
    			if ( fstate.isDirectory())
    				status = FileStatus.DirectoryExists;
    			else
    				status = FileStatus.FileExists;
    		}
   			else
   			{
   				// Invalid pseudo file path
   				
   				status = FileStatus.NotExist;
    		}
    		
    		// Return the file status
    		
    		return status;
    	}
    	
    	// Search for the file/folder
    	
    	sess.beginReadTransaction( m_transactionService);
    	
    	AVMNodeDescriptor nodeDesc = m_avmService.lookup( storePath.getVersion(), storePath.getAVMPath());
    	
    	if ( nodeDesc != null)
    	{
    		// Check if the path is to a file or folder
    		
    		if ( nodeDesc.isDirectory())
    			status = FileStatus.DirectoryExists;
    		else
    			status = FileStatus.FileExists;
    	}
    	
    	// Return the file status
    	
    	return status;
    }

    /**
     * Flush any buffered output for the specified file.
     * 
     * @param sess Server session
     * @param tree Tree connection
     * @param file Network file context.
     * @exception java.io.IOException The exception description.
     */
    public void flushFile(SrvSession sess, TreeConnection tree, NetworkFile file)
    	throws java.io.IOException
    {
        //	Flush the file
        
        file.flushFile();
    }

    /**
     * Get the file information for the specified file.
     * 
     * @param sess Server session
     * @param tree Tree connection
     * @param name File name/path that information is required for.
     * @return File information if valid, else null
     * @exception java.io.IOException The exception description.
     */
    public FileInfo getFileInformation(SrvSession sess, TreeConnection tree, String name)
    	throws java.io.IOException
    {
    	// Convert the relative path to a store path
    	
    	AVMContext ctx = (AVMContext) tree.getContext();
    	AVMPath storePath = buildStorePath( ctx, name);
    	
    	// DEBUG
    	
    	if ( logger.isDebugEnabled())
    		logger.debug("Get file information, path=" + name + ", storePath=" + storePath);

    	// Check if hte path is valid
    	
    	if ( storePath.isValid() == false)
    		throw new FileNotFoundException( name);
    	
    	// Check if the filesystem is the virtualization view
    	
    	if ( ctx.isVirtualizationView() && storePath.isReadOnlyPseudoPath())
    	{
    		// Check if the search path is for the root, a store or version folder
    		
    		if ( storePath.isRootPath())
    		{
    			// Return dummy file informatiom for the root folder
    			
    			return new FileInfo( name, 0L, FileAttribute.Directory);
    		}
    		else
    		{
    			// Find the pseudo file for the store/version folder
    			
    			PseudoFile psFile = findPseudoFolder( storePath, ctx);
    			if ( psFile != null)
    			{
    				// DEBUG
    				
    				if ( logger.isDebugEnabled())
    					logger.debug( "  Found pseudo file " + psFile);
    				return psFile.getFileInfo();
    			}
    			else
    				throw new FileNotFoundException( name);
    		}
    	}
    	
    	// Search for the file/folder
    	
    	sess.beginReadTransaction( m_transactionService);
    	
    	FileInfo info = null;
    	
    	try
    	{
	    	AVMNodeDescriptor nodeDesc = m_avmService.lookup( ctx.isVersion(), storePath.getAVMPath());
	    	
	    	if ( nodeDesc != null)
	    	{
	    		// Create, and fill in, the file information
	    		
	    		info = new FileInfo();
	        	
	        	info.setFileName( nodeDesc.getName());
	        	
	        	if ( nodeDesc.isFile())
	        	{
	        		info.setFileSize( nodeDesc.getLength());
	        		info.setAllocationSize((nodeDesc.getLength() + 512L) & 0xFFFFFFFFFFFFFE00L);
	        	}
	        	else
	        		info.setFileSize( 0L);
	
	        	info.setAccessDateTime( nodeDesc.getAccessDate());
	        	info.setCreationDateTime( nodeDesc.getCreateDate());
	        	info.setModifyDateTime( nodeDesc.getModDate());
	
	        	// Build the file attributes
	        	
	        	int attr = 0;
	        	
	        	if ( nodeDesc.isDirectory())
	        		attr += FileAttribute.Directory;
	        	
	        	if ( nodeDesc.getName().startsWith( ".") ||
	        			nodeDesc.getName().equalsIgnoreCase( "Desktop.ini") ||
	        			nodeDesc.getName().equalsIgnoreCase( "Thumbs.db"))
	        		attr += FileAttribute.Hidden;
	        	
	        	// Mark the file/folder as read-only if not the head version
	        	
	        	if ( ctx.isVersion() != AVMContext.VERSION_HEAD)
	        		attr += FileAttribute.ReadOnly;
	        	
	        	info.setFileAttributes( attr);
	
	        	// DEBUG
	        	
	        	if ( logger.isDebugEnabled())
	        		logger.debug("  File info=" + info);
	    	}
    	}
    	catch ( AVMNotFoundException ex)
    	{
    		throw new FileNotFoundException( name);
    	}
    	catch ( AVMWrongTypeException ex)
    	{
    		throw new PathNotFoundException( name);
    	}
    	
    	// Return the file information
    	
    	return info;
    }

    /**
     * Determine if the disk device is read-only.
     * 
     * @param sess Server session
     * @param ctx Device context
     * @return boolean
     * @exception java.io.IOException If an error occurs.
     */
    public boolean isReadOnly(SrvSession sess, DeviceContext ctx)
    	throws java.io.IOException
    {
    	// Check if the version indicates the head version, only the head is writable
    	
    	AVMContext avmCtx = (AVMContext) ctx;
    	return avmCtx.isVersion() == AVMContext.VERSION_HEAD ? true : false;
    }

    /**
     * Open a file on the file system.
     * 
     * @param sess Server session
     * @param tree Tree connection
     * @param params File open parameters
     * @return NetworkFile
     * @exception java.io.IOException If an error occurs.
     */
    public NetworkFile openFile(SrvSession sess, TreeConnection tree, FileOpenParams params)
    	throws java.io.IOException
    {
    	// Convert the relative path to a store path
    	
    	AVMContext ctx = (AVMContext) tree.getContext();
    	AVMPath storePath = buildStorePath( ctx, params.getPath());
    	
    	// DEBUG
    	
    	if ( logger.isDebugEnabled())
    		logger.debug("Open file params=" + params + ", storePath=" + storePath);
    	
    	// Check if the filesystem is the virtualization view
    	
    	if ( ctx.isVirtualizationView() && storePath.isReadOnlyPseudoPath())
    	{
    		// Check if the path is for the root, a store or version folder
    		
    		if ( storePath.isRootPath())
    		{
    			// Return a dummy file for the root folder
    			
    			return new PseudoFolderNetworkFile( FileName.DOS_SEPERATOR_STR);
    		}
    		else
    		{
    			// Find the pseudo file for the store/version folder
    			
    			PseudoFile psFile = findPseudoFolder( storePath, ctx);
    			if ( psFile != null)
    			{
    				// DEBUG
    				
    				if ( logger.isDebugEnabled())
    					logger.debug( "  Found pseudo file " + psFile);
    				return psFile.getFile( params.getPath());
    			}
    			else
    				return null;
    		}
    	}
    	
    	// Search for the file/folder
    	
    	sess.beginReadTransaction( m_transactionService);
    	
    	AVMNetworkFile netFile = null;
    	
    	try
    	{
    		// Get the details of the file/folder

    		AVMNodeDescriptor nodeDesc = m_avmService.lookup( storePath.getVersion(), storePath.getAVMPath());
    	
	    	if ( nodeDesc != null)
	    	{
	    	    //	Check if the filesystem is read-only and write access has been requested
	    	    
	    	    if ( storePath.getVersion() != AVMContext.VERSION_HEAD && ( params.isReadWriteAccess() || params.isWriteOnlyAccess()))
	    	      throw new AccessDeniedException("File " + params.getPath() + " is read-only");
	    	    
	    	    //	Create the network file object for the opened file/folder
	    	    
	    	    netFile = new AVMNetworkFile( nodeDesc, storePath.getAVMPath(), storePath.getVersion(), m_avmService);
	    	    
	    	    if ( params.isReadOnlyAccess() || storePath.getVersion() != AVMContext.VERSION_HEAD)
	    	    	netFile.setGrantedAccess(NetworkFile.READONLY);
	    		else
	    	    	netFile.setGrantedAccess(NetworkFile.READWRITE);
	    	    	
	    	    netFile.setFullName(params.getPath());
	    	    
	    	    
	    	    // Set the mime-type for the new file
	    	    
	    	    netFile.setMimeType( m_mimetypeService.guessMimetype( params.getPath()));
	    	}
	    	else
	    		throw new FileNotFoundException( params.getPath());
    	}
    	catch ( AVMNotFoundException ex)
    	{
    		throw new FileNotFoundException( params.getPath());
    	}
    	catch ( AVMWrongTypeException ex)
    	{
    		throw new FileNotFoundException( params.getPath());
    	}
    	
    	// Return the file
    	
    	return netFile;
    }

    /**
     * Read a block of data from the specified file.
     * 
     * @param sess Session details
     * @param tree Tree connection
     * @param file Network file
     * @param buf Buffer to return data to
     * @param bufPos Starting position in the return buffer
     * @param siz Maximum size of data to return
     * @param filePos File offset to read data
     * @return Number of bytes read
     * @exception java.io.IOException The exception description.
     */
    public int readFile(SrvSession sess, TreeConnection tree, NetworkFile file, byte[] buf, int bufPos, int siz,
    					long filePos)
    	throws java.io.IOException
    {
  	  	// Check if the file is a directory
    	
		if ( file.isDirectory())
			throw new AccessDeniedException();
      
    	// If the content channel is not open for the file then start a transaction
    	
    	AVMNetworkFile avmFile = (AVMNetworkFile) file;
    	
    	if ( avmFile.hasContentChannel() == false)
    		sess.beginReadTransaction( m_transactionService);
    	
		// Read the file

		int rdlen = file.readFile(buf, siz, bufPos, filePos);

		// If we have reached end of file return a zero length read

		if (rdlen == -1)
			rdlen = 0;

		//  Return the actual read length

		return rdlen;
    }

    /**
     * Rename the specified file.
     * 
     * @param sess Server session
     * @param tree Tree connection
     * @param oldName java.lang.String
     * @param newName java.lang.String
     * @exception java.io.IOException The exception description.
     */
    public void renameFile(SrvSession sess, TreeConnection tree, String oldName, String newName)
    	throws java.io.IOException
    {
    	// Split the relative paths into parent and file/folder name pairs
    	
    	AVMContext ctx = (AVMContext) tree.getContext();
    	
    	String[] oldPaths = FileName.splitPath( oldName);
    	String[] newPaths = FileName.splitPath( newName);
    	
    	// Convert the parent paths to store paths
    	
    	AVMPath oldAVMPath = buildStorePath( ctx, oldPaths[0]);
    	AVMPath newAVMPath = buildStorePath( ctx, newPaths[0]);
    	
    	// DEBUG
    	
    	if ( logger.isDebugEnabled())
    	{
    		logger.debug("Rename from path=" + oldPaths[0] + ", name=" + oldPaths[1]);
    		logger.debug("        new path=" + newPaths[0] + ", name=" + newPaths[1]);
    	}

    	// Check if the filesystem is the virtualization view
    	
    	if ( ctx.isVirtualizationView() && oldAVMPath.isReadOnlyPseudoPath())
    	{
    		throw new AccessDeniedException( "Cannot rename folder in store/version layer, " + oldName);
    	}
    	
    	// Start a transaction for the rename
    	
    	sess.beginWriteTransaction( m_transactionService);
    	
    	try
    	{
    		// Rename the file/folder
    		
    		m_avmService.rename( oldAVMPath.getAVMPath(), oldPaths[1], newAVMPath.getAVMPath(), newPaths[1]);
    	}
    	catch ( AVMNotFoundException ex)
    	{
    		throw new IOException( "Source not found, " + oldName);
    	}
    	catch ( AVMWrongTypeException ex)
    	{
    		throw new IOException( "Invalid path, " + oldName);
    	}
    	catch ( AVMExistsException ex)
    	{
    		throw new FileExistsException( "Destination exists, " + newName);
    	}
    }

    /**
     * Seek to the specified file position.
     * 
     * @param sess Server session
     * @param tree Tree connection
     * @param file Network file.
     * @param pos Position to seek to.
     * @param typ Seek type.
     * @return New file position, relative to the start of file.
     */
    public long seekFile(SrvSession sess, TreeConnection tree, NetworkFile file, long pos, int typ)
    	throws java.io.IOException
    {
  	  	// Check if the file is a directory
    	
		if ( file.isDirectory())
			throw new AccessDeniedException();
      
    	// If the content channel is not open for the file then start a transaction
    	
    	AVMNetworkFile avmFile = (AVMNetworkFile) file;
    	
    	if ( avmFile.hasContentChannel() == false)
    		sess.beginReadTransaction( m_transactionService);
    	
		// Set the file position

		return file.seekFile(pos, typ);
    }

    /**
     * Set the file information for the specified file.
     * 
     * @param sess Server session
     * @param tree Tree connection
     * @param name java.lang.String
     * @param info FileInfo
     * @exception java.io.IOException The exception description.
     */
    public void setFileInformation(SrvSession sess, TreeConnection tree, String name, FileInfo info)
    	throws java.io.IOException
    {
        // Check if the file is being marked for deletion, check if the file is writable
        
        if ( info.hasSetFlag(FileInfo.SetDeleteOnClose) && info.hasDeleteOnClose())
        {
        	// If this is not the head version then it's not writable
        	
        	AVMContext avmCtx = (AVMContext) tree.getContext();
        	if ( avmCtx.isVersion() != AVMContext.VERSION_HEAD)
        		throw new AccessDeniedException( "Store not writable, cannot set delete on close");
        }
    }

    /**
     * Start a new search on the filesystem using the specified searchPath that may contain
     * wildcards.
     * 
     * @param sess Server session
     * @param tree Tree connection
     * @param searchPath File(s) to search for, may include wildcards.
     * @param attrib Attributes of the file(s) to search for, see class SMBFileAttribute.
     * @return SearchContext
     * @exception java.io.FileNotFoundException If the search could not be started.
     */
    public SearchContext startSearch(SrvSession sess, TreeConnection tree, String searchPath, int attrib)
    	throws java.io.FileNotFoundException
    {
    	// Access the AVM context
    	
    	AVMContext avmCtx = (AVMContext) tree.getContext();
    	
    	// DEBUG
    	
    	if ( logger.isDebugEnabled())
    		logger.debug("Start search path=" + searchPath);

    	// Split the search path into relative path and search name
    	
    	String[] paths = FileName.splitPath( searchPath);
    	
    	// Build the store path to the folder being searched
    	
    	AVMPath storePath = buildStorePath( avmCtx, paths[0]);
    	
    	// Check if the filesystem is the virtualization view
    	
    	if ( avmCtx.isVirtualizationView())
    	{
    		// Check for a search of a pseudo folder

    		if (storePath.isReadOnlyPseudoPath())
	    	{
	    		// Get the file state for the folder being searched
	
	    		FileState fstate = findPseudoState( storePath, avmCtx);
	
	    		if ( fstate != null)
	    		{
	    			// Get the pseudo file list for the parent directory
	    			
	    			PseudoFileList searchList = fstate.getPseudoFileList();
	    			
		   			// Check if this is a single file or wildcard search
		    			
		   			if ( WildCard.containsWildcards( searchPath))
		   			{
		   	    		// Create the search context, wildcard filter will take care of secondary filtering of the
		   	    		// folder listing
		    	    		
		   	    		WildCard wildCardFilter = new WildCard( paths[1], false);
		   	    		return new PseudoFileListSearchContext( searchList, attrib, wildCardFilter);
		   			}
		   			else
		   			{
		   				// Search the pseudo file list for the required file
		    				
		   				PseudoFile pseudoFile = searchList.findFile( paths[1], false);
		   				if ( pseudoFile != null)
		   				{
		   					// Create a search context using the single file details
		    					
		   					PseudoFileList singleList = new PseudoFileList();
		   					singleList.addFile( pseudoFile);
		    					
		       	    		return new PseudoFileListSearchContext( singleList, attrib, null);
		   				}
		   			}
	    		}
	    		
	   			// File not found
	    			
	   			throw new FileNotFoundException( searchPath);
	    	}
	    	else if ( storePath.isLevel() == AVMPath.LevelId.HeadMetaData || storePath.isLevel() == AVMPath.LevelId.VersionMetaData)
	    	{
	    		// Return an empty file list for now
	    		
	    		PseudoFileList metaFiles = new PseudoFileList();
	    		
	    		return new PseudoFileListSearchContext( metaFiles, attrib, null);
	    	}
    	}

    	// Check if the path is a wildcard search
    	
		sess.beginReadTransaction( m_transactionService);
    	SearchContext context = null;
    	
    	if ( WildCard.containsWildcards( searchPath))
    	{
	    	// Get the file listing for the folder
	    	
	    	AVMNodeDescriptor[] fileList = m_avmService.getDirectoryListingArray( storePath.getVersion(), storePath.getAVMPath(), false);
	    	
	    	// Create the search context
	    	
	    	if ( fileList != null) {

	    		// DEBUG
	    		
	    		if ( logger.isDebugEnabled())
	    			logger.debug("  Wildcard search returned " + fileList.length + " files");
	    		
	    		// Create the search context, wildcard filter will take care of secondary filtering of the
	    		// folder listing
	    		
	    		WildCard wildCardFilter = new WildCard( paths[1], false);
	    		context = new AVMSearchContext( fileList, attrib, wildCardFilter);
	    	}
    	}
    	else
    	{
    		// Single file/folder search, convert the path to a store path
    		
    		storePath = buildStorePath( avmCtx, searchPath);
    		
    		// Get the single file/folder details
    		
    		AVMNodeDescriptor nodeDesc = m_avmService.lookup( storePath.getVersion(), storePath.getAVMPath());
    		
    		if ( nodeDesc != null)
    		{
    			// Create the search context for the single file/folder
    			
    			context = new AVMSingleFileSearchContext( nodeDesc);
    		}
    		
    	}
    	
    	// Return the search context
    	
    	return context;
    }

    /**
     * Truncate a file to the specified size
     * 
     * @param sess Server session
     * @param tree Tree connection
     * @param file Network file details
     * @param siz New file length
     * @exception java.io.IOException The exception description.
     */
    public void truncateFile(SrvSession sess, TreeConnection tree, NetworkFile file, long siz)
    	throws java.io.IOException
    {
        // Check if the file is a directory, or only has read access

		if ( file.getGrantedAccess() == NetworkFile.READONLY)
			throw new AccessDeniedException();
		
    	// If the content channel is not open for the file then start a transaction
    	
    	AVMNetworkFile avmFile = (AVMNetworkFile) file;
    	
    	if ( avmFile.hasContentChannel() == false || avmFile.isWritable() == false)
    		sess.beginWriteTransaction( m_transactionService);
    	
  	  	// Truncate or extend the file
  	  
  	  	file.truncateFile(siz);
  	  	file.flushFile();
    }

    /**
     * Write a block of data to the file.
     * 
     * @param sess Server session
     * @param tree Tree connection
     * @param file Network file details
     * @param buf byte[] Data to be written
     * @param bufoff Offset within the buffer that the data starts
     * @param siz int Data length
     * @param fileoff Position within the file that the data is to be written.
     * @return Number of bytes actually written
     * @exception java.io.IOException The exception description.
     */
    public int writeFile(SrvSession sess, TreeConnection tree, NetworkFile file, byte[] buf, int bufoff, int siz,
            				long fileoff)
    	throws java.io.IOException
    {
        // Check if the file is a directory, or only has read access

		if ( file.isDirectory() || file.getGrantedAccess() == NetworkFile.READONLY)
			throw new AccessDeniedException();

    	// If the content channel is not open for the file, or the channel is not writable, then start a transaction
    	
    	AVMNetworkFile avmFile = (AVMNetworkFile) file;
    	
    	if ( avmFile.hasContentChannel() == false || avmFile.isWritable() == false)
    		sess.beginWriteTransaction( m_transactionService);
    	
		// Write the data to the file
		      
		file.writeFile(buf, siz, bufoff, fileoff);

		//  Return the actual write length

		return siz;
    }
    
    /**
     * Connection opened to this disk device
     * 
     * @param sess Server session
     * @param tree Tree connection
     */
    public void treeClosed(SrvSession sess, TreeConnection tree)
    {
        // Nothing to do
    }

    /**
     * Connection closed to this device
     * 
     * @param sess Server session
     * @param tree Tree connection
     */
    public void treeOpened(SrvSession sess, TreeConnection tree)
    {
        // Nothing to do
    }
    
    /**
     * Find the pseudo file for a virtual path
     * 
     * @param avmPath AVMPath
     * @param avmCtx AVMContext
     * @return PseudoFile
     */
    private final PseudoFile findPseudoFolder( AVMPath avmPath, AVMContext avmCtx)
    {
    	// Check if the path is to a store pseudo folder

    	if ( avmPath.isRootPath())
    		return null;
    	
    	// Get the file state for the parent of the required folder
    	
    	FileState fstate = null;
    	StringBuilder str = null;
    	PseudoFile psFile = null;
    	
    	switch ( avmPath.isLevel())
    	{
    		// Store root folder
    	
	    	case StoreRoot:
	    		
	    		// Get the root folder file state
	    		
	    		fstate = avmCtx.getStateTable().findFileState( FileName.DOS_SEPERATOR_STR);
	    		if ( fstate != null)
	    			psFile = fstate.getPseudoFileList().findFile( avmPath.getStoreName(), false);
	    		break;
	    		
	    	// Versions root or Head folder
	    		
	    	case VersionRoot:
	    	case Head:
	    		
	    		// Create a path to the parent store
	    		
	    		str = new StringBuilder();
	    		
	    		str.append( FileName.DOS_SEPERATOR);
	    		str.append( avmPath.getStoreName());
	    		
	    		// Find/create the file state for the store
	    		
	    		AVMPath storePath = new AVMPath( str.toString());
	    		fstate = findPseudoState( storePath, avmCtx);
	    		
	    		// Find the version root or head pseudo folder
	    		
	    		if ( fstate != null)
	    		{
	    			if ( avmPath.isLevel() == AVMPath.LevelId.Head)
	    				psFile = fstate.getPseudoFileList().findFile( AVMPath.VersionNameHead, true);
	    			else
	    				psFile = fstate.getPseudoFileList().findFile( AVMPath.VersionsFolder, true);
	    		}
	    		break;
	    		
	    	// Version folder
	    		
	    	case Version:

	    		// Create a path to the versions folder
	    		
	    		str = new StringBuilder();
	    		
	    		str.append( FileName.DOS_SEPERATOR);
	    		str.append( avmPath.getStoreName());
	    		str.append( FileName.DOS_SEPERATOR);
	    		str.append( AVMPath.VersionsFolder);
	    		
	    		// Find/create the file state for the store
	    		
	    		AVMPath verrootPath = new AVMPath( str.toString());
	    		fstate = findPseudoState( verrootPath, avmCtx);

	    		// Find the version pseudo file
	    		
	    		if ( fstate != null)
	    		{
	    			// Build the version folder name string
		    		
		    		str.setLength( 0);
		    		
		    		str.append( AVMPath.VersionFolderPrefix);
		    		str.append( avmPath.getVersion());
		    		
	    			// find the version folder pseduo file
	    			
	    			psFile = fstate.getPseudoFileList().findFile( str.toString(), true);
	    		}
	    		break;
	    		
	    	// Head data or metadata folder
	    		
	    	case HeadData:
	    	case HeadMetaData:

	    		// Create a path to the head folder
	    		
	    		str = new StringBuilder();
	    		
	    		str.append( FileName.DOS_SEPERATOR);
	    		str.append( avmPath.getStoreName());
	    		str.append( FileName.DOS_SEPERATOR);
	    		str.append( AVMPath.VersionNameHead);
	    		
	    		// Find/create the file state for the store
	    		
	    		AVMPath headPath = new AVMPath( str.toString());
	    		fstate = findPseudoState( headPath, avmCtx);

	    		// Find the data or metadata pseudo folder
	    		
	    		if ( fstate != null)
	    		{
	    			// Find the pseudo folder
	    			
	    			if ( avmPath.isLevel() == AVMPath.LevelId.HeadData)
	    			{
	    				psFile = fstate.getPseudoFileList().findFile( AVMPath.DataFolder, true);
	    			}
	    			else
	    			{
	    				psFile = fstate.getPseudoFileList().findFile( AVMPath.MetaDataFolder, true);
	    			}
	    		}
	    		break;

	    	// Version data or metadata folder
	    		
	    	case VersionData:
	    	case VersionMetaData:

	    		// Create a path to the version folder
	    		
	    		str = new StringBuilder();
	    		
	    		str.append( FileName.DOS_SEPERATOR);
	    		str.append( avmPath.getStoreName());
	    		str.append( FileName.DOS_SEPERATOR);
	    		str.append( AVMPath.VersionFolderPrefix);
	    		str.append( avmPath.getVersion());
	    		
	    		// Find/create the file state for the store
	    		
	    		AVMPath verPath = new AVMPath( str.toString());
	    		fstate = findPseudoState( verPath, avmCtx);

	    		// Find the data or metadata pseudo folder
	    		
	    		if ( fstate != null)
	    		{
	    			// Find the pseudo folder
	    			
	    			if ( avmPath.isLevel() == AVMPath.LevelId.VersionData)
	    			{
	    				psFile = fstate.getPseudoFileList().findFile( AVMPath.DataFolder, true);
	    			}
	    			else
	    			{
	    				psFile = fstate.getPseudoFileList().findFile( AVMPath.MetaDataFolder, true);
	    			}
	    		}
	    		break;
    	}
    	
		// Return the pseudo file, or null if not found
		
		return psFile;
    }
    
    /**
     * Find the file state for a pseudo folder path
     * 
     * @param avmPath AVMPath
     * @param avmCtx AVMContext
     * @return FileState
     */
    private final FileState findPseudoState( AVMPath avmPath, AVMContext avmCtx)
    {
    	// Make sure the is to a pseudo file/folder
    	
    	if ( avmPath.isPseudoPath() == false)
    		return null;
    	
    	// Check if the path is to a store pseudo folder
    	
    	FileState fstate = null;
    	StringBuilder str = null;

    	switch ( avmPath.isLevel())
    	{
	    	// Root of the hieararchy
	    	
	    	case Root:

	    		// Get the root path file state
				
				fstate = avmCtx.getStateTable().findFileState( FileName.DOS_SEPERATOR_STR);
	    		break;
	    		
	    	// Store folder
	    		
	    	case StoreRoot:
	    		
				// Build the path to the parent store folder
				
				str = new StringBuilder();
				
				str.append( FileName.DOS_SEPERATOR);
				str.append( avmPath.getStoreName());
				
				// Search for the file state for the store pseudo folder
				
				fstate = avmCtx.getStateTable().findFileState( str.toString());
				
				if ( fstate == null)
				{
					// Create a file state for the store path
					
					fstate = avmCtx.getStateTable().findFileState( str.toString(), true, true);
					
					// Add a pseudo file for the head version
					
					fstate.addPseudoFile( new VersionPseudoFile( AVMPath.VersionNameHead));
					
					// Add a pseudo file for the version root folder
					
					fstate.addPseudoFile( new DummyFolderPseudoFile( AVMPath.VersionsFolder));
				}
				break;

			// Head folder
				
	    	case Head:

	    		// Build the path to the store head version folder
				
				str = new StringBuilder();
				
				str.append( FileName.DOS_SEPERATOR);
				str.append( avmPath.getStoreName());
				str.append( FileName.DOS_SEPERATOR);
				str.append( AVMPath.VersionNameHead);
				
				// Search for the file state for the store head version pseudo folder
				
				fstate = avmCtx.getStateTable().findFileState( str.toString());
				
				if ( fstate == null)
				{
					// Create a file state for the store head folder path
					
					fstate = avmCtx.getStateTable().findFileState( str.toString(), true, true);
					
					// Add a pseudo file for the data pseudo folder
					
					fstate.addPseudoFile( new DummyFolderPseudoFile( AVMPath.DataFolder));
					
					// Add a pseudo file for the metadata pseudo folder
					
					fstate.addPseudoFile( new DummyFolderPseudoFile( AVMPath.MetaDataFolder));
				}
	    		break;
	    		
			// Version root folder
				
	    	case VersionRoot:

	    		// Get the list of AVM store versions

				try
				{
					// Build the path to the parent store folder
					
					str = new StringBuilder();
					
					str.append( FileName.DOS_SEPERATOR);
					str.append( avmPath.getStoreName());
					str.append( FileName.DOS_SEPERATOR);
					str.append( AVMPath.VersionsFolder);
					
					// Create a file state for the store path
					
					fstate = avmCtx.getStateTable().findFileState( str.toString(), true, true);
					
					// Add pseudo folders if the list is empty
					
					if ( fstate.hasPseudoFiles() == false)
					{
						// Build the version folder name for the head version
						
						StringBuilder verStr = new StringBuilder( AVMPath.VersionFolderPrefix);
						verStr.append( "-1");
						
						// Add a pseudo file for the head version
						
						fstate.addPseudoFile( new VersionPseudoFile( verStr.toString()));
						
						// Get the list of versions for the store
						
						List<VersionDescriptor> verList = m_avmService.getStoreVersions( avmPath.getStoreName());
						
						// Add pseudo files for the versions to the store state
	
						if ( verList.size() > 0)
						{
							for ( VersionDescriptor verDesc : verList)
							{
								// Generate the version string
								
								String verName = null;
								
								verStr.setLength( AVMPath.VersionFolderPrefix.length());
								verStr.append( verDesc.getVersionID());
								
								verName = verStr.toString();
								
								// Add the version pseudo folder
								
								fstate.addPseudoFile( new VersionPseudoFile ( verName, verDesc));
							}
						}
					}
				}
				catch ( AVMNotFoundException ex)
				{
					// Invalid store name
				}
				break;

			// Version folder
				
	    	case Version:

	    		// Build the path to the store version folder
				
				str = new StringBuilder();
				
				str.append( FileName.DOS_SEPERATOR);
				str.append( avmPath.getStoreName());
				str.append( FileName.DOS_SEPERATOR);
				str.append( AVMPath.VersionFolderPrefix);
				str.append( avmPath.getVersion());
				
				// Search for the file state for the version pseudo folder
				
				fstate = avmCtx.getStateTable().findFileState( str.toString());
				
				if ( fstate == null)
				{
					// Create a file state for the version folder path
					
					fstate = avmCtx.getStateTable().findFileState( str.toString(), true, true);
					
					// Add a pseudo file for the data pseudo folder
					
					fstate.addPseudoFile( new DummyFolderPseudoFile( AVMPath.DataFolder));
					
					// Add a pseudo file for the metadata pseudo folder
					
					fstate.addPseudoFile( new DummyFolderPseudoFile( AVMPath.MetaDataFolder));
				}
	    		break;
		}

    	// Return the file state
	    
	    return fstate;
    }
}
