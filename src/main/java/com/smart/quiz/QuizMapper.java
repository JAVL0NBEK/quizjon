package com.smart.quiz;

import com.smart.quiz.dto.SubjectEntity;
import com.smart.quiz.dto.SubjectRequestDto;
import org.mapstruct.Mapping;
import static org.mapstruct.ap.internal.gem.MappingConstantsGem.ComponentModelGem.SPRING;

import com.smart.quiz.dto.OptionResponseDto;
import com.smart.quiz.dto.OptionsEntity;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = SPRING)
public interface QuizMapper {

  List<OptionsEntity> toEntity(List<OptionResponseDto> request);

  SubjectEntity toSubjectEntity(SubjectRequestDto subjectRequestDto);
}
