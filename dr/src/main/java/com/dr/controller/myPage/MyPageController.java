package com.dr.controller.myPage;

import com.dr.dto.myPage.*;
import com.dr.service.myPage.MyPageService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor // 생성자를 자동 생성
@RequestMapping("/myPage") // 기본 url 설정
@SessionAttributes("userNumber") // 모델 속성 값을 세션에 저장
@Slf4j //로그 출력하려고. .
public class MyPageController {

    private final MyPageService myPageService;

    // -- 내 정보 확인하기 --
    @GetMapping("/myPageInformation")
    public String getUserInfo(@SessionAttribute(value = "userNumber", required = false) Long userNumber, Model model) {
        // value = userNumber는 세션에 저장된 값
        // userNumber가 없을 경우 null이 나오고, required = true로 설정하면 "userNumber" 세션이 없을 때 예외 발생
        // 세션에 userNumber가 없는 경우 로그인 페이지로 리다이렉트
        if (userNumber == null) {
            return "redirect:/user/login";
        }

        UserInfoDTO userInfo = myPageService.getUserInfo(userNumber);
        model.addAttribute("userInfo", userInfo);
        //html에서 타임리프로 사용하기위해 userInfo 객체를 model에 추가

        return "myPage/myPageInformation";


    }

    //닉네임 중복 확인
    @PostMapping("/checkNickname")
    public ResponseEntity<Boolean> checkNickname(@RequestBody Map<String, String> requestBody) {
        String nickname = requestBody.get("nickname");
        String currentNickname = requestBody.get("currentNickname"); // 현재 사용자의 닉네임

        // 닉네임 중복 확인
        boolean isAvailable = myPageService.checkNickname(nickname, currentNickname);

        if (isAvailable) {
            return ResponseEntity.ok(false); //중복된 닉네임
        } else {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(true);
            // HTTP 응답 상태를 409(CONFLICT)로 설정하고 true를 반환합// 사용 가능
        }
    }


    @PostMapping("/updateProfile")
    public ResponseEntity<?> updateProfile(
            @SessionAttribute("userNumber") Long userNumber,
            @RequestParam("nickname") String nickname,
            @RequestParam(value = "profileImage", required = false) MultipartFile profileImage) throws IOException {

        // 닉네임 업데이트
        if (nickname != null && !nickname.isEmpty()) {
            myPageService.updateNickname(userNumber, nickname);
        }

        String photoPath = null;

        // 이미지 파일 저장
        if (profileImage != null && !profileImage.isEmpty()) {
            photoPath = myPageService.saveProfileImage(userNumber, profileImage);
        }

        // 새 이미지 경로와 메시지를 포함하여 응답
        Map<String, Object> response = new HashMap<>();
        response.put("photoPath", photoPath);
        response.put("message", "수정이 완료되었습니다.");

        return ResponseEntity.ok(response); // 성공적으로 업데이트된 경우 응답
    }


    // 회원탈퇴 주의사항 페이지
    @GetMapping("/myPageCaution")
    public String showCaution() {
        return "myPage/myPageCaution";
    }

    @PostMapping("/myPageUserDeleted")
    public String deleteUser(@SessionAttribute(value = "userNumber", required = false) Long userNumber,
                             HttpSession session) {

        System.out.println("유저 번호: " + userNumber);

        myPageService.deleteUser(userNumber);

        session.invalidate();

        return "redirect:/myPage/myPageDeleted"; // 세션이 무효화된 후 리다이렉트
    }


//    @GetMapping("/myPageDeleted")
//    public String deletedPage(HttpSession session) {
//
//        session.invalidate();
//        return "myPage/myPageDeleted"; // 뷰 이름 반환
//    }

    @GetMapping("/myPageDeleted")
    public RedirectView deletedPage(HttpSession session){
        session.invalidate();
        return new RedirectView("/main");
    }

//    @GetMapping("/myPageDeleted")
//    public String showDeletedPage(@SessionAttribute(value = "userNumber", required = false) Long userNumber,
//                                  HttpSession session) {
//
//        if (userNumber != null) {
//            System.out.println("유저 번호 (삭제 전): " + userNumber);
//            myPageService.deleteUser(userNumber);
//            session.invalidate();  // 세션 종료
//            System.out.println("세션이 종료되었습니다.");
//
//            // 삭제 후 다른 페이지로 리다이렉트
//            return "redirect:/myPage/myPageDeleted";
//        } else {
//            System.out.println("유저 번호가 없습니다. 이미 세션이 만료되었거나 로그인되지 않은 상태입니다.");
//            return "redirect:/login";
//        }
//    }

    @GetMapping("/myPage/myPageDeleted")
    public String deletedPage(HttpSession session, Model model) {
        if (session.getAttribute("userNumber") == null) {
            return "redirect:/login";  // 세션이 없으면 로그인 페이지로 리다이렉트
        }

        model.addAttribute("message", "계정이 성공적으로 삭제되었습니다.");
        return "myPage/myPageDeleted"; // 삭제 완료 페이지로 이동
    }

    // -- 내정보 포인트 내역 확인 -- //
    @GetMapping("/myPageMyPoint")
    public String getPointHistory(@SessionAttribute(value = "userNumber", required = false) Long userNumber, HttpSession session, Model model) {

        // 세션에 userNumber가 없는 경우 로그인 페이지로 리다이렉트
        if (userNumber == null) {
            return "redirect:/user/login";
        }

        List<PointDetailDTO> pointHistory = myPageService.pointHistory(userNumber);
        model.addAttribute("pointHistory", pointHistory);

        return "myPage/myPageMyPoint";
    }

