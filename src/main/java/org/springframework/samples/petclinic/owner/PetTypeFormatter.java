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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.Formatter;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.util.Collection;
import java.util.Locale;

/**
 * Instructs Spring MVC on how to parse and print elements of type 'PetType'. Starting
 * from Spring 3.0, Formatters have come as an improvement in comparison to legacy
 * PropertyEditors. See the following links for more details: - The Spring ref doc:
 * https://docs.spring.io/spring-framework/docs/current/spring-framework-reference/core.html#format
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author Michael Isvy
 */

/*
 * 1. 끄적끄적
 *
 * Formatter 뭔가 검사하는 클래스?
 * PetType에 맞게 Format하는 클래스
 *
 * 협력은?
 * 	ownerRepository와 하네?
 * Formatter에서 제공하는 메서드 두 개 print와 parse를 오버라이딩하여 구현한다.
 *
 * */

/*
 * 스프링이 @Component 어노테이션을 통해 이 클래스를 감지한다.
 * */

/*
 * 최종
 *     사용자가 문자열 입력
    폼 데이터 서버로 전송
    parse 메서드 실행: 사용자가 입력한 문자열을 객체로 변환
    비즈니스 로직 처리
    print 메서드 실행: 객체를 문자열로 변환
    클라이언트에 데이터 표시

이 과정을 통해 사용자가 입력한 데이터가 적절한 객체로 변환되고, 필요한 비즈니스 로직이 처리된 후, 다시 사용자에게 친숙한 형식으로 데이터가 표시됩니다.
이 흐름은 사용자가 입력한 데이터가 폼에서 처리되고 다시 렌더링되는 전형적인 데이터 바인딩 및 변환 과정입니다.
 * */
@Component
public class PetTypeFormatter implements Formatter<PetType> {

	/*
	 * 레포지토리와 상호작용하는 이유는 클라이언트가 입력한 문자열을 토대로 db에서 찾기 위해
	 * PetType도 엄연한 하나의 테이블
	 * */
	private final OwnerRepository owners;

	@Autowired
	public PetTypeFormatter(OwnerRepository owners) {
		this.owners = owners;
	}

	@Override
	public String print(PetType petType, Locale locale) {
		return petType.getName();
	}

	@Override
	public PetType parse(String text, Locale locale) throws ParseException {

		Collection<PetType> findPetTypes = this.owners.findPetTypes();
		for (PetType type : findPetTypes) {
			if (type.getName().equals(text)) {
				return type;
			}
		}
		throw new ParseException("type not found: " + text, 0);
	}

}
