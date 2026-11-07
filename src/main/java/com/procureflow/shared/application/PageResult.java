package com.procureflow.shared.application;

import java.util.List;

/** Our own page type, so the application layer doesn't depend on Spring Data. */
public record PageResult<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
}