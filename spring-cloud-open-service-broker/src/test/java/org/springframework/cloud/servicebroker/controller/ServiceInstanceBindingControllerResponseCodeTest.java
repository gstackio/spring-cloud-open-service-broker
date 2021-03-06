/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.servicebroker.controller;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;
import org.springframework.cloud.servicebroker.exception.ServiceInstanceBindingDoesNotExistException;
import org.springframework.cloud.servicebroker.exception.ServiceInstanceBindingExistsException;
import org.springframework.cloud.servicebroker.model.CreateServiceInstanceAppBindingResponse;
import org.springframework.cloud.servicebroker.model.CreateServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.CreateServiceInstanceBindingResponse;
import org.springframework.cloud.servicebroker.model.DeleteServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.ErrorMessage;
import org.springframework.cloud.servicebroker.model.ServiceDefinition;
import org.springframework.cloud.servicebroker.service.CatalogService;
import org.springframework.cloud.servicebroker.service.ServiceInstanceBindingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(Theories.class)
public class ServiceInstanceBindingControllerResponseCodeTest {
	private final CatalogService catalogService = mock(CatalogService.class);
	private final ServiceInstanceBindingService bindingService = mock(ServiceInstanceBindingService.class);

	private final Map<String, String> pathVariables = Collections.emptyMap();

	private ServiceInstanceBindingController controller;

	@DataPoints("createResponsesWithExpectedStatus")
	public static List<CreateResponseAndExpectedStatus> createDataPoints() {
		return Arrays.asList(
				new CreateResponseAndExpectedStatus(
						null,
						HttpStatus.CREATED
				),
				new CreateResponseAndExpectedStatus(
						CreateServiceInstanceAppBindingResponse.builder()
								.bindingExisted(false)
								.build(),
						HttpStatus.CREATED
				),
				new CreateResponseAndExpectedStatus(
						CreateServiceInstanceAppBindingResponse.builder()
								.bindingExisted(true)
								.build(),
						HttpStatus.OK
				)
		);
	}

	@Before
	public void setUp() {
		controller = new ServiceInstanceBindingController(catalogService, bindingService);

		when(catalogService.getServiceDefinition(anyString()))
				.thenReturn(ServiceDefinition.builder().build());
	}


	@Theory
	public void createServiceBindingWithResponseGivesExpectedStatus(CreateResponseAndExpectedStatus data) {
		when(bindingService.createServiceInstanceBinding(any(CreateServiceInstanceBindingRequest.class)))
				.thenReturn(data.response);

		CreateServiceInstanceBindingRequest createRequest = CreateServiceInstanceBindingRequest.builder()
				.serviceDefinitionId("service-definition-id")
				.build();

		ResponseEntity<CreateServiceInstanceBindingResponse> responseEntity = controller
				.createServiceInstanceBinding(pathVariables, null, null, null, null,
						createRequest);

		assertThat(responseEntity.getStatusCode(), equalTo(data.expectedStatus));
		assertThat(responseEntity.getBody(), equalTo(data.response));
	}

	@Test
	public void deleteServiceBindingWithSuccessGivesExpectedStatus() {
		ResponseEntity<String> responseEntity = controller
				.deleteServiceInstanceBinding(pathVariables, null, null, null, null, null, null);

		assertThat(responseEntity.getStatusCode(), equalTo(HttpStatus.OK));
		assertThat(responseEntity.getBody(), equalTo("{}"));

		verify(bindingService).deleteServiceInstanceBinding(any(DeleteServiceInstanceBindingRequest.class));
	}

	@Test
	public void deleteServiceBindingWithMissingBindingGivesExpectedStatus() {
		doThrow(new ServiceInstanceBindingDoesNotExistException("binding-id"))
				.when(bindingService).deleteServiceInstanceBinding(any(DeleteServiceInstanceBindingRequest.class));

		ResponseEntity<String> responseEntity = controller
				.deleteServiceInstanceBinding(pathVariables, null, null, null, null, null, null);

		assertThat(responseEntity.getStatusCode(), equalTo(HttpStatus.GONE));
		assertThat(responseEntity.getBody(), equalTo("{}"));
	}

	@Test
	public void bindingExistsGivesExpectedStatus() {
		ResponseEntity<ErrorMessage> responseEntity = controller
				.handleException(new ServiceInstanceBindingExistsException("service-instance-id", "binding-id"));

		assertThat(responseEntity.getStatusCode(), equalTo(HttpStatus.CONFLICT));
		assertThat(responseEntity.getBody().getMessage(), containsString("serviceInstanceId=service-instance-id"));
		assertThat(responseEntity.getBody().getMessage(), containsString("bindingId=binding-id"));
	}

	public static class CreateResponseAndExpectedStatus {
		protected final CreateServiceInstanceBindingResponse response;
		protected final HttpStatus expectedStatus;

		public CreateResponseAndExpectedStatus(CreateServiceInstanceBindingResponse response, HttpStatus expectedStatus) {
			this.response = response;
			this.expectedStatus = expectedStatus;
		}

		@Override
		public String toString() {
			String responseValue = response == null ? "null" :
					"{" +
						"bindingExisted=" + response.isBindingExisted() +
					"}";

			return "response=" + responseValue +
					", expectedStatus=" + expectedStatus;
		}
	}

}