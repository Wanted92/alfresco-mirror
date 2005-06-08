/**
 * 
 */
package org.alfresco.repo.rule;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.repo.dictionary.NamespaceService;
import org.alfresco.repo.dictionary.impl.DictionaryBootstrap;
import org.alfresco.repo.node.NodeService;
import org.alfresco.repo.ref.NodeRef;
import org.alfresco.repo.ref.QName;
import org.alfresco.repo.ref.StoreRef;
import org.alfresco.util.BaseSpringTest;
import org.springframework.util.StopWatch;

/**
 * @author Roy Wetherall
 */
public class RuleServiceSystemTest extends BaseSpringTest
{
    private RuleService ruleService;
    private NodeService nodeService;
    private StoreRef testStoreRef;
    private NodeRef rootNodeRef;
    private NodeRef nodeRef;
    private NodeRef configFolder;
    
    /**
     * 
     */
    public RuleServiceSystemTest()
    {
        super();
    }
    
    @Override
    protected void onSetUpInTransaction() throws Exception
    {
        // Get the required services
        this.nodeService = (NodeService)this.applicationContext.getBean("indexingNodeService");
        this.ruleService = (RuleService)this.applicationContext.getBean("ruleService");
        
        this.testStoreRef = this.nodeService.createStore(StoreRef.PROTOCOL_WORKSPACE, "Test_" + System.currentTimeMillis());
        this.rootNodeRef = this.nodeService.getRootNode(this.testStoreRef);
        
        // Create the node used for tests
        this.nodeRef = this.nodeService.createNode(
                this.rootNodeRef,
				QName.createQName(NamespaceService.ALFRESCO_URI, "children"),
                QName.createQName(NamespaceService.ALFRESCO_URI, "children"),
                DictionaryBootstrap.TYPE_QNAME_CONTAINER).getChildRef();
        
        // Create the config folder
        this.configFolder = this.nodeService.createNode(
                this.rootNodeRef,
				QName.createQName(NamespaceService.ALFRESCO_URI, "children"),
                QName.createQName(NamespaceService.ALFRESCO_URI, "children"),
                DictionaryBootstrap.TYPE_QNAME_CONFIGURATIONS).getChildRef();
    }

    /**
     * Test:
     *          rule type:  inbound
     *          condition:  no-condition()
     *          action:     add-features(
     *                          aspect-name = versionable)
     */
    public void testAddFeaturesAction()
    {
        this.ruleService.makeActionable(this.nodeRef, this.configFolder);
        
        RuleType ruleType = this.ruleService.getRuleType("inbound");
        RuleConditionDefinition cond = this.ruleService.getConditionDefintion("no-condition");
        RuleActionDefinition action = this.ruleService.getActionDefinition("add-features");
        
        Map<String, Serializable> params = new HashMap<String, Serializable>(1);
        params.put("aspect-name", DictionaryBootstrap.ASPECT_QNAME_VERSIONABLE);        
        
        Rule rule = this.ruleService.createRule(ruleType);
        rule.addRuleCondition(cond, null);
        rule.addRuleAction(action, params);
        
        this.ruleService.addRule(this.nodeRef, rule);
        
        NodeRef newNodeRef = this.nodeService.createNode(
                this.nodeRef,
                QName.createQName(NamespaceService.ALFRESCO_URI, "children"),                
                QName.createQName(NamespaceService.ALFRESCO_URI, "children"),
                DictionaryBootstrap.TYPE_QNAME_CONTAINER).getChildRef();
        
        assertTrue(this.nodeService.hasAspect(newNodeRef, DictionaryBootstrap.ASPECT_QNAME_VERSIONABLE));        
    }    
    
    public void testPerformanceOfRuleExecution()
    {
        StopWatch sw = new StopWatch();
        
        // Create actionable nodes
        sw.start("create nodes with no rule executed");
        for (int i = 0; i < 100; i++)
        {
            this.nodeService.createNode(
                    this.nodeRef,
                    DictionaryBootstrap.CHILD_ASSOC_QNAME_CONTAINS,
                    DictionaryBootstrap.CHILD_ASSOC_QNAME_CONTAINS,
                    DictionaryBootstrap.TYPE_QNAME_CONTAINER).getChildRef(); 
            assertFalse(this.nodeService.hasAspect(nodeRef, DictionaryBootstrap.ASPECT_QNAME_VERSIONABLE));
        }
        sw.stop();
        
        this.ruleService.makeActionable(this.nodeRef, this.configFolder);
        
        RuleType ruleType = this.ruleService.getRuleType("inbound");
        RuleConditionDefinition cond = this.ruleService.getConditionDefintion("no-condition");
        RuleActionDefinition action = this.ruleService.getActionDefinition("add-features");
        
        Map<String, Serializable> params = new HashMap<String, Serializable>(1);
        params.put("aspect-name", DictionaryBootstrap.ASPECT_QNAME_VERSIONABLE);        
        
        Rule rule = this.ruleService.createRule(ruleType);
        rule.addRuleCondition(cond, null);
        rule.addRuleAction(action, params);
        
        this.ruleService.addRule(this.nodeRef, rule);
        
        sw.start("create nodes with one rule run (apply versionable aspect)");
        for (int i = 0; i < 100; i++)
        {
            NodeRef nodeRef = this.nodeService.createNode(
                    this.nodeRef,
					QName.createQName(NamespaceService.ALFRESCO_URI, "children"),
					QName.createQName(NamespaceService.ALFRESCO_URI, "children"),
                    DictionaryBootstrap.TYPE_QNAME_CONTAINER).getChildRef();
            assertTrue(this.nodeService.hasAspect(nodeRef, DictionaryBootstrap.ASPECT_QNAME_VERSIONABLE));
        }
        sw.stop();
        
        System.out.println(sw.prettyPrint());
    }
}
