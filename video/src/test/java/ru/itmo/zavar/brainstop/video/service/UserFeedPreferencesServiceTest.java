package ru.itmo.zavar.brainstop.video.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.itmo.zavar.brainstop.common.test.FR;
import ru.itmo.zavar.brainstop.common.test.Layer;
import ru.itmo.zavar.brainstop.common.test.TestLayer;
import ru.itmo.zavar.brainstop.common.test.TestType;
import ru.itmo.zavar.brainstop.video.model.UserFeedPreferencesEntity;
import ru.itmo.zavar.brainstop.video.repo.UserFeedPreferencesRepository;
import ru.itmo.zavar.brainstop.video.streaming.dto.request.FeedPreferencesUpdateRequest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@FR({"FR-1.2"})
@Layer(TestLayer.BE_UNIT)
class UserFeedPreferencesServiceTest {

    @Mock
    private UserFeedPreferencesRepository repository;

    @InjectMocks
    private UserFeedPreferencesService service;

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-PREF-01: getPreferences возвращает дефолтные значения")
    void getPreferences_returnsDefaults() {
        UserFeedPreferencesEntity entity = new UserFeedPreferencesEntity();
        entity.setUserId(1L);
        entity.setLikeWeight(1.0);
        entity.setFollowWeight(1.0);
        entity.setWatchWeight(1.0);
        entity.setWatchThreshold(0.5);

        when(repository.findById(1L)).thenReturn(Optional.of(entity));

        UserFeedPreferencesService.FeedPreferences result = service.getPreferences(1L);

        assertThat(result.likeWeight()).isEqualTo(1.0);
        assertThat(result.followWeight()).isEqualTo(1.0);
        assertThat(result.watchWeight()).isEqualTo(1.0);
        assertThat(result.watchThreshold()).isEqualTo(0.5);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-PREF-02: getPreferences создает новые preferences")
    void getPreferences_createsNew() {
        when(repository.findById(1L)).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UserFeedPreferencesService.FeedPreferences result = service.getPreferences(1L);

        assertThat(result).isNotNull();
        verify(repository).save(any());
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-PREF-03: updatePreferences обновляет likeWeight")
    void updatePreferences_updatesLikeWeight() {
        UserFeedPreferencesEntity entity = new UserFeedPreferencesEntity();
        entity.setUserId(1L);
        entity.setLikeWeight(1.0);

        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        FeedPreferencesUpdateRequest request = new FeedPreferencesUpdateRequest(2.0, null, null, null);

        UserFeedPreferencesEntity result = service.updatePreferences(1L, request);

        assertThat(result.getLikeWeight()).isEqualTo(2.0);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-PREF-04: updatePreferences обновляет followWeight")
    void updatePreferences_updatesFollowWeight() {
        UserFeedPreferencesEntity entity = new UserFeedPreferencesEntity();
        entity.setUserId(1L);
        entity.setFollowWeight(1.0);

        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        FeedPreferencesUpdateRequest request = new FeedPreferencesUpdateRequest(null, 1.5, null, null);

        UserFeedPreferencesEntity result = service.updatePreferences(1L, request);

        assertThat(result.getFollowWeight()).isEqualTo(1.5);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-PREF-05: updatePreferences обновляет watchWeight")
    void updatePreferences_updatesWatchWeight() {
        UserFeedPreferencesEntity entity = new UserFeedPreferencesEntity();
        entity.setUserId(1L);
        entity.setWatchWeight(1.0);

        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        FeedPreferencesUpdateRequest request = new FeedPreferencesUpdateRequest(null, null, 0.5, null);

        UserFeedPreferencesEntity result = service.updatePreferences(1L, request);

        assertThat(result.getWatchWeight()).isEqualTo(0.5);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-PREF-06: updatePreferences обновляет watchThreshold")
    void updatePreferences_updatesWatchThreshold() {
        UserFeedPreferencesEntity entity = new UserFeedPreferencesEntity();
        entity.setUserId(1L);
        entity.setWatchThreshold(0.5);

        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        FeedPreferencesUpdateRequest request = new FeedPreferencesUpdateRequest(null, null, null, 0.7);

        UserFeedPreferencesEntity result = service.updatePreferences(1L, request);

        assertThat(result.getWatchThreshold()).isEqualTo(0.7);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-PREF-07: updatePreferences не изменяет null поля")
    void updatePreferences_doesNotChangeNullFields() {
        UserFeedPreferencesEntity entity = new UserFeedPreferencesEntity();
        entity.setUserId(1L);
        entity.setLikeWeight(1.0);
        entity.setFollowWeight(1.0);

        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        FeedPreferencesUpdateRequest request = new FeedPreferencesUpdateRequest(2.0, null, null, null);

        UserFeedPreferencesEntity result = service.updatePreferences(1L, request);

        assertThat(result.getFollowWeight()).isEqualTo(1.0);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-PREF-08: updatePreferences создает preferences если не существует")
    void updatePreferences_createsIfNotExists() {
        when(repository.findById(1L)).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        FeedPreferencesUpdateRequest request = new FeedPreferencesUpdateRequest(2.0, null, null, null);

        UserFeedPreferencesEntity result = service.updatePreferences(1L, request);

        assertThat(result.getUserId()).isEqualTo(1L);
        assertThat(result.getLikeWeight()).isEqualTo(2.0);
    }
}
