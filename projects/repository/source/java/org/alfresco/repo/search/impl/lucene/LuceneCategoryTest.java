/*
 * Created on 29-Apr-2005
 *
 * TODO Comment this class
 * 
 * 
 */
package org.alfresco.repo.search.impl.lucene;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Random;

import junit.framework.TestCase;

import org.alfresco.repo.dictionary.DictionaryService;
import org.alfresco.repo.dictionary.NamespaceService;
import org.alfresco.repo.dictionary.PropertyTypeDefinition;
import org.alfresco.repo.dictionary.impl.DictionaryBootstrap;
import org.alfresco.repo.dictionary.impl.DictionaryDAO;
import org.alfresco.repo.dictionary.impl.M2Aspect;
import org.alfresco.repo.dictionary.impl.M2Model;
import org.alfresco.repo.dictionary.impl.M2Property;
import org.alfresco.repo.node.NodeService;
import org.alfresco.repo.ref.ChildAssocRef;
import org.alfresco.repo.ref.DynamicNamespacePrefixResolver;
import org.alfresco.repo.ref.NamespacePrefixResolver;
import org.alfresco.repo.ref.NodeRef;
import org.alfresco.repo.ref.QName;
import org.alfresco.repo.ref.StoreRef;
import org.alfresco.repo.search.CategoryService;
import org.alfresco.repo.search.ResultSet;
import org.alfresco.repo.search.Searcher;
import org.alfresco.repo.search.impl.lucene.fts.FullTextSearchIndexer;
import org.alfresco.repo.search.transaction.LuceneIndexLock;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class LuceneCategoryTest extends TestCase
{    
    ApplicationContext ctx;
    NodeService nodeService;
    DictionaryService dictionaryService;
    LuceneIndexLock luceneIndexLock;
    private NodeRef rootNodeRef;
    private NodeRef n1;
    private NodeRef n2;
    private NodeRef n3;
    private NodeRef n4;
    private NodeRef n6;
    private NodeRef n5;
    private NodeRef n7;
    private NodeRef n8;
    private NodeRef n9;
    private NodeRef n10;
    private NodeRef n11;
    private NodeRef n12;
    private NodeRef n13;
    private NodeRef n14;
    
    private NodeRef catContainer;
    private NodeRef catRoot;
    private NodeRef catACBase;
    private NodeRef catACOne;
    private NodeRef catACTwo;
    private NodeRef catACThree;
    private FullTextSearchIndexer luceneFTS;
    private DictionaryDAO dictionaryDAO;
    private String TEST_NAMESPACE = "http://www.alfresco.org/test/lucenecategorytest";
    private QName regionCategorisationQName;
    private QName assetClassCategorisationQName;
    private QName investmentRegionCategorisationQName;
    private QName marketingRegionCategorisationQName;
    private NodeRef catRBase;
    private NodeRef catROne;
    private NodeRef catRTwo;
    private NodeRef catRThree;
    private Searcher searcher;
    private LuceneIndexerAndSearcher indexerAndSearcher;

    public LuceneCategoryTest()
    {
        super();
    }

    public LuceneCategoryTest(String arg0)
    {
        super(arg0);
    }

    public void setUp()
    {
        ctx = new ClassPathXmlApplicationContext("classpath:applicationContext.xml");
        nodeService = (NodeService)ctx.getBean("dbNodeService");
        luceneIndexLock = (LuceneIndexLock)ctx.getBean("luceneIndexLock");
        dictionaryService = (DictionaryService)ctx.getBean("dictionaryService");
        luceneFTS = (FullTextSearchIndexer) ctx.getBean("LuceneFullTextSearchIndexer");
        dictionaryDAO = (DictionaryDAO) ctx.getBean("dictionaryDAO");
        searcher = (Searcher) ctx.getBean("searcherComponent");
        indexerAndSearcher = (LuceneIndexerAndSearcher) ctx.getBean("luceneIndexerAndSearcherFactory");
        
        createTestTypes();
        
        StoreRef storeRef = nodeService.createStore(
                StoreRef.PROTOCOL_WORKSPACE,
                "Test_" + System.currentTimeMillis());
        rootNodeRef = nodeService.getRootNode(storeRef);
        
        n1 = nodeService.createNode(rootNodeRef, null, QName.createQName("{namespace}one"), DictionaryBootstrap.TYPE_QNAME_CONTAINER).getChildRef();
        nodeService.setProperty(n1, QName.createQName("{namespace}property-1"), "value-1");
        n2 = nodeService.createNode(rootNodeRef, null, QName.createQName("{namespace}two"), DictionaryBootstrap.TYPE_QNAME_CONTAINER).getChildRef();
        nodeService.setProperty(n2, QName.createQName("{namespace}property-1"), "value-1");
        nodeService.setProperty(n2, QName.createQName("{namespace}property-2"), "value-2");
        n3 = nodeService.createNode(rootNodeRef, null, QName.createQName("{namespace}three"), DictionaryBootstrap.TYPE_QNAME_CONTAINER).getChildRef();
        n4 = nodeService.createNode(rootNodeRef, null, QName.createQName("{namespace}four"), DictionaryBootstrap.TYPE_QNAME_CONTAINER).getChildRef();
        n5 = nodeService.createNode(n1, null, QName.createQName("{namespace}five"), DictionaryBootstrap.TYPE_QNAME_CONTAINER).getChildRef();
        n6 = nodeService.createNode(n1, null, QName.createQName("{namespace}six"), DictionaryBootstrap.TYPE_QNAME_CONTAINER).getChildRef();
        n7 = nodeService.createNode(n2, null, QName.createQName("{namespace}seven"), DictionaryBootstrap.TYPE_QNAME_CONTAINER).getChildRef();
        n8 = nodeService.createNode(n2, null, QName.createQName("{namespace}eight-2"), DictionaryBootstrap.TYPE_QNAME_CONTAINER).getChildRef();
        n9 = nodeService.createNode(n5, null, QName.createQName("{namespace}nine"), DictionaryBootstrap.TYPE_QNAME_CONTAINER).getChildRef();
        n10 = nodeService.createNode(n5, null, QName.createQName("{namespace}ten"), DictionaryBootstrap.TYPE_QNAME_CONTAINER).getChildRef();
        n11 = nodeService.createNode(n5, null, QName.createQName("{namespace}eleven"), DictionaryBootstrap.TYPE_QNAME_CONTAINER).getChildRef();
        n12 = nodeService.createNode(n5, null, QName.createQName("{namespace}twelve"), DictionaryBootstrap.TYPE_QNAME_CONTAINER).getChildRef();
        n13 = nodeService.createNode(n12, null, QName.createQName("{namespace}thirteen"), DictionaryBootstrap.TYPE_QNAME_CONTAINER).getChildRef();
        n14 = nodeService.createNode(n13, null, QName.createQName("{namespace}fourteen"), DictionaryBootstrap.TYPE_QNAME_CONTAINER).getChildRef();
        
        nodeService.addChild(rootNodeRef, n8, QName.createQName("{namespace}eight-0"));
        nodeService.addChild(n1, n8, QName.createQName("{namespace}eight-1"));
        nodeService.addChild(n2, n13, QName.createQName("{namespace}link"));
        
        nodeService.addChild(n1, n14, QName.createQName("{namespace}common"));
        nodeService.addChild(n2, n14, QName.createQName("{namespace}common"));
        nodeService.addChild(n5, n14, QName.createQName("{namespace}common"));
        nodeService.addChild(n6, n14, QName.createQName("{namespace}common"));
        nodeService.addChild(n12, n14, QName.createQName("{namespace}common"));
        nodeService.addChild(n13, n14, QName.createQName("{namespace}common"));
        
        // Categories
        
        catContainer = nodeService.createNode(rootNodeRef, null, QName.createQName(NamespaceService.ALFRESCO_URI, "categoryContainer"), DictionaryBootstrap.TYPE_QNAME_CONTAINER).getChildRef();
        catRoot = nodeService.createNode(catContainer, null, QName.createQName(NamespaceService.ALFRESCO_URI, "categoryRoot"), DictionaryBootstrap.TYPE_QNAME_CATEGORYROOT).getChildRef();
       
       
        
        catRBase = nodeService.createNode(catRoot, null, QName.createQName(TEST_NAMESPACE, "Region"), DictionaryBootstrap.TYPE_QNAME_CATEGORY).getChildRef();
        catROne = nodeService.createNode(catRBase, null, QName.createQName(TEST_NAMESPACE, "Europe"), DictionaryBootstrap.TYPE_QNAME_CATEGORY).getChildRef();
        catRTwo = nodeService.createNode(catRBase, null, QName.createQName(TEST_NAMESPACE, "RestOfWorld"), DictionaryBootstrap.TYPE_QNAME_CATEGORY).getChildRef();
        catRThree = nodeService.createNode(catRTwo, null, QName.createQName(TEST_NAMESPACE, "US"), DictionaryBootstrap.TYPE_QNAME_CATEGORY).getChildRef();
        
        nodeService.addChild(catRoot, catRBase, QName.createQName(TEST_NAMESPACE, "InvestmentRegion"));
        nodeService.addChild(catRoot, catRBase, QName.createQName(TEST_NAMESPACE, "MarketingRegion"));
        
        
        catACBase = nodeService.createNode(catRoot, null, QName.createQName(TEST_NAMESPACE, "AssetClass"), DictionaryBootstrap.TYPE_QNAME_CATEGORY).getChildRef();
        catACOne = nodeService.createNode(catACBase, null, QName.createQName(TEST_NAMESPACE, "Fixed"), DictionaryBootstrap.TYPE_QNAME_CATEGORY).getChildRef();
        catACTwo = nodeService.createNode(catACBase, null, QName.createQName(TEST_NAMESPACE, "Equity"), DictionaryBootstrap.TYPE_QNAME_CATEGORY).getChildRef();
        catACThree = nodeService.createNode(catACTwo, null, QName.createQName(TEST_NAMESPACE, "SpecialEquity"), DictionaryBootstrap.TYPE_QNAME_CATEGORY).getChildRef();
        
        
       
        nodeService.addAspect(n1, assetClassCategorisationQName, createMap("assetClass", catACBase));
        nodeService.addAspect(n1, regionCategorisationQName, createMap("region", catRBase));
        
        nodeService.addAspect(n2, assetClassCategorisationQName, createMap("assetClass", catACOne));
        nodeService.addAspect(n3, assetClassCategorisationQName, createMap("assetClass", catACOne));
        nodeService.addAspect(n4, assetClassCategorisationQName, createMap("assetClass", catACOne));
        nodeService.addAspect(n5, assetClassCategorisationQName, createMap("assetClass", catACOne));
        nodeService.addAspect(n6, assetClassCategorisationQName, createMap("assetClass", catACOne));
        
        nodeService.addAspect(n7, assetClassCategorisationQName, createMap("assetClass", catACTwo));
        nodeService.addAspect(n8, assetClassCategorisationQName, createMap("assetClass", catACTwo));
        nodeService.addAspect(n9, assetClassCategorisationQName, createMap("assetClass", catACTwo));
        nodeService.addAspect(n10, assetClassCategorisationQName, createMap("assetClass", catACTwo));
        nodeService.addAspect(n11, assetClassCategorisationQName, createMap("assetClass", catACTwo));
        
        nodeService.addAspect(n12, assetClassCategorisationQName, createMap("assetClass", catACOne, catACTwo));
        nodeService.addAspect(n13, assetClassCategorisationQName, createMap("assetClass", catACOne, catACTwo, catACThree));
        nodeService.addAspect(n14, assetClassCategorisationQName, createMap("assetClass", catACOne, catACTwo));
        
        nodeService.addAspect(n2, regionCategorisationQName, createMap("region", catROne));
        nodeService.addAspect(n3, regionCategorisationQName, createMap("region", catROne));
        nodeService.addAspect(n4, regionCategorisationQName, createMap("region", catRTwo));
        nodeService.addAspect(n5, regionCategorisationQName, createMap("region", catRTwo));
        
        nodeService.addAspect(n5, investmentRegionCategorisationQName, createMap("investmentRegion", catRBase));
        nodeService.addAspect(n5, marketingRegionCategorisationQName, createMap("marketingRegion", catRBase));
        nodeService.addAspect(n6, investmentRegionCategorisationQName, createMap("investmentRegion", catRBase));
        nodeService.addAspect(n7, investmentRegionCategorisationQName, createMap("investmentRegion", catRBase));
        nodeService.addAspect(n8, investmentRegionCategorisationQName, createMap("investmentRegion", catRBase));
        nodeService.addAspect(n9, investmentRegionCategorisationQName, createMap("investmentRegion", catRBase));
        nodeService.addAspect(n10, marketingRegionCategorisationQName, createMap("marketingRegion", catRBase));
        nodeService.addAspect(n11, marketingRegionCategorisationQName, createMap("marketingRegion", catRBase));
        nodeService.addAspect(n12, marketingRegionCategorisationQName, createMap("marketingRegion", catRBase));
        nodeService.addAspect(n13, marketingRegionCategorisationQName, createMap("marketingRegion", catRBase));
        nodeService.addAspect(n14, marketingRegionCategorisationQName, createMap("marketingRegion", catRBase));
        
    }
    
    private HashMap<QName, Serializable> createMap(String name, NodeRef[] nodeRefs)
    {
        HashMap<QName, Serializable> map = new HashMap<QName, Serializable>();
        Serializable value = null;
        if(nodeRefs.length > 1)
        {
            value = (Serializable) Arrays.asList(nodeRefs);
        }
        else if(nodeRefs.length == 1)
        {
            value = nodeRefs[0];
        }
        map.put(QName.createQName(TEST_NAMESPACE, name), value);
        return map;
    }
    
    private HashMap<QName, Serializable> createMap(String name, NodeRef nodeRef)
    {
        return createMap(name, new NodeRef[]{nodeRef});
    }
    
    private HashMap<QName, Serializable> createMap(String name, NodeRef nodeRef1, NodeRef nodeRef2)
    {
        return createMap(name, new NodeRef[]{nodeRef1, nodeRef2});
    }
    
    private HashMap<QName, Serializable> createMap(String name, NodeRef nodeRef1, NodeRef nodeRef2, NodeRef nodeRef3)
    {
        return createMap(name, new NodeRef[]{nodeRef1, nodeRef2, nodeRef3});
    }
    
    private void createTestTypes()
    {
        M2Model model = M2Model.createModel("test:lucenecategory");
        model.createNamespace(TEST_NAMESPACE, "test");
        model.createImport(NamespaceService.ALFRESCO_DICTIONARY_URI, "d");
        model.createImport(NamespaceService.ALFRESCO_URI, "alf");
        
        regionCategorisationQName = QName.createQName(TEST_NAMESPACE, "Region");
        M2Aspect generalCategorisation = model.createAspect("test:" + regionCategorisationQName.getLocalName());
        generalCategorisation.setParentName("alf:" + DictionaryBootstrap.ASPECT_QNAME_CLASSIFIABLE.getLocalName());
        M2Property genCatProp = generalCategorisation.createProperty("test:region");
        genCatProp.setIndexed(true);
        genCatProp.setIndexedAtomically(true);
        genCatProp.setMandatory(true);
        genCatProp.setMultiValued(true);
        genCatProp.setStoredInIndex(true);
        genCatProp.setTokenisedInIndex(true);
        genCatProp.setType("d:" + PropertyTypeDefinition.CATEGORY.getLocalName());
        
        assetClassCategorisationQName = QName.createQName(TEST_NAMESPACE, "AssetClass");
        M2Aspect assetClassCategorisation = model.createAspect("test:" + assetClassCategorisationQName.getLocalName());
        assetClassCategorisation.setParentName("alf:" + DictionaryBootstrap.ASPECT_QNAME_CLASSIFIABLE.getLocalName());
        M2Property acProp = assetClassCategorisation.createProperty("test:assetClass");
        acProp.setIndexed(true);
        acProp.setIndexedAtomically(true);
        acProp.setMandatory(true);
        acProp.setMultiValued(true);
        acProp.setStoredInIndex(true);
        acProp.setTokenisedInIndex(true);
        acProp.setType("d:" + PropertyTypeDefinition.CATEGORY.getLocalName());
        
        investmentRegionCategorisationQName = QName.createQName(TEST_NAMESPACE, "InvestmentRegion");
        M2Aspect investmentRegionCategorisation = model.createAspect("test:" + investmentRegionCategorisationQName.getLocalName());
        investmentRegionCategorisation.setParentName("alf:" + DictionaryBootstrap.ASPECT_QNAME_CLASSIFIABLE.getLocalName());
        M2Property irProp = investmentRegionCategorisation.createProperty("test:investmentRegion");
        irProp.setIndexed(true);
        irProp.setIndexedAtomically(true);
        irProp.setMandatory(true);
        irProp.setMultiValued(true);
        irProp.setStoredInIndex(true);
        irProp.setTokenisedInIndex(true);
        irProp.setType("d:" + PropertyTypeDefinition.CATEGORY.getLocalName());
        
        marketingRegionCategorisationQName = QName.createQName(TEST_NAMESPACE, "MarketingRegion");
        M2Aspect marketingRegionCategorisation = model.createAspect("test:" + marketingRegionCategorisationQName.getLocalName());
        marketingRegionCategorisation.setParentName("alf:" + DictionaryBootstrap.ASPECT_QNAME_CLASSIFIABLE.getLocalName());
        M2Property mrProp =  marketingRegionCategorisation.createProperty("test:marketingRegion");
        mrProp.setIndexed(true);
        mrProp.setIndexedAtomically(true);
        mrProp.setMandatory(true);
        mrProp.setMultiValued(true);
        mrProp.setStoredInIndex(true);
        mrProp.setTokenisedInIndex(true);
        mrProp.setType("d:" + PropertyTypeDefinition.CATEGORY.getLocalName());

        dictionaryDAO.putModel(model);
    }
    
    private void buildBaseIndex()
    {
        LuceneIndexerImpl indexer = LuceneIndexerImpl.getUpdateIndexer(rootNodeRef.getStoreRef(), "delta" + System.currentTimeMillis() + "_" + (new Random().nextInt()), indexerAndSearcher.getIndexLocation());
        indexer.setNodeService(nodeService);
        indexer.setLuceneIndexLock(luceneIndexLock);
        indexer.setDictionaryService(dictionaryService);
        indexer.setLuceneFullTextSearchIndexer(luceneFTS);
        indexer.clearIndex();
        indexer.createNode(new ChildAssocRef(null, null, rootNodeRef));
        indexer.createNode(new ChildAssocRef(rootNodeRef, QName.createQName("{namespace}one"), n1));
        indexer.createNode(new ChildAssocRef(rootNodeRef, QName.createQName("{namespace}two"), n2));
        indexer.createNode(new ChildAssocRef(rootNodeRef, QName.createQName("{namespace}three"), n3));
        indexer.createNode(new ChildAssocRef(rootNodeRef, QName.createQName("{namespace}four"), n4));
        
        indexer.createNode(new ChildAssocRef(rootNodeRef, QName.createQName("{namespace}categoryContainer"), catContainer));
        indexer.createNode(new ChildAssocRef(catContainer, QName.createQName("{cat}categoryRoot"), catRoot));
        indexer.createNode(new ChildAssocRef(catRoot, QName.createQName(TEST_NAMESPACE, "AssetClass"), catACBase));
        indexer.createNode(new ChildAssocRef(catACBase, QName.createQName(TEST_NAMESPACE, "Fixed"), catACOne));
        indexer.createNode(new ChildAssocRef(catACBase, QName.createQName(TEST_NAMESPACE, "Equity"), catACTwo));
        indexer.createNode(new ChildAssocRef(catACTwo, QName.createQName(TEST_NAMESPACE, "SpecialEquity"), catACThree));
        
        indexer.createNode(new ChildAssocRef(catRoot, QName.createQName(TEST_NAMESPACE, "Region"), catRBase));
        indexer.createNode(new ChildAssocRef(catRBase, QName.createQName(TEST_NAMESPACE, "Europe"), catROne));
        indexer.createNode(new ChildAssocRef(catRBase, QName.createQName(TEST_NAMESPACE, "RestOfWorld"), catRTwo));
        indexer.createNode(new ChildAssocRef(catRTwo, QName.createQName(TEST_NAMESPACE, "US"), catRThree));
        
        indexer.createNode(new ChildAssocRef(n1, QName.createQName("{namespace}five"), n5));
        indexer.createNode(new ChildAssocRef(n1, QName.createQName("{namespace}six"), n6));
        indexer.createNode(new ChildAssocRef(n2, QName.createQName("{namespace}seven"), n7));
        indexer.createNode(new ChildAssocRef(n2, QName.createQName("{namespace}eight"), n8));
        indexer.createNode(new ChildAssocRef(n5, QName.createQName("{namespace}nine"), n9));
        indexer.createNode(new ChildAssocRef(n5, QName.createQName("{namespace}ten"), n10));
        indexer.createNode(new ChildAssocRef(n5, QName.createQName("{namespace}eleven"), n11));
        indexer.createNode(new ChildAssocRef(n5, QName.createQName("{namespace}twelve"), n12));
        indexer.createNode(new ChildAssocRef(n12, QName.createQName("{namespace}thirteen"), n13));
        indexer.createNode(new ChildAssocRef(n13, QName.createQName("{namespace}fourteen"), n14));
        indexer.prepare();
        indexer.commit();
    }

    
    public void testBasic()
    {
        buildBaseIndex();
        
        LuceneSearcherImpl searcher = LuceneSearcherImpl.getSearcher(rootNodeRef.getStoreRef(), indexerAndSearcher.getIndexLocation());
        
        searcher.setNodeService(nodeService);
        searcher.setDictionaryService(dictionaryService);
        searcher.setNamespacePrefixResolver(getNamespacePrefixReolsver(""));
        ResultSet results;
        
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/test:MarketingRegion\"", null, null);
        //printPaths(results);
        assertEquals(1, results.length());
        results.close();
        
        
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/test:MarketingRegion//member\"", null, null);
        //printPaths(results);
        assertEquals(6, results.length());
        results.close();
        
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/alf:categoryContainer\"", null, null);
        assertEquals(1, results.length());
        results.close();
        
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/alf:categoryContainer/alf:categoryRoot\"", null, null);
        assertEquals(1, results.length());
        results.close();
        
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/alf:categoryContainer/alf:categoryRoot\"", null, null);
        assertEquals(1, results.length());
        results.close();
        
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/alf:categoryContainer/alf:categoryRoot/test:AssetClass\"", null, null);
        assertEquals(1, results.length());
        results.close();
        
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/alf:categoryContainer/alf:categoryRoot/test:AssetClass/member\" ", null, null);
        assertEquals(1, results.length());
        results.close();
        
        
        
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/alf:categoryContainer/alf:categoryRoot/test:AssetClass/test:Fixed\"", null, null);
        assertEquals(1, results.length());
        results.close();
        
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/alf:categoryContainer/alf:categoryRoot/test:AssetClass/test:Equity\"", null, null);
        assertEquals(1, results.length());
        results.close();
        
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/test:AssetClass\"", null, null);
        assertEquals(1, results.length());
        results.close();
        
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/test:AssetClass/test:Fixed\"", null, null);
        assertEquals(1, results.length());
        results.close();
        
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/test:AssetClass/test:Equity\"", null, null);
        assertEquals(1, results.length());
        results.close();
        
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/test:AssetClass/test:*\"", null, null);
        assertEquals(2, results.length());
        results.close();
        
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/test:AssetClass//test:*\"", null, null);
        assertEquals(3, results.length());
        results.close();
        
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/test:AssetClass/test:Fixed/member\"", null, null);
        //printPaths(results);
        assertEquals(8, results.length());
        results.close();
        
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/test:AssetClass/test:Equity/member\"", null, null);
        //printPaths(results);
        assertEquals(8, results.length());
        results.close();
        
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/test:AssetClass/test:Equity/test:SpecialEquity/member//.\"", null, null);
        assertEquals(1, results.length());
        results.close();
        
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/test:AssetClass/test:Equity/test:SpecialEquity/member//*\"", null, null);
        assertEquals(0, results.length());
        results.close();
        
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/test:AssetClass/test:Equity/test:SpecialEquity/member\"", null, null);
        assertEquals(1, results.length());
        results.close();
        
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "+PATH:\"/test:AssetClass/test:Equity/member\" AND +PATH:\"/test:AssetClass/test:Fixed/member\"", null, null);
        //printPaths(results);
        assertEquals(3, results.length());
        results.close();
        
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/test:AssetClass/test:Equity/member\" PATH:\"/test:AssetClass/test:Fixed/member\"", null, null);
        //printPaths(results);
        assertEquals(13, results.length());
        results.close();
        
        // Region 
        
        assertEquals(4, nodeService.getChildAssocs(catRoot).size());
        
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/test:Region\"", null, null);
        //printPaths(results);
        assertEquals(1, results.length());
        results.close();
        
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/test:Region/member\"", null, null);
        //printPaths(results);
        assertEquals(1, results.length());
        results.close();
        
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/test:Region/test:Europe/member\"", null, null);
        //printPaths(results);
        assertEquals(2, results.length());
        results.close();
        
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/test:Region/test:RestOfWorld/member\"", null, null);
        //printPaths(results);
        assertEquals(2, results.length());
        results.close();
        
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/test:Region//member\"", null, null);
        //printPaths(results);
        assertEquals(5, results.length());
        results.close();
        
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/test:InvestmentRegion//member\"", null, null);
        //printPaths(results);
        assertEquals(5, results.length());
        results.close();
        
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/test:MarketingRegion//member\"", null, null);
        //printPaths(results);
        assertEquals(6, results.length());
        results.close();
        
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "+PATH:\"/test:AssetClass/test:Fixed/member\" AND +PATH:\"/test:Region/test:Europe/member\"", null, null);
        //printPaths(results);
        assertEquals(2, results.length());
        results.close();
        
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "+PATH:\"/alf:categoryContainer/alf:categoryRoot/test:AssetClass/test:Fixed/member\" AND +PATH:\"/alf:categoryContainer/alf:categoryRoot/test:Region/test:Europe/member\"", null, null);
        //printPaths(results);
        assertEquals(2, results.length());
        results.close();
        
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/test:AssetClass/test:Equity/member\" PATH:\"/test:MarketingRegion/member\"", null, null);
        //printPaths(results);
        assertEquals(9, results.length());
        results.close();
    }
    
    public void testCategoryService()
    {
        buildBaseIndex();
        
        LuceneSearcherImpl searcher = LuceneSearcherImpl.getSearcher(rootNodeRef.getStoreRef(), indexerAndSearcher.getIndexLocation());
        
        searcher.setNodeService(nodeService);
        searcher.setDictionaryService(dictionaryService);
        searcher.setNamespacePrefixResolver(getNamespacePrefixReolsver(""));
        
        ResultSet 
        
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/alf:categoryContainer/alf:categoryRoot/test:AssetClass/*\" ", null, null);
        assertEquals(3, results.length());
        results.close();
        
        results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/alf:categoryContainer/alf:categoryRoot/test:AssetClass/member\" ", null, null);
        assertEquals(1, results.length());
        results.close();
        
        LuceneCategoryServiceImpl impl = new LuceneCategoryServiceImpl();
        impl.setNodeService(nodeService);
        impl.setNamespacePrefixResolver(getNamespacePrefixReolsver(""));
        impl.setSearcher(searcher);
        impl.setDictionaryService(dictionaryService);
        
        Collection<ChildAssocRef>
        result = impl.getChildren(catACBase , CategoryService.Mode.MEMBERS, CategoryService.Depth.IMMEDIATE);
        assertEquals(1, result.size());
        
        result = impl.getChildren(catACBase , CategoryService.Mode.ALL, CategoryService.Depth.IMMEDIATE);
        assertEquals(3, result.size());
        
        result = impl.getChildren(catACBase , CategoryService.Mode.SUB_CATEGORIES, CategoryService.Depth.IMMEDIATE);
        assertEquals(2, result.size());
        
        result = impl.getChildren(catACBase , CategoryService.Mode.MEMBERS, CategoryService.Depth.ANY);
        assertEquals(18, result.size());
        
        result = impl.getChildren(catACBase , CategoryService.Mode.ALL, CategoryService.Depth.ANY);
        assertEquals(21, result.size());
        
        result = impl.getChildren(catACBase , CategoryService.Mode.SUB_CATEGORIES, CategoryService.Depth.ANY);
        assertEquals(3, result.size());
        
        result = impl.getRootCategories(rootNodeRef.getStoreRef());
        assertEquals(4, result.size());
        
        result = impl.getCategories(rootNodeRef.getStoreRef(), QName.createQName(TEST_NAMESPACE, "assetClass"), CategoryService.Depth.IMMEDIATE);
        assertEquals(2, result.size());
        
        Collection<QName> aspects = impl.getCategoryAspects();
        assertEquals(6, aspects.size());    
    }
    
    private NamespacePrefixResolver getNamespacePrefixReolsver(String defaultURI)
    {
        DynamicNamespacePrefixResolver nspr = new DynamicNamespacePrefixResolver(null);
        nspr.addDynamicNamespace(NamespaceService.ALFRESCO_PREFIX, NamespaceService.ALFRESCO_URI);
        nspr.addDynamicNamespace(NamespaceService.ALFRESCO_TEST_PREFIX, NamespaceService.ALFRESCO_TEST_URI);
        nspr.addDynamicNamespace("namespace", "namespace");
        nspr.addDynamicNamespace("test", TEST_NAMESPACE);
        nspr.addDynamicNamespace(NamespaceService.DEFAULT_PREFIX, defaultURI);
        return nspr;
    }
}
