package com.mka.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;

public record MusicPageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last) {

    public static <T> MusicPageResponse<T> from(Page<T> source) {
        return new MusicPageResponse<>(source.getContent(), source.getNumber(), source.getSize(),
                source.getTotalElements(), source.getTotalPages(), source.isFirst(), source.isLast());
    }
}
