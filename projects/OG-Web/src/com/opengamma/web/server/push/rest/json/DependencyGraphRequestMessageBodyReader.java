/**
 * Copyright (C) 2012 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.web.server.push.rest.json;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;

import com.opengamma.web.server.push.analytics.DependencyGraphRequest;

/**
 *
 */
@Provider
@Consumes(MediaType.APPLICATION_JSON)
public class DependencyGraphRequestMessageBodyReader implements MessageBodyReader<DependencyGraphRequest> {

  @Override
  public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
    return type.equals(DependencyGraphRequest.class);
  }

  @Override
  public DependencyGraphRequest readFrom(Class<DependencyGraphRequest> type,
                                         Type genericType,
                                         Annotation[] annotations,
                                         MediaType mediaType,
                                         MultivaluedMap<String, String> httpHeaders,
                                         InputStream entityStream) throws IOException, WebApplicationException {
    //JSONObject jsonObject = new JSONObject(IOUtils.toString(new BufferedInputStream(entityStream)));
    return new DependencyGraphRequest(0, 0);
  }
}
