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
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.identity.integration.test.asyncOperationStatusMgt;

import io.restassured.response.Response;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.carbon.automation.engine.context.AutomationContext;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.identity.api.server.asynchronous.operation.status.management.v1.model.Operation;
import org.wso2.carbon.identity.api.server.asynchronous.operation.status.management.v1.model.OperationSummary;
import org.wso2.carbon.identity.api.server.asynchronous.operation.status.management.v1.model.UnitOperation;
import org.wso2.carbon.identity.api.server.asynchronous.operation.status.management.v1.model.UnitOperations;
import org.wso2.identity.integration.test.rest.api.server.user.sharing.management.v1.UserSharingBaseTest;
import org.wso2.identity.integration.test.rest.api.server.user.sharing.management.v1.constant.UserSharingConstants;
import org.wso2.identity.integration.test.rest.api.server.user.sharing.management.v1.model.RoleWithAudience;
import org.wso2.identity.integration.test.rest.api.server.user.sharing.management.v1.model.UserShareRequestBody;
import org.wso2.identity.integration.test.rest.api.server.user.sharing.management.v1.model.UserShareWithAllRequestBody;
import org.wso2.identity.integration.test.rest.api.server.user.sharing.management.v1.model.UserUnshareRequestBody;
import org.wso2.identity.integration.test.rest.api.server.user.sharing.management.v1.model.UserUnshareWithAllRequestBody;
import org.wso2.identity.integration.test.restclients.AsyncOperationStatusMgtRestClient;
import org.wso2.identity.integration.test.restclients.OAuth2RestClient;
import org.wso2.identity.integration.test.restclients.OrgMgtRestClient;
import org.wso2.identity.integration.test.restclients.SCIM2RestClient;
import org.wso2.identity.integration.test.utils.OAuth2Constant;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.wso2.identity.integration.test.rest.api.server.user.sharing.management.v1.constant.UserSharingConstants.API_VERSION;
import static org.wso2.identity.integration.test.rest.api.server.user.sharing.management.v1.constant.UserSharingConstants.APPLICATION_AUDIENCE;
import static org.wso2.identity.integration.test.rest.api.server.user.sharing.management.v1.constant.UserSharingConstants.APP_1_NAME;
import static org.wso2.identity.integration.test.rest.api.server.user.sharing.management.v1.constant.UserSharingConstants.APP_2_NAME;
import static org.wso2.identity.integration.test.rest.api.server.user.sharing.management.v1.constant.UserSharingConstants.APP_ROLE_1;
import static org.wso2.identity.integration.test.rest.api.server.user.sharing.management.v1.constant.UserSharingConstants.APP_ROLE_2;
import static org.wso2.identity.integration.test.rest.api.server.user.sharing.management.v1.constant.UserSharingConstants.APP_ROLE_3;
import static org.wso2.identity.integration.test.rest.api.server.user.sharing.management.v1.constant.UserSharingConstants.AUTHORIZED_APIS_JSON;
import static org.wso2.identity.integration.test.rest.api.server.user.sharing.management.v1.constant.UserSharingConstants.L1_ORG_1_NAME;
import static org.wso2.identity.integration.test.rest.api.server.user.sharing.management.v1.constant.UserSharingConstants.L1_ORG_1_USER_1_USERNAME;
import static org.wso2.identity.integration.test.rest.api.server.user.sharing.management.v1.constant.UserSharingConstants.L1_ORG_1_USER_2_USERNAME;
import static org.wso2.identity.integration.test.rest.api.server.user.sharing.management.v1.constant.UserSharingConstants.L1_ORG_1_USER_3_USERNAME;
import static org.wso2.identity.integration.test.rest.api.server.user.sharing.management.v1.constant.UserSharingConstants.L1_ORG_2_NAME;
import static org.wso2.identity.integration.test.rest.api.server.user.sharing.management.v1.constant.UserSharingConstants.L1_ORG_3_NAME;
import static org.wso2.identity.integration.test.rest.api.server.user.sharing.management.v1.constant.UserSharingConstants.L2_ORG_1_NAME;
import static org.wso2.identity.integration.test.rest.api.server.user.sharing.management.v1.constant.UserSharingConstants.L2_ORG_2_NAME;
import static org.wso2.identity.integration.test.rest.api.server.user.sharing.management.v1.constant.UserSharingConstants.L2_ORG_3_NAME;
import static org.wso2.identity.integration.test.rest.api.server.user.sharing.management.v1.constant.UserSharingConstants.L3_ORG_1_NAME;
import static org.wso2.identity.integration.test.rest.api.server.user.sharing.management.v1.constant.UserSharingConstants.MAP_KEY_EXPECTED_ORG_COUNT;
import static org.wso2.identity.integration.test.rest.api.server.user.sharing.management.v1.constant.UserSharingConstants.MAP_KEY_EXPECTED_ORG_IDS;
import static org.wso2.identity.integration.test.rest.api.server.user.sharing.management.v1.constant.UserSharingConstants.MAP_KEY_EXPECTED_ORG_NAMES;
import static org.wso2.identity.integration.test.rest.api.server.user.sharing.management.v1.constant.UserSharingConstants.MAP_KEY_EXPECTED_ROLES_FOR_ORG;
import static org.wso2.identity.integration.test.rest.api.server.user.sharing.management.v1.constant.UserSharingConstants.MAP_KEY_EXPECTED_ROLE_IDS_FOR_APP;
import static org.wso2.identity.integration.test.rest.api.server.user.sharing.management.v1.constant.UserSharingConstants.MAP_KEY_EXPECTED_SHARED_USER_IDS;
import static org.wso2.identity.integration.test.rest.api.server.user.sharing.management.v1.constant.UserSharingConstants.MAP_KEY_EXPECTED_UNSHARED_ORG_IDS;
import static org.wso2.identity.integration.test.rest.api.server.user.sharing.management.v1.constant.UserSharingConstants.MAP_KEY_MAP_APPS;
import static org.wso2.identity.integration.test.rest.api.server.user.sharing.management.v1.constant.UserSharingConstants.MAP_KEY_MAP_ORGS;
import static org.wso2.identity.integration.test.rest.api.server.user.sharing.management.v1.constant.UserSharingConstants.MAP_KEY_MAP_ROLES;
import static org.wso2.identity.integration.test.rest.api.server.user.sharing.management.v1.constant.UserSharingConstants.MAP_KEY_SELECTIVE_ORG_ID;
import static org.wso2.identity.integration.test.rest.api.server.user.sharing.management.v1.constant.UserSharingConstants.MAP_KEY_SELECTIVE_ORG_NAME;
import static org.wso2.identity.integration.test.rest.api.server.user.sharing.management.v1.constant.UserSharingConstants.MAP_KEY_SELECTIVE_POLICY;
import static org.wso2.identity.integration.test.rest.api.server.user.sharing.management.v1.constant.UserSharingConstants.MAP_KEY_SELECTIVE_ROLES;
import static org.wso2.identity.integration.test.rest.api.server.user.sharing.management.v1.constant.UserSharingConstants.MAP_SHARED_ORGANIZATIONS_COUNT;
import static org.wso2.identity.integration.test.rest.api.server.user.sharing.management.v1.constant.UserSharingConstants.MAP_SHARED_ORGANIZATIONS_IDS;
import static org.wso2.identity.integration.test.rest.api.server.user.sharing.management.v1.constant.UserSharingConstants.MAP_SHARED_ROLES;
import static org.wso2.identity.integration.test.rest.api.server.user.sharing.management.v1.constant.UserSharingConstants.MAP_SHARED_USER_IDS;
import static org.wso2.identity.integration.test.rest.api.server.user.sharing.management.v1.constant.UserSharingConstants.MAP_UNSHARED_ORGANIZATIONS_IDS;
import static org.wso2.identity.integration.test.rest.api.server.user.sharing.management.v1.constant.UserSharingConstants.ORGANIZATION_AUDIENCE;
import static org.wso2.identity.integration.test.rest.api.server.user.sharing.management.v1.constant.UserSharingConstants.RESPONSE_DETAIL_VALUE_SHARING;
import static org.wso2.identity.integration.test.rest.api.server.user.sharing.management.v1.constant.UserSharingConstants.RESPONSE_DETAIL_VALUE_UNSHARING;
import static org.wso2.identity.integration.test.rest.api.server.user.sharing.management.v1.constant.UserSharingConstants.RESPONSE_STATUS;
import static org.wso2.identity.integration.test.rest.api.server.user.sharing.management.v1.constant.UserSharingConstants.RESPONSE_STATUS_VALUE;
import static org.wso2.identity.integration.test.rest.api.server.user.sharing.management.v1.constant.UserSharingConstants.ROOT_ORG_NAME;
import static org.wso2.identity.integration.test.rest.api.server.user.sharing.management.v1.constant.UserSharingConstants.ROOT_ORG_USER_1_USERNAME;
import static org.wso2.identity.integration.test.rest.api.server.user.sharing.management.v1.constant.UserSharingConstants.ROOT_ORG_USER_2_USERNAME;
import static org.wso2.identity.integration.test.rest.api.server.user.sharing.management.v1.constant.UserSharingConstants.ROOT_ORG_USER_3_USERNAME;
import static org.wso2.identity.integration.test.rest.api.server.user.sharing.management.v1.constant.UserSharingConstants.SHARE_PATH;
import static org.wso2.identity.integration.test.rest.api.server.user.sharing.management.v1.constant.UserSharingConstants.SHARE_WITH_ALL_PATH;
import static org.wso2.identity.integration.test.rest.api.server.user.sharing.management.v1.constant.UserSharingConstants.SELECTED_ORG_WITH_ALL_EXISTING_CHILDREN_ONLY;
import static org.wso2.identity.integration.test.rest.api.server.user.sharing.management.v1.constant.UserSharingConstants.SELECTED_ORG_WITH_IMMEDIATE_EXISTING_AND_FUTURE_CHILDREN;
import static org.wso2.identity.integration.test.rest.api.server.user.sharing.management.v1.constant.UserSharingConstants.SELECTED_ORG_WITH_IMMEDIATE_EXISTING_CHILDREN_ONLY;
import static org.wso2.identity.integration.test.rest.api.server.user.sharing.management.v1.constant.UserSharingConstants.UNSHARE_PATH;
import static org.wso2.identity.integration.test.rest.api.server.user.sharing.management.v1.constant.UserSharingConstants.UNSHARE_WITH_ALL_PATH;
import static org.wso2.identity.integration.test.rest.api.server.user.sharing.management.v1.constant.UserSharingConstants.USER_DOMAIN_PRIMARY;
import static org.wso2.identity.integration.test.rest.api.server.user.sharing.management.v1.constant.UserSharingConstants.USER_SHARING_API_BASE_PATH;
import static org.wso2.identity.integration.test.restclients.RestBaseClient.PATH_SEPARATOR;

