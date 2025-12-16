package ru.itmo.zavar.brainstop.transcoding.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.itmo.zavar.brainstop.common.api.transcoding.TranscodedRendition;
import ru.itmo.zavar.brainstop.common.api.transcoding.*;
import ru.itmo.zavar.brainstop.transcoding.messaging.TranscodingEventsProducer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class VideoTranscodingService implements VideoTranscoder {

    @Value("${transcoder.ffmpeg-path:ffmpeg}")
    private String ffmpegPath;

    @Value("${transcoder.ffprobe-path:ffprobe}")
    private String ffprobePath;

    private final TranscodingJobRegistry jobRegistry;
    private final TranscodingEventsProducer eventsProducer;

    public VideoTranscodingService(TranscodingJobRegistry jobRegistry, TranscodingEventsProducer eventsProducer) {
        this.jobRegistry = jobRegistry;
        this.eventsProducer = eventsProducer;
    }

    @Override
    public List<TranscodedRendition> transcodeToMp4WithProgress(Path inputTempFile,
                                                                UUID jobId,
                                                                List<Integer> targetHeights) throws IOException, InterruptedException {

        double totalSeconds = normalizeDuration(probeDurationSeconds(inputTempFile));
        int sourceHeight = probeHeight(inputTempFile);
        List<Integer> heights = normalizeTargets(targetHeights, sourceHeight);
        if (heights.isEmpty()) {
            throw new IllegalStateException("No target heights available for transcoding");
        }

        log.info("Transcoding start: input={}, totalSeconds={}, targetHeights={}, sourceHeight={}",
                inputTempFile, totalSeconds, heights, sourceHeight);

        List<TranscodedRendition> outputs = new ArrayList<>();

        for (int i = 0; i < heights.size(); i++) {
            int height = heights.get(i);
            Path output = Files.createTempFile("upload-" + height + "p-", ".mp4");
            transcodeSingleRendition(inputTempFile, output, height, totalSeconds, jobId, i, heights.size());
            long size = Files.size(output);
            outputs.add(new TranscodedRendition(height, output, size));
            log.info("Rendition {}p ready, size={} bytes", height, size);
        }

        this.jobRegistry.update(jobId, st -> {
            st.setStage(TranscodingJobStage.TRANSCODING);
            st.setPercent(100);
            st.setMessage("Перекодирование: 100%");
        });
        this.eventsProducer.sendProgress(new TranscodingProgressEvent(jobId, 100, "Перекодирование: 100%", TranscodingJobStage.TRANSCODING, java.time.Instant.now()));

        return outputs;
    }

    private double probeDurationSeconds(Path input) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(
                this.ffprobePath,
                "-v", "error",
                "-show_entries", "format=duration",
                "-of", "default=noprint_wrappers=1:nokey=1",
                input.toString()
        );
        pb.redirectErrorStream(true);
        Process p = pb.start();

        String line;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            line = reader.readLine();
        }
        int code = p.waitFor();
        if (code != 0 || line == null) {
            log.warn("ffprobe returned code {} and line='{}'", code, line);
            return -1;
        }
        try {
            return Double.parseDouble(line.trim());
        } catch (NumberFormatException e) {
            log.warn("Cannot parse ffprobe duration '{}'", line);
            return -1;
        }
    }

    private int probeHeight(Path input) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(
                this.ffprobePath,
                "-v", "error",
                "-select_streams", "v:0",
                "-show_entries", "stream=height",
                "-of", "csv=p=0",
                input.toString()
        );
        pb.redirectErrorStream(true);
        Process p = pb.start();

        String line;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            line = reader.readLine();
        }
        int code = p.waitFor();
        if (code != 0 || line == null || line.isBlank()) {
            log.warn("ffprobe height returned code {} and line='{}'", code, line);
            return -1;
        }
        try {
            return Integer.parseInt(line.trim());
        } catch (NumberFormatException e) {
            log.warn("Cannot parse ffprobe height '{}'", line);
            return -1;
        }
    }

    private void transcodeSingleRendition(Path inputTempFile,
                                          Path output,
                                          int height,
                                          double totalSeconds,
                                          UUID jobId,
                                          int renditionIndex,
                                          int totalRenditions) throws IOException, InterruptedException {

        ProcessBuilder pb = new ProcessBuilder(
                this.ffmpegPath,
                "-y",
                "-i", inputTempFile.toString(),
                "-vf", "scale=-2:" + height,
                "-c:v", "libx264",
                "-preset", "veryfast",
                "-crf", "23",
                "-c:a", "aac",
                "-b:a", "128k",
                "-progress", "pipe:1",
                "-nostats",
                output.toString()
        );
        pb.redirectErrorStream(true);
        Process p = pb.start();

        long started = System.currentTimeMillis();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String line;
            double currentSeconds = 0.0;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("out_time=")) {
                    String timeStr = line.substring("out_time=".length());
                    currentSeconds = parseFfmpegTimeToSeconds(timeStr);
                    pushProgress(jobId, totalSeconds, currentSeconds, renditionIndex, totalRenditions, height);
                } else if (line.startsWith("out_time_ms=") && currentSeconds == 0.0) {
                    try {
                        long ms = Long.parseLong(line.substring("out_time_ms=".length()));
                        currentSeconds = ms / 1_000_000.0;
                        pushProgress(jobId, totalSeconds, currentSeconds, renditionIndex, totalRenditions, height);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }

        int code = p.waitFor();
        long took = System.currentTimeMillis() - started;

        if (code != 0) {
            log.error("ffmpeg failed with code {} in {} ms ({}p)", code, took, height);
            throw new IllegalStateException("ffmpeg failed with code " + code + " for " + height + "p");
        }
    }

    private void pushProgress(UUID jobId,
                              double totalSeconds,
                              double currentSeconds,
                              int renditionIndex,
                              int totalRenditions,
                              int height) {
        double partSize = 1.0 / totalRenditions;
        double partProgress = totalSeconds <= 0 ? 1.0 : Math.min(1.0, currentSeconds / totalSeconds);
        double totalProgress = Math.min(1.0, (renditionIndex * partSize) + (partSize * partProgress));
        int percent = (int) Math.round(totalProgress * 100);

        this.jobRegistry.update(jobId, st -> {
            st.setStage(TranscodingJobStage.TRANSCODING);
            st.setPercent(percent);
            st.setMessage("Перекодирование " + height + "p: " + percent + "%");
        });
        this.eventsProducer.sendProgress(new TranscodingProgressEvent(jobId, percent, "Перекодирование " + height + "p: " + percent + "%", TranscodingJobStage.TRANSCODING, java.time.Instant.now()));
    }

    private double parseFfmpegTimeToSeconds(String timeStr) {
        // format HH:MM:SS.microseconds
        try {
            String[] parts = timeStr.split(":");
            double h = Double.parseDouble(parts[0]);
            double m = Double.parseDouble(parts[1]);
            double s = Double.parseDouble(parts[2]);
            return h * 3600 + m * 60 + s;
        } catch (Exception e) {
            return 0.0;
        }
    }

    private double normalizeDuration(double seconds) {
        if (seconds <= 0) {
            return 60.0;
        }
        return seconds;
    }

    private List<Integer> normalizeTargets(List<Integer> targetHeights, int sourceHeight) {
        LinkedHashSet<Integer> set = new LinkedHashSet<>();
        if (targetHeights != null) {
            set.addAll(targetHeights);
        }
        set.add(144);
        set.add(240);
        set.add(360);
        set.add(480);
        set.add(720);
        set.add(1080);

//        if (sourceHeight > 0) {
//            // включаем оригинал, даже если он ниже 1080p — апскейл до 1080 разрешён
//            set.add(sourceHeight);
//        }

        return set.stream()
                .filter(h -> h > 0)
                .sorted(Comparator.naturalOrder())
                .toList();
    }
}
