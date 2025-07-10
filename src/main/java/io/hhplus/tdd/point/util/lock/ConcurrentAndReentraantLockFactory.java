package io.hhplus.tdd.point.util.lock;

import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class ConcurrentAndReentraantLockFactory {
    private final ConcurrentHashMap<Long, ReentrantLock> lockMap = new ConcurrentHashMap<>();
    @Getter
    private final ReentrantLock reentrantLock = new ReentrantLock();

    public ReentrantLock getLock(long id) {
        return lockMap.computeIfAbsent(id, key -> new ReentrantLock());
    }

}