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

import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

/**
 * <code>Validator</code> for <code>Pet</code> forms.
 * <p>
 * We're not using Bean Validation annotations here because it is easier to
 * define such
 * validation rule in Java.
 * </p>
 *
 * @author Ken Krebs
 * @author Juergen Hoeller
 */
public class PetValidator implements Validator {

	/*
	 * gpt 궁금점
	 *
	 * 1. String REQUIRED = "required"; 굳이 이걸 왜 써주는 거지? 매개변수에 정수나 문자열을 직접 넣는건 지양해야해서
	 * 그런가?
	 * 2. petType을 별도 로직을 짜서 유효성 검증하는 건 오케이. 생일이나 이름은 왜?
	 */
	private static final String REQUIRED = "required";

	@Override
	public void validate(Object obj, Errors errors) {
		Pet pet = (Pet) obj;
		String name = pet.getName();
		// name validation
		if (!StringUtils.hasText(name)) {
			errors.rejectValue("name", REQUIRED, REQUIRED);
		}

		// type validation
		if (pet.isNew() && pet.getType() == null) {
			errors.rejectValue("type", REQUIRED, REQUIRED);
		}

		// birth date validation
		if (pet.getBirthDate() == null) {
			errors.rejectValue("birthDate", REQUIRED, REQUIRED);
		}
	}

	/**
	 * This Validator validates *just* Pet instances
	 */
	/*
	 * 이 메서드는 현재 Validator가 검증할 수 있는 객체 타입을 지정합니다.
	 * Pet.class.isAssignableFrom(clazz): 
	 * 이 표현식은 clazz가 Pet 클래스이거나 Pet의 서브클래스인 경우에 true를 반환합니다.
	 * Pet 클래스를 포함하여 그 하위 클래스에 대해서도 이 Validator가 유효성 검사를 수행할 수 있음을 의미합니다.
	 */
	@Override
	public boolean supports(Class<?> clazz) {
		return Pet.class.isAssignableFrom(clazz);
	}

}
