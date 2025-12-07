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
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.identity.integration.test.rest.api.server.user.sharing.management.v1.model.UserShareRequestBodyUserCriteria;
import org.wso2.identity.integration.test.rest.api.server.user.sharing.management.v1.model.UserShareWithAllRequestBody;
import org.wso2.identity.integration.test.rest.api.user.common.model.Email;
import org.wso2.identity.integration.test.rest.api.user.common.model.Name;
import org.wso2.identity.integration.test.rest.api.user.common.model.UserObject;
import org.wso2.identity.integration.test.restclients.OAuth2RestClient;
import org.wso2.identity.integration.test.restclients.OrgMgtRestClient;
import org.wso2.identity.integration.test.restclients.SCIM2RestClient;
import org.wso2.identity.integration.test.restclients.UserSharingRestClient;

import static org.wso2.identity.integration.test.rest.api.server.user.sharing.management.v1.constant.UserSharingConstants.AUTHORIZED_APIS_JSON;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Positive path tests for Async Operation Status Management REST API.
 */
public class AsyncOperationStatusMgtPositiveTest extends AsyncOperationStatusMgtBaseTest {

    // private static final String ROOT_TENANT_DOMAIN = "carbon.super";
    // private static final String ROOT_ORG_ID = "";
    // private static final String APPLICATION_NAME = "Async Test App";
    // private static final String ORGANIZATION_NAME = "Async Test Org";
    // public static final String AUTHORIZED_APIS_JSON = "user-sharing-apis.json";

    // private OrgMgtRestClient orgMgtRestClient;
    // private String createdOrgId;
    // private String createdAppId;
    private UserSharingRestClient userSharingRestClient;

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
        // setupDetailMaps();
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
    public void testCreateAppAndOrgAndVerifyAsyncStatus() throws Exception {

        // ApplicationPostRequest appRequest = new ApplicationPostRequest().name(APPLICATION_NAME);
        // Response appResponse = applicationClient.createApplication(appRequest);
        // appResponse.then().statusCode(HttpStatus.SC_CREATED);
        // createdAppId = appResponse.getHeader(HttpHeaders.LOCATION);

        // RegisterOrganizationRequestDTO orgRequest = new RegisterOrganizationRequestDTO().name(ORGANIZATION_NAME);
        // OrganizationResponseModel orgResponse = orgMgtRestClient.addOrganization(orgRequest);
        // createdOrgId = orgResponse.getId();

        // Operation latestOperation = asyncStatusClient.getLatestOperation();
        // UnitOperations unitOperations = asyncStatusClient.getUnitOperations(latestOperation.getCorrelationId());

        // org.testng.Assert.assertEquals(latestOperation.getStatus(), StatusEnum.SUCCESS);
        // org.testng.Assert.assertTrue(unitOperations.getUnitOperations().stream()
        //         .anyMatch(unit -> createdOrgId.equals(unit.getTargetOrgId())));
        Set<String> initialOperationIds = getAsyncOperationIds();
        String createdSubOrgId = null;
        String sharedUserId = null;
        try {
            createdSubOrgId = createChildOrganizationForSharing();
            sharedUserId = createUserForSharing();
            shareUserWithAllOrganizations(sharedUserId);
            waitForNewAsyncOperationRecord(initialOperationIds);
            Set<String> updatedOperationIds = getAsyncOperationIds();
            Set<String> newOperationIds = new HashSet<>(updatedOperationIds);
            newOperationIds.removeAll(initialOperationIds);
            org.testng.Assert.assertFalse(newOperationIds.isEmpty(),
                    "Async operations table was not updated after sharing the user.");
        } finally {
            if (sharedUserId != null) {
                scim2RestClient.deleteUser(sharedUserId);
            }
            if (createdSubOrgId != null) {
                orgMgtRestClient.deleteOrganization(createdSubOrgId);
            }
        }
    }

    private String createChildOrganizationForSharing() throws Exception {

        String uniqueIdentifier = UUID.randomUUID().toString().replace("-", "");
        String orgName = "AsyncChildOrg" + uniqueIdentifier.substring(0, 8);
        return orgMgtRestClient.addOrganization(orgName);
    }

    private String createUserForSharing() throws Exception {

        String uniqueIdentifier = UUID.randomUUID().toString().replace("-", "");
           String userName = "asyncSharedUser" + unique45679Identifier.substring(0, 8);
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

        Response response = getResponseOfGet(ASYNC_OPERATIONS_ENDPOINT_URI + "?limit=25");
        response.then().log().ifValidationFails().statusCode(HttpStatus.SC_OK);
        Set<String> operationIds = new HashSet<>();
        try {
            JSONObject responseJson = new JSONObject(response.getBody().asString());
            JSONArray operations = responseJson.optJSONArray("operations");
            if (operations != null) {
                for (int i = 0; i < operations.length(); i++) {
                    JSONObject operation = operations.getJSONObject(i);
                    operationIds.add(operation.optString("operationId"));
                }
            }
            return operationIds;
        } catch (JSONException e) {
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
