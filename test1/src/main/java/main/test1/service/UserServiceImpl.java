package main.test1.service;

import main.test1.dto.UserRequestDto;
import main.test1.entity.Role;
import main.test1.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import main.test1.entity.User;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    public UserServiceImpl(UserRepository userRepository, BCryptPasswordEncoder bCryptPasswordEncoder) {
        this.userRepository = userRepository;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
    }

    public void join(UserRequestDto.JoinDto joinDto){

        String username = joinDto.getUsername();
        String password = joinDto.getPassword();
        String email = joinDto.getEmail();

        boolean isexist = userRepository.existsByUsername(username);
        if (isexist) {
            throw new IllegalArgumentException("Username already exists");
        }

        User new_user = User.builder()
                .username(username)
                .password(bCryptPasswordEncoder.encode(password))
                .email(email)
                .role(Role.ROLE_USER)
                .build();

        userRepository.save(new_user);

    }

}
