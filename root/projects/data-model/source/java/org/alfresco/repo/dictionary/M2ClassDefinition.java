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
package org.alfresco.repo.dictionary;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.service.cmr.dictionary.AspectDefinition;
import org.alfresco.service.cmr.dictionary.AssociationDefinition;
import org.alfresco.service.cmr.dictionary.ChildAssociationDefinition;
import org.alfresco.service.cmr.dictionary.ClassDefinition;
import org.alfresco.service.cmr.dictionary.ConstraintDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryException;
import org.alfresco.service.cmr.dictionary.ModelDefinition;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.namespace.NamespacePrefixResolver;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.EqualsHelper;


/**
 * Compiled Class Definition
 * 
 * @author David Caruana
 */
/*package*/ class M2ClassDefinition implements ClassDefinition
{
    protected ModelDefinition model;
    protected M2Class m2Class;
    protected QName name;
    protected QName parentName = null;
    
    private Map<QName, M2PropertyOverride> propertyOverrides = new HashMap<QName, M2PropertyOverride>();
    private Map<QName, PropertyDefinition> properties = new HashMap<QName, PropertyDefinition>();
    private Map<QName, PropertyDefinition> inheritedProperties = new HashMap<QName, PropertyDefinition>();
    private Map<QName, AssociationDefinition> associations = new HashMap<QName, AssociationDefinition>();
    private Map<QName, AssociationDefinition> inheritedAssociations = new HashMap<QName, AssociationDefinition>();
    private Map<QName, ChildAssociationDefinition> inheritedChildAssociations = new HashMap<QName, ChildAssociationDefinition>();
    private List<AspectDefinition> defaultAspects = new ArrayList<AspectDefinition>();
    private List<QName> defaultAspectNames = new ArrayList<QName>();
    private List<AspectDefinition> inheritedDefaultAspects = new ArrayList<AspectDefinition>();
    private Set<QName> inheritedDefaultAspectNames = new HashSet<QName>();
    private Boolean archive = null;
    private Boolean inheritedArchive = null;
    private Boolean includedInSuperTypeQuery = null;
    private Boolean inheritedIncludedInSuperTypeQuery = null;
    
    /**
     * Construct
     * 
     * @param m2Class  class definition
     * @param resolver  namepsace resolver
     * @param modelProperties  global list of model properties
     * @param modelAssociations  global list of model associations
     */
    /*package*/ M2ClassDefinition(ModelDefinition model, M2Class m2Class, NamespacePrefixResolver resolver, Map<QName, PropertyDefinition> modelProperties, Map<QName, AssociationDefinition> modelAssociations)
    {
        this.model = model;
        this.m2Class = m2Class;
        
        // Resolve Names
        this.name = QName.createQName(m2Class.getName(), resolver);
        if (!model.isNamespaceDefined(name.getNamespaceURI()))
        {
            throw new DictionaryException("Cannot define class " + name.toPrefixString() + " as namespace " + name.getNamespaceURI() + " is not defined by model " + model.getName().toPrefixString());
        }
        this.archive = m2Class.getArchive();
        this.includedInSuperTypeQuery = m2Class.getIncludedInSuperTypeQuery();
        if (m2Class.getParentName() != null && m2Class.getParentName().length() > 0)
        {
            this.parentName = QName.createQName(m2Class.getParentName(), resolver);
        }
        
        // Construct Properties
        for (M2Property property : m2Class.getProperties())
        {
            PropertyDefinition def = new M2PropertyDefinition(this, property, resolver);
            if (!model.isNamespaceDefined(def.getName().getNamespaceURI()))
            {
                throw new DictionaryException("Cannot define property " + def.getName().toPrefixString() + " as namespace " + def.getName().getNamespaceURI() + " is not defined by model " + model.getName().toPrefixString());
            }
            if (properties.containsKey(def.getName()))
            {
                throw new DictionaryException("Found duplicate property definition " + def.getName().toPrefixString() + " within class " + name.toPrefixString());
            }
            
            // Check for existence of property elsewhere within the model
            PropertyDefinition existingDef = modelProperties.get(def.getName());
            if (existingDef != null)
            {
                // TODO: Consider sharing property, if property definitions are equal
                throw new DictionaryException("Found duplicate property definition " + def.getName().toPrefixString() + " within class " 
                    + name.toPrefixString() + " and class " + existingDef.getContainerClass().getName().toPrefixString());
            }
            
            properties.put(def.getName(), def);
            modelProperties.put(def.getName(), def);
        }
        
        // Construct Associations
        for (M2ClassAssociation assoc : m2Class.getAssociations())
        {
            AssociationDefinition def;
            if (assoc instanceof M2ChildAssociation)
            {
                def = new M2ChildAssociationDefinition(this, (M2ChildAssociation)assoc, resolver);
            }
            else
            {
                def = new M2AssociationDefinition(this, assoc, resolver);
            }
            if (!model.isNamespaceDefined(def.getName().getNamespaceURI()))
            {
                throw new DictionaryException("Cannot define association " + def.getName().toPrefixString() + " as namespace " + def.getName().getNamespaceURI() + " is not defined by model " + model.getName().toPrefixString());
            }
            if (associations.containsKey(def.getName()))
            {
                throw new DictionaryException("Found duplicate association definition " + def.getName().toPrefixString() + " within class " + name.toPrefixString());
            }
            
            // Check for existence of association elsewhere within the model
            AssociationDefinition existingDef = modelAssociations.get(def.getName());
            if (existingDef != null)
            {
                // TODO: Consider sharing association, if association definitions are equal
                throw new DictionaryException("Found duplicate association definition " + def.getName().toPrefixString() + " within class " 
                    + name.toPrefixString() + " and class " + existingDef.getSourceClass().getName().toPrefixString());
            }
            
            associations.put(def.getName(), def);
            modelAssociations.put(def.getName(), def);
        }
        
        // Construct Property overrides
        for (M2PropertyOverride override : m2Class.getPropertyOverrides())
        {
            QName overrideName = QName.createQName(override.getName(), resolver);
            if (properties.containsKey(overrideName))
            {
                throw new DictionaryException("Found duplicate property and property override definition " + overrideName.toPrefixString() + " within class " + name.toPrefixString());
            }
            if (propertyOverrides.containsKey(overrideName))
            {
                throw new DictionaryException("Found duplicate property override definition " + overrideName.toPrefixString() + " within class " + name.toPrefixString());
            }
            propertyOverrides.put(overrideName, override);
        }
        
        // Resolve qualified names
        for (String aspectName : m2Class.getMandatoryAspects())
        {
            QName name = QName.createQName(aspectName, resolver);
            if (!defaultAspectNames.contains(name))
            {
                defaultAspectNames.add(name);
            }
        }
    }
    
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder(120);
        sb.append("ClassDef")
          .append("[name=").append(name)
          .append("]");
        return sb.toString();
    }
    
    
    /*package*/ void resolveDependencies(
            ModelQuery query,
            NamespacePrefixResolver prefixResolver,
            Map<QName, ConstraintDefinition> modelConstraints)
    {
        if (parentName != null)
        {
            ClassDefinition parent = query.getClass(parentName);
            if (parent == null)
            {
                throw new DictionaryException("Parent class " + parentName.toPrefixString() + " of class " + name.toPrefixString() + " is not found");
            }
        }
        
        for (PropertyDefinition def : properties.values())
        {
            ((M2PropertyDefinition)def).resolveDependencies(query, prefixResolver, modelConstraints);
        }
        for (AssociationDefinition def : associations.values())
        {
            ((M2AssociationDefinition)def).resolveDependencies(query);
        }

        for (Map.Entry<QName, M2PropertyOverride> override : propertyOverrides.entrySet())
        {
            PropertyDefinition propDef = query.getProperty(override.getKey());
            if (propDef == null)
            {
                throw new DictionaryException("Class " + name.toPrefixString() + " attempting to override property " + override.getKey().toPrefixString() + " which does not exist");
            }
        }
        
        for (QName aspectName : defaultAspectNames)
        {
            AspectDefinition aspect = query.getAspect(aspectName);
            if (aspect == null)
            {
                throw new DictionaryException("Mandatory aspect " + aspectName.toPrefixString() + " of class " + name.toPrefixString() + " is not found");
            }
            defaultAspects.add(aspect);
        }
    }
    

    /*package*/ void resolveInheritance(
            ModelQuery query,
            NamespacePrefixResolver prefixResolver,
            Map<QName, ConstraintDefinition> modelConstraints)
    {
        // Retrieve parent class
        ClassDefinition parentClass = (parentName == null) ? null : query.getClass(parentName);
        
        // Build list of inherited properties (and process overridden values)
        if (parentClass != null)
        {
            for (PropertyDefinition def : parentClass.getProperties().values())
            {
                M2PropertyOverride override = propertyOverrides.get(def.getName());
                if (override == null)
                {
                    inheritedProperties.put(def.getName(), def);
                }
                else
                {
                    inheritedProperties.put(
                            def.getName(),
                            new M2PropertyDefinition(this, def, override, prefixResolver, modelConstraints));
                }
            }
        }
        
        // Append list of defined properties
        for (PropertyDefinition def : properties.values())
        {
            if (inheritedProperties.containsKey(def.getName()))
            {
                throw new DictionaryException("Duplicate property definition " + def.getName().toPrefixString() + " found in class hierarchy of " + name.toPrefixString()); 
            }
            inheritedProperties.put(def.getName(), def);
        }
        
        // Build list of inherited associations
        if (parentClass != null)
        {
            inheritedAssociations.putAll(parentClass.getAssociations());
        }
        
        // Append list of defined associations
        for (AssociationDefinition def : associations.values())
        {
            if (inheritedAssociations.containsKey(def.getName()))
            {
                throw new DictionaryException("Duplicate association definition " + def.getName().toPrefixString() + " found in class hierarchy of " + name.toPrefixString()); 
            }
            inheritedAssociations.put(def.getName(), def);
        }

        // Derive Child Associations
        for (AssociationDefinition def : inheritedAssociations.values())
        {
            if (def instanceof ChildAssociationDefinition)
            {
                inheritedChildAssociations.put(def.getName(), (ChildAssociationDefinition)def);
            }
        }
        
        // Build list of inherited default aspects
        if (parentClass != null)
        {
            inheritedDefaultAspects.addAll(parentClass.getDefaultAspects());
        }
        
        // Append list of defined default aspects
        for (AspectDefinition def : defaultAspects)
        {
            if (!inheritedDefaultAspects.contains(def))
            {
                inheritedDefaultAspects.add(def);
            }
        }
        
        // Convert to set of names
        for (AspectDefinition aspDef : inheritedDefaultAspects)
        {
            inheritedDefaultAspectNames.add(aspDef.getName());
        }
        
        // resolve archive inheritance
        if (parentClass != null && archive == null)
        {
            // archive not explicitly set on this class and there is a parent class
            inheritedArchive = ((M2ClassDefinition)parentClass).getArchive();
        }
        
        // resolve includedInSuperTypeQuery inheritance
        if (parentClass != null && includedInSuperTypeQuery == null)
        {
            // archive not explicitly set on this class and there is a parent class
            inheritedIncludedInSuperTypeQuery = ((M2ClassDefinition)parentClass).getIncludedInSuperTypeQuery();
        }
    }
    
    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.dictionary.ClassDefinition#getModel()
     */
    public ModelDefinition getModel()
    {
        return model;
    }

    /* (non-Javadoc)
     * @see org.alfresco.repo.dictionary.ClassDefinition#getName()
     */
    public QName getName()
    {
        return name;
    }

    /* (non-Javadoc)
     * @see org.alfresco.repo.dictionary.ClassDefinition#getTitle()
     */
    public String getTitle()
    {
        String value = M2Label.getLabel(model, "class", name, "title"); 
        if (value == null)
        {
            value = m2Class.getTitle();
        }
        return value;
    }

    /* (non-Javadoc)
     * @see org.alfresco.repo.dictionary.ClassDefinition#getDescription()
     */
    public String getDescription()
    {
        String value = M2Label.getLabel(model, "class", name, "description"); 
        if (value == null)
        {
            value = m2Class.getDescription();
        }
        return value;
    }

    /* (non-Javadoc)
     * @see org.alfresco.repo.dictionary.ClassDefinition#getParentName()
     */
    public QName getParentName()
    {
        return parentName;
    }
    
    /* (non-Javadoc)
     * @see org.alfresco.repo.dictionary.ClassDefinition#isAspect()
     */
    public boolean isAspect()
    {
        return (m2Class instanceof M2Aspect);
    }
    
    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.dictionary.ClassDefinition#getArchive()
     */
    public Boolean getArchive()
    {
        return archive == null ? inheritedArchive : archive;
    }
    
    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.dictionary.ClassDefinition#includedInSuperTypeQuery()
     */
    public Boolean getIncludedInSuperTypeQuery()
    {
        if(includedInSuperTypeQuery != null)
        {
            return includedInSuperTypeQuery;
        }
        if(inheritedIncludedInSuperTypeQuery != null)
        {
            return inheritedIncludedInSuperTypeQuery;
        }
        return Boolean.TRUE;
    }

    /* (non-Javadoc)
     * @see org.alfresco.repo.dictionary.ClassDefinition#getProperties()
     */
    public Map<QName, PropertyDefinition> getProperties()
    {
        return Collections.unmodifiableMap(inheritedProperties);
    }
 
    /**
     * @see org.alfresco.service.cmr.dictionary.ClassDefinition#getDefaultValues()
     */
    public Map<QName, Serializable> getDefaultValues()
    {
        Map<QName, Serializable> result = new HashMap<QName, Serializable>(5);
        
        for(Map.Entry<QName, PropertyDefinition> entry : inheritedProperties.entrySet())
        {
            PropertyDefinition propertyDefinition = entry.getValue();
            String defaultValue = propertyDefinition.getDefaultValue();
            if (defaultValue != null)
            {
                result.put(entry.getKey(), defaultValue);
            }
        }
        
        return Collections.unmodifiableMap(result);
    }

    /* (non-Javadoc)
     * @see org.alfresco.repo.dictionary.ClassDefinition#getAssociations()
     */
    public Map<QName, AssociationDefinition> getAssociations()
    {
        return Collections.unmodifiableMap(inheritedAssociations);
    }
    
    /**
     * @see org.alfresco.service.cmr.dictionary.ClassDefinition#getDefaultAspects()
     */
    public List<AspectDefinition> getDefaultAspects()
    {
        return inheritedDefaultAspects;
    }

    /**
     * @see org.alfresco.service.cmr.dictionary.ClassDefinition#getDefaultAspects(boolean)
     */
    public List<AspectDefinition> getDefaultAspects(boolean inherited)
    {
        return inherited ? getDefaultAspects() : defaultAspects;
    }
    
    /**
     * @see org.alfresco.service.cmr.dictionary.ClassDefinition#getDefaultAspectNames()
     */
    public Set<QName> getDefaultAspectNames()
    {
        return inheritedDefaultAspectNames;
    }

    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.dictionary.ClassDefinition#isContainer()
     */
    public boolean isContainer()
    {
        return !inheritedChildAssociations.isEmpty();
    }
    
    /* (non-Javadoc)
     * @see org.alfresco.repo.dictionary.ClassDefinition#getChildAssociations()
     */
    public Map<QName, ChildAssociationDefinition> getChildAssociations()
    {
        return Collections.unmodifiableMap(inheritedChildAssociations);
    }

    @Override
    public int hashCode()
    {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (!(obj instanceof M2ClassDefinition))
        {
            return false;
        }
        return name.equals(((M2ClassDefinition)obj).name);
    }

    /**
     * return differences in class definition
     * 
     * note:
     * - checks properties for incremental updates, but does not include the diffs
     * - checks assocs & child assocs for incremental updates, but does not include the diffs
     * - incremental updates include changes in title/description, property default value, etc
     */
    /* package */ List<M2ModelDiff> diffClass(ClassDefinition classDef)
    {
        List<M2ModelDiff> modelDiffs = new ArrayList<M2ModelDiff>();
        boolean isUpdated = false;
        boolean isUpdatedIncrementally = false;
        
        if (this == classDef)
        {
            return modelDiffs;
        }
        
        // check name - cannot be null
        if (! getName().equals(classDef.getName()))
        { 
            isUpdated = true;
        }
        
        // check title
        if (! EqualsHelper.nullSafeEquals(getTitle(), classDef.getTitle(), false))
        { 
            isUpdatedIncrementally = true;
        }
        
        // check description
        if (! EqualsHelper.nullSafeEquals(getDescription(), classDef.getDescription(), false))
        { 
            isUpdatedIncrementally = true;
        }
        
        // check parent name
        if (getParentName() != null) 
        {
            if (! getParentName().equals(classDef.getParentName())) 
            { 
                isUpdated = true;
            }
        } 
        else if (classDef.getParentName() != null)
        {
            isUpdated = true;
        }
        
        // check if aspect (or type)
        if (isAspect() != classDef.isAspect())
        {
            isUpdated = true;
        }
        
        // check if container
        if (isContainer() != classDef.isContainer())
        {
            if (isContainer())
            {
                // updated (non-incrementally) if class was a container and now is not a container - ie. all child associations removed
                isUpdated = true; 
            }
            
            if (classDef.isContainer())
            {
                // updated incrementally if class was not a container and now is a container - ie. some child associations added
                isUpdatedIncrementally = true;
            }
        }
        
        // check all properties (including inherited properties)
        Collection<M2ModelDiff> propertyDiffs = M2PropertyDefinition.diffPropertyLists(getProperties().values(), classDef.getProperties().values());
        
        modelDiffs.addAll(propertyDiffs);
        
        // check all associations (including inherited associations, child associations and inherited child associations)
        Collection<M2ModelDiff> assocDiffs = M2AssociationDefinition.diffAssocLists(getAssociations().values(), classDef.getAssociations().values());
        
        modelDiffs.addAll(assocDiffs);
        
        // check default/mandatory aspects (including inherited default aspects)
        Collection<M2ModelDiff> defaultAspectsDiffs = M2ClassDefinition.diffClassLists(new ArrayList<ClassDefinition>(getDefaultAspects()), new ArrayList<ClassDefinition>(classDef.getDefaultAspects()), M2ModelDiff.TYPE_DEFAULT_ASPECT);
       
        for (M2ModelDiff defaultAspectDiff : defaultAspectsDiffs)
        {
            // note: incremental default/mandatory aspect updates not supported yet, added for completeness
            if (defaultAspectDiff.getDiffType().equals(M2ModelDiff.DIFF_UPDATED_INC))
            {
                isUpdatedIncrementally = true;
            }
            
            if (defaultAspectDiff.getDiffType().equals(M2ModelDiff.DIFF_CREATED) || defaultAspectDiff.getDiffType().equals(M2ModelDiff.DIFF_UPDATED) || defaultAspectDiff.getDiffType().equals(M2ModelDiff.DIFF_DELETED))
            {
                isUpdated = true;
                break;
            }
        }
        
        // check archive/inheritedArchive
        if (getArchive() == null)
        {
            if (classDef.getArchive() != null)
            {
                isUpdatedIncrementally = true;
            }
        }
        else
        {
            Boolean classArchive = classDef.getArchive();
            if (classArchive == null || classArchive.booleanValue() != getArchive().booleanValue())
            {
                isUpdatedIncrementally = true;
            }
        }
       
        // check includedInSuperTypeQuery/inheritedIncludedInSuperTypeQuery
        if (getIncludedInSuperTypeQuery() == null)
        {
            // belts-and-braces (currently does not return null)
            if (classDef.getIncludedInSuperTypeQuery() != null)
            {
                isUpdatedIncrementally = true;
            }
        }
        else
        {
            Boolean classIncludedInSuperTypeQuery = classDef.getIncludedInSuperTypeQuery();
            if (classIncludedInSuperTypeQuery == null || classIncludedInSuperTypeQuery.booleanValue() != getIncludedInSuperTypeQuery().booleanValue())
            {
                isUpdatedIncrementally = true;
            }
        }
        
        String modelDiffType;
        if (isAspect())
        {
            modelDiffType = M2ModelDiff.TYPE_ASPECT;
        }
        else
        {
            modelDiffType = M2ModelDiff.TYPE_TYPE;
        }
        
        if (isUpdated)
        {
            modelDiffs.add(new M2ModelDiff(name, modelDiffType, M2ModelDiff.DIFF_UPDATED));
        }
        else if (isUpdatedIncrementally)
        {
            modelDiffs.add(new M2ModelDiff(name, modelDiffType, M2ModelDiff.DIFF_UPDATED_INC));
        }
        else
        {
            modelDiffs.add(new M2ModelDiff(name, modelDiffType, M2ModelDiff.DIFF_UNCHANGED));
        }
        
        return modelDiffs;
    }
    
    /**
     * return differences in class definition lists
     *
     */
    /*package*/ static List<M2ModelDiff> diffClassLists(Collection<ClassDefinition> previousClasses, Collection<ClassDefinition> newClasses, String M2ModelDiffType)
    {                
        List<M2ModelDiff> modelDiffs = new ArrayList<M2ModelDiff>();
        
        for (ClassDefinition previousClass : previousClasses)
        {
            boolean found = false;
            for (ClassDefinition newClass : newClasses)
            {
                if (newClass.getName().equals(previousClass.getName()))
                {
                    modelDiffs.addAll(((M2ClassDefinition)previousClass).diffClass(newClass));
                    found = true;
                    break;
                }
            }
            
            if (! found)
            {
                modelDiffs.add(new M2ModelDiff(previousClass.getName(), M2ModelDiffType, M2ModelDiff.DIFF_DELETED));
            }
        }
        
        for (ClassDefinition newClass : newClasses)
        {
            boolean found = false;
            for (ClassDefinition previousClass : previousClasses)
            {
                if (newClass.getName().equals(previousClass.getName()))
                {
                    found = true;
                    break;
                }
            }
            
            if (! found)
            {
                modelDiffs.add(new M2ModelDiff(newClass.getName(), M2ModelDiffType, M2ModelDiff.DIFF_CREATED));
            }
        }
        
        return modelDiffs;
    }
}