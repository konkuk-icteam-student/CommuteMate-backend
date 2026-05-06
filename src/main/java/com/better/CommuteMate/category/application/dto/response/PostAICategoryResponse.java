package com.better.CommuteMate.category.application.dto.response;

import com.better.CommuteMate.category.application.dto.CategoryDto;

import java.util.List;

public record PostAICategoryResponse(
        List<CategoryDto> categories
) {}