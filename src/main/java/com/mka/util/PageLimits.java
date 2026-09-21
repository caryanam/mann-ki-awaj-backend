package com.mka.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class PageLimits {
    private PageLimits() {}

    public static Pageable of(int page, int size, Sort sort) {
        if (page < 0 || size < 1) throw new IllegalArgumentException("Page must be non-negative and size must be positive.");
        return PageRequest.of(page, Math.min(size, 100), sort);
    }
}
