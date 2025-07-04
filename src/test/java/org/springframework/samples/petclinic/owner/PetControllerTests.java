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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.aot.DisabledInAotMode;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Test class for the {@link PetController}
 *
 * @author Colin But
 * @author Wick Dynex
 */
@WebMvcTest(value = PetController.class,
		includeFilters = @ComponentScan.Filter(value = PetTypeFormatter.class, type = FilterType.ASSIGNABLE_TYPE))
@DisabledInNativeImage
@DisabledInAotMode
class PetControllerTests {

	private static final int TEST_OWNER_ID = 1;

	private static final int TEST_PET_ID = 1;

	private static final String HAMSTER = "hamster";

	private static final String REQUEST_MAPPING_NEW_PET = "/owners/{ownerId}/pets/new";

	private static final String VIEW_CREATE_OR_UPDATE_PET_FORM = "pets/createOrUpdatePetForm";

	private static final String NAME_BETTY = "Betty";

	private static final String BIRTH_DATE = "birthDate";

	private static final String DATE_2015_02_12 = "2015-02-12";

	private static final String OWNER = "owner";

	private static final String REQUIRED = "required";

	private static final String REQUEST_MAPPING_EDIT_PET = "/owners/{ownerId}/pets/{petId}/edit";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private OwnerRepository owners;

	@BeforeEach
	void setup() {
		PetType cat = new PetType();
		cat.setId(3);
		cat.setName(HAMSTER);
		given(this.owners.findPetTypes()).willReturn(List.of(cat));

		Owner owner = new Owner();
		Pet pet = new Pet();
		Pet dog = new Pet();
		owner.addPet(pet);
		owner.addPet(dog);
		pet.setId(TEST_PET_ID);
		dog.setId(TEST_PET_ID + 1);
		pet.setName("petty");
		dog.setName("doggy");
		given(this.owners.findById(TEST_OWNER_ID)).willReturn(Optional.of(owner));
	}

	@Test
	void testInitCreationForm() throws Exception {
		mockMvc.perform(get(REQUEST_MAPPING_NEW_PET, TEST_OWNER_ID))
			.andExpect(status().isOk())
			.andExpect(view().name(VIEW_CREATE_OR_UPDATE_PET_FORM))
			.andExpect(model().attributeExists("pet"));
	}

	@Test
	void testProcessCreationFormSuccess() throws Exception {
		mockMvc
				.perform(post(REQUEST_MAPPING_NEW_PET, TEST_OWNER_ID).param("name", NAME_BETTY)
						.param("type", HAMSTER)
						.param(BIRTH_DATE, DATE_2015_02_12))
			.andExpect(status().is3xxRedirection())
			.andExpect(view().name("redirect:/owners/{ownerId}"));
	}

	@Nested
	class ProcessCreationFormHasErrors {

		@Test
		void testProcessCreationFormWithBlankName() throws Exception {
			mockMvc
					.perform(post(REQUEST_MAPPING_NEW_PET, TEST_OWNER_ID).param("name", "	 \n")
						.param(BIRTH_DATE, DATE_2015_02_12))
				.andExpect(model().attributeHasNoErrors(OWNER))
				.andExpect(model().attributeHasErrors("pet"))
				.andExpect(model().attributeHasFieldErrors("pet", "name"))
				.andExpect(model().attributeHasFieldErrorCode("pet", "name", REQUIRED))
				.andExpect(status().isOk())
				.andExpect(view().name(VIEW_CREATE_OR_UPDATE_PET_FORM));
		}

		@Test
		void testProcessCreationFormWithDuplicateName() throws Exception {
			mockMvc
					.perform(post(REQUEST_MAPPING_NEW_PET, TEST_OWNER_ID).param("name", "petty")
						.param(BIRTH_DATE, DATE_2015_02_12))
				.andExpect(model().attributeHasNoErrors(OWNER))
				.andExpect(model().attributeHasErrors("pet"))
				.andExpect(model().attributeHasFieldErrors("pet", "name"))
				.andExpect(model().attributeHasFieldErrorCode("pet", "name", "duplicate"))
				.andExpect(status().isOk())
				.andExpect(view().name(VIEW_CREATE_OR_UPDATE_PET_FORM));
		}

