/**
 * Copyright (C) 2009 - Present by OpenGamma Inc.
 *
 * Please see distribution for license.
 */
package com.opengamma.transport;

/**
 * 
 *
 * @author jim
 */
public class InMemoryRequestConduit {
  public static FudgeRequestSender create(FudgeRequestReceiver receiver) {
    FudgeRequestDispatcher requestDispatcher = new FudgeRequestDispatcher(receiver);
    InMemoryByteArrayRequestConduit requestConduit = new InMemoryByteArrayRequestConduit(requestDispatcher);
    ByteArrayFudgeRequestSender requestSender = new ByteArrayFudgeRequestSender(requestConduit);
    return requestSender;
  }
}
