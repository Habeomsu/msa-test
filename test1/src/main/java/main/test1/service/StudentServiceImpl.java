package main.test1.service;

import main.test1.client.SchoolFeignClient;
import main.test1.dto.SchoolResponseDto;
import main.test1.dto.StudentRequestDto;
import main.test1.dto.StudentResponseDto;
import main.test1.entity.Student;
import main.test1.global.exception.GeneralException;
import main.test1.repository.StudentRepository;
import org.springframework.stereotype.Service;

@Service
public class StudentServiceImpl implements StudentService {

    private final StudentRepository studentRepository;
    private final SchoolFeignClient schoolFeignClient;

    public StudentServiceImpl(StudentRepository studentRepository, SchoolFeignClient schoolFeignClient) {
        this.studentRepository = studentRepository;
        this.schoolFeignClient = schoolFeignClient;
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

    @Override
    public StudentResponseDto.StudentDto findById(Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new GeneralException("_STUDENT_NOT_FOUND"));
        Long schoolId = student.getSchool_id();
        SchoolResponseDto.School school = schoolFeignClient.getSchool(schoolId);
        StudentResponseDto.StudentDto studentResponseDto = StudentResponseDto.StudentDto.builder()
                .id(student.getId())
                .name(student.getName())
                .email(student.getEmail())
                .school_name(school.getName())
                .school_email(school.getEmail())
                .build();

        return studentResponseDto;
    }
}
