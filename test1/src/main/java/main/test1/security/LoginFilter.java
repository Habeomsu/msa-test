package main.test1.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import main.test1.dto.CustomUserDetails;
import main.test1.dto.UserRequestDto;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Iterator;

public class LoginFilter extends UsernamePasswordAuthenticationFilter {

    private final AuthenticationManager authenticationManager;


    public LoginFilter(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
        setFilterProcessesUrl("/auth/login");
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException {

        UserRequestDto.LoginDto loginDTO = new UserRequestDto.LoginDto();

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            ServletInputStream inputStream = request.getInputStream();
            String messageBody = StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
            loginDTO = objectMapper.readValue(messageBody,UserRequestDto.LoginDto.class);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        String username = loginDTO.getUsername();
        String password = loginDTO.getPassword();

        // 스프링 시큐리티에서 username과 password를 검증하기 위해서는 token에 담아야 함
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(username, password,
                null);

        // token에 담은 검증을 위한 AuthenticationManager로 전달
        return authenticationManager.authenticate(authToken);
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain,
                                            Authentication authentication) throws IOException {

        // UserDetails
        CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();
        String username = customUserDetails.getUsername();

        // role 가지고 오기
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        Iterator<? extends GrantedAuthority> iterator = authorities.iterator();
        GrantedAuthority auth = iterator.next();

        String role = auth.getAuthority();

        // 로그인 성공 시 응답 바디에 성공 메시지 추가
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json");

        // 로그인 성공 메시지 객체
        String successMessage = "{\"message\": \"Login successful\", \"username\": \"" + username + "\"}";

        // 응답 바디에 성공 메시지 전달
        response.getWriter().write(successMessage);


    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                              AuthenticationException failed) throws IOException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.getWriter().write("{\"message\":\"Authentication failed\"}");

    }


}
