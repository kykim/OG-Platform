/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.joda.beans.BeanBuilder;
import org.joda.beans.BeanDefinition;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectBean;
import org.joda.beans.impl.direct.DirectBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

/**
 * The complete set of components published in a RESTful way by JAX-RS.
 * <p>
 * Components may be managed by {@link DataComponentsResource} or by JAX-RS directly.
 */
@BeanDefinition
public class PublishedComponents extends DirectBean {

  /**
   * The managed components.
   * These will be controlled by {@link DataComponentsResource}.
   */
  @PropertyDefinition(validate = "notNull")
  private final List<PublishedComponent> _managedComponents = new ArrayList<PublishedComponent>();
  /**
   * The set of additional singleton objects that are used by JAX-RS.
   * This may include filters, providers, consumer and resources that should
   * be used directly by JAX-RS and not managed by {@link DataComponentsResource}.
   */
  @PropertyDefinition(validate = "notNull")
  private final Set<Object> _additionalSingletons = new LinkedHashSet<Object>();

  /**
   * Creates an instance.
   */
  public PublishedComponents() {
  }

  //-------------------------------------------------------------------------
  /**
   * Adds a managed component to the known set.
   * 
   * @param info  the managed component info, not null
   * @param instance  the managed component instance, not null
   */
  public void addManagedComponent(ComponentInfo info, Object instance) {
    getManagedComponents().add(new PublishedComponent(info, instance));
  }

  /**
   * Adds a managed component to the known set.
   * 
   * @param instance  the JAX-RS singleton instance, not null
   */
  public void addAdditionalSingleton(Object instance) {
    getAdditionalSingletons().add(instance);
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the complete set of singletons, handling managed components.
   * <p>
   * This method wraps the managed components in an instance of {@link DataComponentsResource}.
   * 
   * @return the complete set of singletons, not null
   */
  public Set<Object> getAllSingletons() {
    DataComponentsResource dcr = new DataComponentsResource(getManagedComponents());
    Set<Object> set = new LinkedHashSet<Object>();
    set.add(dcr);
    set.addAll(getAdditionalSingletons());
    return set;
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code PublishedComponents}.
   * @return the meta-bean, not null
   */
  public static PublishedComponents.Meta meta() {
    return PublishedComponents.Meta.INSTANCE;
  }
  static {
    JodaBeanUtils.registerMetaBean(PublishedComponents.Meta.INSTANCE);
  }

  @Override
  public PublishedComponents.Meta metaBean() {
    return PublishedComponents.Meta.INSTANCE;
  }

  @Override
  protected Object propertyGet(String propertyName, boolean quiet) {
    switch (propertyName.hashCode()) {
      case -2026654699:  // managedComponents
        return getManagedComponents();
      case -566781617:  // additionalSingletons
        return getAdditionalSingletons();
    }
    return super.propertyGet(propertyName, quiet);
  }

  @SuppressWarnings("unchecked")
  @Override
  protected void propertySet(String propertyName, Object newValue, boolean quiet) {
    switch (propertyName.hashCode()) {
      case -2026654699:  // managedComponents
        setManagedComponents((List<PublishedComponent>) newValue);
        return;
      case -566781617:  // additionalSingletons
        setAdditionalSingletons((Set<Object>) newValue);
        return;
    }
    super.propertySet(propertyName, newValue, quiet);
  }

  @Override
  protected void validate() {
    JodaBeanUtils.notNull(_managedComponents, "managedComponents");
    JodaBeanUtils.notNull(_additionalSingletons, "additionalSingletons");
    super.validate();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      PublishedComponents other = (PublishedComponents) obj;
      return JodaBeanUtils.equal(getManagedComponents(), other.getManagedComponents()) &&
          JodaBeanUtils.equal(getAdditionalSingletons(), other.getAdditionalSingletons());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash += hash * 31 + JodaBeanUtils.hashCode(getManagedComponents());
    hash += hash * 31 + JodaBeanUtils.hashCode(getAdditionalSingletons());
    return hash;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the managed components.
   * These will be controlled by {@link DataComponentsResource}.
   * @return the value of the property, not null
   */
  public List<PublishedComponent> getManagedComponents() {
    return _managedComponents;
  }

  /**
   * Sets the managed components.
   * These will be controlled by {@link DataComponentsResource}.
   * @param managedComponents  the new value of the property
   */
  public void setManagedComponents(List<PublishedComponent> managedComponents) {
    this._managedComponents.clear();
    this._managedComponents.addAll(managedComponents);
  }

  /**
   * Gets the the {@code managedComponents} property.
   * These will be controlled by {@link DataComponentsResource}.
   * @return the property, not null
   */
  public final Property<List<PublishedComponent>> managedComponents() {
    return metaBean().managedComponents().createProperty(this);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the set of additional singleton objects that are used by JAX-RS.
   * This may include filters, providers, consumer and resources that should
   * be used directly by JAX-RS and not managed by {@link DataComponentsResource}.
   * @return the value of the property, not null
   */
  public Set<Object> getAdditionalSingletons() {
    return _additionalSingletons;
  }

  /**
   * Sets the set of additional singleton objects that are used by JAX-RS.
   * This may include filters, providers, consumer and resources that should
   * be used directly by JAX-RS and not managed by {@link DataComponentsResource}.
   * @param additionalSingletons  the new value of the property
   */
  public void setAdditionalSingletons(Set<Object> additionalSingletons) {
    this._additionalSingletons.clear();
    this._additionalSingletons.addAll(additionalSingletons);
  }

  /**
   * Gets the the {@code additionalSingletons} property.
   * This may include filters, providers, consumer and resources that should
   * be used directly by JAX-RS and not managed by {@link DataComponentsResource}.
   * @return the property, not null
   */
  public final Property<Set<Object>> additionalSingletons() {
    return metaBean().additionalSingletons().createProperty(this);
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code PublishedComponents}.
   */
  public static class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code managedComponents} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<List<PublishedComponent>> _managedComponents = DirectMetaProperty.ofReadWrite(
        this, "managedComponents", PublishedComponents.class, (Class) List.class);
    /**
     * The meta-property for the {@code additionalSingletons} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<Set<Object>> _additionalSingletons = DirectMetaProperty.ofReadWrite(
        this, "additionalSingletons", PublishedComponents.class, (Class) Set.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<Object>> _map = new DirectMetaPropertyMap(
        this, null,
        "managedComponents",
        "additionalSingletons");

    /**
     * Restricted constructor.
     */
    protected Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case -2026654699:  // managedComponents
          return _managedComponents;
        case -566781617:  // additionalSingletons
          return _additionalSingletons;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends PublishedComponents> builder() {
      return new DirectBeanBuilder<PublishedComponents>(new PublishedComponents());
    }

    @Override
    public Class<? extends PublishedComponents> beanType() {
      return PublishedComponents.class;
    }

    @Override
    public Map<String, MetaProperty<Object>> metaPropertyMap() {
      return _map;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code managedComponents} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<List<PublishedComponent>> managedComponents() {
      return _managedComponents;
    }

    /**
     * The meta-property for the {@code additionalSingletons} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<Set<Object>> additionalSingletons() {
      return _additionalSingletons;
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}