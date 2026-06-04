package com.campusgear.demo.dto;

import com.fasterxml.jackson.annotation.JsonInclude;


@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserAuthResponseDTO(
        String email,
        String role,
        String firstName,
        String lastName
) {}