/**
 * Covers user sharing flows while asserting asynchronous operation status propagation.
 */
public class UserSharingTestCase extends UserSharingBaseTest {

    private static final Duration ASYNC_TIMEOUT = Duration.ofMinutes(2);

    private AsyncOperationStatusMgtRestClient asyncStatusClient;
    private AutomationContext automationContext;

    @Factory(dataProvider = "restAPIUserConfigProvider")
    public UserSharingTestCase(TestUserMode userMode) throws Exception {

        super(userMode);
    }

    @BeforeClass(alwaysRun = true)
    public void init() throws Exception {

        automationContext = new AutomationContext(OAuth2Constant.ISSUER, TestUserMode.SUPER_TENANT_ADMIN);
        super.testInit(API_VERSION, swaggerDefinition, tenant);
        setupDetailMaps();
        setupRestClients();
        setupOrganizations();
        setupApplicationsAndRoles();
        setupUsers();

        asyncStatusClient = new AsyncOperationStatusMgtRestClient(serverURL, automationContext.getContextTenant());
    }

    @AfterClass(alwaysRun = true)
    public void tearDown() throws Exception {

        cleanUpUsers();
        cleanUpRoles(APPLICATION_AUDIENCE, ORGANIZATION_AUDIENCE);
        cleanUpApplications();
        cleanUpOrganizations();
        cleanUpDetailMaps();
        closeRestClients();
        if (asyncStatusClient != null) {
            asyncStatusClient.close();
        }
    }