    // -- 내정보 내가 쓴 레시피 확인 -- //
    @GetMapping("/myPageMyRecipe")
    public String getUserRecipes(@SessionAttribute(value = "userNumber", required = false) Long userNumber, Model model) {

        // 세션에 userNumber가 없는 경우 로그인 페이지로 리다이렉트
        if (userNumber == null) {
            return "redirect:/user/login";
        }

        // 사용자가 쓴 레시피 목록 가져오기
        List<UserRecipeDTO> userRecipes = myPageService.getUserRecipe(userNumber);
        model.addAttribute("userRecipes", userRecipes);

        return "myPage/myPageMyRecipe";
    }

    // -- 내정보 내가 쓴 게시글 목록 확인 -- //
    @GetMapping("/myPageMyPost")
    public String getUserPosts(@SessionAttribute(value = "userNumber", required = false) Long userNumber, Model model) {
        // 세션에 userNumber가 없는 경우 로그인 페이지로 리다이렉트
        if (userNumber == null) {
            return "redirect:/user/login";
        }

        // 사용자가 쓴 게시글 목록 가져오기
        List<UserPostDTO> userPosts = myPageService.getUserPost(userNumber);
        model.addAttribute("userPosts", userPosts);

        return "myPage/myPageMyPost";
    }

    // -- 내정보 찜 목록 확인 --
    @GetMapping("/myPageSteamedList")
    public String getUserSteam(@SessionAttribute(value = "userNumber", required = false) Long userNumber, Model model) {
        if (userNumber == null) {
            return "redirect:/user/login";
        }

        List<UserSteamDTO> userSteamList = myPageService.getUserSteam(userNumber);
        System.out.println("찜 번호 : " + userSteamList);
        model.addAttribute("userSteamList", userSteamList);

        return "myPage/myPageSteamedList";
    }

    // -- 찜 삭제 -- //
    @PostMapping("/steamedDelete")
    public RedirectView deleteSteam(@SessionAttribute(value = "userNumber", required = false) Long userNumber,
                                    @RequestParam(name = "recipeNumber") Long recipeNumber) {

        UserSteamDTO userSteamDTO = new UserSteamDTO();
        userSteamDTO.setUserNumber(userNumber);
        userSteamDTO.setRecipeNumber(recipeNumber);

        myPageService.deleteUserSteam(userSteamDTO);

        return new RedirectView("/myPage/myPageSteamedList");// 목록 페이지로 리다이렉트
    }

    // -- 신고 내역 목록 -- //
    @GetMapping("/myPageMyComplaint")
    public String getSirenList(@SessionAttribute(value = "userNumber", required = false) Long userNumber, Model model) {
        // 세션에 userNumber가 없는 경우 로그인 페이지로 리다이렉트
        if (userNumber == null) {
            return "redirect:/user/login";
        }

        // 사용자의 신고 내역 목록 가져오기
        List<SirenListDTO> sirenList = myPageService.getSirenList(userNumber);
        model.addAttribute("sirenList", sirenList);

        return "myPage/myPageMyComplaint";
    }

    @GetMapping("/myPageCheckedPlease")
    public String checkAttendance(
            @SessionAttribute(value = "userNumber", required = false) Long userNumber,
            @RequestParam(value = "date", required = false) String date,
            Model model) {

        // 현재 날짜를 문자열로 포맷하여 모델에 추가
        String formattedCurrentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        model.addAttribute("currentDate", formattedCurrentDate);

        // 로그인 여부 확인
        if (userNumber == null) {
            model.addAttribute("message", "사용자가 로그인되지 않았습니다.");
            return "error";
        }

        // 날짜가 제공되지 않은 경우 현재 날짜로 설정
        if (date == null) {
            date = formattedCurrentDate;
        }

        System.out.println("User Number: " + userNumber);
        System.out.println("Date: " + date);

        try {
            // 오늘 출석 체크 여부 확인
            boolean isCheckedIn = myPageService.todayCheck(userNumber);

            if (!isCheckedIn) { //0보다 크지않으면...(출석체크 안됬을때)
                // 출석 체크 수행
                myPageService.insertCheck(userNumber, date);

                // 포인트 적립 로직
                PointCheckDTO pointCheckDTO = new PointCheckDTO();
                pointCheckDTO.setUserNumber(userNumber);
                pointCheckDTO.setPointGet(10); // 적립할 포인트 (여기서는 10포인트)
                pointCheckDTO.setPointNote("출석체크"); // 포인트 사유
                pointCheckDTO.setPointDate(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))); // 현재 날짜

                // 포인트 기록 삽입
                myPageService.insertPointRecord(pointCheckDTO);
                model.addAttribute("message", "출석 체크가 완료되었습니다. 10 포인트가 적립되었습니다.");
            } else {
                model.addAttribute("message", "이미 출석 체크가 완료되었습니다.");
            }
        } catch (Exception e) {
            System.err.println("출석 체크 중 오류 발생: " + e.getMessage());
            model.addAttribute("message", "출석 체크 중 오류가 발생했습니다. 다시 시도해 주세요.");
        }

        return "myPage/myPageChecked";
    }
}


