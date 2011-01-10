/**
 * Copyright (C) 2009 - Present by OpenGamma Inc.
 *
 * Please see distribution for license.
 */
package com.opengamma.id;

/**
 * Provides uniform access to objects that support having their unique identifier
 * updated after construction.
 * <p>
 * For example, code in the database layer will need to update the unique identifier
 * when the object is stored.
 * <p>
 * This interface makes no guarantees about the thread-safety of implementations.
 */
public interface MutableUniqueIdentifiable {

  /**
   * Sets the unique identifier for this item.
   * 
   * @param uid  the unique identifier to set, not null
   */
  void setUniqueId(UniqueIdentifier uid);

}
