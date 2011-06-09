/**
 * 
 */
package org.alfresco.module.org_alfresco_module_dod5015.test.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.module.org_alfresco_module_dod5015.EventCompletionDetails;
import org.alfresco.module.org_alfresco_module_dod5015.disposition.DispositionAction;
import org.alfresco.module.org_alfresco_module_dod5015.disposition.DispositionActionDefinition;
import org.alfresco.module.org_alfresco_module_dod5015.disposition.DispositionSchedule;
import org.alfresco.module.org_alfresco_module_dod5015.disposition.DispositionService;
import org.alfresco.module.org_alfresco_module_dod5015.job.publish.PublishExecutor;
import org.alfresco.module.org_alfresco_module_dod5015.job.publish.PublishExecutorRegistry;
import org.alfresco.module.org_alfresco_module_dod5015.model.RecordsManagementModel;
import org.alfresco.module.org_alfresco_module_dod5015.test.util.BaseRMTestCase;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

/**
 * Disposition service implementation unit test.
 * 
 * @author Roy Wetherall
 */
public class DispositionServiceImplTest extends BaseRMTestCase
{
    @Override
    protected boolean isMultiHierarchyTest()
    {
        return true;
    }
    
    /**
     * @see DispositionService#getDispositionSchedule(org.alfresco.service.cmr.repository.NodeRef)
     */
    public void testGetDispositionSchedule() throws Exception
    {        
        doTestInTransaction(new Test<Void>()
        {
            @Override
            public Void run()
            {
                // Check for null lookup's                
                assertNull(dispositionService.getDispositionSchedule(rmRootContainer));
                
                // Get the containers disposition schedule
                DispositionSchedule ds = dispositionService.getDispositionSchedule(rmContainer);
                assertNotNull(ds);                
                checkDispositionSchedule(ds);
                
                // Get the folders disposition schedule
                ds = dispositionService.getDispositionSchedule(rmContainer);
                assertNotNull(ds);                  
                checkDispositionSchedule(ds);
                
                return null;
            }

        });  
        
        // Failure: Root node
        doTestInTransaction(new FailureTest
        (
        	"Should not be able to get adisposition schedule for the root node",
        	AlfrescoRuntimeException.class
        )
        {
            @Override
            public void run()
            {
                dispositionService.getDispositionSchedule(rootNodeRef);                        
            }
        }); 
        
        // Failure: Non-rm node
        doTestInTransaction(new FailureTest()
        {
            @Override
            public void run()
            {
                dispositionService.getDispositionSchedule(folder);                        
            }
        }); 
    }
    
    /**
     * @see DispositionService#getDispositionSchedule(org.alfresco.service.cmr.repository.NodeRef)
     */
    public void testGetDispositionScheduleMultiHier() throws Exception
    {
        doTestInTransaction(new Test<Void>()
        {
            @Override
            public Void run()
            {
                assertNull(dispositionService.getDispositionSchedule(mhContainer));
                
                // Level 1
                doCheck(mhContainer11, "ds11");  
                doCheck(mhContainer12, "ds12");
                
                // Level 2
                doCheck(mhContainer21, "ds11");
                doCheck(mhContainer22, "ds12");
                doCheck(mhContainer23, "ds23");
                
                // Level 3
                doCheck(mhContainer31, "ds11");
                doCheck(mhContainer32, "ds12");
                doCheck(mhContainer33, "ds33");
                doCheck(mhContainer34, "ds23");
                doCheck(mhContainer35, "ds35");    
                
                // Folders
                doCheckFolder(mhRecordFolder41, "ds11");
                doCheckFolder(mhRecordFolder42, "ds12");
                doCheckFolder(mhRecordFolder43, "ds33");
                doCheckFolder(mhRecordFolder44, "ds23");
                doCheckFolder(mhRecordFolder45, "ds35"); 
                
                return null;
            }
            
            private void doCheck(NodeRef container, String dispositionInstructions)
            {
                DispositionSchedule ds = dispositionService.getDispositionSchedule(container);
                assertNotNull(ds);
                checkDispositionSchedule(ds, dispositionInstructions, DEFAULT_DISPOSITION_AUTHORITY); 
            }
            
            private void doCheckFolder(NodeRef container, String dispositionInstructions)
            {
                doCheck(container, dispositionInstructions);
                assertNotNull(dispositionService.getNextDispositionAction(container));
            }
        }); 
        
    }
    
