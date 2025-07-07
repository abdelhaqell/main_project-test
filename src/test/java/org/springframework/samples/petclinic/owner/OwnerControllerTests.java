/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.samples.petclinic.owner;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.aot.DisabledInAotMode;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test class for {@link OwnerController}
 *
 * @author Colin But
 * @author Wick Dynex
 */
@WebMvcTest(OwnerController.class)
@DisabledInNativeImage
@DisabledInAotMode
class OwnerControllerTests {

	private static final int TEST_OWNER_ID = 1;

	private static final String OWNER_STR = "owner";
	private static final String FIRST_NAME_STR = "firstName";
	private static final String LAST_NAME_STR = "lastName";
	private static final String ADDRESS_STR = "address";
	private static final String CITY_STR = "city";
	private static final String TELEPHONE_STR = "telephone";

	private static final String NEW_OWNER_URI = "/owners/new";
	private static final String EDIT_OWNER_URI_TEMPLATE = "/owners/{ownerId}/edit";
	private static final String OWNERS_LIST_VIEW = "owners/ownersList";
	private static final String CREATE_OR_UPDATE_OWNER_FORM_VIEW = "owners/createOrUpdateOwnerForm";

	private static final String OWNER_LAST_NAME = "Franklin";
	private static final String OWNER_FIRST_NAME = "George";
	private static final String OWNER_ADDRESS = "110 W. Liberty St.";
	private static final String OWNER_CITY = "Madison";
	private static final String OWNER_TELEPHONE = "6085551023";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private OwnerRepository owners;

	private Owner george() {
		Owner george = new Owner();
		george.setId(TEST_OWNER_ID);
		george.setFirstName(OWNER_FIRST_NAME);
		george.setLastName(OWNER_LAST_NAME);
		george.setAddress(OWNER_ADDRESS);
		george.setCity(OWNER_CITY);
		george.setTelephone(OWNER_TELEPHONE);
		Pet max = new Pet();
		PetType dog = new PetType();
		dog.setName("dog");
		max.setType(dog);
		max.setName("Max");
		max.setBirthDate(LocalDate.now());
		george.addPet(max);
		max.setId(1);
		return george;
	}

	@BeforeEach
	void setup() {

		Owner george = george();
		given(this.owners.findByLastNameStartingWith(eq(OWNER_LAST_NAME), any(Pageable.class)))
			.willReturn(new PageImpl<>(List.of(george)));

		given(this.owners.findAll(any(Pageable.class))).willReturn(new PageImpl<>(List.of(george)));

		given(this.owners.findById(TEST_OWNER_ID)).willReturn(Optional.of(george));
		Visit visit = new Visit();
		visit.setDate(LocalDate.now());
		george.getPet("Max").getVisits().add(visit);

	}

	@Test
	void testInitCreationForm() throws Exception {
		mockMvc.perform(get(NEW_OWNER_URI))
			.andExpect(status().isOk())
			.andExpect(model().attributeExists(OWNER_STR))
			.andExpect(view().name(CREATE_OR_UPDATE_OWNER_FORM_VIEW));
	}

	@Test
	void testProcessCreationFormSuccess() throws Exception {
		mockMvc
				.perform(post(NEW_OWNER_URI).param(FIRST_NAME_STR, "Joe")
					.param(LAST_NAME_STR, "Bloggs")
					.param(ADDRESS_STR, "123 Caramel Street")
					.param(CITY_STR, "London")
					.param(TELEPHONE_STR, "1316761638"))
				.andExpect(status().is3xxRedirection());
	}

	@Test
	void testProcessCreationFormHasErrors() throws Exception {
		mockMvc
				.perform(post(NEW_OWNER_URI).param(FIRST_NAME_STR, "Joe").param(LAST_NAME_STR, "Bloggs").param(CITY_STR, "London"))
				.andExpect(status().isOk())
				.andExpect(model().attributeHasErrors(OWNER_STR))
				.andExpect(model().attributeHasFieldErrors(OWNER_STR, ADDRESS_STR))
				.andExpect(model().attributeHasFieldErrors(OWNER_STR, TELEPHONE_STR))
				.andExpect(view().name(CREATE_OR_UPDATE_OWNER_FORM_VIEW));
	}

	@Test
	void testInitFindForm() throws Exception {
		mockMvc.perform(get("/owners/find"))
			.andExpect(status().isOk())
			.andExpect(model().attributeExists(OWNER_STR))
			.andExpect(view().name("owners/findOwners"));
	}

	@Test
	void testProcessFindFormSuccess() throws Exception {
		Page<Owner> tasks = new PageImpl<>(List.of(george(), new Owner()));
		when(this.owners.findByLastNameStartingWith(anyString(), any(Pageable.class))).thenReturn(tasks);
		mockMvc.perform(get("/owners?page=1")).andExpect(status().isOk()).andExpect(view().name(OWNERS_LIST_VIEW));
	}

	@Test
	void testProcessFindFormByLastName() throws Exception {
		Page<Owner> tasks = new PageImpl<>(List.of(george()));
		when(this.owners.findByLastNameStartingWith(eq(OWNER_LAST_NAME), any(Pageable.class))).thenReturn(tasks);
		mockMvc.perform(get("/owners?page=1").param(LAST_NAME_STR, OWNER_LAST_NAME))
			.andExpect(status().is3xxRedirection())
			.andExpect(view().name("redirect:/owners/" + TEST_OWNER_ID));
	}

