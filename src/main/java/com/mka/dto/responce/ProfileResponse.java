package com.mka.dto.responce;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileResponse {
    private Long id;

    private Long userId;

    private String username;

    private String bio;

    private String avatar;
}