    @DataProvider(name = "restAPIUserConfigProvider")
    public static Object[][] restAPIUserConfigProvider() {

        return new Object[][]{
                {TestUserMode.SUPER_TENANT_ADMIN},
                {TestUserMode.TENANT_ADMIN}
        };
    }

    @Test(dataProvider = "selectiveUserSharingDataProvider")
    public void testSelectiveUserSharing(List<String> userIds, Map<String, Map<String, Object>> organizations,
                                         Map<String, Object> expectedResults) throws Exception {

        UserShareRequestBody requestBody = new UserShareRequestBody()
                .userCriteria(getUserCriteriaForBaseUserSharing(userIds))
                .organizations(getOrganizationsForSelectiveUserSharing(organizations));

        Response response = getResponseOfPost(USER_SHARING_API_BASE_PATH + SHARE_PATH, toJSONString(requestBody));

        response.then()
                .log().ifValidationFails()
                .assertThat()
                .statusCode(HttpStatus.SC_ACCEPTED)
                .body(RESPONSE_STATUS, equalTo(RESPONSE_STATUS_VALUE))
                .body(UserSharingConstants.RESPONSE_DETAILS, equalTo(RESPONSE_DETAIL_VALUE_SHARING));

        verifyAsyncOperation(expectedResults, true);
        validateUserSharingResults(userIds, expectedResults);
    }

