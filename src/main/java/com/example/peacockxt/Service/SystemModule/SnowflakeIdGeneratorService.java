package com.example.peacockxt.Service.SystemModule;

import org.springframework.stereotype.Service;
@Service
public class SnowflakeIdGeneratorService {

    private long lastTimestamp = -1;
    private long sequence = 0L;
    private final long epoch = 1704067200000L;
    private final long workerId = 99;
    private final long workerBits = 10;
    private final long sequenceBits = 12;

    public synchronized long nextId() {
        long currentTime = currentTimeMillis();
        if (currentTime == lastTimestamp) {
            long maxSequence = 4095;
            sequence = (sequence + 1) & maxSequence;
            if (sequence == 0) {
                while (currentTime <= lastTimestamp) {
                    currentTime = currentTimeMillis();
                }
            }
        } else {
            sequence = 0;
        }
        lastTimestamp = currentTime;
        return ((currentTime - epoch) << (workerBits + sequenceBits)) | (workerId << sequenceBits) | sequence;
    }

    private long currentTimeMillis() {
        return System.currentTimeMillis();
    }
}

