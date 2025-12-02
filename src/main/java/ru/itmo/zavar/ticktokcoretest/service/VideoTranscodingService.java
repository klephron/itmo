package ru.itmo.zavar.ticktokcoretest.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.itmo.zavar.ticktokcoretest.upload.JobStage;
import ru.itmo.zavar.ticktokcoretest.upload.UploadJobRegistry;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Slf4j
@Service
public class VideoTranscodingService {

    @Value("${transcoder.ffmpeg-path:ffmpeg}")
    private String ffmpegPath;

    @Value("${transcoder.ffprobe-path:ffprobe}")
    private String ffprobePath;

    private final UploadJobRegistry jobRegistry;

    public VideoTranscodingService(UploadJobRegistry jobRegistry) {
        this.jobRegistry = jobRegistry;
    }

    /**
     * Полный цикл: узнаём длительность, запускаем ffmpeg и обновляем прогресс jobId.
     */
    public Path transcodeToMp4WithProgress(Path inputTempFile,
                                           UUID jobId) throws IOException, InterruptedException {

        double totalSeconds = probeDurationSeconds(inputTempFile);
        if (totalSeconds <= 0) {
            log.warn("ffprobe did not return valid duration, using fake 1 second");
            totalSeconds = 1.0;
        }

        Path output = Files.createTempFile("upload-out-", ".mp4");

        log.info("Transcoding start: input={}, output={}, totalSeconds={}",
                inputTempFile, output, totalSeconds);

        ProcessBuilder pb = new ProcessBuilder(
                ffmpegPath,
                "-y",
                "-i", inputTempFile.toString(),
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

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(p.getInputStream()))) {

            String line;
            double currentSeconds = 0.0;
            while ((line = reader.readLine()) != null) {
                // можно логировать в debug, чтобы не засрать лог
                // log.debug("ffmpeg: {}", line);

                if (line.startsWith("out_time=")) {
                    String timeStr = line.substring("out_time=".length());
                    currentSeconds = parseFfmpegTimeToSeconds(timeStr);
                    int percent = (int) Math.min(100,
                            Math.round((currentSeconds / totalSeconds) * 100.0));

                    jobRegistry.update(jobId, st -> {
                        st.setStage(JobStage.TRANSCODING);
                        st.setPercent(percent);
                        st.setMessage("Перекодирование: " + percent + "%");
                    });
                } else if (line.startsWith("out_time_ms=") && currentSeconds == 0.0) {
                    // запасной вариант: парсим по миллисекундам
                    try {
                        long ms = Long.parseLong(line.substring("out_time_ms=".length()));
                        currentSeconds = ms / 1_000_000.0;
                        int percent = (int) Math.min(100,
                                Math.round((currentSeconds / totalSeconds) * 100.0));
                        jobRegistry.update(jobId, st -> {
                            st.setStage(JobStage.TRANSCODING);
                            st.setPercent(percent);
                            st.setMessage("Перекодирование: " + percent + "%");
                        });
                    } catch (NumberFormatException ignored) {}
                }
            }
        }

        int code = p.waitFor();
        long took = System.currentTimeMillis() - started;

        if (code != 0) {
            log.error("ffmpeg failed with code {} in {} ms", code, took);
            throw new IllegalStateException("ffmpeg failed with code " + code);
        }

        long size = Files.size(output);
        log.info("Transcoding finished successfully in {} ms, output size={} bytes", took, size);

        // гарантируем, что к концу прогресса 100%
        jobRegistry.update(jobId, st -> {
            st.setStage(JobStage.TRANSCODING);
            st.setPercent(100);
            st.setMessage("Перекодирование: 100%");
        });

        return output;
    }

    /**
     * ffprobe - узнаём длительность файла в секундах.
     */
    private double probeDurationSeconds(Path input) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(
                ffprobePath,
                "-v", "error",
                "-show_entries", "format=duration",
                "-of", "default=noprint_wrappers=1:nokey=1",
                input.toString()
        );
        pb.redirectErrorStream(true);
        Process p = pb.start();

        String line;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(p.getInputStream()))) {
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

    /**
     * hh:mm:ss.xx → seconds
     */
    private double parseFfmpegTimeToSeconds(String timeStr) {
        // пример: 00:01:23.45
        String[] parts = timeStr.split(":");
        if (parts.length != 3) return 0.0;

        try {
            int hours = Integer.parseInt(parts[0]);
            int minutes = Integer.parseInt(parts[1]);
            double seconds = Double.parseDouble(parts[2]);
            return hours * 3600 + minutes * 60 + seconds;
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}
