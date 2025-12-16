package ru.itmo.zavar.brainstop.video.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import ru.itmo.zavar.brainstop.common.api.video.VideoMetadataSummary;
import ru.itmo.zavar.brainstop.video.model.FileMetadataEntity;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface FileMetadataToVideoMetadataSummaryMapper {
    VideoMetadataSummary toSummary(FileMetadataEntity meta);
}
