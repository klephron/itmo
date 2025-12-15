package ru.itmo.zavar.ticktokcoretest.common.api.transcoding;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TranscodingJobStatus {
    private TranscodingJobStage stage;
    private int percent;
    private String message;
    private String fileName;
    private Long userId;
}
