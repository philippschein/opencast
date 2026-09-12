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

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Tests the {@link RequestUriValidationFilter}
 */
public class RequestUriValidationFilterTest {

  private RequestUriValidationFilter filter;

  @Before
  public void setUp() {
    filter = new RequestUriValidationFilter();
  }

  private HttpServletRequest requestFor(String uri) {
    HttpServletRequest request = EasyMock.createNiceMock(HttpServletRequest.class);
    EasyMock.expect(request.getRequestURI()).andReturn(uri).anyTimes();
    EasyMock.replay(request);
    return request;
  }

  /** A request with a malformed path is rejected with a 400 and never reaches the rest of the chain. */
  private void assertRejected(String uri) throws IOException, ServletException {
    HttpServletResponse response = EasyMock.createNiceMock(HttpServletResponse.class);
    response.sendError(EasyMock.eq(HttpServletResponse.SC_BAD_REQUEST), EasyMock.anyObject(String.class));
    EasyMock.expectLastCall().once();
    EasyMock.replay(response);

    FilterChain chain = EasyMock.createStrictMock(FilterChain.class);
    EasyMock.replay(chain);

    filter.doFilter(requestFor(uri), response, chain);

    EasyMock.verify(response, chain);
  }

  /** A request with a well formed path is passed on untouched. */
  private void assertAccepted(String uri) throws IOException, ServletException {
    HttpServletResponse response = EasyMock.createStrictMock(HttpServletResponse.class);
    EasyMock.replay(response);

    HttpServletRequest request = requestFor(uri);

    FilterChain chain = EasyMock.createNiceMock(FilterChain.class);
    chain.doFilter(request, response);
    EasyMock.expectLastCall().once();
    EasyMock.replay(chain);

    filter.doFilter(request, response, chain);

    EasyMock.verify(response, chain);
  }

  @Test
  public void testIllegalCharacterInPathIsRejected() throws Exception {
    assertRejected("/api/agents/l|||");
  }

  @Test
  public void testFurtherIllegalCharactersInPathAreRejected() throws Exception {
    assertRejected("/api/events/a^b");
    assertRejected("/api/events/a{b}");
    assertRejected("/staticfiles/my file.mp4");
  }

  @Test
  public void testMalformedEscapeSequenceIsRejected() throws Exception {
    assertRejected("/api/agents/%zz");
  }

  @Test
  public void testWellFormedPathIsAccepted() throws Exception {
    assertAccepted("/api/agents/my-capture-agent");
    assertAccepted("/api/events/3f2a1b4c-0d5e-4f6a-8b9c-0d1e2f3a4b5c/acl");
    assertAccepted("/staticfiles/my%20file.mp4");
    assertAccepted("/");
  }

  @Test
  public void testAsteriskFormIsAccepted() throws Exception {
    // OPTIONS * HTTP/1.1
    assertAccepted("*");
  }
}