    @Test(dataProvider = "generalUserSharingDataProvider")
    public void testGeneralUserSharing(List<String> userIds, Map<String, Object> policyWithRoles,
                                       Map<String, Object> expectedResults) throws Exception {

        UserShareWithAllRequestBody requestBody = new UserShareWithAllRequestBody()
                .userCriteria(getUserCriteriaForBaseUserSharing(userIds))
                .policy(getPolicyEnumForGeneralUserSharing(policyWithRoles))
                .roles(getRolesForGeneralUserSharing(policyWithRoles));

        Response response =
                getResponseOfPost(USER_SHARING_API_BASE_PATH + SHARE_WITH_ALL_PATH, toJSONString(requestBody));

        response.then()
                .log().ifValidationFails()
                .assertThat()
                .statusCode(HttpStatus.SC_ACCEPTED)
                .body(RESPONSE_STATUS, equalTo(RESPONSE_STATUS_VALUE))
                .body(UserSharingConstants.RESPONSE_DETAILS, equalTo(RESPONSE_DETAIL_VALUE_SHARING));

        verifyAsyncOperation(expectedResults, true);
        validateUserSharingResults(userIds, expectedResults);
    }

    @Test(dataProvider = "generalUserUnsharingDataProvider")
    public void testGeneralUserUnsharing(List<String> userIds, Map<String, Object> policyWithRoles,
                                         Map<String, Object> expectedSharedResults,
                                         Map<String, Object> expectedResults) throws Exception {

        testGeneralUserSharing(userIds, policyWithRoles, expectedSharedResults);

        UserUnshareWithAllRequestBody requestBody = new UserUnshareWithAllRequestBody()
                .userCriteria(getUserCriteriaForBaseUserUnsharing(userIds));

        Response response =
                getResponseOfPost(USER_SHARING_API_BASE_PATH + UNSHARE_WITH_ALL_PATH, toJSONString(requestBody));

        response.then()
                .log().ifValidationFails()
                .assertThat()
                .statusCode(HttpStatus.SC_ACCEPTED)
                .body(RESPONSE_STATUS, equalTo(RESPONSE_STATUS_VALUE))
                .body(UserSharingConstants.RESPONSE_DETAILS, equalTo(RESPONSE_DETAIL_VALUE_UNSHARING));

        verifyAsyncOperation(expectedResults, false);
        validateUserSharingResults(userIds, expectedResults);
    }

    @Test(dataProvider = "selectiveUserUnsharingDataProvider")
    public void testSelectiveUserUnsharing(List<String> userIds, Map<String, Object> policyWithRoles,
                                           Map<String, Object> sharedResults, List<String> removingOrgIds,
                                           Map<String, Object> expectedResults) throws Exception {

        testGeneralUserSharing(userIds, policyWithRoles, sharedResults);

        UserUnshareRequestBody requestBody = new UserUnshareRequestBody()
                .userCriteria(getUserCriteriaForBaseUserUnsharing(userIds))
                .organizations(getOrganizationsForSelectiveUserUnsharing(removingOrgIds));

        Response response = getResponseOfPost(USER_SHARING_API_BASE_PATH + UNSHARE_PATH, toJSONString(requestBody));

        response.then()
                .log().ifValidationFails()
                .assertThat()
                .statusCode(HttpStatus.SC_ACCEPTED)
                .body(RESPONSE_STATUS, equalTo(RESPONSE_STATUS_VALUE))
                .body(UserSharingConstants.RESPONSE_DETAILS, equalTo(RESPONSE_DETAIL_VALUE_UNSHARING));

        verifyAsyncOperation(expectedResults, false);
        validateUserSharingResults(userIds, expectedResults);
    }

