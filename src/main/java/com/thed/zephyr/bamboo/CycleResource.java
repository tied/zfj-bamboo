package com.thed.zephyr.bamboo;

/**
 * @author mohan.kumar
 */


import static com.thed.zephyr.bamboo.utils.ZeeConstants.NEW_CYCLE;
import static com.thed.zephyr.bamboo.utils.ZeeConstants.NEW_CYCLE_KEY_IDENTIFIER;
import static com.thed.zephyr.bamboo.utils.ZeeConstants.PLUGIN_KEY;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.log4j.Logger;

import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.atlassian.sal.api.transaction.TransactionTemplate;
import com.atlassian.sal.api.user.UserManager;
import com.thed.zephyr.bamboo.utils.rest.Cycle;
import com.thed.zephyr.bamboo.utils.rest.RestClient;

@Path("/cycle")
public class CycleResource {
	private final UserManager userManager;
	private final TransactionTemplate transactionTemplate;
	private PluginSettingsFactory pluginSettingsFactory;
	private static final Logger log = Logger.getLogger(CycleResource.class);
	
	public CycleResource(UserManager userManager,
			PluginSettingsFactory pluginSettingsFactory,
			TransactionTemplate transactionTemplate) {
		this.userManager = userManager;
		this.pluginSettingsFactory = pluginSettingsFactory;
		this.transactionTemplate = transactionTemplate;
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response get(@Context final HttpServletRequest request) {

		return Response.ok(
				transactionTemplate.execute(new TransactionCallback() {
					public Object doInTransaction() {
						
						String serverAddress = request.getParameter("serverAddress");
						String projectKey = request.getParameter("projectKey");
						String releaseKey = request.getParameter("releaseKey");
						
						long projectId = 0;
						long releaseId = 0;
						try {
							 projectId = Long.parseLong(projectKey);
							 releaseId = Long.parseLong(releaseKey);
						} catch (NumberFormatException e) {
							log.debug("Error parsing release Id");
						}

						RestClient restClient = null;
						Map<Long, String> cycleMap = new HashMap<Long, String>();
						cycleMap.put(NEW_CYCLE_KEY_IDENTIFIER, NEW_CYCLE);

						try {
					    	restClient = getRestclient(serverAddress);
					    	cycleMap.putAll(Cycle.getAllCyclesByVersionId(releaseId, restClient, projectKey));
						} finally {
							ConfigResource.closeHTTPClient(restClient);
						}
						
						return cycleMap;
					}
				})).build();
	}

	private RestClient getRestclient(String serverAddr) {

		RestClient restClient = null;
		
		PluginSettings settings = pluginSettingsFactory
				.createGlobalSettings();
		
		@SuppressWarnings("unchecked")
		List<Map<String, String>> credList=  (List<Map<String, String>>) settings.get(PLUGIN_KEY + ".zephyrserverconfig");
		
		if(credList != null && credList.size() > 0) {
			for (Iterator<Map<String, String>> iterator = credList.iterator(); iterator.hasNext();) {
				Map<String, String> map = iterator.next();
				String serverAddrFromStore = map.get("serverAddr");
				
				if (serverAddrFromStore.equals(serverAddr)) {
					String userName = map.get("user");
					String password = map.get("password");

					restClient = new RestClient(serverAddr, userName, password);
					
				}
			}
		}


		return restClient;
	}

}