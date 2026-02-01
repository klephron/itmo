package ru.itmo.zavar.brainstop.profile.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import ru.itmo.zavar.brainstop.common.api.PageResponse;
import ru.itmo.zavar.brainstop.common.api.video.dto.PublicUserVideoDto;
import ru.itmo.zavar.brainstop.common.test.FR;
import ru.itmo.zavar.brainstop.common.test.Layer;
import ru.itmo.zavar.brainstop.common.test.TestLayer;
import ru.itmo.zavar.brainstop.common.test.TestType;
import ru.itmo.zavar.brainstop.profile.client.RemoteVideoProfileApi;
import ru.itmo.zavar.brainstop.profile.client.VideoInternalClient;

import java.util.List;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@FR({"FR-4.1"})
@Layer(TestLayer.BE_UNIT)
class RemoteVideoProfileApiTest {

    @Mock
    private VideoInternalClient videoInternalClient;

    @InjectMocks
    private RemoteVideoProfileApi remoteVideoProfileApi;

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-VIDEO-API-02: Передача параметров сортировки")
    void findUserVideos_withSort() {
        PageResponse<PublicUserVideoDto> response = new PageResponse<>(List.of(), 0, 10, 0);
        when(videoInternalClient.getUserVideos(anyLong(), anyInt(), anyInt(), anyString(), any()))
                .thenReturn(response);

        remoteVideoProfileApi.findUserVideos(
                100L, null, PageRequest.of(0, 10, Sort.by("likesCount").descending())
        );

        verify(videoInternalClient).getUserVideos(eq(100L), eq(0), eq(10), contains("likesCount"), any());
    }
}
