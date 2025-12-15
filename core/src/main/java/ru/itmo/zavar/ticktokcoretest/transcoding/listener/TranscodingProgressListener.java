package ru.itmo.zavar.ticktokcoretest.transcoding.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import ru.itmo.zavar.ticktokcoretest.common.api.transcoding.TranscodingJobStatus;
import ru.itmo.zavar.ticktokcoretest.common.api.transcoding.TranscodingJobRegistry;
import ru.itmo.zavar.ticktokcoretest.common.api.transcoding.TranscodingProgressEvent;
import ru.itmo.zavar.ticktokcoretest.transcoding.config.TranscodingRabbitConfig;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class TranscodingProgressListener {

    private final TranscodingJobRegistry jobRegistry;

    @RabbitListener(queues = TranscodingRabbitConfig.QUEUE_PROGRESS)
    public void handleProgress(TranscodingProgressEvent event) {
        if (event == null || event.jobId() == null) {
            return;
        }

        Optional<TranscodingJobStatus> current = jobRegistry.get(event.jobId());
        if (current.isPresent()) {
            jobRegistry.update(event.jobId(), st -> {
                st.setStage(event.stage());
                st.setPercent(event.percent());
                st.setMessage(event.message());
            });
        } else {
            log.debug("Received progress for unknown job {}. Creating stub status.", event.jobId());
            jobRegistry.put(event.jobId(), new TranscodingJobStatus(
                    event.stage(),
                    event.percent(),
                    event.message(),
                    null,
                    null
            ));
        }
    }
}
