package ru.itmo.zavar.brainstop.video.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import ru.itmo.zavar.brainstop.video.model.FileMetadataEntity;
import ru.itmo.zavar.brainstop.video.repo.FileMetadataRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;

@Configuration
@Profile("dev-seed")
public class DevSeedConfig {

    private static final Logger log = LoggerFactory.getLogger(DevSeedConfig.class);

    @Bean
    CommandLineRunner seedDemoVideos(FileMetadataRepository repository) {
        return args -> {
            long existing = repository.count();
            if (existing >= 10) {
                log.info("Skipping dev-seed, repository already has {} records", existing);
                return;
            }

            RandomGenerator rnd = ThreadLocalRandom.current();
            List<FileMetadataEntity> seeds = new ArrayList<>();

            for (int i = 1; i <= 20; i++) {
                FileMetadataEntity entity = new FileMetadataEntity();
                entity.setName("Demo video " + i);
                entity.setUuid(UUID.randomUUID());
                entity.setContentType("video/mp4");
                entity.setContentSize(5_000_000L + rnd.nextLong(15_000_000L));
                entity.setUploaderId(rnd.nextLong(1, 6));
                entity.setUploaderName("DemoUser" + entity.getUploaderId());
                entity.setUploadedAt(LocalDateTime.now().minusDays(rnd.nextLong(0, 30)));
                entity.setLikesCount(rnd.nextLong(0, 5_000));
                entity.setViewsCount(rnd.nextLong(entity.getLikesCount(), entity.getLikesCount() + 20_000));
                entity.setThumbnailKey(String.format("thumbnails/demo-%02d.jpg", i));
                entity.setDefaultHeight(rnd.nextBoolean() ? 720 : 1080);
                entity.setDescription("Demo video " + i + " — sample feed item for development.");
                entity.setTitleEmbedding(randomEmbedding(rnd));
                entity.setDescriptionEmbedding(randomEmbedding(rnd));
                entity.setCombinedEmbedding(randomEmbedding(rnd));
                seeds.add(entity);
            }

            repository.saveAll(seeds);
            log.info("Seeded {} demo videos for dev-seed profile", seeds.size());
        };
    }

    private float[] randomEmbedding(RandomGenerator rnd) {
        float[] vector = new float[384];
        for (int i = 0; i < vector.length; i++) {
            vector[i] = (float) (rnd.nextDouble(-1.0, 1.0));
        }
        return vector;
    }
}
