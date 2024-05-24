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

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import jakarta.validation.Valid;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * @author Juergen Hoeller
 * @author Ken Krebs
 * @author Arjen Poutsma
 * @author Michael Isvy
 */
@Controller
class OwnerController {

	private static final String VIEWS_OWNER_CREATE_OR_UPDATE_FORM = "owners/createOrUpdateOwnerForm";

	private final OwnerRepository owners;

	public OwnerController(OwnerRepository clinicService) {
		this.owners = clinicService;
	}

	/*
	 * 1. 주요 개념
	 *
	 * @InitBinder : 컨트롤러 메서드가 실행되기 전 자동으로 실행되어 바인딩을 지원하는 어노테이션
	 * WebDataBinder : 필드 데이터 바인딩을 커스텀하는 스프링 클래스
	 * -----------
	 * 2. 로직
	 *
	 * 그래 역으로 생각해보자
	 * id 필드는 어떻게든 변경되지 않게 하고 싶어.
	 * 그럼 어떻게 해야 할까?
	 * 애플리케이션이 실행되면 클라이언트의 요청에 따라 컨트롤러 메서드가 실행됨.
	 * 컨트롤러 메서드가 실행되면, 클라이언트가 입력한 데이터가 필드에 바인딩됨.
	 * 바인딩된 필드 데이터는 DB에 저장되거나 사용됨.
	 *
	 * id 필드 바인딩을 방지하기 위해서는 컨트롤러 메서드가 실행되기 전에 사전 검증을 해야 함.
	 * 이를 위해 @InitBinder 어노테이션을 사용하여 메서드를 구현해야 함.
	 * 그러면 Spring Boot가 @InitBinder 어노테이션을 감지하여 컨트롤러 메서드가 실행되기 전에 해당 메서드를 자동으로
	 * 실행시켜줌.
	 *
	 * 따라서 이 어노테이션이 붙은 메서드를 구현해야 함.
	 * 필드 데이터 바인딩을 커스터마이징할 수 있도록 지원하는 Spring 클래스가 무엇이냐면, DataBinder를 확장한
	 * WebDataBinder임.
	 * DataBinder의 메서드인 setDisallowedFields를 통해 "id" 필드를 바인딩되지 않도록 커스터마이징할 수 있음.
	 */
	@InitBinder
	public void setAllowedFields(WebDataBinder dataBinder) {
		dataBinder.setDisallowedFields("id");
	}

	/*
	 * 1. 주요 개념
	 *
	 * @ModelAttrubute : 메서드가 반환하는 객체를 Model에 추가하는 Annotation
	 *
	 * @PathVariable : URL 경로에서 가져온 변수를 메서드 파라미터에 매핑하는 어노테이션
	 * required = false : URL 경로에 변수가 없어도 됨
	 * ------
	 * 2. 로직
	 * URL 경로에서 가져온 ownerId를 메서드 매개변수 ownerId에 지정한다.
	 * 반환값은 owner이다.
	 * ownerId가 null일 경우 new Owner()를 생성한다.
	 * null이 아닐 경우 매개변수로 받은 ownerId에 해당하는 정보를 찾는 fingById 메서드를 실행한다.
	 * 메서드 이름은 findOwner이며 해당 메서드를 통해 반환된 owner값은 Model에 추가한다.
	 * -------
	 * 요청: http://localhost:8080/owners/find로 접속.
	 * 컨트롤러: initFindForm 메서드 호출.
	 * 뷰 반환: "owners/findOwners" 뷰 반환.
	 * 모델에 owner 객체 없음: @ModelAttribute 메서드가 주석 처리되어 모델에 owner 객체가 추가되지 않음.
	 * 템플릿 렌더링 오류: findOwners.html에서 th:object="${owner}"가 owner 객체를 찾지 못해 오류 발생.
	 * 에러 메시지: "An error happened during template parsing..." 메시지 출력.
	 */
	@ModelAttribute("owner")
	public Owner findOwner(@PathVariable(name = "ownerId", required = false) Integer ownerId) {
		return ownerId == null ? new Owner() : this.owners.findById(ownerId);
	}


