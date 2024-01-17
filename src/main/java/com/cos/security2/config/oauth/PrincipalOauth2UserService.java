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

public class PrincipalOauth2UserService extends DefaultOAuth2UserService {

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest){
        //accessToken으로 서드파티에 요청하여 사용자 정보를 얻어옴

        System.out.println("getClientRegistration : " + userRequest.getClientRegistration());
        System.out.println("getAccessToken: " + userRequest.getAccessToken().getTokenValue());
        // 구글로그인 버튼 클릭 -> 구글 로그인 창 -> 로그인을 완료 -> 코드를 리턴(OAuth - Client라이브러리) -> AccessToken 요청
        // userRequest 정보 -> loadUser함수 호출 -> 구글로부터 회원프로필 받아준다.

        OAuth2User oAuth2User = super.loadUser(userRequest);
        System.out.println("getAttributes: " + oAuth2User.getAttributes());

        String provider = userRequest.getClientRegistration().getClientId(); // google
        String providerId = oAuth2User.getAttribute("sub");
        String username = provider+ "_" + providerId; // google_109742856182916427686
        String password = passwordEncoder.encode("겟인데어");
        String email = oAuth2User.getAttribute("email");
        String role = "ROLL_USER";

        User userEntity = userRepository.findByUsername(username);
        if (userEntity == null){
            System.out.println("구글 로그인이 최초입니다.");
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
            System.out.println("구글 로그인을 이미 한적이 있습니다.");
        }


        // 회원 가입을 강제로 진행할 예정
        return new PrincipalDetails(userEntity, oAuth2User.getAttributes());
    }
}