    /**
     * Checks a disposition schedule
     * 
     * @param ds    disposition scheduleS
     */
    private void checkDispositionSchedule(DispositionSchedule ds, String dispositionInstructions, String dispositionAuthority)
    {
        assertEquals(dispositionAuthority, ds.getDispositionAuthority());
        assertEquals(dispositionInstructions, ds.getDispositionInstructions());
        assertFalse(ds.isRecordLevelDisposition());     
        
        List<DispositionActionDefinition> defs = ds.getDispositionActionDefinitions();
        assertNotNull(defs);
        assertEquals(2, defs.size());
        
        DispositionActionDefinition defCutoff = ds.getDispositionActionDefinitionByName("cutoff");
        assertNotNull(defCutoff);
        assertEquals("cutoff", defCutoff.getName());
        
        DispositionActionDefinition defDestroy = ds.getDispositionActionDefinitionByName("destroy");
        assertNotNull(defDestroy);
        assertEquals("destroy", defDestroy.getName());
    }
    
    /**
     * 
     * @param ds
     */
    private void checkDispositionSchedule(DispositionSchedule ds)
    {
        checkDispositionSchedule(ds, DEFAULT_DISPOSITION_INSTRUCTIONS, DEFAULT_DISPOSITION_AUTHORITY);
    }
    
    /**
     * @see DispositionService#getAssociatedDispositionSchedule(NodeRef)
     */
    public void testGetAssociatedDispositionSchedule() throws Exception
    {      
        // Get associated disposition schedule for rmContainer
        doTestInTransaction(new Test<Void>()
        {
            @Override
            public Void run()
            {
                // Get the containers disposition schedule
                DispositionSchedule ds = dispositionService.getAssociatedDispositionSchedule(rmContainer);
                assertNotNull(ds);                
                checkDispositionSchedule(ds);
                
                // Show the null disposition schedules
                assertNull(dispositionService.getAssociatedDispositionSchedule(rmRootContainer));
                assertNull(dispositionService.getAssociatedDispositionSchedule(rmFolder));

                return null;
            }
        });      
        
        // Failure: associated disposition schedule for non-rm node
        doTestInTransaction(new FailureTest()
        {
            @Override
            public void run()
            {
                dispositionService.getAssociatedDispositionSchedule(folder);                        
            }
        });        
    }
    
    /**
     * @see DispositionService#getAssociatedDispositionSchedule(NodeRef)
     */
    public void testGetAssociatedDispositionScheduleMultiHier() throws Exception
    {
        doTestInTransaction(new Test<Void>()
        {
            @Override
            public Void run()
            {
                assertNull(dispositionService.getAssociatedDispositionSchedule(mhContainer));
                
                // Level 1
                doCheck(mhContainer11, "ds11");  
                doCheck(mhContainer12, "ds12");
                
                // Level 2
                assertNull(dispositionService.getAssociatedDispositionSchedule(mhContainer21));
                assertNull(dispositionService.getAssociatedDispositionSchedule(mhContainer22));
                doCheck(mhContainer23, "ds23");
                
                // Level 3
                assertNull(dispositionService.getAssociatedDispositionSchedule(mhContainer31));
                assertNull(dispositionService.getAssociatedDispositionSchedule(mhContainer32));
                doCheck(mhContainer33, "ds33");
                assertNull(dispositionService.getAssociatedDispositionSchedule(mhContainer34));
                doCheck(mhContainer35, "ds35");    
                
                return null;
            }
            
            private void doCheck(NodeRef container, String dispositionInstructions)
            {
                DispositionSchedule ds = dispositionService.getAssociatedDispositionSchedule(container);
                assertNotNull(ds);
                checkDispositionSchedule(ds, dispositionInstructions, DEFAULT_DISPOSITION_AUTHORITY); 
            }
        });       
    }
    
