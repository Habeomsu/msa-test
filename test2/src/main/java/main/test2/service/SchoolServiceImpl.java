package main.test2.service;

import main.test2.dto.SchoolRequestDto;
import main.test2.dto.SchoolResponseDto;
import main.test2.entity.School;
import main.test2.global.exception.GeneralException;
import main.test2.repository.SchoolRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

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

    @Override
    public SchoolResponseDto.SchoolDto findById(Long id) {
        Optional<School> school = schoolRepository.findById(id);
        SchoolResponseDto.SchoolDto schoolDto = SchoolResponseDto.SchoolDto.builder()
                .id(school.get().getId())
                .name(school.get().getName())
                .email(school.get().getEmail())
                .build();
        return schoolDto;
    }

    @Override
    public void update(Long id,SchoolRequestDto.School schoolDto) {
        School school = schoolRepository.findById(id).orElseThrow(
                ()-> new GeneralException("_NOT_FOUND_SCHOOL")
        );
        school.setName(schoolDto.getName());
        school.setEmail(schoolDto.getEmail());

        schoolRepository.save(school);

    }
}
