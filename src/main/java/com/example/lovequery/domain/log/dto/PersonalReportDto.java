package com.example.lovequery.domain.log.dto;

import com.example.lovequery.common.EndingType;

import java.util.List;

public record PersonalReportDto(
        Long routeId,
        String routeTitle,
        String characterName,
        int finalAffection,
        EndingType endingType,
        List<PlayStepDto> steps
) {
}
