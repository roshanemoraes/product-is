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

import io.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.apache.http.impl.client.HttpClientBuilder;
import org.awaitility.Awaitility;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.identity.integration.test.rest.api.server.async.operation.status.mgt.v1.model.Operation;
import org.wso2.identity.integration.test.rest.api.server.async.operation.status.mgt.v1.model.Operations;
import org.wso2.identity.integration.test.rest.api.server.user.sharing.management.v1.model.UserShareRequestBodyUserCriteria;
import org.wso2.identity.integration.test.rest.api.server.user.sharing.management.v1.model.UserShareWithAllRequestBody;
import org.wso2.identity.integration.test.rest.api.user.common.model.Email;
import org.wso2.identity.integration.test.rest.api.user.common.model.Name;
import org.wso2.identity.integration.test.rest.api.user.common.model.UserObject;
import org.wso2.identity.integration.test.restclients.OAuth2RestClient;
import org.wso2.identity.integration.test.restclients.OrgMgtRestClient;
import org.wso2.identity.integration.test.restclients.SCIM2RestClient;
import org.wso2.identity.integration.test.restclients.UserSharingRestClient;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Positive path tests for Async Operation Status Management REST API.
 */
public class AsyncOperationStatusMgtPositiveTest extends AsyncOperationStatusMgtBaseTest {

    private UserSharingRestClient userSharingRestClient;
    private static final int DEFAULT_LIMIT = 10;
    private static final String DEFAULT_FILTER = "";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Factory(dataProvider = "restAPIUserConfigProvider")
    public AsyncOperationStatusMgtPositiveTest(TestUserMode userMode) throws Exception {

        super.init(userMode);
        this.context = isServer;
        this.authenticatingUserName = context.getContextTenant().getTenantAdmin().getUserName();
        this.authenticatingCredential = context.getContextTenant().getTenantAdmin().getPassword();
        this.tenant = context.getContextTenant().getDomain();
    }

    @DataProvider(name = "restAPIUserConfigProvider")
    public static Object[][] restAPIUserConfigProvider() {

        return new Object[][]{
            {TestUserMode.SUPER_TENANT_ADMIN},
            {TestUserMode.TENANT_ADMIN}
        };
    }

    @BeforeClass(alwaysRun = true)
    public void init() throws Exception {

        super.testInit(API_VERSION, swaggerDefinition, tenant);
        setupRestClients();
    }

    /**
     * @throws Exception
     */
    @AfterClass(alwaysRun = true)
    public void cleanup() throws Exception {

        closeRestClients();
    }

    @Test
    public void testShareUserWithAllOrganizationsAndVerifyAsyncStatus() throws Exception {

        String createdSubOrgId = null;
        String sharedUserId = null;
        try {
            createdSubOrgId = createChildOrganizationForSharing();
            sharedUserId = createUserForSharing();

            shareUserWithAllOrganizations(sharedUserId);
            
            Response response = getAsyncOperations();
            
            response.then()
                    .log().ifValidationFails()
                    .assertThat()
                    .statusCode(HttpStatus.SC_OK);

            String responseBody = response.getBody().asString();
            Operations operations = OBJECT_MAPPER.readValue(responseBody, Operations.class);
            
            Assert.assertNotNull(operations);
            Assert.assertNotNull(operations.getOperations());
            Assert.assertTrue(operations.getOperations().size() >= 1, "No async operations returned");

            Operation op = operations.getOperations().stream()
                    .filter(o -> "B2B_USER_SHARE".equals(o.getOperationType()))
                    .filter(o -> "USER".equals(o.getSubjectType()))
                    .filter(o -> sharedUserId.equals(o.getSubjectId()))
                    .findFirst()
                    .orElse(null);

            Assert.assertNotNull(op, "Expected B2B_USER_SHARE operation for shared user not found");

        } finally {
            if (sharedUserId != null) {
                scim2RestClient.deleteUser(sharedUserId);
            }
            if (createdSubOrgId != null) {
                orgMgtRestClient.deleteOrganization(createdSubOrgId);
            }
        }
    }

    // Helper methods

    private String createChildOrganizationForSharing() throws Exception {

        String uniqueIdentifier = UUID.randomUUID().toString().replace("-", "");
        String orgName = "AsyncChildOrg" + uniqueIdentifier.substring(0, 8);
        return orgMgtRestClient.addOrganization(orgName);
    }

    private String createUserForSharing() throws Exception {

        String uniqueIdentifier = UUID.randomUUID().toString().replace("-", "");
           String userName = "asyncSharedUser" + uniqueIdentifier.substring(0, 8);
        UserObject userObject = new UserObject()
                .userName(userName)
                .password("Password@123")
                .name(new Name().givenName("Async").familyName("ShareUser"));
        userObject.addEmail(new Email().value(userName + "@example.com").primary(true));
        return scim2RestClient.createUser(userObject);
    }

    private void shareUserWithAllOrganizations(String userId) throws Exception {

        UserShareRequestBodyUserCriteria userCriteria = new UserShareRequestBodyUserCriteria()
                .addUserIdsItem(userId);
        UserShareWithAllRequestBody requestBody = new UserShareWithAllRequestBody()
                .userCriteria(userCriteria)
                .policy(UserShareWithAllRequestBody.PolicyEnum.ALL_EXISTING_ORGS_ONLY);
        userSharingRestClient.shareUsersWithAll(requestBody);
    }

    private Set<String> getAsyncOperationIds() {
        return getAsyncOperationIds(DEFAULT_LIMIT, DEFAULT_FILTER);
    }

