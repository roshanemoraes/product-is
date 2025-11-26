/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.identity.integration.test.restclients;

import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.wso2.carbon.automation.engine.context.AutomationContext;
import org.wso2.carbon.automation.engine.context.beans.Tenant;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;
import org.wso2.identity.integration.test.utils.OAuth2Constant;

import java.io.Closeable;
import java.io.IOException;

/**
 * REST client for the Asynchronous Operation Status Management APIs.
 */
public class AsyncOperationStatusMgtRestClient extends RestBaseClient implements Closeable {

    private static final String ASYNC_OPERATION_STATUS_PATH = "async-operation-status";
    private static final String OPERATIONS_PATH = "operations";
    private static final String UNIT_OPERATIONS_PATH = "units";

    private final Tenant tenantInfo;
    private final OAuth2RestClient oAuth2RestClient;
    private final String asyncStatusBasePath;
    private final String tenantDomain;
    private final String baseUrl;

    public AsyncOperationStatusMgtRestClient(String serverUrl, AutomationContext context) {

        this.tenantInfo = context.getContextTenant();
        this.baseUrl = serverUrl;
        this.tenantDomain = tenantInfo.getDomain();
        this.oAuth2RestClient = new OAuth2RestClient(serverUrl, tenantInfo);
        this.asyncStatusBasePath = getAsyncStatusBasePath();
    }

    /**
     * Retrieve the list of operations for the tenant.
     */
    public JSONArray getOperations() throws Exception {

        try (CloseableHttpResponse response = getResponseOfHttpGet(buildOperationsPath(), getHeadersWithBearerToken())) {
            String responseString = EntityUtils.toString(response.getEntity());
            return getJSONArray(responseString);
        }
    }

    /**
     * Retrieve a single operation by correlation ID.
     */
    public JSONObject getOperation(String correlationId) throws Exception {

        try (CloseableHttpResponse response = getResponseOfHttpGet(buildOperationPath(correlationId),
                getHeadersWithBearerToken())) {
            String responseString = EntityUtils.toString(response.getEntity());
            return getJSONObject(responseString);
        }
    }

    /**
     * Retrieve unit operation records for a given correlation ID.
     */
    public JSONArray getUnitOperations(String correlationId) throws Exception {

        try (CloseableHttpResponse response = getResponseOfHttpGet(buildOperationPath(correlationId) +
                PATH_SEPARATOR + UNIT_OPERATIONS_PATH, getHeadersWithBearerToken())) {
            String responseString = EntityUtils.toString(response.getEntity());
            return getJSONArray(responseString);
        }
    }

    private String buildOperationsPath() {

        return asyncStatusBasePath + PATH_SEPARATOR + OPERATIONS_PATH;
    }

    private String buildOperationPath(String correlationId) {

        return buildOperationsPath() + PATH_SEPARATOR + correlationId;
    }

    private String getAsyncStatusBasePath() {

        String tenantDomain = tenantInfo.getDomain();
        if (StringUtils.equals(tenantDomain, MultitenantConstants.SUPER_TENANT_DOMAIN_NAME)) {
            return baseUrl + PATH_SEPARATOR + API_SERVER_PATH + PATH_SEPARATOR + ASYNC_OPERATION_STATUS_PATH;
        }
        return baseUrl + PATH_SEPARATOR + TENANT_PATH + tenantDomain + PATH_SEPARATOR + API_SERVER_PATH +
                PATH_SEPARATOR + ASYNC_OPERATION_STATUS_PATH;
    }

    private Header[] getHeadersWithBearerToken() throws Exception {

        return new Header[] {
                new BasicHeader(USER_AGENT_ATTRIBUTE, OAuth2Constant.USER_AGENT),
                new BasicHeader(AUTHORIZATION_ATTRIBUTE, BEARER_TOKEN_AUTHORIZATION_ATTRIBUTE + getAccessToken()),
                new BasicHeader(CONTENT_TYPE_ATTRIBUTE, "application/json")
        };
    }

    private String getAccessToken() throws Exception {

        return oAuth2RestClient.getM2MAccessToken();
    }

    @Override
    public void close() throws IOException {

        client.close();
    }
}