	/*
	 * 1. 주요 개념
	 * Map<String, Object> model : Spring MVC는 메서드 매개변수 타입이 Map<String, Object>일 경우
	 * 이를 자동으로 모델 객체로 인식합니다.
	 * ----
	 * 2. 로직
	 * 클라이언트가 "Add Owner" 버튼을 클릭하면
	 * /owners/new URL에 매핑된 @GetMapping이 붙은 컨트롤러 메서드가 호출됩니다.
	 * initCreationForm이라는 이름의 메서드는 Model에서 데이터를 가져오기 위해
	 * Map<String, Object> model 타입의 매개변수를 사용합니다.
	 * 메서드가 실행되면, 새로운 Owner 객체를 생성하고
	 * 이를 모델에 추가합니다. (model에 owner 객체를 전달합니다.)
	 * 그런 다음, VIEWS_OWNER_CREATE_OR_UPDATE_FORM에 해당하는 뷰 이름을 반환합니다.
	 */
	@GetMapping("/owners/new")
	public String initCreationForm(Map<String, Object> model) {
		Owner owner = new Owner();
		model.put("owner", owner);
		return VIEWS_OWNER_CREATE_OR_UPDATE_FORM;
	}
	/*@GetMapping("/owners/new")
	public String initCreationForm(Model model) {
		Owner owner = new Owner();
		model.addAttribute("owner", owner);
		return VIEWS_OWNER_CREATE_OR_UPDATE_FORM;
	}*/


	/*
	 * 0. 훑어보기
	 *
	 * @Vaild
	 * BindingResult
	 * RedirectAttributes
	 * ---
	 * 1. 주요 개념
	 *
	 * ---
	 * /*
	 * 2. 로직
	 * initCreationForm 실행 후 "owners/createOrUpdateOwnerForm" 뷰로 렌더링됩니다.
	 * 해당 뷰에서 클라이언트가 데이터를 입력할 수 있는 화면이 제공됩니다.
	 * 클라이언트가 데이터를 입력 후 확인을 누르면 @PostMapping("/owners/new")가 붙은 processCreationForm
	 * 메서드가 실행됩니다.
	 *
	 * 1. 유효성 검사 및 오류 처리
	 * if BindingResult 타입의 result에 error가 있으면:
	 * - redirectAttributes를 사용하여 "error"라는 키와
	 * "There was an error in creating the owner."라는 메시지를 추가합니다.
	 * - "owners/createOrUpdateOwnerForm" 뷰를 반환하여 클라이언트에게 폼을 다시 보여줍니다.
	 *
	 * 2. 데이터 저장 및 성공 처리
	 * 클라이언트가 입력한 owner 객체 정보를 저장하여 데이터베이스에 저장합니다.
	 * - redirectAttributes를 사용하여 "message"라는 키와 "New Owner Created"라는 메시지를 추가합니다.
	 * - 새로 생성된 소유자의 상세 페이지로 리다이렉트합니다: "redirect:/owners/" + owner.getId();
	 */
	@PostMapping("/owners/new")
	public String processCreationForm(@Valid Owner owner, BindingResult result, RedirectAttributes redirectAttributes) {
		if (result.hasErrors()) {
			redirectAttributes.addFlashAttribute("error", "There was an error in creating the owner.");
			return VIEWS_OWNER_CREATE_OR_UPDATE_FORM;
		}

		this.owners.save(owner);
		redirectAttributes.addFlashAttribute("message", "New Owner Created");
		return "redirect:/owners/" + owner.getId();
	}


	/*
	 * 로직
	 * 클라이언트가 "Find Owner"를 클릭하면, /owners/find URL을 요청하게 된다.
	 * 이 요청을 핸들러 메서드가 감지하여 initFindForm 메서드가 실행된다.
	 * initFindForm 메서드는 "owners/findOwners" 페이지를 반환하여,
	 * 클라이언트에게 소유자 검색 폼을 렌더링한다.
	 */
	/*
	 * 확실히 알아두자
	 * 클라이언트는 홈페이지의 find owners를 클릭하는게 아니라
	 * /owners/find url을 get 방식으로 요청하는 것이다.
	 * */
	@GetMapping("/owners/find")
	public String initFindForm() {
		return "owners/findOwners";
	}

