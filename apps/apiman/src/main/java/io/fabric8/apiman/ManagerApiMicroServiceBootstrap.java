/**
 *  Copyright 2005-2015 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.apiman;

import io.apiman.manager.api.beans.idm.PermissionType;
import io.apiman.manager.api.beans.idm.RoleBean;
import io.apiman.manager.api.beans.policies.PolicyDefinitionBean;
import io.apiman.manager.api.beans.search.SearchCriteriaBean;
import io.apiman.manager.api.beans.search.SearchCriteriaFilterOperator;
import io.apiman.manager.api.core.IIdmStorage;
import io.apiman.manager.api.core.IStorage;
import io.apiman.manager.api.core.exceptions.StorageException;
import io.apiman.manager.api.core.logging.IApimanLogger;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;


/**
 * The Boostrap class loads up default roles and policies
 *
 */
@SuppressWarnings("nls")
@ApplicationScoped
public class ManagerApiMicroServiceBootstrap {

	@Inject
	IStorage storage;
	
	@Inject
	IIdmStorage idmStorage;
	
	@Inject
	IApimanLogger logger;
	
	public void loadDefaultPolicies() {
		
		try {
			//1. Find the policies
			InputStream is = getClass().getResourceAsStream("/data/all-policyDefs.json");
			ObjectMapper mapper = new ObjectMapper();
			TypeReference<List<PolicyDefinitionBean>> tRef = new TypeReference<List<PolicyDefinitionBean>>() {};
			List<PolicyDefinitionBean> policyDefList = mapper.readValue(is, tRef);
			//2. Store the policies if they are not already installed
			for (PolicyDefinitionBean policyDefinitionBean : policyDefList) {
				logger.info("Loading up APIMan policies");
				storage.beginTx();
				if (storage.getPolicyDefinition(policyDefinitionBean.getId()) == null) {
					storage.createPolicyDefinition(policyDefinitionBean);
					storage.commitTx();
				} else {
					storage.rollbackTx();
				}
			}
		} catch (StorageException | IOException e) {
			logger.error(e);
		}
	}
	
	public void loadDefaultRoles() {
		try {
			//Organization Owner
			SearchCriteriaBean searchRoles = new SearchCriteriaBean();
			searchRoles.addFilter("name", "ServiceDeveloper", SearchCriteriaFilterOperator.eq);
			if (idmStorage.findRoles(searchRoles).getTotalSize() == 0) {
				logger.info("Creating Organization Owner Role");
				RoleBean roleBean = new RoleBean();
				roleBean.setAutoGrant(true);
				roleBean.setName("Organization Owner");
				roleBean.setDescription("Automatically granted to the user who creates an Organization.  Grants all privileges.");
				Set<PermissionType> permissions = new HashSet<PermissionType>();
				permissions.add(PermissionType.orgAdmin);
				permissions.add(PermissionType.orgEdit);
				permissions.add(PermissionType.orgView);
				permissions.add(PermissionType.appAdmin);
				permissions.add(PermissionType.appEdit);
				permissions.add(PermissionType.appView);
				permissions.add(PermissionType.planAdmin);
				permissions.add(PermissionType.planEdit);
				permissions.add(PermissionType.planView);
				permissions.add(PermissionType.svcAdmin);
				permissions.add(PermissionType.svcEdit);
				permissions.add(PermissionType.svcView);
				roleBean.setPermissions(permissions);
				idmStorage.createRole(roleBean);
			}
			
			//Application Developer
			searchRoles = new SearchCriteriaBean();
			searchRoles.addFilter("name", "Application Developer", SearchCriteriaFilterOperator.eq);
			if (idmStorage.findRoles(searchRoles).getTotalSize() == 0) {
				logger.info("Creating Application Developer Role");
				RoleBean roleBean = new RoleBean();
				roleBean.setName("Application Developer");
				roleBean.setDescription("Users responsible for creating and managing applications should be granted this role within an Organization.");
				Set<PermissionType> permissions = new HashSet<PermissionType>();
				permissions.add(PermissionType.appAdmin);
				permissions.add(PermissionType.appEdit);
				permissions.add(PermissionType.appView);
				roleBean.setPermissions(permissions);
				idmStorage.createRole(roleBean);
			}
			
			//Service Developer
			searchRoles = new SearchCriteriaBean();
			searchRoles.addFilter("name", "Service Developer", SearchCriteriaFilterOperator.eq);
			if (idmStorage.findRoles(searchRoles).getTotalSize() == 0) {
				logger.info("Creating Service Developer Role");
				RoleBean roleBean = new RoleBean();
				roleBean.setAutoGrant(true);
				roleBean.setName("Service Developer");
				roleBean.setDescription("Users responsible for creating and managing services should be granted this role within an Organization.");
				Set<PermissionType> permissions = new HashSet<PermissionType>();
				permissions.add(PermissionType.planAdmin);
				permissions.add(PermissionType.planEdit);
				permissions.add(PermissionType.planView);
				permissions.add(PermissionType.svcAdmin);
				permissions.add(PermissionType.svcEdit);
				permissions.add(PermissionType.svcView);
				roleBean.setPermissions(permissions);
				idmStorage.createRole(roleBean);
			}
			
		} catch (StorageException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