    /**
     * @see DispositionService#hasDisposableItems(DispositionSchedule)
     */
    public void testHasDisposableItems() throws Exception
    {
    	doTestInTransaction(new Test<Void>()
        {
            @Override
            public Void run()
            {
            	// Add a new disposition schedule
            	NodeRef container = rmService.createRecordsManagementContainer(rmContainer, "hasDisposableTest");
            	DispositionSchedule ds = createBasicDispositionSchedule(container);
            	
                assertTrue(dispositionService.hasDisposableItems(dispositionSchedule));
                assertFalse(dispositionService.hasDisposableItems(ds));
                
                return null;
            }
        });    	
    }
    
    /**
     * @see DispositionService#hasDisposableItems(DispositionSchedule)
     */
    public void testHasDisposableItemsMultiHier() throws Exception
    {   
        doTestInTransaction(new Test<Void>()
        {
            @Override
            public Void run()
            {
            	assertTrue(dispositionService.hasDisposableItems(mhDispositionSchedule11));
            	assertTrue(dispositionService.hasDisposableItems(mhDispositionSchedule12));
            	assertTrue(dispositionService.hasDisposableItems(mhDispositionSchedule23));
            	assertTrue(dispositionService.hasDisposableItems(mhDispositionSchedule33));
            	assertTrue(dispositionService.hasDisposableItems(mhDispositionSchedule35));
                
                return null;
            }
        });         
    }
    
    /**
     * @see DispositionService#getDisposableItems(DispositionSchedule)
     */
    public void testGetDisposableItems() throws Exception
    {   
        doTestInTransaction(new Test<Void>()
        {
            @Override
            public Void run()
            {
                List<NodeRef> nodeRefs = dispositionService.getDisposableItems(dispositionSchedule);
                assertNotNull(nodeRefs);
                assertEquals(1, nodeRefs.size());
                assertTrue(nodeRefs.contains(rmFolder));
                
                return null;
            }
        });         
    }
    
    /**
     * @see DispositionService#getDisposableItems(DispositionSchedule)
     */
    public void testGetDisposableItemsMultiHier() throws Exception
    {   
        doTestInTransaction(new Test<Void>()
        {
            @Override
            public Void run()
            {
                List<NodeRef> nodeRefs = dispositionService.getDisposableItems(mhDispositionSchedule11);
                assertNotNull(nodeRefs);
                assertEquals(1, nodeRefs.size());
                assertTrue(nodeRefs.contains(mhRecordFolder41));
                
                nodeRefs = dispositionService.getDisposableItems(mhDispositionSchedule12);
                assertNotNull(nodeRefs);
                assertEquals(1, nodeRefs.size());
                assertTrue(nodeRefs.contains(mhRecordFolder42));
                
                nodeRefs = dispositionService.getDisposableItems(mhDispositionSchedule23);
                assertNotNull(nodeRefs);
                assertEquals(1, nodeRefs.size());
                assertTrue(nodeRefs.contains(mhRecordFolder44));
                
                nodeRefs = dispositionService.getDisposableItems(mhDispositionSchedule33);
                assertNotNull(nodeRefs);
                assertEquals(1, nodeRefs.size());
                assertTrue(nodeRefs.contains(mhRecordFolder43));
                
                nodeRefs = dispositionService.getDisposableItems(mhDispositionSchedule35);
                assertNotNull(nodeRefs);
                assertEquals(1, nodeRefs.size());
                assertTrue(nodeRefs.contains(mhRecordFolder45));
                
                return null;
            }
        });         
    }
    
    /**
     * @see DispositionService#createDispositionSchedule(NodeRef, Map)
     */
    public void testCreateDispositionSchedule() throws Exception
    {
    	// Test: simple disposition create
        doTestInTransaction(new Test<NodeRef>()
        {
            @Override
            public NodeRef run()
            {
            	// Create a new container
            	NodeRef container = rmService.createRecordsManagementContainer(rmRootContainer, "testCreateDispositionSchedule");
            	
            	// Create a new disposition schedule
            	createBasicDispositionSchedule(container, "testCreateDispositionSchedule", "testCreateDispositionSchedule", false, true);
            	
            	return container;
            }                   
            
            @Override
            public void test(NodeRef result) throws Exception 
            {
            	// Get the created disposition schedule
            	DispositionSchedule ds = dispositionService.getAssociatedDispositionSchedule(result);
            	assertNotNull(ds);
            	
            	// Check the disposition schedule
            	checkDispositionSchedule(ds, "testCreateDispositionSchedule", "testCreateDispositionSchedule");
            }
        });      
        
        // Failure: create disposition schedule on container with existing disposition schedule
        doTestInTransaction(new FailureTest
        (
        	"Can not create a disposition schedule on a container with an existing disposition schedule"
        )
        {
            @Override
            public void run()
            {
            	createBasicDispositionSchedule(rmContainer);                        
            }
        }); 
    }
    
