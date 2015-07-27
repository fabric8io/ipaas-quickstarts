/*
 * Copyright 2015 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.apiman;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.apiman.common.auth.AuthPrincipal;
import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.openshift.api.model.OAuthClientAuthorizationList;
import io.fabric8.utils.Systems;
import io.fabric8.utils.ssl.TrustEverythingSSLTrustManager;

import javax.net.ssl.HttpsURLConnection;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.Principal;

/**
 * A simple implementation of an bearer token filter that checks the validity of
 * an incoming bearer token with the OpenShift issuer. The OpenShift call
 * returns a JSON from which the UserPrincipal can be set.
 * 
 * A great way to test is to use
 * 
 * curl https://172.30.0.2:443/oapi/v1/users/ -k -H "Authorization: Bearer NVy_uBscz-Efm4s4h3zYfTvDUh_BvGP1d8S-g-oGuJY"
 * 
 * where the IP address needs to be the one of your KUBERNETES_SERVICE_HOST, and
 * off course you need to set a valid Bearer token.
 * 
 * TODO:
 * 1. switch the kubernetesTrustCert default to false
 * 2. Set the roles on the principle?
 * 3. Enable BasicAuth, as the next filter?
 * 4. Cache the result of validation test for a certain amount of time?
 */
public class BearerTokenFilter implements Filter {

	public static final String KUBERNETES_OSAPI_URL = "/oapi/"
			+ KubernetesHelper.defaultOsApiVersion;
	public static final String KUBERNETES_SERVICE_HOST = "KUBERNETES_SERVICE_HOST";
	public static final String KUBERNETES_SERVICE_PORT = "KUBERNETES_SERVICE_PORT";
	URL kubernetesOsapiUrl = null;
	boolean kubernetesTrustCert = false;

	/**
	 * Constructor.
	 */
	public BearerTokenFilter() {
	}

	/**
	 * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
	 */
	@Override
	public void init(FilterConfig config) throws ServletException {
		String kubernetesHostAndPort = Systems.getServiceHostAndPort(
				"KUBERNETES", "172.28.128.4", "8443");
		kubernetesTrustCert = Boolean.parseBoolean(Systems.getEnvVarOrSystemProperty("KUBERNETES_TRUST_CERT", "true"));
		try {
			kubernetesOsapiUrl = new URL("https://" + kubernetesHostAndPort
					+ KUBERNETES_OSAPI_URL + "/users/");
		} catch (MalformedURLException e) {
			throw new ServletException(e);
		}
	}

	/**
	 * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest,
	 *      javax.servlet.ServletResponse, javax.servlet.FilterChain)
	 */
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
	ServletException {
		HttpServletRequest req = (HttpServletRequest) request;
		String authHeader = req.getHeader("Authorization"); //$NON-NLS-1$
		if (authHeader != null && authHeader.toUpperCase().startsWith("BEARER")) {
			//validate with issuer
			try {
				String username = validateBearerToken(authHeader);
				AuthPrincipal principal = new AuthPrincipal(username);
				// roles should come from keycloak, but for now we hard code.
				principal.addRole("apiuser");
				if ("admin".equals(username)) {
					principal.addRole("apiadmin");
				}
				request = wrapTheRequest(request, principal);
				chain.doFilter(request, response);
			} catch (IOException e) {
				String errMsg = e.getMessage();
				if (e.getMessage().contains("Server returned HTTP response code")) {
					errMsg = "Invalid BearerToken";
				} else {
					errMsg = "Cannot validate BearerToken";
					e.printStackTrace();
				}
				sendInvalidTokenResponse((HttpServletResponse)response, errMsg);
			}
		} else {
			//I think it makes sense to have the next filter be the AuthenticationFilter
			//to also allow basic Auth, but that filter does not support this filter
			//allowing access. So for now let's not support it.
			
			//no bearer token present - go to the next filter
			//chain.doFilter(request, response);
			sendInvalidTokenResponse((HttpServletResponse)response, "No BearerToken");
		}
	}
	
	/**
	 * Validates the bearer token with the kubernetes oapi and returns the username
	 * if it is a valid token.
	 * 
	 * @param bearerHeader
	 * @return username of the user to whom the token was issued to
	 * @throws IOException - when the token is invalid, or oapi cannot be reached.
	 */
	protected String validateBearerToken(String bearerHeader) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		HttpsURLConnection con = (HttpsURLConnection) kubernetesOsapiUrl.openConnection();
		con.setRequestProperty("Authorization", bearerHeader);
		con.setConnectTimeout(10000);  
		con.setReadTimeout(10000);
		con.setRequestProperty("Content-Type","application/json");
		if (kubernetesTrustCert) TrustEverythingSSLTrustManager.trustAllSSLCertificates(con);
		con.connect();
		OAuthClientAuthorizationList userList = mapper.readValue(con.getInputStream(), OAuthClientAuthorizationList.class); 
		String userName = userList.getItems().get(0).getMetadata().getName();
		return userName;
	}

	/**
	 * Wrap the request to provide the principal.
	 * 
	 * @param request
	 *            the request
	 * @param principal
	 *            the principal
	 */
	private HttpServletRequest wrapTheRequest(final ServletRequest request,
			final AuthPrincipal principal) {
		HttpServletRequestWrapper wrapper = new HttpServletRequestWrapper(
				(HttpServletRequest) request) {
			@Override
			public Principal getUserPrincipal() {
				return principal;
			}

			@Override
			public boolean isUserInRole(String role) {
				return principal.getRoles().contains(role);
			}

			@Override
			public String getRemoteUser() {
				return principal.getName();
			}
		};
		return wrapper;
	}

	/**
	 * Sends a response that tells the client that authentication is required.
	 * 
	 * @param response
	 *            the response
	 * @throws IOException
	 *             when an error cannot be sent
	 */
	private void sendInvalidTokenResponse(HttpServletResponse response, String errMsg)
			throws IOException {
		
		response.sendError(HttpServletResponse.SC_UNAUTHORIZED, errMsg);
	}

	/**
	 * @see javax.servlet.Filter#destroy()
	 */
	@Override
	public void destroy() {
	}

}
