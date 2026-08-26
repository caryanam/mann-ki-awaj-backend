package com.mka.service;

import com.mka.dto.request.MusicTrackApprovalRequest;
import com.mka.dto.request.MusicTrackUploadRequest;
import com.mka.dto.request.UserMusicTrackUploadRequest;
import com.mka.enums.LanguageCode;
import com.mka.enums.MusicMood;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class MusicMoodContractValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void sadExistsAndAllIsNotAStoredMood() {
        assertThat(MusicMood.valueOf("SAD")).isEqualTo(MusicMood.SAD);
        assertThat(MusicMood.values()).extracting(Enum::name).doesNotContain("ALL");
    }

    @Test
    void adminAndApprovalRequireAtLeastOneMood() {
        MusicTrackUploadRequest upload = validAdminUpload();
        upload.setMoods(Set.of());
        MusicTrackApprovalRequest approval = new MusicTrackApprovalRequest();
        approval.setMoods(Set.of());

        assertThat(validator.validate(upload)).extracting(v -> v.getPropertyPath().toString()).contains("moods");
        assertThat(validator.validate(approval)).extracting(v -> v.getPropertyPath().toString()).contains("moods");
    }

    @Test
    void userRequiresOneToThreeNonNullSuggestions() {
        UserMusicTrackUploadRequest request = validUserUpload();
        request.setMoods(Set.of());
        assertThat(validator.validate(request)).extracting(v -> v.getPropertyPath().toString()).contains("moods");

        request.setMoods(Set.of(MusicMood.ROMANTIC, MusicMood.SAD, MusicMood.CALM, MusicMood.FOCUS));
        assertThat(validator.validate(request)).extracting(v -> v.getPropertyPath().toString()).contains("moods");

        LinkedHashSet<MusicMood> withNull = new LinkedHashSet<>();
        withNull.add(MusicMood.CALM);
        withNull.add(null);
        request.setMoods(withNull);
        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    void setContractNormalizesDuplicateSuggestions() {
        UserMusicTrackUploadRequest request = validUserUpload();
        request.setMoods(new LinkedHashSet<>(java.util.List.of(MusicMood.SAD, MusicMood.SAD, MusicMood.CALM)));
        assertThat(request.getMoods()).containsExactly(MusicMood.SAD, MusicMood.CALM);
        assertThat(validator.validate(request)).isEmpty();
    }

    private MusicTrackUploadRequest validAdminUpload() {
        MusicTrackUploadRequest request = new MusicTrackUploadRequest();
        request.setTitle("Song");
        request.setArtistName("Artist");
        request.setLanguage(LanguageCode.HI);
        request.setMoods(Set.of(MusicMood.CALM));
        return request;
    }

    private UserMusicTrackUploadRequest validUserUpload() {
        UserMusicTrackUploadRequest request = new UserMusicTrackUploadRequest();
        request.setTitle("Song");
        request.setArtistName("Artist");
        request.setLanguage(LanguageCode.HI);
        request.setMoods(Set.of(MusicMood.CALM));
        return request;
    }
}
