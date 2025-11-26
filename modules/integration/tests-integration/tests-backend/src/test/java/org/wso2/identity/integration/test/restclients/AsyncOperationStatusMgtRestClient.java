/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.identity.integration.test.restclients;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.wso2.carbon.identity.api.server.asynchronous.operation.status.management.v1.model.Operation;
import org.wso2.carbon.identity.api.server.asynchronous.operation.status.management.v1.model.Operations;
import org.wso2.carbon.identity.api.server.asynchronous.operation.status.management.v1.model.UnitOperations;
import org.wso2.carbon.automation.engine.context.beans.Tenant;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;
import org.wso2.identity.integration.test.utils.OAuth2Constant;

import java.io.Closeable;
import java.io.IOException;

/**
 * REST client for asynchronous operation status management APIs.
 */
public class AsyncOperationStatusMgtRestClient extends RestBaseClient implements Closeable {

    private static final String ASYNC_OPERATION_STATUS_PATH = "async-operation-status";
    private static final String OPERATIONS_PATH = "operations";
    private static final String UNIT_OPERATIONS_PATH = "units";

    private final String serverUrl;
    private final String tenantDomain;
    private final String basePath;
    private final String username;
    private final String password;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AsyncOperationStatusMgtRestClient(String serverUrl, Tenant tenantInfo) {

        this.serverUrl = serverUrl;
        this.tenantDomain = tenantInfo.getContextUser().getUserDomain();
        this.username = tenantInfo.getContextUser().getUserName();
        this.password = tenantInfo.getContextUser().getPassword();
        this.basePath = resolveBasePath();
    }

    /**
     * Retrieve asynchronous operations created within the tenant.
     */
    public Operations getOperations() throws Exception {

        try (CloseableHttpResponse response = getResponseOfHttpGet(basePath + PATH_SEPARATOR + OPERATIONS_PATH,
                getHeadersWithBasicAuth())) {
            String responseString = EntityUtils.toString(response.getEntity());
            return objectMapper.readValue(responseString, Operations.class);
        }
    }

    /**
     * Retrieve a specific asynchronous operation by correlation identifier.
     */
    public Operation getOperation(String correlationId) throws Exception {

        String endpoint = basePath + PATH_SEPARATOR + OPERATIONS_PATH + PATH_SEPARATOR + correlationId;
        try (CloseableHttpResponse response = getResponseOfHttpGet(endpoint, getHeadersWithBasicAuth())) {
            String responseString = EntityUtils.toString(response.getEntity());
            return objectMapper.readValue(responseString, Operation.class);
        }
    }

    /**
     * Retrieve unit operation records for a given correlation identifier.
     */
    public UnitOperations getUnitOperations(String correlationId) throws Exception {

        String endpoint = basePath + PATH_SEPARATOR + OPERATIONS_PATH + PATH_SEPARATOR + correlationId +
                PATH_SEPARATOR + UNIT_OPERATIONS_PATH;
        try (CloseableHttpResponse response = getResponseOfHttpGet(endpoint, getHeadersWithBasicAuth())) {
            String responseString = EntityUtils.toString(response.getEntity());
            return objectMapper.readValue(responseString, UnitOperations.class);
        }
    }

    private String resolveBasePath() {

        if (StringUtils.equals(tenantDomain, MultitenantConstants.SUPER_TENANT_DOMAIN_NAME)) {
            return serverUrl + API_SERVER_PATH + PATH_SEPARATOR + ASYNC_OPERATION_STATUS_PATH;
        }
        return serverUrl + TENANT_PATH + tenantDomain + PATH_SEPARATOR + API_SERVER_PATH + PATH_SEPARATOR +
                ASYNC_OPERATION_STATUS_PATH;
    }

    private Header[] getHeadersWithBasicAuth() {

        String credentials = Base64.encodeBase64String((username + ":" + password).getBytes()).trim();
        return new Header[] {
                new BasicHeader(USER_AGENT_ATTRIBUTE, OAuth2Constant.USER_AGENT),
                new BasicHeader(AUTHORIZATION_ATTRIBUTE, BASIC_AUTHORIZATION_ATTRIBUTE + credentials),
                new BasicHeader(CONTENT_TYPE_ATTRIBUTE, "application/json")
        };
    }

    @Override
    public void close() throws IOException {

        client.close();
    }
}
