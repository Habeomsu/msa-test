package main.test1.service;

import main.test1.dto.StudentRequestDto;
import main.test1.dto.StudentResponseDto;
import main.test1.entity.Student;
import main.test1.repository.StudentRepository;
import org.springframework.stereotype.Service;

@Service
public class StudentServiceImpl implements StudentService {

    private final StudentRepository studentRepository;

    public StudentServiceImpl(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    @Override
    public void save(StudentRequestDto.Student studentDto) {
        Student student = Student.builder()
                .email(studentDto.getEmail())
                .name(studentDto.getName())
                .school_id(studentDto.getSchool_id())
                .build();

        studentRepository.save(student);

    }
}
