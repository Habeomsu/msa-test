package main.test1.service;

import main.test1.dto.StudentRequestDto;
import main.test1.dto.StudentResponseDto;
import main.test1.entity.Student;

public interface StudentService {

    public void save(StudentRequestDto.Student studentDto);
    public StudentResponseDto.StudentDto findById(Long id);

}
