package com.cos.security2.config.oauth;

import com.cos.security2.config.auth.PrincipalDetails;
import com.cos.security2.model.User;
import com.cos.security2.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.sql.SQLOutput;
import java.util.Map;

public class PrincipalOauth2UserService extends DefaultOAuth2UserService {

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest){
        //accessToken으로 서드파티에 요청하여 사용자 정보를 얻어옴

        System.out.println("getClientRegistration : " + userRequest.getClientRegistration().getRegistrationId());
        System.out.println("getAccessToken: " + userRequest.getAccessToken().getTokenValue());

        // 구글로그인 버튼 클릭 -> 구글 로그인 창 -> 로그인을 완료 -> 코드를 리턴(OAuth - Client라이브러리) -> AccessToken 요청
        // userRequest 정보 -> loadUser함수 호출 -> 구글로부터 회원프로필 받아준다.

        OAuth2User oAuth2User = super.loadUser(userRequest);
        System.out.println("getAttributes: " + oAuth2User.getAttributes());

        // 공급자(Provider) 구분
        String provider = userRequest.getClientRegistration().getRegistrationId();
        String providerId = null;
        String email = null;
        String name = null;

        if ("google".equals(provider)){
            providerId = oAuth2User.getAttribute("sub");
            email = oAuth2User.getAttribute("email");
            name = oAuth2User.getAttribute("name");
        } else if ("naver".equals(provider)){
            Map<String, Object> response = oAuth2User.getAttribute("response");
            providerId = (String) response.get("id");
            email = (String) response.get("email");
            name = (String) response.get("name");
        }


        String username = provider+ "_" + providerId; // google_109742856182916427686
        String password = passwordEncoder.encode("임의의 비밀번호");
        String role = "ROLL_USER";

        User userEntity = userRepository.findByUsername(username);
        if (userEntity == null){
            System.out.println(provider + " 로그인이 최초입니다.");
            userEntity = User.builder()
                    .username(username)
                    .password(password)
                    .email(email)
                    .role(role)
                    .provider(provider)
                    .providerId(providerId)
                    .build();
            userRepository.save(userEntity);
        }else{
            System.out.println(provider + " 로그인을 이미 한 적이 있습니다.");
        }


        // 회원 가입을 강제로 진행할 예정
        return new PrincipalDetails(userEntity, oAuth2User.getAttributes());
    }
}