    /**
     * @see DispositionService#createDispositionSchedule(NodeRef, Map)
     */
    public void testCreateDispositionScheduleMultiHier() throws Exception
    {
    	// Test: simple disposition create
        doTestInTransaction(new Test<Void>()
        {
            @Override
            public Void run()
            {
            	// Create a new structure container
            	NodeRef testA = rmService.createRecordsManagementContainer(mhContainer, "testA");
            	NodeRef testB = rmService.createRecordsManagementContainer(testA, "testB");
            	
            	// Create new disposition schedules
            	createBasicDispositionSchedule(testA, "testA", "testA", false, true);
            	createBasicDispositionSchedule(testB, "testB", "testB", false, true);
            	
            	// Add created containers to model
            	setNodeRef("testA", testA);
            	setNodeRef("testB", testB);
            	
            	return null;
            }                        
            
            @Override
            public void test(Void result) throws Exception 
            {
            	// Get the created disposition schedule
            	DispositionSchedule testA = dispositionService.getAssociatedDispositionSchedule(getNodeRef("testA"));
            	assertNotNull(testA);
            	DispositionSchedule testB = dispositionService.getAssociatedDispositionSchedule(getNodeRef("testB"));
            	assertNotNull(testB);
            	
            	// Check the disposition schedule
            	checkDispositionSchedule(testA, "testA", "testA");
            	checkDispositionSchedule(testB, "testB", "testB");
            }
        });      
        
        // Failure: create disposition schedule on container with existing disposition schedule
        doTestInTransaction(new FailureTest
        (
        	"Can not create a disposition schedule on container with an existing disposition schedule"
        )
        {
            @Override
            public void run()
            {
            	createBasicDispositionSchedule(mhContainer11);                        
            }
        }); 
        
        // Failure: create disposition schedule on a container where there are disposable items under management
        doTestInTransaction(new FailureTest
        (
        	"Can not create a disposition schedule on a container where there are already disposable items under management"
        )
        {
            @Override
            public void run()
            {
            	createBasicDispositionSchedule(mhContainer21);  
            }
        }); 
    }
    
    /**
     * @see DispositionService#getAssociatedRecordsManagementContainer(DispositionSchedule)
     */
    public void testGetAssociatedRecordsManagementContainer() throws Exception
    {
        doTestInTransaction(new Test<Void>()
        {
            @Override
            public Void run()
            {
                NodeRef nodeRef = dispositionService.getAssociatedRecordsManagementContainer(dispositionSchedule);
                assertNotNull(nodeRef);
                assertEquals(rmContainer, nodeRef);

                return null;
            }
        });         
    }
    
    /**
     * @see DispositionService#getAssociatedRecordsManagementContainer(DispositionSchedule)
     */
    public void testGetAssociatedRecordsManagementContainerMultiHier() throws Exception
    {
        doTestInTransaction(new Test<Void>()
        {
            @Override
            public Void run()
            {
                NodeRef nodeRef = dispositionService.getAssociatedRecordsManagementContainer(mhDispositionSchedule11);
                assertNotNull(nodeRef);
                assertEquals(mhContainer11, nodeRef);
                
                nodeRef = dispositionService.getAssociatedRecordsManagementContainer(mhDispositionSchedule12);
                assertNotNull(nodeRef);
                assertEquals(mhContainer12, nodeRef);
                
                nodeRef = dispositionService.getAssociatedRecordsManagementContainer(mhDispositionSchedule23);
                assertNotNull(nodeRef);
                assertEquals(mhContainer23, nodeRef);
                
                nodeRef = dispositionService.getAssociatedRecordsManagementContainer(mhDispositionSchedule33);
                assertNotNull(nodeRef);
                assertEquals(mhContainer33, nodeRef);
                
                nodeRef = dispositionService.getAssociatedRecordsManagementContainer(mhDispositionSchedule35);
                assertNotNull(nodeRef);
                assertEquals(mhContainer35, nodeRef);

                return null;
            }
        });         
    }
       
