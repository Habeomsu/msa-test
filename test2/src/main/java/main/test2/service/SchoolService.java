package main.test2.service;

import main.test2.dto.SchoolRequestDto;
import main.test2.dto.SchoolResponseDto;

public interface SchoolService {

    public void save(SchoolRequestDto.School schoolDto);
    public SchoolResponseDto.SchoolDto findById(Long id);

}
