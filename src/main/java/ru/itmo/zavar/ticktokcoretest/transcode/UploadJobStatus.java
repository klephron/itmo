package ru.itmo.zavar.ticktokcoretest.transcode;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UploadJobStatus {
    private JobStage stage;
    private int percent;
    private String message;
    private String fileName;
    private Long userId;
}

