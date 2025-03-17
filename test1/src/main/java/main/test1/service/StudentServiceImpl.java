package main.test1.service;


import lombok.extern.slf4j.Slf4j;
import main.test1.client.SchoolFeignClient;
import main.test1.dto.SchoolResponseDto;
import main.test1.dto.StudentRequestDto;
import main.test1.dto.StudentResponseDto;
import main.test1.entity.Student;
import main.test1.global.exception.GeneralException;
import main.test1.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class StudentServiceImpl implements StudentService {

    private final StudentRepository studentRepository;
    private final SchoolFeignClient schoolFeignClient;
    private final RedisTemplate<String, SchoolResponseDto.School> redisTemplate;
    public StudentServiceImpl(StudentRepository studentRepository, SchoolFeignClient schoolFeignClient,
                              RedisTemplate<String, SchoolResponseDto.School> redisTemplate) {
        this.studentRepository = studentRepository;
        this.schoolFeignClient = schoolFeignClient;
        this.redisTemplate = redisTemplate;
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
        String cacheKey = "school:" + schoolId;
        SchoolResponseDto.School school = redisTemplate.opsForValue().get(cacheKey);
        if (school == null) {
            // 캐시에 없으면 Feign Client를 통해 학교 서비스 호출 후 캐싱
            log.info("School not in cache");
            school = schoolFeignClient.getSchool(schoolId);
            redisTemplate.opsForValue().set(cacheKey, school);
            log.info("School inserted");
        }
        log.info("School in cache");
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
