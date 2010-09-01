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
package org.alfresco.wcm.client;

import junit.framework.TestCase;

import org.alfresco.wcm.client.impl.DictionaryServiceImpl;
import org.alfresco.wcm.client.impl.SectionFactoryCmisImpl;
import org.alfresco.wcm.client.util.CmisSessionHelper;
import org.alfresco.wcm.client.util.CmisSessionPool;
import org.alfresco.wcm.client.util.impl.CmisSessionPoolImpl;
import org.alfresco.wcm.client.util.impl.GuestSessionFactoryImpl;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.commons.pool.impl.GenericObjectPool;

/**
 * Dictionary service test
 * 
 * @author Roy Wetherall
 */
public class DictionaryServiceTest extends TestCase
{
	private CmisSessionPool sessionPool;
	private Session session;
	private SectionFactoryCmisImpl sectionFactory;
	
	private DictionaryService dictionaryService;
	
	@Override
    protected void setUp() throws Exception
    {
	    super.setUp();
	    
	    // Create a CMIS session
	    GuestSessionFactoryImpl guestSessionFactory = new GuestSessionFactoryImpl("http://localhost:8080/alfresco/service/cmis","admin","admin");	
	    GenericObjectPool guestSessionPool = new GenericObjectPool(guestSessionFactory, 5, GenericObjectPool.WHEN_EXHAUSTED_GROW, 30, 5);	    
	    sessionPool = new CmisSessionPoolImpl(guestSessionPool);
	    session = sessionPool.getGuestSession();
		CmisSessionHelper.setSession(session);
		
		sectionFactory = new SectionFactoryCmisImpl();
		sectionFactory.setSectionsRefreshAfter(30);
		
		dictionaryService = new DictionaryServiceImpl();
		((DictionaryServiceImpl)dictionaryService).init();
    }

	@Override
    protected void tearDown() throws Exception
    {
	    super.tearDown();
		sessionPool.closeSession(session);
    }
	
	public void testDictionaryService()
	{
		assertTrue(dictionaryService.isDocumentSubType(DictionaryService.TYPE_CMIS_DOCUMENT));
		assertTrue(dictionaryService.isDocumentSubType(DictionaryService.FULL_TYPE_CMIS_DOCUMENT));
		assertFalse(dictionaryService.isDocumentSubType(DictionaryService.TYPE_CMIS_FOLDER));
		assertFalse(dictionaryService.isDocumentSubType(DictionaryService.FULL_TYPE_CMIS_FOLDER));
		assertTrue(dictionaryService.isDocumentSubType("ws:indexPage"));
		assertTrue(dictionaryService.isDocumentSubType("D:ws:indexPage"));
		assertFalse(dictionaryService.isDocumentSubType("ws:section"));
		assertFalse(dictionaryService.isDocumentSubType("F:ws:section"));
		assertFalse(dictionaryService.isDocumentSubType("jk:junk"));
		
		assertFalse(dictionaryService.isFolderSubType(DictionaryService.TYPE_CMIS_DOCUMENT));
		assertFalse(dictionaryService.isFolderSubType(DictionaryService.FULL_TYPE_CMIS_DOCUMENT));
		assertTrue(dictionaryService.isFolderSubType(DictionaryService.TYPE_CMIS_FOLDER));
		assertTrue(dictionaryService.isFolderSubType(DictionaryService.FULL_TYPE_CMIS_FOLDER));
		assertFalse(dictionaryService.isFolderSubType("ws:indexPage"));
		assertFalse(dictionaryService.isFolderSubType("D:ws:indexPage"));
		assertTrue(dictionaryService.isFolderSubType("ws:section"));
		assertTrue(dictionaryService.isFolderSubType("F:ws:section"));
		assertFalse(dictionaryService.isFolderSubType("jk:junk"));
		
		assertEquals("D:cmis:document", dictionaryService.getParentType("ws:indexPage"));
		assertEquals("D:cmis:document", dictionaryService.getParentType("D:ws:indexPage"));
		assertEquals("cmis:document", dictionaryService.getParentType("ws:indexPage", true));
		assertEquals("cmis:document", dictionaryService.getParentType("D:ws:indexPage", true));
	}

}