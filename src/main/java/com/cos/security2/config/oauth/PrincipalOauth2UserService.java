package com.cos.security2.config.oauth;

import com.cos.security2.config.auth.PrincipalDetails;
import com.cos.security2.config.auth.userinfo.GoogleUserInfo;
import com.cos.security2.config.auth.userinfo.KakaoUserInfo;
import com.cos.security2.config.auth.userinfo.NaverUserInfo;
import com.cos.security2.config.auth.userinfo.OAuth2UserInfo;
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
        OAuth2UserInfo oAuth2UserInfo = null;

        System.out.println("getClientRegistration : " + userRequest.getClientRegistration().getRegistrationId());
        System.out.println("getAccessToken: " + userRequest.getAccessToken().getTokenValue());

        // 구글로그인 버튼 클릭 -> 구글 로그인 창 -> 로그인을 완료 -> 코드를 리턴(OAuth - Client라이브러리) -> AccessToken 요청
        // userRequest 정보 -> loadUser함수 호출 -> 구글로부터 회원프로필 받아준다.


        OAuth2User oAuth2User = super.loadUser(userRequest);
        System.out.println("getAttributes: " + oAuth2User.getAttributes());

        // 공급자(Provider) 구분
        String provider = userRequest.getClientRegistration().getRegistrationId();

        if ("google".equals(provider)){
            oAuth2UserInfo = new GoogleUserInfo(oAuth2User.getAttributes());
        } else if ("naver".equals(provider)){
            oAuth2UserInfo = new NaverUserInfo(oAuth2User.getAttributes());
        } else if ("kakao".equals(provider)){
            oAuth2UserInfo = new KakaoUserInfo(oAuth2User.getAttributes());
        }  else if (oAuth2UserInfo == null) {
            System.out.println("oAuth2UserInfo is null for "+provider);
        }


        User userEntity = userRepository.findByEmailAndProvider(oAuth2UserInfo.getEmail(), oAuth2UserInfo.getProvider());
        if (userEntity == null){
            System.out.println(provider + " 로그인이 최초입니다.");
            userEntity = User.builder()
                    .username(oAuth2UserInfo.getName())
                    .password(passwordEncoder.encode("임의의 비밀번호"))
                    .email(oAuth2UserInfo.getEmail())
                    .role("ROLL_USER")
                    .provider(oAuth2UserInfo.getProvider())
                    .providerId(oAuth2UserInfo.getProviderId())
                    .build();
            userRepository.save(userEntity);
        }else{
            System.out.println(provider + " 로그인을 이미 한 적이 있습니다.");
        }

        // 회원 가입을 강제로 진행할 예정
        return new PrincipalDetails(userEntity, oAuth2User.getAttributes());
    }
}
