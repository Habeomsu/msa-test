package main.test1.global.exception;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
public class GeneralException extends RuntimeException {

  private String code;

}
