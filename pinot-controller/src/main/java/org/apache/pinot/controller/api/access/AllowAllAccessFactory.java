/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.pinot.controller.api.access;

import javax.ws.rs.core.HttpHeaders;


public class AllowAllAccessFactory implements AccessControlFactory {
  private static final AccessControl ALLOW_ALL_ACCESS = new AccessControl() {
    @Override
    public boolean hasAccess(String tableName, AccessType accessType, HttpHeaders httpHeaders, String endpointUrl) {
      return true;
    }
  };

  @Override
  public AccessControl create() {
    return ALLOW_ALL_ACCESS;
  }
}
