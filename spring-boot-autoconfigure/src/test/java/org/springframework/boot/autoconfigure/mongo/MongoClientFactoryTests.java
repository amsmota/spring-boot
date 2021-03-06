/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.autoconfigure.mongo;

import java.net.UnknownHostException;
import java.util.List;

import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.connection.Cluster;
import com.mongodb.connection.ClusterSettings;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MongoClientFactory}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Mark Paluch
 */
public class MongoClientFactoryTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void portCanBeCustomized() throws UnknownHostException {
		MongoProperties properties = new MongoProperties();
		properties.setPort(12345);
		MongoClient client = createMongoClient(properties);
		List<ServerAddress> allAddresses = extractServerAddresses(client);
		assertThat(allAddresses).hasSize(1);
		assertServerAddress(allAddresses.get(0), "localhost", 12345);
	}

	@Test
	public void hostCanBeCustomized() throws UnknownHostException {
		MongoProperties properties = new MongoProperties();
		properties.setHost("mongo.example.com");
		MongoClient client = createMongoClient(properties);
		List<ServerAddress> allAddresses = extractServerAddresses(client);
		assertThat(allAddresses).hasSize(1);
		assertServerAddress(allAddresses.get(0), "mongo.example.com", 27017);
	}

	@Test
	public void credentialsCanBeCustomized() throws UnknownHostException {
		MongoProperties properties = new MongoProperties();
		properties.setUsername("user");
		properties.setPassword("secret".toCharArray());
		MongoClient client = createMongoClient(properties);
		assertMongoCredential(client.getCredentialsList().get(0), "user", "secret",
				"test");
	}

	@Test
	public void databaseCanBeCustomized() throws UnknownHostException {
		MongoProperties properties = new MongoProperties();
		properties.setDatabase("foo");
		properties.setUsername("user");
		properties.setPassword("secret".toCharArray());
		MongoClient client = createMongoClient(properties);
		assertMongoCredential(client.getCredentialsList().get(0), "user", "secret",
				"foo");
	}

	@Test
	public void authenticationDatabaseCanBeCustomized() throws UnknownHostException {
		MongoProperties properties = new MongoProperties();
		properties.setAuthenticationDatabase("foo");
		properties.setUsername("user");
		properties.setPassword("secret".toCharArray());
		MongoClient client = createMongoClient(properties);
		assertMongoCredential(client.getCredentialsList().get(0), "user", "secret",
				"foo");
	}

	@Test
	public void uriCanBeCustomized() throws UnknownHostException {
		MongoProperties properties = new MongoProperties();
		properties.setUri("mongodb://user:secret@mongo1.example.com:12345,"
				+ "mongo2.example.com:23456/test");
		MongoClient client = createMongoClient(properties);
		List<ServerAddress> allAddresses = extractServerAddresses(client);
		assertThat(allAddresses).hasSize(2);
		assertServerAddress(allAddresses.get(0), "mongo1.example.com", 12345);
		assertServerAddress(allAddresses.get(1), "mongo2.example.com", 23456);
		List<MongoCredential> credentialsList = client.getCredentialsList();
		assertThat(credentialsList).hasSize(1);
		assertMongoCredential(credentialsList.get(0), "user", "secret", "test");
	}

	@Test
	public void uriCannotBeSetWithCredentials() throws UnknownHostException {
		MongoProperties properties = new MongoProperties();
		properties.setUri("mongodb://127.0.0.1:1234/mydb");
		properties.setUsername("user");
		properties.setPassword("secret".toCharArray());
		this.thrown.expect(IllegalStateException.class);
		this.thrown.expectMessage("Invalid mongo configuration, "
				+ "either uri or host/port/credentials must be specified");
		createMongoClient(properties);
	}

	@Test
	public void uriCannotBeSetWithHostPort() throws UnknownHostException {
		MongoProperties properties = new MongoProperties();
		properties.setUri("mongodb://127.0.0.1:1234/mydb");
		properties.setHost("localhost");
		properties.setPort(4567);
		this.thrown.expect(IllegalStateException.class);
		this.thrown.expectMessage("Invalid mongo configuration, "
				+ "either uri or host/port/credentials must be specified");
		createMongoClient(properties);
	}

	private MongoClient createMongoClient(MongoProperties properties)
			throws UnknownHostException {
		return new MongoClientFactory(properties, null).createMongoClient(null);
	}

	private List<ServerAddress> extractServerAddresses(MongoClient client) {
		Cluster cluster = (Cluster) ReflectionTestUtils.getField(client, "cluster");
		ClusterSettings clusterSettings = (ClusterSettings) ReflectionTestUtils
				.getField(cluster, "settings");
		List<ServerAddress> allAddresses = clusterSettings.getHosts();
		return allAddresses;
	}

	private void assertServerAddress(ServerAddress serverAddress, String expectedHost,
			int expectedPort) {
		assertThat(serverAddress.getHost()).isEqualTo(expectedHost);
		assertThat(serverAddress.getPort()).isEqualTo(expectedPort);
	}

	private void assertMongoCredential(MongoCredential credentials,
			String expectedUsername, String expectedPassword, String expectedSource) {
		assertThat(credentials.getUserName()).isEqualTo(expectedUsername);
		assertThat(credentials.getPassword()).isEqualTo(expectedPassword.toCharArray());
		assertThat(credentials.getSource()).isEqualTo(expectedSource);
	}

	@Configuration
	@EnableConfigurationProperties(MongoProperties.class)
	static class Config {

	}

}
