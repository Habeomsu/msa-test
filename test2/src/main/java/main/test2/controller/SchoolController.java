package main.test2.controller;

import main.test2.dto.SchoolRequestDto;
import main.test2.service.SchoolService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/school")
public class SchoolController {

    private final SchoolService schoolService;

    public SchoolController(SchoolService schoolService) {
        this.schoolService = schoolService;
    }

    @PostMapping
    public String createSchool(@RequestBody SchoolRequestDto.School schoolDto){
        schoolService.save(schoolDto);
        return "success";
    }
}
