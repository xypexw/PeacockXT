package com.example.peacockxt.ServiceTest.SystemModuleTest;

import com.example.peacockxt.Service.SystemModule.SnowflakeIdGeneratorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;
import java.util.stream.IntStream;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class SnowflakeIdGeneratorServiceTest {

    private SnowflakeIdGeneratorService idGenerator;

    @BeforeEach
    void setUp() {
        idGenerator = new SnowflakeIdGeneratorService();
    }

    @Test
    void testNextIdIsUnique() {
        Set<Long> ids = new HashSet<>();
        int count = 10000; // test 10k IDs
        for (int i = 0; i < count; i++) {
            long id = idGenerator.nextId();
            assertThat(ids).doesNotContain(id);
            ids.add(id);
        }
        assertThat(ids.size()).isEqualTo(count);
    }

    @Test
    void testSequenceRollover() throws InterruptedException {
        // Generate IDs in the same millisecond to force sequence increment
        long currentTime = System.currentTimeMillis();
        long lastTimestamp = currentTime;

        for (int i = 0; i < 4096; i++) { // sequence max is 4095
            long id = idGenerator.nextId();
            assertThat(id).isGreaterThan(0);
        }
    }

    @Test
    void testThreadSafety() throws InterruptedException, ExecutionException {
        int threads = 20;
        int idsPerThread = 5000;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        Set<Long> allIds = ConcurrentHashMap.newKeySet();

        Callable<Void> task = () -> {
            IntStream.range(0, idsPerThread).forEach(i -> allIds.add(idGenerator.nextId()));
            return null;
        };

        List<Future<Void>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            futures.add(executor.submit(task));
        }

        for (Future<Void> future : futures) {
            future.get();
        }

        executor.shutdown();
        assertThat(allIds.size()).isEqualTo(threads * idsPerThread);
    }
}
