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

import io.apiman.common.util.AbstractMessages;
import io.apiman.manager.api.beans.idm.PermissionType;
import io.apiman.manager.api.beans.idm.RoleBean;
import io.apiman.manager.api.beans.policies.PolicyDefinitionBean;
import io.apiman.manager.api.beans.summary.PolicyDefinitionSummaryBean;
import io.apiman.manager.api.core.IIdmStorage;
import io.apiman.manager.api.core.IStorage;
import io.apiman.manager.api.core.IStorageQuery;
import io.apiman.manager.api.core.exceptions.StorageException;
import io.apiman.manager.api.core.logging.ApimanLogger;
import io.apiman.manager.api.core.logging.IApimanLogger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;


/**
 * The Boostrap class loads up default roles and policies
 *
 */
@SuppressWarnings("nls")
@ApplicationScoped
public class BootstrapFilter implements Filter {

	@Inject
	IStorage storage;
	
	@Inject
	IStorageQuery storageQuery;
	
	@Inject
	IIdmStorage idmStorage;
	
	@Inject @ApimanLogger(BootstrapFilter.class)
	IApimanLogger logger;
	
	public void loadDefaultPolicies() {
		
		try {
			//1. Find the policies
			logger.info("Looking up /data/all-policyDefs.json on the classpath...");
			InputStream is = getClass().getResourceAsStream("/data/all-policyDefs.json");
			ObjectMapper mapper = new ObjectMapper();
			TypeReference<List<PolicyDefinitionBean>> tRef = new TypeReference<List<PolicyDefinitionBean>>() {};
			List<PolicyDefinitionBean> policyDefList = mapper.readValue(is, tRef);
			logger.info("Found " + policyDefList.size() + " policyDefs");
			//2. Look up the already installed policies
			Map<String,PolicyDefinitionSummaryBean> existingPolicyDefinitions = new HashMap<String,PolicyDefinitionSummaryBean>(); 
			List<PolicyDefinitionSummaryBean> policyDefinitions = storageQuery.listPolicyDefinitions();
			logger.info("Found " + policyDefinitions.size() + " existing policies");
			for (PolicyDefinitionSummaryBean policyDefinitionSummaryBean: policyDefinitions) {
				existingPolicyDefinitions.put(policyDefinitionSummaryBean.getName(),policyDefinitionSummaryBean);
			}
			//3. Store the policies if they are not already installed
			for (PolicyDefinitionBean policyDefinitionBean : policyDefList) {
				String policyName = policyDefinitionBean.getName();
				if (policyDefinitionBean.getId() == null || policyDefinitionBean.getId().equals("")) policyDefinitionBean.setId(policyDefinitionBean.getName().replaceAll(" ", ""));
				if (! existingPolicyDefinitions.containsKey(policyName)) {
					storage.beginTx();
					logger.info("Creating Policy " + policyDefinitionBean.getName());
					storage.createPolicyDefinition(policyDefinitionBean);
					storage.commitTx();
				} else {
					//update if the policyImpl changed
					if (existingPolicyDefinitions.get(policyName).getPolicyImpl().length() != policyDefinitionBean.getPolicyImpl().length()) {
						logger.info("Updating Policy " + policyDefinitionBean.getName());
						storage.beginTx();
						storage.updatePolicyDefinition(policyDefinitionBean);
						storage.commitTx();
					}
				}
			}
		} catch (StorageException | IOException e) {
			logger.error(e);
		}
	}
	
	public void loadDefaultRoles() {
		try {
			Date now = new Date();
			//Organization Owner
			String name = "Organization Owner";
			if (idmStorage.getRole(name) == null) {
				logger.info("Creating Organization Owner Role");
				RoleBean roleBean = new RoleBean();
				roleBean.setAutoGrant(true);
				roleBean.setCreatedBy("admin");
				roleBean.setCreatedOn(now);
				roleBean.setId(name);
				roleBean.setName(name);
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
			name = "Application Developer";
			if (idmStorage.getRole(name) == null) {
				logger.info("Creating Application Developer Role");
				RoleBean roleBean = new RoleBean();
				roleBean.setCreatedBy("admin");
				roleBean.setCreatedOn(now);
				roleBean.setId(name);
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
			name = "Service Developer";
			if (idmStorage.getRole(name) == null) {
				logger.info("Creating Service Developer Role");
				RoleBean roleBean = new RoleBean();
				roleBean.setCreatedBy("admin");
				roleBean.setCreatedOn(now);
				roleBean.setId(name);
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
			logger.error(e);
		}
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		loadDefaultPolicies();
		loadDefaultRoles();
	}

	/**
	 * No-opt filter, we really only care about the init phase to bootstrap apiman.
	 */
	@Override
	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {
		//no-opt, we only cared about bootstrapping on startup
        try {
            chain.doFilter(request, response);
        } finally {
            AbstractMessages.clearLocale();
        }
	}

	@Override
	public void destroy() {}

}
