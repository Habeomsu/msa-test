package main.test2.service;

import main.test2.dto.SchoolRequestDto;
import main.test2.entity.School;
import main.test2.repository.SchoolRepository;
import org.springframework.stereotype.Service;

@Service
public class SchoolServiceImpl implements SchoolService {

    private final SchoolRepository schoolRepository;

    public SchoolServiceImpl(SchoolRepository schoolRepository) {
        this.schoolRepository = schoolRepository;
    }

    @Override
    public void save(SchoolRequestDto.School schoolDto) {

        School school = School.builder()
                .name(schoolDto.getName())
                .email(schoolDto.getEmail())
                .build();

        schoolRepository.save(school);
    }
}
