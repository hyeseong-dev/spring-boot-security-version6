package com.cos.security2.config.oauth;

import com.cos.security2.model.User;
import com.cos.security2.repository.UserRepository;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.Buffer;

@Service
public class OAuthService {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    @Value("${kakao.client.id}")
    private String KAKAO_CLIENT_ID;

    @Value("${kakao.client.redirect.url}")
    private String KAKAO_REDIRECT_URL;

    private String KAKAO_TOKEN_BASE_URL = "https://kauth.kakao.com/oauth/token";

    private String KAKAO_USER_ME_URL = "https://kapi.kakao.com/v2/user/me";

    public String getKakaoAccessToken(String authorizeCode) {
        String tokenResponse = sendHttpRequest(KAKAO_TOKEN_BASE_URL, buildPostParams(authorizeCode), "POST");
        JsonObject tokenObject = parseJson(tokenResponse);
        String accessToken = tokenObject.get("access_token").getAsString();

        String userResponse = sendHttpRequest(KAKAO_USER_ME_URL, "Bearer " + accessToken, "GET");
        User user = processKakaoUser(parseJson(userResponse));

        return accessToken;
    }

    private String buildPostParams(String authorizeCode) {
        return "grant_type=authorization_code" +
                "&client_id=" + KAKAO_CLIENT_ID +
                "&redirect_uri=" + KAKAO_REDIRECT_URL +
                "&code=" + authorizeCode;
    }

    private String sendHttpRequest(String urlString, String paramsOrToken, String requestMethod) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(requestMethod);
            conn.setDoOutput(true);

            if ("POST".equals(requestMethod)) {
                // try 구문은 catch, finally를 사용하지 않아도 자동으로 리소스를 닫아준다.
                try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream()))) {
                    writer.write(paramsOrToken);
                    writer.flush();
                }
            } else if ("GET".equals(requestMethod)) {
                conn.setRequestProperty("Authorization", paramsOrToken);
            }

            return getResponse(conn);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private String getResponse(HttpURLConnection conn) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String line;
            StringBuilder response = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        }
    }

    private JsonObject parseJson(String json) {
        return JsonParser.parseString(json).getAsJsonObject();
    }

    private User processKakaoUser(JsonElement userInfoElement) {
        JsonObject properties = userInfoElement.getAsJsonObject().get("properties").getAsJsonObject();
        JsonObject kakaoAccount = userInfoElement.getAsJsonObject().get("kakao_account").getAsJsonObject();

        String username = properties.get("nickname").getAsString();
        String email = kakaoAccount.get("email").getAsString();
        String profileImage = properties.get("profile_image").getAsString(); // 선택적 사용
        String providerId = userInfoElement.getAsJsonObject().get("id").getAsString();
        String provider = "kakao";

        User userEntity = userRepository.findByEmailAndProvider(email, provider);
        if (userEntity == null) {
            userEntity = User.builder()
                    .username(username)
                    .password(passwordEncoder.encode("임의의 비밀번호"))
                    .email(email)
                    .role("ROLE_USER")
                    .provider(provider)
                    .providerId(providerId)
                    .build();
            userRepository.save(userEntity);
            System.out.println(provider + " 로그인이 최초입니다.");
        } else {
            System.out.println(provider + " 로그인을 이미 한 적이 있습니다.");
        }

        return userEntity;
    }
}