    private void verifyAsyncOperation(Map<String, Object> expectedResults, boolean sharing) throws Exception {

        String targetOrgId = sharing ? ROOT_ORG_NAME : ROOT_ORG_NAME;
        Operation operation = awaitCompletion();
        Assert.assertNotNull(operation, "Operation did not complete within timeout");

        OperationSummary summary = operation.getSummary();
        Assert.assertNotNull(summary, "Operation summary cannot be null");

        if (expectedResults.containsKey(MAP_SHARED_USER_IDS)) {
            List<String> sharedUserIds = (List<String>) expectedResults.get(MAP_SHARED_USER_IDS);
            Assert.assertEquals(summary.getSuccessCount(), Integer.valueOf(sharedUserIds.size()),
                    "Mismatch in successful shared user count");
        }

        if (expectedResults.containsKey(MAP_UNSHARED_ORGANIZATIONS_IDS)) {
            List<String> removedOrgIds = (List<String>) expectedResults.get(MAP_UNSHARED_ORGANIZATIONS_IDS);
            Assert.assertEquals(summary.getSuccessCount(), Integer.valueOf(removedOrgIds.size()),
                    "Mismatch in unshared organization count");
        }

        UnitOperations units = asyncStatusClient.getUnitOperations(operation.getCorrelationId());
        Assert.assertFalse(units.getUnitOperations().isEmpty(), "Async unit operations should not be empty");

        for (UnitOperation unit : units.getUnitOperations()) {
            Assert.assertEquals(unit.getStatus(), Operation.StatusEnum.SUCCESS,
                    "Unexpected unit operation status");
            Assert.assertTrue(StringUtils.isNotBlank(unit.getTargetOrgId()), "Missing target org id in async record");
        }
    }

    private Operation awaitCompletion() throws Exception {

        final String[] correlationIdHolder = new String[1];

        await().atMost(ASYNC_TIMEOUT)
                .pollInterval(Duration.ofSeconds(5))
                .until(() -> {
                    List<Operation> operations = asyncStatusClient.getOperations().getList();
                    if (operations == null || operations.isEmpty()) {
                        return false;
                    }
                    Operation latestOperation = operations.get(operations.size() - 1);
                    if (latestOperation.getStatus() == Operation.StatusEnum.SUCCESS ||
                            latestOperation.getStatus() == Operation.StatusEnum.FAILED ||
                            latestOperation.getStatus() == Operation.StatusEnum.PARTIALLY_COMPLETED) {
                        correlationIdHolder[0] = latestOperation.getCorrelationId();
                        return true;
                    }
                    return false;
                });

        return asyncStatusClient.getOperation(correlationIdHolder[0]);
    }

    private Map<String, Map<String, Object>> setOrganizationsForSelectiveUserSharingTestCase1() {

        Map<String, Map<String, Object>> organizations = new HashMap<>();

        Map<String, Object> org1 = new HashMap<>();
        org1.put(MAP_KEY_SELECTIVE_ORG_ID, getOrgId(L1_ORG_1_NAME));
        org1.put(MAP_KEY_SELECTIVE_ORG_NAME, L1_ORG_1_NAME);
        org1.put(MAP_KEY_SELECTIVE_POLICY, SELECTED_ORG_WITH_ALL_EXISTING_CHILDREN_ONLY);
        org1.put(MAP_KEY_SELECTIVE_ROLES,
                Collections.singletonList(createRoleWithAudience(APP_ROLE_1, APP_1_NAME, APPLICATION_AUDIENCE)));
        organizations.put(L1_ORG_1_NAME, org1);

        Map<String, Object> org2 = new HashMap<>();
        org2.put(MAP_KEY_SELECTIVE_ORG_ID, getOrgId(L2_ORG_1_NAME));
        org2.put(MAP_KEY_SELECTIVE_ORG_NAME, L2_ORG_1_NAME);
        org2.put(MAP_KEY_SELECTIVE_POLICY, SELECTED_ORG_WITH_IMMEDIATE_EXISTING_CHILDREN_ONLY);
        org2.put(MAP_KEY_SELECTIVE_ROLES,
                Collections.singletonList(createRoleWithAudience(APP_ROLE_2, APP_2_NAME, APPLICATION_AUDIENCE)));
        organizations.put(L2_ORG_1_NAME, org2);

        Map<String, Object> org3 = new HashMap<>();
        org3.put(MAP_KEY_SELECTIVE_ORG_ID, getOrgId(L3_ORG_1_NAME));
        org3.put(MAP_KEY_SELECTIVE_ORG_NAME, L3_ORG_1_NAME);
        org3.put(MAP_KEY_SELECTIVE_POLICY, SELECTED_ORG_WITH_IMMEDIATE_EXISTING_AND_FUTURE_CHILDREN);
        org3.put(MAP_KEY_SELECTIVE_ROLES,
                Arrays.asList(createRoleWithAudience(APP_ROLE_1, APP_1_NAME, APPLICATION_AUDIENCE),
                        createRoleWithAudience(APP_ROLE_3, APP_2_NAME, APPLICATION_AUDIENCE)));
        organizations.put(L3_ORG_1_NAME, org3);

        return organizations;
    }