	/*
	 * 0. 훑어보기
	 * @RequestParam
	 * Model -> initCreationForm에는 Map을 썼는데??
	 * ----
	 * 1. 주요 개념
	 * Page
	 * @RequestParam(defaultValue = "1") int page,
	 * -> 클라이언트가 요청한 Param은 디폴트가 1이다. (1페이지)
	 * defaultValue 속성은 요청 매개변수가 제공되지 않을 때 사용할 기본값을 지정합니다.
	 * 이걸 int page에 매핑한다.
	 * 따라서 find owner에 아무것도 입력하지 않을 시 owner 1페이지가 반환된다.
	 *
	 * rejectValue : BindingResult를 하위 인터페이스로 둔 Errors 인터페이스의 메서드
	 *
	 * ---
	 /*
     * 2. 로직
     * 클라이언트가 /owners url을 get으로 요청시 processFindForm 메서드를 실행한다.
     * find owner에서 정보를 입력하지 않을 시, db에 저장된 모든 owner의 데이터가 반환되는데 요청 파라미터가 없을 시
     * 디폴트를 1로 설정하는(@RequestParam) page 매개변수와
     * owner 객체, 바인딩한 결과 객체, model에 담기 위해 model 객체를 매개변수로 받는다.
     *
     * if 클라이언트가 입력한 owner의 lastName이 null이면 null처리가 아닌 ""을 set한다.
     *
     * page 정보와 owner의 lastName을 가져오는 매개변수를 받는 findPaginatedForOwnersLastName을 실행후
     * 이를 Owner타입의 Page 객체에 저장한다. (참조변수 : ownersResult)
     *
     * if owner가 조회되지 않는 경우, 이를 바인딩한 result가 lastName, notFound라는 rejectValue를 실행한다.
     * 그리고 초기 /owners/find를 반환한다.
     *
     * if 조회된 owner의 결과가 총 1개이면
     * owner를 순환하여 참조해서 결과를 추출하여 owner에 담는다.
     * 그리고/owners/owner.getId()를 redirect한다.
     *
     * 여러개 이면
     * addPaginationModel(page, model, ownersResults)을 return한다.
     */
	/*
	 * 1. ""을 입력했을 시 -> /owners
	 * 2. last name이 없을 시 -> 찾을 수 없습니다 문구 출력
	 * 3. last name이 있을 시 -> 해당 owner 정보 출력
	 *
	 */
	@GetMapping("/owners")
	public String processFindForm(@RequestParam(defaultValue = "1") int page, Owner owner, BindingResult result,
								  Model model) {
		// allow parameterless GET request for /owners to return all records
		if (owner.getLastName() == null) {
			owner.setLastName(""); // empty string signifies broadest possible search
		}

		// getLastName()이 null일 경우
		// find owners by last name
		Page<Owner> ownersResults = findPaginatedForOwnersLastName(page, owner.getLastName()); // 이시점에서 getLastname()은 ""이다. 따라서 owner 전체가 조회된다.
		if (ownersResults.isEmpty()) {
			// no owners found
			// result를 view에서 확인하는구나!!
			// Spring MVC에서 유효성 검증을 수행할 때 사용하는 메서드
			result.rejectValue("lastName", "notFound", "not found");
			return "owners/findOwners";
		}

		if (ownersResults.getTotalElements() == 1) {
			// 1 owner found
			/*
			 * 컬렉션에서 데이터를 순회하여 가져오는 방법중에 하나
			 * iterator!!
			 * iterator().next():
			 * 장점: 표준화된 방법으로, Iterable 인터페이스를 구현하는 모든 객체에서 사용 가능.
			 * 단점: 약간의 추가 코드(iterator() 호출)와 직관적이지 않은 점.
			 *
			 * getContent().get(0):
			 * 장점: 간단하고 직관적이며, List의 첫 번째 요소를 직접 가져옴.
			 * 단점: Page 객체가 비어 있으면 IndexOutOfBoundsException이 발생할 수 있음.
			 *
			 * getContent().stream().findFirst().orElse(null):
			 * 장점: 스트림 API를 사용하여 안전하게 첫 번째 요소를 가져옴.
			 * 단점: 약간의 추가 오버헤드가 발생할 수 있음.
			 */
			owner = ownersResults.iterator().next();
			return "redirect:/owners/" + owner.getId();
		}

		// multiple owners found
		return addPaginationModel(page, model, ownersResults);
	}

