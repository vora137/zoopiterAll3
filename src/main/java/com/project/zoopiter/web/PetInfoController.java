package com.project.zoopiter.web;

import com.project.zoopiter.domain.common.file.svc.UploadFileSVC;
import com.project.zoopiter.domain.entity.Member;
import com.project.zoopiter.domain.entity.PetInfo;
import com.project.zoopiter.domain.entity.UploadFile;
import com.project.zoopiter.domain.member.svc.MemberSVC;
import com.project.zoopiter.domain.petinfo.svc.PetInfoSVC;
import com.project.zoopiter.web.common.AttachFileType;
import com.project.zoopiter.web.common.LoginMember;
import com.project.zoopiter.web.form.member.DetailForm;
import com.project.zoopiter.web.form.member.ModifyForm;
import com.project.zoopiter.web.form.pet.PetDetailForm;
import com.project.zoopiter.web.form.pet.PetSaveForm;
import com.project.zoopiter.web.form.pet.PetUpdateForm;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Slf4j
@Controller
@RequestMapping("/mypage")
@RequiredArgsConstructor
public class PetInfoController {
  private final PetInfoSVC petInfoSVC;
  private final MemberSVC memberSVC;
  private final UploadFileSVC uploadFileSVC;

  @GetMapping
  public String findAll(Model model, HttpServletRequest request
  ) {
    String userId = null;
    HttpSession session = request.getSession(false);
    if(session != null) {
      LoginMember loginMember = (LoginMember)session.getAttribute(SessionConst.LOGIN_MEMBER);
      userId = loginMember.getUserId();
    }else{
      return "redirect:/login?redirectUrl=/mypage";
    }
    Optional<Member> member = memberSVC.findById(userId);
    model.addAttribute("member",member);

//    String userPw = member.map(Member::getUserPw).orElse("");
    String userEmail = member.map(Member::getUserEmail).orElse("");
    String userNick = member.map(Member::getUserNick).orElse("");

    DetailForm detailForm = new DetailForm();
    detailForm.setUserEmail(userEmail);
    detailForm.setUserNick(userNick);
    detailForm.setUserId(userId);
    model.addAttribute("DetailForm", detailForm);

    return "mypage/mypage_main";
  }

  @ModelAttribute("petInfos")
  public List<PetInfo> getPetInfo(HttpServletRequest request) {
    List<PetInfo> petInfos = null;
    HttpSession session = request.getSession(false);
    if (session != null) {
      Optional<LoginMember> loginMemberOpt = Optional.ofNullable((LoginMember) session.getAttribute(SessionConst.LOGIN_MEMBER));
      if (loginMemberOpt.isPresent()) {
        LoginMember loginMember = loginMemberOpt.get();
        petInfos = petInfoSVC.findAll(loginMember.getUserId());
      }
    }
    return petInfos;
  }

  // 등록 pet_reg
  // 등록양식
  @GetMapping("/petreg")
  public String saveInfo(Model model){
    PetSaveForm petSaveForm = new PetSaveForm();
    model.addAttribute("petSaveForm", petSaveForm);

    return "mypage/mypage_pet_reg";
  }
  // 등록처리
  @PostMapping("/petreg")
  public String save(
      @Valid @ModelAttribute PetSaveForm petSaveForm,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes
  ){
    log.info("petSaveForm={}",petSaveForm);
    // 데이터 검증
    if (bindingResult.hasErrors()){
      log.info("bindingResult={}",bindingResult);
      return "mypage/mypage_pet_reg";
    }

    PetInfo petInfo = new PetInfo();
    petInfo.setPetImg(petSaveForm.getPetImg());
    petInfo.setPetType(petSaveForm.getPetType());
    petInfo.setPetBirth(petSaveForm.getPetBirth());
    petInfo.setPetGender(petSaveForm.getPetGender());
    petInfo.setPetName(petSaveForm.getPetName());
    petInfo.setPetYn(petSaveForm.getPetYn());
    petInfo.setPetDate(petSaveForm.getPetDate());
    petInfo.setPetVac(petSaveForm.getPetVac());
    petInfo.setPetInfo(petSaveForm.getPetInfo());
    petInfo.setUserId(petSaveForm.getUserId());

    List<UploadFile> imageFiles = uploadFileSVC.convert(petSaveForm.getImageFiles(), AttachFileType.F0103);

    String savedPetInfo = petInfoSVC.saveInfo(petInfo,imageFiles);
    redirectAttributes.addAttribute("userId", savedPetInfo);

    log.info("savedPetInfo={}",savedPetInfo);
    return "redirect:/mypage";
  }

