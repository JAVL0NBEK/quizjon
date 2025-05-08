package com.smart.quiz;

import com.smart.quiz.dto.SubjectEntity;
import com.smart.quiz.dto.SubjectRequestDto;
import static org.mapstruct.ap.internal.gem.MappingConstantsGem.ComponentModelGem.SPRING;

import com.smart.quiz.dto.OptionResponseDto;
import com.smart.quiz.dto.OptionsEntity;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = SPRING)
public interface QuizMapper {

  List<OptionsEntity> toEntity(List<OptionResponseDto> request);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "users", ignore = true)
  SubjectEntity toSubjectEntity(SubjectRequestDto subjectRequestDto);
}