	/*
	 * 로직
	 * page, model, Page 인스턴스인 paginated를 매개변수로 받는 addPaginationModel 메서드
	 * Page 객체의 데이터를 Owner타입의 List에 담는다 (참조변수 : listOwners)
	 * page -> "currentPage"
	 * Page 객체의 totalPage -> "totalPages":
	 * Page 객체의 totalElements -> "totalItems"
	 * Page 객체의 내용물 List -> "listOwners"
	 * 이들을 model에 추가후 "owners/ownersList"로 return한다.
	 * */
	/*
	 * model에 page 정보를 추가하는 메서드
	 * model에 무엇을 추가해야 할까?
	 * view가 필요로 하는 데이터가 뭘까?
	 * owner들의 리스트, 클라이언트가 요청한 page
	 * owner들이 총 몇 페이지인지
	 * owner들이 총 몇 명인지
	 * */
	private String addPaginationModel(int page, Model model, Page<Owner> paginated) {
		List<Owner> listOwners = paginated.getContent();
		model.addAttribute("currentPage", page);
		model.addAttribute("totalPages", paginated.getTotalPages());
		model.addAttribute("totalItems", paginated.getTotalElements());
		model.addAttribute("listOwners", listOwners);
		return "owners/ownersList";
	}


	/*
	* 1. 주요 개념
	* Page : Spring Data JPA에서 제공하는 인터페이스로, 페이징된 결과 데이터를 나타냅니다.
	* 		 페이징된 데이터 목록뿐만 아니라 페이지 관련 메타데이터(총 페이지 수, 현재 페이지, 총 요소 수 등)를 포함합니다.
	* Pageable : 페이징 정보를 캡슐화하는 인터페이스입니다.
	* 			 페이지 번호, 페이지 크기, 정렬 기준 등을 포함합니다.
	* PageRequest : Pageable 인터페이스의 구현체로, 페이지 요청을 나타냅니다.
	* 				PageRequest 객체는 페이지 번호와 페이지 크기 정보를 포함하여 특정 페이지의 데이터를 요청할 때 사용됩니다.
	* 정적 팩토리 메서드인 PageRequest.of(int page, int size)를 사용하여 인스턴스를 생성할 수 있습니다.
	* 팩토리 메서드 : 객체 생성을 캡슐화하는 메서드 (메서드 반환 값이 객체다.)
	*
	* 2. 로직
	* owner의 lastname으로 팩토리 메서드는 객체 생성 로직을 캡슐화하여 내부 구현을 변경하지 않고도 객체 생성 방식을 유연하게 변경할 수 있습니다.
		예를 들어, 추가적인 페이징된 정보를 찾는다. (findPaginatedForOwnersLastName)
	* page와 lastname을 매개변수로 받아 Page 타입으로 반환한다.
	* pageSize는 5로 설정한다. (1페이지당 5개)
	* 클라이언트가 요청한 페이지에 -1처리하고 설정된 pageSize를 받아 PageRequest.of를 실행하여 캡슐화된 pageable에 저장한다.
	* lastname과 pageable을 받아 findbylastname을 실행한 값을 return한다.
	* */
	private Page<Owner> findPaginatedForOwnersLastName(int page, String lastname) {
		int pageSize = 5;
		// page -1인 이유 : view -> 1 / 서버 0 (배열의 시작은 0부터다.)
		Pageable pageable = PageRequest.of(page - 1, pageSize);
		return owners.findByLastName(lastname, pageable);
	}