    private Map<String, Object> setExpectedResultsForSelectiveUserSharingTestCase1() {

        Map<String, Object> expectedResults = new HashMap<>();
        expectedResults.put(MAP_SHARED_USER_IDS,
                Collections.singletonList(getUserId(ROOT_ORG_USER_1_USERNAME, USER_DOMAIN_PRIMARY, ROOT_ORG_NAME)));
        expectedResults.put(MAP_SHARED_ORGANIZATIONS_COUNT, 3);
        expectedResults.put(MAP_SHARED_ORGANIZATIONS_IDS,
                Arrays.asList(getOrgId(L1_ORG_1_NAME), getOrgId(L2_ORG_1_NAME), getOrgId(L3_ORG_1_NAME)));

        Map<String, List<RoleWithAudience>> rolesByOrg = new HashMap<>();
        rolesByOrg.put(getOrgId(L1_ORG_1_NAME),
                Collections.singletonList(createRoleWithAudience(APP_ROLE_1, APP_1_NAME, APPLICATION_AUDIENCE)));
        rolesByOrg.put(getOrgId(L2_ORG_1_NAME),
                Collections.singletonList(createRoleWithAudience(APP_ROLE_2, APP_2_NAME, APPLICATION_AUDIENCE)));
        rolesByOrg.put(getOrgId(L3_ORG_1_NAME), Arrays.asList(
                createRoleWithAudience(APP_ROLE_1, APP_1_NAME, APPLICATION_AUDIENCE),
                createRoleWithAudience(APP_ROLE_3, APP_2_NAME, APPLICATION_AUDIENCE)));
        expectedResults.put(MAP_SHARED_ROLES, rolesByOrg);

        return expectedResults;
    }

    private Map<String, Object> setExpectedResultsForSelectiveUserSharingTestCase2() {

        Map<String, Object> expectedResults = new HashMap<>();
        expectedResults.put(MAP_SHARED_USER_IDS, Arrays.asList(
                getUserId(ROOT_ORG_USER_1_USERNAME, USER_DOMAIN_PRIMARY, ROOT_ORG_NAME),
                getUserId(ROOT_ORG_USER_2_USERNAME, USER_DOMAIN_PRIMARY, ROOT_ORG_NAME),
                getUserId(ROOT_ORG_USER_3_USERNAME, USER_DOMAIN_PRIMARY, ROOT_ORG_NAME)));
        expectedResults.put(MAP_SHARED_ORGANIZATIONS_COUNT, 4);
        expectedResults.put(MAP_SHARED_ORGANIZATIONS_IDS, Arrays.asList(getOrgId(L1_ORG_1_NAME), getOrgId(L1_ORG_2_NAME),
                getOrgId(L1_ORG_3_NAME), getOrgId(L2_ORG_2_NAME)));

        Map<String, List<RoleWithAudience>> rolesByOrg = new HashMap<>();
        rolesByOrg.put(getOrgId(L1_ORG_1_NAME),
                Arrays.asList(createRoleWithAudience(APP_ROLE_1, APP_1_NAME, APPLICATION_AUDIENCE),
                        createRoleWithAudience(APP_ROLE_3, APP_2_NAME, APPLICATION_AUDIENCE)));
        rolesByOrg.put(getOrgId(L1_ORG_2_NAME), Collections.singletonList(
                createRoleWithAudience(APP_ROLE_1, APP_1_NAME, APPLICATION_AUDIENCE)));
        rolesByOrg.put(getOrgId(L1_ORG_3_NAME), Collections.singletonList(
                createRoleWithAudience(APP_ROLE_2, APP_2_NAME, APPLICATION_AUDIENCE)));
        rolesByOrg.put(getOrgId(L2_ORG_2_NAME), Arrays.asList(
                createRoleWithAudience(APP_ROLE_1, APP_1_NAME, APPLICATION_AUDIENCE),
                createRoleWithAudience(APP_ROLE_2, APP_2_NAME, APPLICATION_AUDIENCE)));
        expectedResults.put(MAP_SHARED_ROLES, rolesByOrg);

        return expectedResults;
    }

