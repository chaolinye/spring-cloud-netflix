/*
 * Copyright 2013-2016 the original author or authors.
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

package org.springframework.cloud.netflix.hystrix.security;

import java.util.Base64;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.cloud.netflix.hystrix.security.app.CustomConcurrenyStrategy;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

import com.netflix.hystrix.strategy.HystrixPlugins;
import com.netflix.hystrix.strategy.concurrency.HystrixConcurrencyStrategy;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests that a secured web service returning values using a feign client properly access
 * the security context from a hystrix command.
 * @author Daniel Lavoie
 */
@RunWith(SpringRunner.class)
@DirtiesContext
@SpringBootTest(classes = HystrixSecurityApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT,
		properties = { "username.ribbon.listOfServers=localhost:${local.server.port}",
				"feign.hystrix.enabled=true"})
@ActiveProfiles("proxysecurity")
public class HystrixSecurityTests {
	@Autowired
	private CustomConcurrenyStrategy customConcurrenyStrategy;

	@LocalServerPort
	private String serverPort;

	//TODOO: move to constants in TestAutoConfiguration
	private String username = "user";

	private String password = "password";

	@Test
	public void testSecurityConcurrencyStrategyInstalled() {
		HystrixConcurrencyStrategy concurrencyStrategy = HystrixPlugins.getInstance().getConcurrencyStrategy();
		assertThat(concurrencyStrategy).isInstanceOf(SecurityContextConcurrencyStrategy.class);
	}

	@Test
	public void testFeignHystrixSecurity() {
		HttpHeaders headers = HystrixSecurityTests.createBasicAuthHeader(username,
				password);

		String usernameResult = new RestTemplate()
				.exchange("http://localhost:" + serverPort + "/proxy-username",
						HttpMethod.GET, new HttpEntity<Void>(headers), String.class)
				.getBody();

		Assert.assertTrue("Username should have been intercepted by feign interceptor.",
				username.equals(usernameResult));

		Assert.assertTrue("Custom hook should have been called.",
				customConcurrenyStrategy.isHookCalled());
	}

	public static HttpHeaders createBasicAuthHeader(final String username,
			final String password) {
		return new HttpHeaders() {
			private static final long serialVersionUID = 1766341693637204893L;

			{
				String auth = username + ":" + password;
				byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes());
				String authHeader = "Basic " + new String(encodedAuth);
				this.set("Authorization", authHeader);
			}
		};
	}
}
