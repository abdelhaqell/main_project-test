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
	private static final String OWNER = "owner";
	private static final String FIRST_NAME = "firstName";
	private static final String LAST_NAME = "lastName";
	private static final String ADDRESS = "address";
	private static final String TELEPHONE = "telephone";
	private static final String FRANKLIN = "Franklin";
	private static final String GEORGE = "George";
	private static final String BLOGGS = "Bloggs";
	private static final String MADISON = "Madison";
	private static final String LONDON = "London";
	private static final String LIBERTY_ST = "110 W. Liberty St.";
	private static final String PHONE = "6085551023";
	private static final String OWNERS_CREATE_FORM = "owners/createOrUpdateOwnerForm";
	private static final String OWNERS_NEW = "/owners/new";
	private static final String OWNERS_EDIT = "/owners/{ownerId}/edit";
	private static final String OWNERS_PAGE = "/owners?page=1";
	
	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private OwnerRepository owners;

	private Owner george() {
		Owner george = new Owner();
		george.setId(TEST_OWNER_ID);
		george.setFirstName(GEORGE);
		george.setLastName(FRANKLIN);
		george.setAddress(LIBERTY_ST);
		george.setCity(MADISON);
		george.setTelephone(PHONE);
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
		given(this.owners.findByLastNameStartingWith(eq(FRANKLIN), any(Pageable.class)))
			.willReturn(new PageImpl<>(List.of(george)));

		given(this.owners.findAll(any(Pageable.class))).willReturn(new PageImpl<>(List.of(george)));

		given(this.owners.findById(TEST_OWNER_ID)).willReturn(Optional.of(george));
		Visit visit = new Visit();
		visit.setDate(LocalDate.now());
		george.getPet("Max").getVisits().add(visit);

	}

	@Test
	void testInitCreationForm() throws Exception {
		mockMvc.perform(get(OWNERS_NEW))
			.andExpect(status().isOk())
			.andExpect(model().attributeExists(OWNER))
			.andExpect(view().name(OWNERS_CREATE_FORM));
	}

	@Test
	void testProcessCreationFormSuccess() throws Exception {
		mockMvc
			.perform(post(OWNERS_NEW).param(FIRST_NAME, "Joe")
				.param(LAST_NAME, BLOGGS)
				.param(ADDRESS, "123 Caramel Street")
				.param("city", LONDON)
				.param(TELEPHONE, "1316761638"))
			.andExpect(status().is3xxRedirection());
	}

	@Test
	void testProcessCreationFormHasErrors() throws Exception {
		mockMvc
			.perform(post(OWNERS_NEW).param(FIRST_NAME, "Joe").param(LAST_NAME, BLOGGS).param("city", LONDON))
			.andExpect(status().isOk())
			.andExpect(model().attributeHasErrors(OWNER))
			.andExpect(model().attributeHasFieldErrors(OWNER, ADDRESS))
			.andExpect(model().attributeHasFieldErrors(OWNER, TELEPHONE))
			.andExpect(view().name(OWNERS_CREATE_FORM));
	}

	@Test
	void testInitFindForm() throws Exception {
		mockMvc.perform(get("/owners/find"))
			.andExpect(status().isOk())
			.andExpect(model().attributeExists(OWNER))
			.andExpect(view().name("owners/findOwners"));
	}

	@Test
	void testProcessFindFormSuccess() throws Exception {
		Page<Owner> tasks = new PageImpl<>(List.of(george(), new Owner()));
		when(this.owners.findByLastNameStartingWith(anyString(), any(Pageable.class))).thenReturn(tasks);
		mockMvc.perform(get(OWNERS_PAGE)).andExpect(status().isOk()).andExpect(view().name("owners/ownersList"));
	}

	@Test
	void testProcessFindFormByLastName() throws Exception {
		Page<Owner> tasks = new PageImpl<>(List.of(george()));
		when(this.owners.findByLastNameStartingWith(eq(FRANKLIN), any(Pageable.class))).thenReturn(tasks);
		mockMvc.perform(get(OWNERS_PAGE).param(LAST_NAME, FRANKLIN))
			.andExpect(status().is3xxRedirection())
			.andExpect(view().name("redirect:/owners/" + TEST_OWNER_ID));
	}

	@Test
	void testProcessFindFormNoOwnersFound() throws Exception {
		Page<Owner> tasks = new PageImpl<>(List.of());
		when(this.owners.findByLastNameStartingWith(eq("Unknown Surname"), any(Pageable.class))).thenReturn(tasks);
		mockMvc.perform(get(OWNERS_PAGE).param(LAST_NAME, "Unknown Surname"))
			.andExpect(status().isOk())
			.andExpect(model().attributeHasFieldErrors(OWNER, LAST_NAME))
			.andExpect(model().attributeHasFieldErrorCode(OWNER, LAST_NAME, "notFound"))
			.andExpect(view().name("owners/findOwners"));

	}

	@Test
	void testInitUpdateOwnerForm() throws Exception {
		mockMvc.perform(get(OWNERS_EDIT, TEST_OWNER_ID))
			.andExpect(status().isOk())
			.andExpect(model().attributeExists(OWNER))
			.andExpect(model().attribute(OWNER, hasProperty(LAST_NAME, is(FRANKLIN))))
			.andExpect(model().attribute(OWNER, hasProperty(FIRST_NAME, is(GEORGE))))
			.andExpect(model().attribute(OWNER, hasProperty(ADDRESS, is(LIBERTY_ST))))
			.andExpect(model().attribute(OWNER, hasProperty("city", is(MADISON))))
			.andExpect(model().attribute(OWNER, hasProperty(TELEPHONE, is(PHONE))))
			.andExpect(view().name(OWNERS_CREATE_FORM));
	}

	@Test
	void testProcessUpdateOwnerFormSuccess() throws Exception {
		mockMvc
			.perform(post(OWNERS_EDIT, TEST_OWNER_ID).param(FIRST_NAME, "Joe")
				.param(LAST_NAME, BLOGGS)
				.param(ADDRESS, "123 Caramel Street")
				.param("city", LONDON)
				.param(TELEPHONE, "1616291589"))
			.andExpect(status().is3xxRedirection())
			.andExpect(view().name("redirect:/owners/{ownerId}"));
	}

	@Test
	void testProcessUpdateOwnerFormUnchangedSuccess() throws Exception {
		mockMvc.perform(post(OWNERS_EDIT, TEST_OWNER_ID))
			.andExpect(status().is3xxRedirection())
			.andExpect(view().name("redirect:/owners/{ownerId}"));
	}

	@Test
	void testProcessUpdateOwnerFormHasErrors() throws Exception {
		mockMvc
			.perform(post(OWNERS_EDIT, TEST_OWNER_ID).param(FIRST_NAME, "Joe")
				.param(LAST_NAME, BLOGGS)
				.param(ADDRESS, "")
				.param(TELEPHONE, ""))
			.andExpect(status().isOk())
			.andExpect(model().attributeHasErrors(OWNER))
			.andExpect(model().attributeHasFieldErrors(OWNER, ADDRESS))
			.andExpect(model().attributeHasFieldErrors(OWNER, TELEPHONE))
			.andExpect(view().name(OWNERS_CREATE_FORM));
	}

	@Test
	void testShowOwner() throws Exception {
		mockMvc.perform(get("/owners/{ownerId}", TEST_OWNER_ID))
			.andExpect(status().isOk())
			.andExpect(model().attribute(OWNER, hasProperty(LAST_NAME, is(FRANKLIN))))
			.andExpect(model().attribute(OWNER, hasProperty(FIRST_NAME, is(GEORGE))))
			.andExpect(model().attribute(OWNER, hasProperty(ADDRESS, is(LIBERTY_ST))))
			.andExpect(model().attribute(OWNER, hasProperty("city", is(MADISON))))
			.andExpect(model().attribute(OWNER, hasProperty(TELEPHONE, is(PHONE))))
			.andExpect(model().attribute(OWNER, hasProperty("pets", not(empty()))))
			.andExpect(model().attribute(OWNER,
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

		mockMvc.perform(MockMvcRequestBuilders.post(OWNERS_EDIT, pathOwnerId).flashAttr(OWNER, owner))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/owners/" + pathOwnerId + "/edit"))
			.andExpect(flash().attributeExists("error"));
	}

}