  // 조회
  @GetMapping("/{id}/detail")
  public String findInfo(
      @PathVariable("id") Long id,
      Model model
  ){
    Optional<PetInfo> findPetInfo = petInfoSVC.findInfo(id);
    PetInfo petInfo = findPetInfo.orElseThrow(() -> new RuntimeException("PetInfo not found for id: " + id));

    PetDetailForm detailForm = new PetDetailForm();
    detailForm.setUserId(petInfo.getUserId());
    detailForm.setPetNum(petInfo.getPetNum());
    detailForm.setPetImg(petInfo.getPetImg());
    detailForm.setPetType(petInfo.getPetType());
    detailForm.setPetBirth(petInfo.getPetBirth());
    detailForm.setPetGender(petInfo.getPetGender());
    detailForm.setPetName(petInfo.getPetName());
    detailForm.setPetYn(petInfo.getPetYn());
    detailForm.setPetDate(petInfo.getPetDate());
    detailForm.setPetVac(petInfo.getPetVac());
    detailForm.setPetInfo(petInfo.getPetInfo());

    // 첨부파일 조회
    List<UploadFile> imagedFiles = uploadFileSVC.findFilesByCodeWithRid(AttachFileType.F0103, id);
    if(imagedFiles.size()>0){
      log.info("ImagedFiles={}",imagedFiles);
      model.addAttribute("imagedFiles",imagedFiles);
    }

    model.addAttribute("detailForm",detailForm);
    return "mypage/mypage_pet_detail";
  }

  // 수정 pet_modify > 메인으로 이동(보호자정보페이지)
//  int updateInfo (Long PetNum, PetInfo petInfo);
  // 수정양식
  @GetMapping("/{id}/edit")
  public String updateInfo(
      @PathVariable("id") Long id,
      Model model
  ){
    Optional<PetInfo> findPetInfo = petInfoSVC.findInfo(id);
    PetInfo petInfo = findPetInfo.orElseThrow();

    PetUpdateForm petUpdateForm = new PetUpdateForm();
//    updateForm.setUserId(petInfo.getUserId());
    petUpdateForm.setPetNum(petInfo.getPetNum());
    petUpdateForm.setPetImg(petInfo.getPetImg());
    petUpdateForm.setPetType(petInfo.getPetType());
    petUpdateForm.setPetBirth(petInfo.getPetBirth());
    petUpdateForm.setPetGender(petInfo.getPetGender());
    petUpdateForm.setPetName(petInfo.getPetName());
    petUpdateForm.setPetYn(petInfo.getPetYn());
    petUpdateForm.setPetDate(petInfo.getPetDate());
    petUpdateForm.setPetVac(petInfo.getPetVac());
    petUpdateForm.setPetInfo(petInfo.getPetInfo());

    // 첨부파일 조회
    List<UploadFile> imagedFiles = uploadFileSVC.findFilesByCodeWithRid(AttachFileType.F0103, id);
    if(imagedFiles.size()>0){
      log.info("ImagedFiles={}",imagedFiles);
      model.addAttribute("imagedFiles",imagedFiles);
    }

    model.addAttribute("petUpdateForm",petUpdateForm);

    return "mypage/mypage_pet_modify";
  }

  // 수정
  @SneakyThrows
  @PostMapping("/{id}/edit")
  public String update(
      @PathVariable("id") Long petNum,
      @Valid @ModelAttribute PetUpdateForm petUpdateForm,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes
  ){
    // 데이터 검증
    if(bindingResult.hasErrors()){
      log.info("bindingResult={}",bindingResult);
      return "mypage/mypage_pet_modify";
    }

    PetInfo petInfo = new PetInfo();
    petInfo.setPetNum(petNum);

    // 파일첨부
    List<UploadFile> imageFiles = uploadFileSVC.convert(petUpdateForm.getImageFiles(),AttachFileType.F0103);

    if (!imageFiles.isEmpty()) {
      if (imageFiles.get(0).getFileName() != null) {
        petInfo.setPetImg(imageFiles.get(0).getFileName().getBytes()); // 새로운 사진 파일 설정
      }
    } else if (!petUpdateForm.getImageFiles().isEmpty()) {
      // 기존 사진 파일이 있을 경우에만 기존 파일 설정
      petInfo.setPetImg(petUpdateForm.getImageFiles().get(0).getBytes());
    }
    petInfo.setPetImg(petUpdateForm.getPetImg());
    petInfo.setPetType(petUpdateForm.getPetType());
    petInfo.setPetBirth(petUpdateForm.getPetBirth());
    petInfo.setPetGender(petUpdateForm.getPetGender());
    petInfo.setPetName(petUpdateForm.getPetName());
    petInfo.setPetYn(petUpdateForm.getPetYn());
    petInfo.setPetDate(petUpdateForm.getPetDate());
    petInfo.setPetVac(petUpdateForm.getPetVac());
    petInfo.setPetInfo(petUpdateForm.getPetInfo());

    if (!imageFiles.isEmpty()) {
      // 새로운 사진 파일이 있을 경우, 기존 사진 파일 삭제
      if (petInfo.getPetImg() != null && petInfo.getPetImg().length > 0) {
        uploadFileSVC.deleteFile(AttachFileType.F0103, String.valueOf(petInfo.getPetImg()));
      }
      petInfo.setPetImg(imageFiles.get(0).getBytes());
      petInfoSVC.updateInfo(petNum, petInfo, imageFiles);
    } else {
      petInfoSVC.updateInfo(petNum, petInfo);
    }
    redirectAttributes.addAttribute("id",petNum);
    return "redirect:/mypage";
  }

