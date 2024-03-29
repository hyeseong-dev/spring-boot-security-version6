package com.cos.security2.controller;

import com.cos.security2.config.auth.PrincipalDetails;
import com.cos.security2.config.oauth.OAuthService;
import com.cos.security2.model.User;
import com.cos.security2.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class IndexController {

    @Autowired
    private OAuthService oAuthService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    // localhost:8080/
    // localhost:8080
    @GetMapping({"", "/"})
    public String index(){
        // 머스터쉬 기본폴더 경로 : src/main/resources/
        // 뷰 리졸버 설정 : templates(prefix), .mustache(suffix) 생략 가능.
        return "index"; // src/main/resources/templates/index.mustache
    }


    @GetMapping("/kakao/callback")
    public String kakaoCallback(@RequestParam("code") String code) throws Exception{
        String access_token = oAuthService.getKakaoAccessToken(code);
        System.out.println("컨트롤러 액세스 토큰 : "+access_token);
        return "redirect:/";
    }

    @GetMapping("/user")
    public @ResponseBody String user(@AuthenticationPrincipal PrincipalDetails principalDetails){
        System.out.println("principalDetails : " + principalDetails.getUser());
        return "user";
    }

    @GetMapping("/admin")
    public @ResponseBody String admin(){
        return "admin";
    }

    @GetMapping("/manager")
    public @ResponseBody String manager(){
        return "manager";
    }

    //   스프링 시큐리티 해당 주소를 낚아챔!! 하지만 SecurityConfig 모듈 명세 이후 정상 출력됨.
    @GetMapping("/loginForm")
    public String loginForm(Model model, HttpServletRequest request){
        setCsrfThenGiveToView(model, request);
        return "loginForm";
    }

    @GetMapping("/joinForm")
    public String joinForm(Model model, HttpServletRequest request) {
        setCsrfThenGiveToView(model, request);
        return "joinForm";
    }

    @PostMapping("/login")
    public String login(){
        return "redirect:/";
    }

    @PostMapping("/join")
    public String join(User user){
        user.setPassword(bCryptPasswordEncoder.encode(user.getPassword()));
        user.setRole("ROLE_USER");
        userRepository.save(user);
        System.out.println(user);
        return "redirect:/loginForm";
    }

    @GetMapping("/joinProc")
    public @ResponseBody String joinProc(){
        return "회원가입 완료됨";
    }

    private static void setCsrfThenGiveToView(Model model, HttpServletRequest request) {
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken != null) {
            model.addAttribute("_csrf", csrfToken);
        }
    }

    @Secured("ROLE_ADMIN")
    @GetMapping("/info")
    public @ResponseBody String info(){
        return "개인정보";
    }


}
