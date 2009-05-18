/*
 * Copyright (C) 2005-2009 Alfresco Software Limited.
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
package org.alfresco.repo.management.subsystems;

import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.context.ApplicationContext;

/**
 * A class that wraps an instance of a Java Bean class declared as a composite property type on
 * {@link ChildApplicationContextFactory} making it configurable, either through alfresco-global.properties or a JMX
 * console.
 * 
 * @see ChildApplicationContextFactory
 * @author dward
 */
public class CompositeDataBean extends AbstractPropertyBackedBean
{
    /** The owning bean */
    private final PropertyBackedBean owner;

    /** The Java bean instance. */
    private final Object bean;

    /** A Spring wrapper around the Java bean, allowing easy configuration of properties. */
    private final BeanWrapper wrappedBean;

    /** The property names. */
    private final Set<String> propertyNames;

    /** The writeable properties. */
    private final Set<String> writeableProperties;

    /** The prefix used to look up default values for this bean's properties */
    private String defaultKeyPrefix;

    /** The prefix used to look up instance-specific default values for this bean's properties */
    private String instanceKeyPrefix;

    /**
     * Constructor for dynamically created instances, e.g. through {@link ChildApplicationContextFactory}.
     * 
     * @param parent
     *            the parent application context
     * @param registry
     *            the registry of property backed beans
     * @param propertyDefaults
     *            property defaults provided by the installer or System properties
     * @param category
     *            the category
     * @param id
     *            the instance id
     * @param owner
     *            the owning bean
     * @param type
     *            the class of Java bean to be wrapped
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public CompositeDataBean(ApplicationContext parent, PropertyBackedBean owner, PropertyBackedBeanRegistry registry,
            Properties propertyDefaults, String category, Class<?> type, List<String> id) throws IOException
    {
        setApplicationContext(parent);
        setRegistry(registry);
        setPropertyDefaults(propertyDefaults);
        setBeanName(category);
        setId(id);
        this.owner = owner;

        try
        {
            this.bean = type.newInstance();
            // Tell the bean its name if it cares
            if (this.bean instanceof BeanNameAware)
            {
                ((BeanNameAware) this.bean).setBeanName(id.get(id.size() - 1));
            }
            this.wrappedBean = new BeanWrapperImpl(this.bean);
            PropertyDescriptor[] descriptors = this.wrappedBean.getPropertyDescriptors();
            this.propertyNames = new TreeSet<String>();
            this.writeableProperties = new TreeSet<String>();
            for (PropertyDescriptor descriptor : descriptors)
            {
                Method readMethod = descriptor.getReadMethod();
                if (readMethod != null)
                {
                    if (readMethod.getDeclaringClass().isAssignableFrom(Object.class))
                    {
                        // Ignore Object properties such as class
                        continue;
                    }
                    this.propertyNames.add(descriptor.getName());
                    if (descriptor.getWriteMethod() != null)
                    {
                        this.writeableProperties.add(descriptor.getName());
                    }
                }
            }
            afterPropertiesSet();
        }
        catch (RuntimeException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    /*
     * (non-Javadoc)
     * @see org.alfresco.repo.management.subsystems.AbstractPropertyBackedBean#afterPropertiesSet()
     */
    @Override
    public void afterPropertiesSet() throws Exception
    {
        // Derive a default and instance key prefix of the form "<parent>.default." and "<parent>.value.<this>."
        StringBuilder defaultKeyPrefixBuff = new StringBuilder(200);
        StringBuilder instanceKeyPrefixBuff = new StringBuilder(200);
        List<String> id = getId();
        int size = id.size();
        if (size > 1)
        {
            defaultKeyPrefixBuff.append(id.get(size - 2)).append('.');
            instanceKeyPrefixBuff.append(defaultKeyPrefixBuff);
        }
        defaultKeyPrefixBuff.append("default.");
        instanceKeyPrefixBuff.append("value.").append(id.get(size - 1)).append('.');

        this.defaultKeyPrefix = defaultKeyPrefixBuff.toString();
        this.instanceKeyPrefix = instanceKeyPrefixBuff.toString();

        // Set initial values according to property defaults.
        super.afterPropertiesSet();
    }

    /*
     * (non-Javadoc)
     * @see org.alfresco.repo.management.subsystems.AbstractPropertyBackedBean#resolveDefault(java.lang.String)
     */
    @Override
    protected String resolveDefault(String name)
    {
        // Because we may have multiple instances, we try an instance-specific default before falling back to a general
        // property level default
        String value = super.resolveDefault(this.instanceKeyPrefix + name);
        return value == null ? super.resolveDefault(this.defaultKeyPrefix + name) : value;
    }

    /*
     * (non-Javadoc)
     * @see org.alfresco.repo.management.subsystems.PropertyBackedBean#getProperty(java.lang.String)
     */
    public String getProperty(String name)
    {
        Object value = this.wrappedBean.getPropertyValue(name);
        return value == null ? null : value.toString();
    }

    /*
     * (non-Javadoc)
     * @see org.alfresco.repo.management.subsystems.PropertyBackedBean#getPropertyNames()
     */
    public Set<String> getPropertyNames()
    {
        return this.propertyNames;
    }

    /*
     * (non-Javadoc)
     * @see org.alfresco.repo.management.subsystems.PropertyBackedBean#setProperty(java.lang.String, java.lang.String)
     */
    public void setProperty(String name, String value)
    {
        this.wrappedBean.setPropertyValue(name, value);
    }

    /*
     * (non-Javadoc)
     * @see org.alfresco.repo.management.subsystems.AbstractPropertyBackedBean#isUpdateable(java.lang.String)
     */
    @Override
    public boolean isUpdateable(String name)
    {
        return this.writeableProperties.contains(name);
    }

    /*
     * (non-Javadoc)
     * @see org.alfresco.repo.management.subsystems.PropertyBackedBean#start()
     */
    public void start()
    {
    }

    /*
     * (non-Javadoc)
     * @see org.alfresco.repo.management.subsystems.PropertyBackedBean#stop()
     */
    public void stop()
    {
        // Ensure any edits to child composites cause the parent to be shut down and subsequently re-initialized
        this.owner.stop();
    }

    /**
     * Gets the wrapped Java bean.
     * 
     * @return the Java bean
     */
    protected Object getBean()
    {
        return this.bean;
    }
}
