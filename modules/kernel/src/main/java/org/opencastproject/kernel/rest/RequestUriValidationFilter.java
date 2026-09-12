/*
 * Licensed to The Apereo Foundation under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 *
 * The Apereo Foundation licenses this file to you under the Educational
 * Community License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at:
 *
 *   http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package org.opencastproject.kernel.rest;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.propertytypes.ServiceRanking;
import org.osgi.service.http.whiteboard.propertytypes.HttpWhiteboardContextSelect;
import org.osgi.service.http.whiteboard.propertytypes.HttpWhiteboardFilterName;
import org.osgi.service.http.whiteboard.propertytypes.HttpWhiteboardFilterPattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Rejects requests whose path cannot be parsed as a {@link URI} with a <code>400 Bad Request</code>.
 * <p>
 * Jetty accepts characters in the request path which are not valid according to RFC 3986 (for example
 * <code>|</code>, <code>^</code> or <code>{</code>). CXF, however, parses the request URL into a
 * {@link URI} while looking up the destination for a request, long before the request is dispatched to a
 * JAX-RS resource. The resulting <code>IllegalArgumentException</code> can neither be handled by a JAX-RS
 * exception mapper nor by the endpoint itself, so it escapes the servlet and is rendered as a
 * <code>500 Internal Server Error</code> including a stack trace.
 * <p>
 * Only the path is validated, not the query string. Opencast passes filter expressions and JSON documents as
 * query parameters, and those regularly contain characters a {@link URI} would reject.
 */
@Component(
    service = Filter.class,
    property = {
        "service.description=Request URI Validation Filter",
    }
)
@ServiceRanking(1010)
@HttpWhiteboardFilterName("RequestUriValidationFilter")
@HttpWhiteboardFilterPattern("/*")
@HttpWhiteboardContextSelect("(osgi.http.whiteboard.context.name=opencast)")
public class RequestUriValidationFilter implements Filter {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(RequestUriValidationFilter.class);

  /**
   * {@inheritDoc}
   *
   * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
   */
  @Override
  public void init(FilterConfig config) throws ServletException {
  }

  /**
   * {@inheritDoc}
   *
   * @see javax.servlet.Filter#destroy()
   */
  @Override
  public void destroy() {
  }

  /**
   * {@inheritDoc}
   *
   * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse,
   *      javax.servlet.FilterChain)
   */
  @Override
  public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException,
          ServletException {

    HttpServletRequest request = (HttpServletRequest) req;
    String uri = request.getRequestURI();

    try {
      new URI(uri);
    } catch (URISyntaxException e) {
      logger.debug("Rejecting request with malformed URI '{}' from {}: {}", uri, request.getRemoteAddr(),
          e.getReason());
      ((HttpServletResponse) resp).sendError(HttpServletResponse.SC_BAD_REQUEST, "Malformed request URI");
      return;
    }

    chain.doFilter(req, resp);
  }
}
