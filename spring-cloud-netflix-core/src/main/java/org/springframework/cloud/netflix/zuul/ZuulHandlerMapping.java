package org.springframework.cloud.netflix.zuul;

import java.util.Collection;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.boot.actuate.endpoint.mvc.MvcEndpoint;
import org.springframework.cloud.client.discovery.InstanceRegisteredEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.handler.AbstractUrlHandlerMapping;

/**
 * MVC HandlerMapping that maps incoming request paths to remote services.
 * 
 * @author Spencer Gibb
 * @author Dave Syer
 */
@ManagedResource(description = "Can be used to list and reset the reverse proxy routes")
public class ZuulHandlerMapping extends AbstractUrlHandlerMapping implements
		ApplicationListener<InstanceRegisteredEvent>, MvcEndpoint {

	private ProxyRouteLocator routeLocator;

	private ZuulController zuul;

	@Autowired
	public ZuulHandlerMapping(ProxyRouteLocator routeLocator, ZuulController zuul) {
		this.routeLocator = routeLocator;
		this.zuul = zuul;
		setOrder(-200);
	}

	@Override
	public void onApplicationEvent(InstanceRegisteredEvent event) {
		registerHandlers(routeLocator.getRoutePaths());
	}

	protected void registerHandlers(Collection<String> routes) {
		if (routes.isEmpty()) {
			logger.warn("Neither 'urlMap' nor 'mappings' set on SimpleUrlHandlerMapping");
		}
		else {
			for (String url : routes) {
				registerHandler(url, zuul);
			}
		}
	}

	@RequestMapping(method = RequestMethod.POST)
	@ResponseBody
	@ManagedOperation
	public Map<String, String> reset() {
		routeLocator.resetRoutes();
		registerHandlers(routeLocator.getRoutePaths());
		return getRoutes();
	}

	@RequestMapping(method = RequestMethod.GET)
	@ResponseBody
	@ManagedAttribute
	public Map<String, String> getRoutes() {
		return routeLocator.getRoutes();
	}

	@Override
	public String getPath() {
		return "/routes";
	}

	@Override
	public boolean isSensitive() {
		return true;
	}

	@Override
	public Class<? extends Endpoint<?>> getEndpointType() {
		return null;
	}

}