		@Test
		void testProcessCreationFormWithMissingPetType() throws Exception {
			mockMvc
					.perform(post(REQUEST_MAPPING_NEW_PET, TEST_OWNER_ID).param("name", NAME_BETTY)
						.param(BIRTH_DATE, DATE_2015_02_12))
				.andExpect(model().attributeHasNoErrors(OWNER))
				.andExpect(model().attributeHasErrors("pet"))
				.andExpect(model().attributeHasFieldErrors("pet", "type"))
				.andExpect(model().attributeHasFieldErrorCode("pet", "type", REQUIRED))
				.andExpect(status().isOk())
				.andExpect(view().name(VIEW_CREATE_OR_UPDATE_PET_FORM));
		}

		@Test
		void testProcessCreationFormWithInvalidBirthDate() throws Exception {
			LocalDate currentDate = LocalDate.now();
			String futureBirthDate = currentDate.plusMonths(1).toString();

			mockMvc
					.perform(post(REQUEST_MAPPING_NEW_PET, TEST_OWNER_ID).param("name", NAME_BETTY)
						.param(BIRTH_DATE, futureBirthDate))
				.andExpect(model().attributeHasNoErrors(OWNER))
				.andExpect(model().attributeHasErrors("pet"))
				.andExpect(model().attributeHasFieldErrors("pet", BIRTH_DATE))
				.andExpect(model().attributeHasFieldErrorCode("pet", BIRTH_DATE, "typeMismatch.birthDate"))
				.andExpect(status().isOk())
				.andExpect(view().name(VIEW_CREATE_OR_UPDATE_PET_FORM));
		}

		@Test
		void testInitUpdateForm() throws Exception {
			mockMvc.perform(get(REQUEST_MAPPING_EDIT_PET, TEST_OWNER_ID, TEST_PET_ID))
				.andExpect(status().isOk())
				.andExpect(model().attributeExists("pet"))
				.andExpect(view().name(VIEW_CREATE_OR_UPDATE_PET_FORM));
		}

	}

	@Test
	void testProcessUpdateFormSuccess() throws Exception {
		mockMvc
				.perform(post(REQUEST_MAPPING_EDIT_PET, TEST_OWNER_ID, TEST_PET_ID).param("name", NAME_BETTY)
						.param("type", HAMSTER)
						.param(BIRTH_DATE, DATE_2015_02_12))
			.andExpect(status().is3xxRedirection())
			.andExpect(view().name("redirect:/owners/{ownerId}"));
	}

	@Nested
	class ProcessUpdateFormHasErrors {

		@Test
		void testProcessUpdateFormWithInvalidBirthDate() throws Exception {
			mockMvc
					.perform(post(REQUEST_MAPPING_EDIT_PET, TEST_OWNER_ID, TEST_PET_ID).param("name", " ")
						.param(BIRTH_DATE, "2015/02/12"))
				.andExpect(model().attributeHasNoErrors(OWNER))
				.andExpect(model().attributeHasErrors("pet"))
				.andExpect(model().attributeHasFieldErrors("pet", BIRTH_DATE))
				.andExpect(model().attributeHasFieldErrorCode("pet", BIRTH_DATE, "typeMismatch"))
				.andExpect(view().name(VIEW_CREATE_OR_UPDATE_PET_FORM));
		}

		@Test
		void testProcessUpdateFormWithBlankName() throws Exception {
			mockMvc
					.perform(post(REQUEST_MAPPING_EDIT_PET, TEST_OWNER_ID, TEST_PET_ID).param("name", "  ")
						.param(BIRTH_DATE, DATE_2015_02_12))
				.andExpect(model().attributeHasNoErrors(OWNER))
				.andExpect(model().attributeHasErrors("pet"))
				.andExpect(model().attributeHasFieldErrors("pet", "name"))
				.andExpect(model().attributeHasFieldErrorCode("pet", "name", REQUIRED))
				.andExpect(view().name(VIEW_CREATE_OR_UPDATE_PET_FORM));
		}

	}

}