    private Set<String> getAsyncOperationIds(int limit) {
        return getAsyncOperationIds(limit, DEFAULT_FILTER);
    }

    private Response getAsyncOperations() {
        return getAsyncOperations(DEFAULT_LIMIT, DEFAULT_FILTER);
    }

    private Response getAsyncOperations(int limit, String filter) {

        StringBuilder uri = new StringBuilder(ASYNC_OPERATIONS_ENDPOINT_URI)
                .append("?limit=").append(limit);

        if (filter != null && !filter.isEmpty()) {
            uri.append("&filter=").append(filter);
        }

        return getResponseOfGet(uri.toString());
    }
    // private Operations getAsyncOperations(int limit, String filter) {

    //     StringBuilder uri = new StringBuilder(ASYNC_OPERATIONS_ENDPOINT_URI)
    //             .append("?limit=").append(limit);

    //     if (filter != null && !filter.isEmpty()) {
    //         uri.append("&filter=").append(filter);
    //     }

    //     Response response = getResponseOfGet(uri.toString());

    //     int statusCode = response.getStatusCode();
    //     String responseBody = response.getBody().asString();

    //     System.out.println("Async Operations API Response Status: " + statusCode);
    //     System.out.println("Async Operations API Response Body: " + responseBody);

    //     response.then()
    //             .log().ifValidationFails()
    //             .statusCode(HttpStatus.SC_OK);

    //     try {
    //         return OBJECT_MAPPER.readValue(responseBody, Operations.class);
    //     } catch (Exception e) {
    //         throw new RuntimeException("Failed to deserialize async operations response.", e);
    //     }
    // }

    private Set<String> getAsyncOperationIds(int limit, String filter) {

        StringBuilder uri = new StringBuilder(ASYNC_OPERATIONS_ENDPOINT_URI)
            .append("?limit=").append(limit);

        if (filter != null && !filter.isEmpty()) {
            uri.append("&filter=").append(filter);
        }
        
        Response response = getResponseOfGet(uri.toString());

        int statusCode = response.getStatusCode();
        String responseBody = response.getBody().asString();

        System.out.println("Async Operations API Response Status: " + statusCode);
        System.out.println("Async Operations API Response Body: " + responseBody);

        response.then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.SC_OK);
        Set<String> operationIds = new HashSet<>();
        try {
            JSONObject responseJson = new JSONObject(responseBody);
            JSONArray operations = responseJson.optJSONArray("operations");
            if (operations != null) {
                System.out.println("Found " + operations.length() + " operations in response");
                for (int i = 0; i < operations.length(); i++) {
                    JSONObject operation = operations.getJSONObject(i);
                    String operationId = operation.optString("operationId");
                    operationIds.add(operationId);
                    System.out.println("  Operation " + i + ": " + operationId);
                }
            } else {
                System.out.println("No 'operations' array found in response");
            }
            return operationIds;
        } catch (JSONException e) {
            System.err.println("Failed to parse JSON response: " + e.getMessage());
            throw new RuntimeException("Failed to parse async operations response.", e);
        }
    }

    private void waitForNewAsyncOperationRecord(Set<String> existingOperationIds) {

        Awaitility.await()
                .atMost(60, TimeUnit.SECONDS)
                .pollInterval(2, TimeUnit.SECONDS)
                .ignoreExceptions()
                .until(() -> {
                    Set<String> latestIds = getAsyncOperationIds();
                    Set<String> newIds = new HashSet<>(latestIds);
                    newIds.removeAll(existingOperationIds);
                    return !newIds.isEmpty();
                });
    }

    // Setup methods.

    private void setupRestClients() throws Exception {

        oAuth2RestClient = new OAuth2RestClient(serverURL, tenantInfo);
        scim2RestClient = new SCIM2RestClient(serverURL, tenantInfo);
        orgMgtRestClient = new OrgMgtRestClient(context, tenantInfo, serverURL,
                getAuthorizedAPIList());
        userSharingRestClient = new UserSharingRestClient(serverURL, tenantInfo);
        httpClient = HttpClientBuilder.create().build();
    }

    private void closeRestClients() throws IOException {

        oAuth2RestClient.closeHttpClient();
        scim2RestClient.closeHttpClient();
        orgMgtRestClient.closeHttpClient();
        userSharingRestClient.closeHttpClient();
    }

    /**
     * Get the list of sub APIs that need to be authorized for the B2B application.
     *
     * @return A JSON object containing the API and scopes list.
     * @throws JSONException If an error occurs while creating the JSON object.
     */
    private JSONObject getAuthorizedAPIList() throws JSONException {

        JSONObject jsonObject = new JSONObject();
        // SCIM2 Users.
        jsonObject.put("/o/scim2/Users",
                new String[] {"internal_org_user_mgt_create", "internal_org_user_mgt_delete"});
        // SCIM2 Groups.
        jsonObject.put("/o/scim2/Groups",
                new String[] {"internal_org_group_mgt_create", "internal_org_group_mgt_delete"});
        // Application management.
        jsonObject.put("/o/api/server/v1/applications",
                new String[] {"internal_org_application_mgt_view", "internal_org_application_mgt_create",
                        "internal_org_application_mgt_update"});
        jsonObject.put("/api/server/v1/applications",
                new String[] {"internal_application_mgt_view", "internal_application_mgt_delete"});
        // Organization management.
        jsonObject.put("/api/server/v1/organizations",
                new String[] {"internal_organization_create", "internal_organization_delete"});

        return jsonObject;
    }
}
