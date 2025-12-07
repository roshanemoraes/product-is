/*
 * Copyright (c) 2025, WSO2 LLC. (https://www.wso2.com).
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

package org.wso2.identity.integration.test.rest.api.server.async.operation.status.mgt.v1;

import io.restassured.RestAssured;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.wso2.identity.integration.test.rest.api.server.common.RESTAPIServerTestBase;
import org.wso2.identity.integration.test.restclients.OAuth2RestClient;
import org.wso2.identity.integration.test.restclients.OrgMgtRestClient;
import org.wso2.identity.integration.test.restclients.SCIM2RestClient;
import org.wso2.identity.integration.test.utils.OAuth2Constant;

import java.io.IOException;
import java.util.List;

import static org.wso2.identity.integration.test.restclients.RestBaseClient.USER_AGENT_ATTRIBUTE;

/**
 * Base test class for Asynchronous Operation Status Management REST APIs.
 */
public class AsyncOperationStatusMgtBaseTest extends RESTAPIServerTestBase {

    private static final String API_DEFINITION_NAME = "asynchronous-operation-status.yaml";
    static final String API_VERSION = "v1";
    private static final String API_PACKAGE_NAME =
            "org.wso2.carbon.identity.api.server.asynchronous.operation.status.management.v1";

    public static final String ASYNC_OPERATIONS_ENDPOINT_URI = "/async-operations";
    public static final String ASYNC_UNIT_OPERATIONS_ENDPOINT_URI = "/unit-operations";

    protected OAuth2RestClient oAuth2RestClient;
    protected SCIM2RestClient scim2RestClient;
    protected OrgMgtRestClient orgMgtRestClient;
    protected HttpClient httpClient;

    protected static String swaggerDefinition;

    static {
        try {
            swaggerDefinition = getAPISwaggerDefinition(API_PACKAGE_NAME, API_DEFINITION_NAME);
        } catch (IOException e) {
            Assert.fail(String.format("Unable to read the swagger definition %s from %s", API_DEFINITION_NAME,
                    API_PACKAGE_NAME), e);
        }
    }

    @AfterClass(alwaysRun = true)
    public void testConclude() throws Exception {

        super.conclude();
    }

    @BeforeMethod(alwaysRun = true)
    public void testInit() {

        RestAssured.basePath = basePath;
    }

    @AfterMethod(alwaysRun = true)
    public void testFinish() {

        RestAssured.basePath = StringUtils.EMPTY;
    }

    // Request Sending Methods.

    protected HttpResponse sendGetRequest(String endpointURL, HttpClient client) throws Exception {

        HttpGet request = new HttpGet(endpointURL);
        request.setHeader(USER_AGENT_ATTRIBUTE, OAuth2Constant.USER_AGENT);
        return client.execute(request);
    }

    protected HttpResponse sendPostRequest(String endpointURL, List<NameValuePair> urlParameters, HttpClient client)
            throws Exception {

        HttpPost request = new HttpPost(endpointURL);
        request.setHeader(USER_AGENT_ATTRIBUTE, OAuth2Constant.USER_AGENT);
        request.setEntity(new UrlEncodedFormEntity(urlParameters));
        return client.execute(request);
    }
}
