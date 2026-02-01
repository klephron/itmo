package ru.itmo.zavar.brainstop.transcoding.jobs;

import lombok.NoArgsConstructor;
import ru.itmo.zavar.brainstop.common.api.transcoding.TranscodingJobStatus;

@NoArgsConstructor
public class UploadJobStatus extends TranscodingJobStatus {
    public UploadJobStatus(TranscodingJobStatus other) {
        super(other.getStage(), other.getPercent(), other.getMessage(), other.getFileName(), other.getUserId());
    }
}