	/*
	* 주요 개념
	*
	* 개요
	* owner정보를 edit하는 url을 get요청하는 메서드
	*
	* 로직
	* 클라이언트ㄱ edit owner를 클릭한다.
	* get요청에 따라 해당 메서드가 호출된다.
	* url 경로 변수에서 추출한 ownerId를 int ownerId에 매핑 그리고 Model 객체를 매개변수로 한다.
	* 해당 ownerId로 findById를 호출하여 실행하고 그 값을 owner에 저장한다.
	* 그 owner를 model에 추가한다.
	* VIEWS_OWNER_CREATE_OR_UPDATE_FORM를 return 한다.
	*
	* */
	@GetMapping("/owners/{ownerId}/edit")
	public String initUpdateOwnerForm(@PathVariable("ownerId") int ownerId, Model model) {
		Owner owner = this.owners.findById(ownerId);
		model.addAttribute(owner);
		return VIEWS_OWNER_CREATE_OR_UPDATE_FORM;
	}

	/*
	* 주요 개념
	*
	* 개요
	* 클라이언트가 edit owner 정보 입력후 서버로 post하는 메서드
	* processUpdateOwnerForm() {}
	*
	* 로직
	* 유효성 검증을 위한 @Vaild, BindingResult 그리고 url 경로 변수에서 추출한 값을 ownerId에 매핑, redirect를 위한 RedirectAttributes를 매개변수로 한다.
	*
	* 바인딩result 에러검증
	* if result가 error가 있을 시
	* 리다이렉트시 error문구를 일시적인 데이터로 저장한다. (세션) ("There was an error in updating the owner.")
	* 그리고 VIEWS_OWNER_CREATE_OR_UPDATE_FORM;를 반환한다.
	*
	* 클라이언트가 새로이 입력한 owner의 Id를 set한다.
	* 그 owner를 db에 저장한다.
	* 리다이렉트시 성공 문구를 전달하기 위해 "message"라는 변수로 플래시 속성으로 추가한다. "Owner Values Updated"
	* /owners/{ownerId}로 redirect한다.
	* */
	@PostMapping("/owners/{ownerId}/edit")
	public String processUpdateOwnerForm(@Valid Owner owner, BindingResult result, @PathVariable("ownerId") int ownerId,
										 RedirectAttributes redirectAttributes) {
		if (result.hasErrors()) {
			redirectAttributes.addFlashAttribute("error", "There was an error in updating the owner.");
			return VIEWS_OWNER_CREATE_OR_UPDATE_FORM;
		}

		owner.setId(ownerId);
		this.owners.save(owner);
		redirectAttributes.addFlashAttribute("message", "Owner Values Updated");
		return "redirect:/owners/{ownerId}";
	}

	/*
	* 주요 개념
	*
	* ModelAndView
	*
	* 개요
	* url 경로변수에서 추출한 ownerId를 ownerId에 매핑하여 ownerId에 해당하는 showOwner를 실행한다.
	*
	* 로직
	* 클라이언트가 /owners/{ownerId}을 get으로 요청시 showOwner를 실행한다.
	* url 경로변수에서 추출한 ownerId를 int ownerId에 매핑하는 매개변수를 가지며, ModelAndView타입 값을 받는다.
	* "owners/ownerDetails"를 매개변수로 하는 new ModelAndView 객체를 생성한다.
	* 클라이언트가 입력한 ownerId를 찾아 owner 객체에 저장한다.
	* 해당 owner 객체를 ModelAndView 객체에 추가한다. addObject
	*  ModelAndView 객체를 반환한다.
	*
	* */
	/**
	 * Custom handler for displaying an owner.
	 *
	 * @param ownerId the ID of the owner to display
	 * @return a ModelMap with the model attributes for the view
	 */
	@GetMapping("/owners/{ownerId}")
	public ModelAndView showOwner(@PathVariable("ownerId") int ownerId) {
		ModelAndView mav = new ModelAndView("owners/ownerDetails");
		Owner owner = this.owners.findById(ownerId);
		mav.addObject(owner);
		return mav;
	}

}