    // TODO DispositionActionDefinition addDispositionActionDefinition
   
    // TODO void removeDispositionActionDefinition(
            
    public void testUpdateDispositionActionDefinitionMultiHier() throws Exception
    {
        doTestInTransaction(new Test<Void>()
        {
            @Override
            public Void run() throws Exception
            {
                // Check all the current record folders first
                checkRecordFolderUnchanged(mhRecordFolder41);
                checkRecordFolderUnchanged(mhRecordFolder42);
                checkRecordFolderUnchanged(mhRecordFolder43);
                checkRecordFolderUnchanged(mhRecordFolder44);
                checkRecordFolderUnchanged(mhRecordFolder45);
                
                updateDispositionScheduleOnContainer(mhContainer11);
                
                return null;
            }
            
            @Override
            public void test(Void result) throws Exception
            {
                // Check all the current record folders first
                checkRecordFolderChanged(mhRecordFolder41);
                checkRecordFolderUnchanged(mhRecordFolder42);
                checkRecordFolderUnchanged(mhRecordFolder43);
                checkRecordFolderUnchanged(mhRecordFolder44);
                checkRecordFolderUnchanged(mhRecordFolder45);;
            }
        }); 
        
        doTestInTransaction(new Test<Void>()
        {
            @Override
            public Void run() throws Exception
            {
                updateDispositionScheduleOnContainer(mhContainer12);
                
                return null;
            }
            
            @Override
            public void test(Void result) throws Exception
            {
                // Check all the current record folders first
                checkRecordFolderChanged(mhRecordFolder41);
                checkRecordFolderChanged(mhRecordFolder42);
                checkRecordFolderUnchanged(mhRecordFolder43);
                checkRecordFolderUnchanged(mhRecordFolder44);
                checkRecordFolderUnchanged(mhRecordFolder45);;
            }
        }); 
        
        doTestInTransaction(new Test<Void>()
        {
            @Override
            public Void run() throws Exception
            {
                updateDispositionScheduleOnContainer(mhContainer33);
                
                return null;
            }
            
            @Override
            public void test(Void result) throws Exception
            {
                // Check all the current record folders first
                checkRecordFolderChanged(mhRecordFolder41);
                checkRecordFolderChanged(mhRecordFolder42);
                checkRecordFolderChanged(mhRecordFolder43);
                checkRecordFolderUnchanged(mhRecordFolder44);
                checkRecordFolderUnchanged(mhRecordFolder45);;
            }
        });
        
        doTestInTransaction(new Test<Void>()
        {
            @Override
            public Void run() throws Exception
            {
                updateDispositionScheduleOnContainer(mhContainer23);
                
                return null;
            }
            
            @Override
            public void test(Void result) throws Exception
            {
                // Check all the current record folders first
                checkRecordFolderChanged(mhRecordFolder41);
                checkRecordFolderChanged(mhRecordFolder42);
                checkRecordFolderChanged(mhRecordFolder43);
                checkRecordFolderChanged(mhRecordFolder44);
                checkRecordFolderUnchanged(mhRecordFolder45);;
            }
        });
        
        doTestInTransaction(new Test<Void>()
        {
            @Override
            public Void run() throws Exception
            {
                updateDispositionScheduleOnContainer(mhContainer35);
                
                return null;
            }
            
            @Override
            public void test(Void result) throws Exception
            {
                // Check all the current record folders first
                checkRecordFolderChanged(mhRecordFolder41);
                checkRecordFolderChanged(mhRecordFolder42);
                checkRecordFolderChanged(mhRecordFolder43);
                checkRecordFolderChanged(mhRecordFolder44);
                checkRecordFolderChanged(mhRecordFolder45);;
            }
        });
    }
    
