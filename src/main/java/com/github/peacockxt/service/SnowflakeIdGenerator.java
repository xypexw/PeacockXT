package com.github.peacockxt.service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

@Service
public class SnowflakeIdGenerator {

    // Epoch: 2024-01-01T00:00:00Z
    private final long epoch = 1704067200000L;

    // bit number configure
    private final long workerIdBits = 10L;
    private final long sequenceBits = 12L;

    // Maximun lenght
    private final long maxWorkerId = -1L ^ (-1L << workerIdBits); // 1023
    private final long maxSequence = -1L ^ (-1L << sequenceBits); // 4095

    // bit shift
    private final long workerIdShift = sequenceBits;
    private final long timestampLeftShift = sequenceBits + workerIdBits;

    private long sequence = 0L;
    private long lastTimestamp = -1L;

    @Value("${workerId}")
    private long workerId;

    @PostConstruct
    public void init() {
        if (workerId > maxWorkerId || workerId < 0) {
            throw new IllegalArgumentException(
                    String.format("Worker ID Must Be 0 to %d", maxWorkerId));
        }
    }

    public synchronized long nextId() {
        long currentTime = System.currentTimeMillis();

        if (currentTime < lastTimestamp) {
            throw new RuntimeException(String.format("System clock moved backwards. " +
                    "Refusing to generate ID for %d milliseconds", lastTimestamp - currentTime));
        }

        if (currentTime == lastTimestamp) {
            sequence = (sequence + 1) & maxSequence;
            if (sequence == 0) {
                currentTime = waitNextMillis(currentTime);
            }
        } else {
            sequence = 0L;
        }

        lastTimestamp = currentTime;

        return ((currentTime - epoch) << timestampLeftShift)
                | (workerId << workerIdShift)
                | sequence;
    }

    private long waitNextMillis(long currentTime) {
        while (currentTime <= lastTimestamp) {
            currentTime = System.currentTimeMillis();
        }
        return currentTime;
    }
}