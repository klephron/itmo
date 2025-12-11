package ru.itmo.zavar.ticktokcoretest.common.api.transcoding;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TranscodingJobStatus {
    private TranscodingJobStage stage;
    private int percent;
    private String message;
    private String fileName;
    private Long userId;
}