    private void publishDispositionActionDefinitionChange(DispositionActionDefinition dad)
    {
        PublishExecutorRegistry reg = (PublishExecutorRegistry)applicationContext.getBean("publishExecutorRegistry");
        PublishExecutor pub = reg.get(RecordsManagementModel.UPDATE_TO_DISPOSITION_ACTION_DEFINITION);
        assertNotNull(pub);
        pub.publish(dad.getNodeRef());
    }
    
    private void checkRecordFolderUnchanged(NodeRef recordFolder)
    {
        checkDispositionAction(
                dispositionService.getNextDispositionAction(recordFolder), 
                "cutoff", 
                new String[]{DEFAULT_EVENT_NAME}, 
                PERIOD_NONE);                
    }
    
    private void checkRecordFolderChanged(NodeRef recordFolder) throws Exception
    {
        checkDispositionAction(
                dispositionService.getNextDispositionAction(recordFolder), 
                "cutoff", 
                new String[]{DEFAULT_EVENT_NAME, "abolished"}, 
                "week|1");                
    }
    
    private void updateDispositionScheduleOnContainer(NodeRef container)
    {
        Map<QName, Serializable> updateProps = new HashMap<QName, Serializable>(3);
        updateProps.put(PROP_DISPOSITION_PERIOD, "week|1"); 
        updateProps.put(PROP_DISPOSITION_EVENT, (Serializable)Arrays.asList(DEFAULT_EVENT_NAME, "abolished"));
        
        DispositionSchedule ds = dispositionService.getAssociatedDispositionSchedule(container);
        DispositionActionDefinition dad = ds.getDispositionActionDefinitionByName("cutoff");
        dispositionService.updateDispositionActionDefinition(dad, updateProps);     
        publishDispositionActionDefinitionChange(dad);
    }
    
    /**
     * 
     * @param da
     * @param name
     * @param arrEventNames
     * @param strPeriod
     */
    private void checkDispositionAction(DispositionAction da, String name, String[] arrEventNames, String strPeriod)
    {
        assertNotNull(da);
        assertEquals(name, da.getName());
        
        List<EventCompletionDetails> events = da.getEventCompletionDetails();
        assertNotNull(events);
        assertEquals(arrEventNames.length, events.size());
        
        List<String> origEvents = new ArrayList<String>(events.size());
        for (EventCompletionDetails event : events)
        {
            origEvents.add(event.getEventName());
        }
                        
        List<String> expectedEvents = Arrays.asList(arrEventNames);
        Collection<String> copy = new ArrayList<String>(origEvents);
        
        for (Iterator<String> i = origEvents.iterator(); i.hasNext(); ) 
        {
            String origEvent = i.next();

            if (expectedEvents.contains(origEvent) == true)
            {
                i.remove();
                copy.remove(origEvent);
            }
        }
        
        if (copy.size() != 0 && expectedEvents.size() != 0)
        {
            StringBuffer buff = new StringBuffer(255);
            if (copy.size() != 0)
            {
                buff.append("The following events where found, but not expected: (");
                for (String eventName : copy)
                {
                    buff.append(eventName).append(", ");
                }
                buff.append(").  ");
            }
            if (expectedEvents.size() != 0)
            {
                buff.append("The following events where not found, but expected: (");
                for (String eventName : expectedEvents)
                {
                    buff.append(eventName).append(", ");
                }
                buff.append(").");
            }
            fail(buff.toString());                    
        }
        
        if (PERIOD_NONE.equals(strPeriod) == true)
        {
            assertNull(da.getAsOfDate());
        }
        else
        {
            assertNotNull(da.getAsOfDate());
        }
    }
            
    // TODO boolean isNextDispositionActionEligible(NodeRef nodeRef);
  
    // TODO DispositionAction getNextDispositionAction(NodeRef nodeRef);     
    
    // TODO List<DispositionAction> getCompletedDispositionActions(NodeRef nodeRef);
    
    // TODO DispositionAction getLastCompletedDispostionAction(NodeRef nodeRef);
    
    // TODO List<QName> getDispositionPeriodProperties();

}