    private Map<String, Map<String, Object>> setOrganizationsForSelectiveUserSharingTestCase2() {

        Map<String, Map<String, Object>> organizations = new HashMap<>();

        Map<String, Object> org1 = new HashMap<>();
        org1.put(MAP_KEY_SELECTIVE_ORG_ID, getOrgId(L1_ORG_1_NAME));
        org1.put(MAP_KEY_SELECTIVE_ORG_NAME, L1_ORG_1_NAME);
        org1.put(MAP_KEY_SELECTIVE_POLICY, SELECTED_ORG_WITH_ALL_EXISTING_CHILDREN_ONLY);
        org1.put(MAP_KEY_SELECTIVE_ROLES, Arrays.asList(
                createRoleWithAudience(APP_ROLE_1, APP_1_NAME, APPLICATION_AUDIENCE),
                createRoleWithAudience(APP_ROLE_3, APP_2_NAME, APPLICATION_AUDIENCE)));
        organizations.put(L1_ORG_1_NAME, org1);

        Map<String, Object> org2 = new HashMap<>();
        org2.put(MAP_KEY_SELECTIVE_ORG_ID, getOrgId(L1_ORG_2_NAME));
        org2.put(MAP_KEY_SELECTIVE_ORG_NAME, L1_ORG_2_NAME);
        org2.put(MAP_KEY_SELECTIVE_POLICY, SELECTED_ORG_WITH_IMMEDIATE_EXISTING_CHILDREN_ONLY);
        org2.put(MAP_KEY_SELECTIVE_ROLES,
                Collections.singletonList(createRoleWithAudience(APP_ROLE_1, APP_1_NAME, APPLICATION_AUDIENCE)));
        organizations.put(L1_ORG_2_NAME, org2);

        Map<String, Object> org3 = new HashMap<>();
        org3.put(MAP_KEY_SELECTIVE_ORG_ID, getOrgId(L1_ORG_3_NAME));
        org3.put(MAP_KEY_SELECTIVE_ORG_NAME, L1_ORG_3_NAME);
        org3.put(MAP_KEY_SELECTIVE_POLICY, SELECTED_ORG_WITH_IMMEDIATE_EXISTING_AND_FUTURE_CHILDREN);
        org3.put(MAP_KEY_SELECTIVE_ROLES,
                Collections.singletonList(createRoleWithAudience(APP_ROLE_2, APP_2_NAME, APPLICATION_AUDIENCE)));
        organizations.put(L1_ORG_3_NAME, org3);

        Map<String, Object> org4 = new HashMap<>();
        org4.put(MAP_KEY_SELECTIVE_ORG_ID, getOrgId(L2_ORG_2_NAME));
        org4.put(MAP_KEY_SELECTIVE_ORG_NAME, L2_ORG_2_NAME);
        org4.put(MAP_KEY_SELECTIVE_POLICY, SELECTED_ORG_WITH_IMMEDIATE_EXISTING_CHILDREN_ONLY);
        org4.put(MAP_KEY_SELECTIVE_ROLES, Arrays.asList(
                createRoleWithAudience(APP_ROLE_1, APP_1_NAME, APPLICATION_AUDIENCE),
                createRoleWithAudience(APP_ROLE_2, APP_2_NAME, APPLICATION_AUDIENCE)));
        organizations.put(L2_ORG_2_NAME, org4);

        return organizations;
    }