  // 삭제 > 메인으로 이동(보호자정보페이지)
//  int deleteInfo(Long PetNum);
  @GetMapping("{id}/del")
  public String deleteInfo(@PathVariable("id") Long PetNum){
//    log.info("petNum={}",PetNum);
    petInfoSVC.deleteInfo(PetNum,AttachFileType.F0103);

    return "redirect:/mypage";
  }

  // 마이페이지 - 회원정보수정

  // 목록
//  @GetMapping
//  public String findAllMember(){
//    return "mypage/mypage_main";
//  }

  @GetMapping("/mypage/main")
  public String mypageMain(Model model){
    String userId = "myUserId";
    model.addAttribute("userId", userId);

    return "member/mypage_main";
  }

  // 회원수정
  @GetMapping("/memberedit")
  public String editForm(
      Model model,
      HttpServletRequest request
  ) {
    String userId = null;
    HttpSession session = request.getSession(false);
    if(session != null) {
      LoginMember loginMember = (LoginMember)session.getAttribute(SessionConst.LOGIN_MEMBER);
      userId = loginMember.getUserId();
    }
    Optional<Member> optionalMember = memberSVC.findById(userId);

    Member member = optionalMember.get();

    ModifyForm modifyForm = new ModifyForm();
    modifyForm.setUserId(member.getUserId());
    modifyForm.setUserNick(member.getUserNick());

    // modifyForm.setUserPhoto(member.getUserPhoto());
    model.addAttribute("modifyForm",modifyForm);
    model.addAttribute("userEmail",member.getUserEmail());

    return "mypage/mypage_main_modify";

  }

  //rest로 이동
//  // 회원수정 처리
//  @PostMapping("/memberedit")
//  public String edit(
//          @Valid @ModelAttribute ModifyForm modifyForm,
//          BindingResult bindingResult,
//          HttpServletRequest request
//  ) {
//    // 1) 유효성 체크
//    if(bindingResult.hasErrors()){
//      log.info("bindingResult={}",bindingResult);
//      return "mypage/mypage_main_modify";
//    }
//
//    String userId = null;
//    HttpSession session = request.getSession(false);
//    if(session != null) {
//      LoginMember loginMember = (LoginMember)session.getAttribute(SessionConst.LOGIN_MEMBER);
//      userId = loginMember.getUserId();
//    }
//
//    Member member = new Member();
////    member.setUserEmail(modifyForm.getUserEmail());
//    member.setUserNick(modifyForm.getUserNick());
////    member.setUserPhoto(modifyForm.getUserPhoto());
//
//    boolean flag = memberSVC.updateNick(userId,member);
//    if(flag){
//      LoginMember loginMember = (LoginMember) session.getAttribute(SessionConst.LOGIN_MEMBER); //세션에 저장된 LoginMember 객체 가져오기
//      loginMember.setUserNick(modifyForm.getUserNick()); //값 변경
//      session.setAttribute(SessionConst.LOGIN_MEMBER, loginMember); //세션에 변경된 LoginMember 객체 저장
//    };
//
////    redirectAttributes.addAttribute("id", modifyForm.getUserId());
//    return "redirect:/mypage";
//  }
  //   회원 불러오기
  @GetMapping("/{id}/memberdetail")
  public String detail(@PathVariable("id") String userId, Model model){
    Optional<Member> member = memberSVC.findById(userId);

    DetailForm detailForm = new DetailForm();
    detailForm.setUserId(member.get().getUserId());
    detailForm.setUserEmail(member.get().getUserEmail());
    detailForm.setUserNick(member.get().getUserNick());
    detailForm.setUserPw(member.get().getUserPw());

    model.addAttribute("detailForm", detailForm);

    return "mypage/mypage_main";
  }

  // 회원탈퇴
  @GetMapping("/withdraw")
  public void withdraw(HttpServletRequest request){
    String userId = null;
    HttpSession session = request.getSession(false);
    if(session != null) {
      LoginMember loginMember = (LoginMember)session.getAttribute(SessionConst.LOGIN_MEMBER);
      userId = loginMember.getUserId();
    }
    memberSVC.delete(userId);
    request.getSession().invalidate();
  }
}