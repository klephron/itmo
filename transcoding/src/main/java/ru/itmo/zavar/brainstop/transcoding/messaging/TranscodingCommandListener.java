package ru.itmo.zavar.brainstop.transcoding.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import ru.itmo.zavar.brainstop.common.api.transcoding.TranscodingCommand;
import ru.itmo.zavar.brainstop.common.api.transcoding.TranscodingJobRegistry;
import ru.itmo.zavar.brainstop.common.api.transcoding.TranscodingJobStage;
import ru.itmo.zavar.brainstop.transcoding.config.RabbitConfig;
import ru.itmo.zavar.brainstop.transcoding.service.VideoTranscodingService;

@Slf4j
@Component
@RequiredArgsConstructor
public class TranscodingCommandListener {

    private final VideoTranscodingService transcodingService;
    private final TranscodingJobRegistry jobRegistry;

    @RabbitListener(queues = RabbitConfig.QUEUE_COMMAND)
    public void handleCommand(TranscodingCommand command) {
        if (command == null || command.jobId() == null) {
            return;
        }
        try {
            log.info("Received transcoding command for job {}", command.jobId());
            transcodingService.handleCommand(command);
        } catch (Exception e) {
            log.error("Failed to process transcoding command for job {}", command.jobId(), e);
            jobRegistry.update(command.jobId(), st -> {
                st.setStage(TranscodingJobStage.ERROR);
                st.setPercent(100);
                st.setMessage("Ошибка транскодинга: " + e.getMessage());
            });
        }
    }
}
