package main.test1.client;

import lombok.extern.slf4j.Slf4j;
import main.test1.dto.SchoolResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@FeignClient("test2")
public interface SchoolFeignClient {

    @GetMapping("/school/{id}")
    SchoolResponseDto.School getSchool(@PathVariable("id") Long id);


}
