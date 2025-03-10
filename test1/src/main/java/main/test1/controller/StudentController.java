package main.test1.controller;

import main.test1.dto.StudentRequestDto;
import main.test1.dto.StudentResponseDto;
import main.test1.entity.Student;
import main.test1.service.StudentService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/student")
public class StudentController {


    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @PostMapping
    public String createStudent(@RequestBody StudentRequestDto.Student studentDto) {

        studentService.save(studentDto);
        return "ok";
    }

    @GetMapping("/{id}")
    public StudentResponseDto.StudentDto getStudentById(@PathVariable Long id) {


        return studentService.findById(id);
    }

}