	@Test
	void testProcessFindFormNoOwnersFound() throws Exception {
		Page<Owner> tasks = new PageImpl<>(List.of());
		when(this.owners.findByLastNameStartingWith(eq("Unknown Surname"), any(Pageable.class))).thenReturn(tasks);
		mockMvc.perform(get("/owners?page=1").param(LAST_NAME_STR, "Unknown Surname"))
			.andExpect(status().isOk())
			.andExpect(model().attributeHasFieldErrors(OWNER_STR, LAST_NAME_STR))
			.andExpect(model().attributeHasFieldErrorCode(OWNER_STR, LAST_NAME_STR, "notFound"))
			.andExpect(view().name("owners/findOwners"));

	}

	@Test
	void testInitUpdateOwnerForm() throws Exception {
		mockMvc.perform(get(EDIT_OWNER_URI_TEMPLATE, TEST_OWNER_ID))
			.andExpect(status().isOk())
			.andExpect(model().attributeExists(OWNER_STR))
			.andExpect(model().attribute(OWNER_STR, hasProperty(LAST_NAME_STR, is(OWNER_LAST_NAME))))
			.andExpect(model().attribute(OWNER_STR, hasProperty(FIRST_NAME_STR, is(OWNER_FIRST_NAME))))
			.andExpect(model().attribute(OWNER_STR, hasProperty(ADDRESS_STR, is(OWNER_ADDRESS))))
			.andExpect(model().attribute(OWNER_STR, hasProperty(CITY_STR, is(OWNER_CITY))))
			.andExpect(model().attribute(OWNER_STR, hasProperty(TELEPHONE_STR, is(OWNER_TELEPHONE))))
			.andExpect(view().name(CREATE_OR_UPDATE_OWNER_FORM_VIEW));
	}

	@Test
	void testProcessUpdateOwnerFormSuccess() throws Exception {
		mockMvc
				.perform(post(EDIT_OWNER_URI_TEMPLATE, TEST_OWNER_ID).param(FIRST_NAME_STR, "Joe")
					.param(LAST_NAME_STR, "Bloggs")
					.param(ADDRESS_STR, "123 Caramel Street")
					.param(CITY_STR, "London")
					.param(TELEPHONE_STR, "1616291589"))
				.andExpect(status().is3xxRedirection())
				.andExpect(view().name("redirect:/owners/{ownerId}"));
	}

	@Test
	void testProcessUpdateOwnerFormUnchangedSuccess() throws Exception {
		mockMvc.perform(post(EDIT_OWNER_URI_TEMPLATE, TEST_OWNER_ID))
			.andExpect(status().is3xxRedirection())
			.andExpect(view().name("redirect:/owners/{ownerId}"));
	}

	@Test
	void testProcessUpdateOwnerFormHasErrors() throws Exception {
		mockMvc
				.perform(post(EDIT_OWNER_URI_TEMPLATE, TEST_OWNER_ID).param(FIRST_NAME_STR, "Joe")
					.param(LAST_NAME_STR, "Bloggs")
					.param(ADDRESS_STR, "")
					.param(TELEPHONE_STR, ""))
				.andExpect(status().isOk())
				.andExpect(model().attributeHasErrors(OWNER_STR))
				.andExpect(model().attributeHasFieldErrors(OWNER_STR, ADDRESS_STR))
				.andExpect(model().attributeHasFieldErrors(OWNER_STR, TELEPHONE_STR))
				.andExpect(view().name(CREATE_OR_UPDATE_OWNER_FORM_VIEW));
	}

	@Test
	void testShowOwner() throws Exception {
		mockMvc.perform(get("/owners/{ownerId}", TEST_OWNER_ID))
			.andExpect(status().isOk())
			.andExpect(model().attribute(OWNER_STR, hasProperty(LAST_NAME_STR, is(OWNER_LAST_NAME))))
			.andExpect(model().attribute(OWNER_STR, hasProperty(FIRST_NAME_STR, is(OWNER_FIRST_NAME))))
			.andExpect(model().attribute(OWNER_STR, hasProperty(ADDRESS_STR, is(OWNER_ADDRESS))))
			.andExpect(model().attribute(OWNER_STR, hasProperty(CITY_STR, is(OWNER_CITY))))
			.andExpect(model().attribute(OWNER_STR, hasProperty(TELEPHONE_STR, is(OWNER_TELEPHONE))))
			.andExpect(model().attribute(OWNER_STR, hasProperty("pets", not(empty()))))
			.andExpect(model().attribute(OWNER_STR,
																	hasProperty("pets", hasItem(hasProperty("visits", hasSize(greaterThan(0)))))))
			.andExpect(view().name("owners/ownerDetails"));
	}

	@Test
	public void testProcessUpdateOwnerFormWithIdMismatch() throws Exception {
		int pathOwnerId = 1;

		Owner owner = new Owner();
		owner.setId(2);
		owner.setFirstName("John");
		owner.setLastName("Doe");
		owner.setAddress("Center Street");
		owner.setCity("New York");
		owner.setTelephone("0123456789");

		when(owners.findById(pathOwnerId)).thenReturn(Optional.of(owner));

		mockMvc.perform(MockMvcRequestBuilders.post(EDIT_OWNER_URI_TEMPLATE, pathOwnerId).flashAttr(OWNER_STR, owner))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/owners/" + pathOwnerId + "/edit"))
			.andExpect(flash().attributeExists("error"));
	}

}
