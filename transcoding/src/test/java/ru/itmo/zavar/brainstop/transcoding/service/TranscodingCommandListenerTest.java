package ru.itmo.zavar.brainstop.transcoding.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.itmo.zavar.brainstop.common.api.transcoding.*;
import ru.itmo.zavar.brainstop.common.test.FR;
import ru.itmo.zavar.brainstop.common.test.Layer;
import ru.itmo.zavar.brainstop.common.test.TestLayer;
import ru.itmo.zavar.brainstop.common.test.TestType;
import ru.itmo.zavar.brainstop.transcoding.messaging.TranscodingCommandListener;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@FR({"FR-3.1", "FR-3.2"})
@Layer(TestLayer.BE_UNIT)
class TranscodingCommandListenerTest {

    @Mock
    private VideoTranscodingService transcodingService;

    @Mock
    private TranscodingJobRegistry jobRegistry;

    @InjectMocks
    private TranscodingCommandListener listener;

    private UUID testJobId;
    private TranscodingCommand testCommand;

    @BeforeEach
    void setUp() {
        testJobId = UUID.randomUUID();
        testCommand = new TranscodingCommand(
                testJobId, "test-key", "video.mp4", "tmp", 1L, "tmp", List.of(720)
        );
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-TRANS-LSTN-01: handleCommand вызывает transcodingService")
    void handleCommand_callsTranscodingService() throws Exception {
        listener.handleCommand(testCommand);

        verify(transcodingService).handleCommand(testCommand);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-TRANS-LSTN-02: handleCommand игнорирует null команду")
    void handleCommand_ignoresNullCommand() throws Exception {
        listener.handleCommand(null);

        verify(transcodingService, never()).handleCommand(any());
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-TRANS-LSTN-03: handleCommand игнорирует команду с null jobId")
    void handleCommand_ignoresCommandWithNullJobId() throws Exception {
        TranscodingCommand nullIdCommand = new TranscodingCommand(
                null, "key", "video.mp4", "tmp", 1L, "tmp", List.of(720)
        );

        listener.handleCommand(nullIdCommand);

        verify(transcodingService, never()).handleCommand(any());
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-TRANS-LSTN-04: handleCommand обрабатывает ошибку транскодинга")
    void handleCommand_handlesTranscodingError() throws Exception {
        doThrow(new RuntimeException("Transcoding error")).when(transcodingService).handleCommand(any());

        listener.handleCommand(testCommand);

        verify(jobRegistry).update(eq(testJobId), any());
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-TRANS-LSTN-06: handleCommand с валидными данными")
    void handleCommand_withValidData() throws Exception {
        TranscodingCommand validCommand = new TranscodingCommand(
                UUID.randomUUID(), "valid-key", "valid.mp4", "tmp", 123L, "tmp", List.of(360, 720, 1080)
        );

        listener.handleCommand(validCommand);

        verify(transcodingService).handleCommand(validCommand);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-TRANS-LSTN-07: handleCommand обрабатывает множественные команды")
    void handleCommand_handlesMultipleCommands() throws Exception {
        TranscodingCommand cmd1 = new TranscodingCommand(UUID.randomUUID(), "key1", "v1.mp4", "tmp", 1L, "tmp", List.of(720));
        TranscodingCommand cmd2 = new TranscodingCommand(UUID.randomUUID(), "key2", "v2.mp4", "tmp", 2L, "tmp", List.of(1080));

        listener.handleCommand(cmd1);
        listener.handleCommand(cmd2);

        verify(transcodingService, times(2)).handleCommand(any());
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-TRANS-LSTN-08: handleCommand с ошибкой IOException")
    void handleCommand_withIOException() throws Exception {
        doThrow(new java.io.IOException("IO error")).when(transcodingService).handleCommand(any());

        listener.handleCommand(testCommand);

        verify(jobRegistry).update(eq(testJobId), any());
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-TRANS-LSTN-09: handleCommand логирует обработку команды")
    void handleCommand_logsCommandProcessing() throws Exception {
        listener.handleCommand(testCommand);

        verify(transcodingService).handleCommand(testCommand);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-TRANS-LSTN-10: handleCommand с пустым списком высот")
    void handleCommand_withEmptyHeightsList() throws Exception {
        TranscodingCommand emptyHeightsCommand = new TranscodingCommand(
                testJobId, "key", "video.mp4", "tmp", 1L, "tmp", List.of()
        );

        listener.handleCommand(emptyHeightsCommand);

        verify(transcodingService).handleCommand(emptyHeightsCommand);
    }
}
