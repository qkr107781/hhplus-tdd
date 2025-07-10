package io.hhplus.tdd.integration;

import io.hhplus.tdd.point.dto.UserPoint;
import io.hhplus.tdd.point.service.UserPointService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class ConcurrencyTests {

    @Autowired
    UserPointService userPointService;

    @Test
    @DisplayName("[ConccurentHashMap+ReentrantLock]한명의 유저가 1,000 포인트씩 100번의 포인트 충전을 동시 요청했을때 모두 정상적으로 반영되어야 한다.")
    void concurrencyTest() throws InterruptedException, BrokenBarrierException {
        long id = 11L;
        long ownPointAmount = 0L;
        long chargePointAmount = 1_000L;
        int threadCount = 100;

        //모든 스레드가 동시에 시작할 수 있도록 돕는 Barrier
        //threadCount + 1 (메인 스레드)
        CyclicBarrier startBarrier = new CyclicBarrier(threadCount + 1);

        //모든 스레드가 작업을 완료했음을 알리는 Latch
        CountDownLatch endLatch = new CountDownLatch(threadCount);

        //Thread Pool 생성
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    //모든 스레드가 Barrier에 도착할 때까지 대기
                    startBarrier.await();

                    //포인트 충전
                    userPointService.chargePoint(id, chargePointAmount);
                } catch (Exception e) {
                    System.err.println("스레드 실행 중 오류: " + e.getMessage());
                } finally {
                    //이 스레드가 작업을 완료했음을 Latch에 알림
                    endLatch.countDown();
                }
            });
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

        //모든 스레드가 준비될 때까지 대기
        startBarrier.await();
        LocalDateTime startTime = LocalDateTime.now();
        System.out.println("대기 끝! 모든 스레드 실행 시작: " + startTime.format(formatter));
        //모든 스레드가 완료될 때까지 대기
        endLatch.await();
        
        //종료 처리
        executor.shutdown();
        LocalDateTime endTime = LocalDateTime.now();
        System.out.println("모든 스레드 실행 종료: " + endTime.format(formatter));

        //최종 포인트 검증
        UserPoint afterAllChargeUserPoint = userPointService.selectUserPoint(id);
        long expectedPoint = ownPointAmount + (chargePointAmount * threadCount);

        //예상 값과 실제 값이 일치하는지 검증
        assertEquals(expectedPoint, afterAllChargeUserPoint.point());
    }

    @Test
    @DisplayName("[ReentrantLock]한명의 유저가 1,000 포인트씩 100번의 포인트 충전을 동시 요청했을때 모두 정상적으로 반영되어야 한다.")
    void concurrencyTest2() throws InterruptedException, BrokenBarrierException {
        long id = 11L;
        long ownPointAmount = 0L;
        long chargePointAmount = 1_000L;
        int threadCount = 100;

        //모든 스레드가 동시에 시작할 수 있도록 돕는 Barrier
        //threadCount + 1 (메인 스레드)
        CyclicBarrier startBarrier = new CyclicBarrier(threadCount + 1);

        //모든 스레드가 작업을 완료했음을 알리는 Latch
        CountDownLatch endLatch = new CountDownLatch(threadCount);

        //Thread Pool 생성
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    //모든 스레드가 Barrier에 도착할 때까지 대기
                    startBarrier.await();

                    //포인트 충전
                    userPointService.chargePointOnlyReentrantLock(id, chargePointAmount);
                } catch (Exception e) {
                    System.err.println("스레드 실행 중 오류: " + e.getMessage());
                } finally {
                    //이 스레드가 작업을 완료했음을 Latch에 알림
                    endLatch.countDown();
                }
            });
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

        //모든 스레드가 준비될 때까지 대기
        startBarrier.await();
        LocalDateTime startTime = LocalDateTime.now();
        System.out.println("대기 끝! 모든 스레드 실행 시작: " + startTime.format(formatter));
        //모든 스레드가 완료될 때까지 대기
        endLatch.await();

        //종료 처리
        executor.shutdown();
        LocalDateTime endTime = LocalDateTime.now();
        System.out.println("모든 스레드 실행 종료: " + endTime.format(formatter));

        //최종 포인트 검증
        UserPoint afterAllChargeUserPoint = userPointService.selectUserPoint(id);
        long expectedPoint = ownPointAmount + (chargePointAmount * threadCount);

        //예상 값과 실제 값이 일치하는지 검증
        assertEquals(expectedPoint, afterAllChargeUserPoint.point());
    }

    @Test
    @DisplayName("[ConccurentHashMap+ReentrantLock]5명의 유저가 1,000 포인트씩 100번의 포인트 충전을 동시 요청했을때 모두 정상적으로 반영되어야 한다.")
    void concurrencyTestFiveUser() throws InterruptedException, BrokenBarrierException {
        long[] ids = {11L,12L,13L,14L,15L};
        long ownPointAmount = 0L;
        long chargePointAmount = 1_000L;
        int threadCount = 100;
        int totalThreadCount = threadCount * ids.length;

        //모든 스레드가 동시에 시작할 수 있도록 돕는 Barrier
        //threadCount + 1 (메인 스레드)
        CyclicBarrier startBarrier = new CyclicBarrier(totalThreadCount + 1);

        //모든 스레드가 작업을 완료했음을 알리는 Latch
        CountDownLatch endLatch = new CountDownLatch(totalThreadCount);

        //Thread Pool 생성
        ExecutorService executor = Executors.newFixedThreadPool(totalThreadCount);

        for (long id : ids) {
            for (int i = 0; i < threadCount; i++) {
                final long currentId = id;
                executor.submit(() -> {
                    try {
                        //모든 스레드가 Barrier에 도착할 때까지 대기
                        startBarrier.await();

                        //포인트 충전
                        userPointService.chargePoint(currentId, chargePointAmount);
                    } catch (Exception e) {
                        System.err.println("스레드 실행 중 오류: " + e.getMessage());
                    } finally {
                        //이 스레드가 작업을 완료했음을 Latch에 알림
                        endLatch.countDown();
                    }
                });
            }
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

        //모든 스레드가 준비될 때까지 대기
        startBarrier.await();
        LocalDateTime startTime = LocalDateTime.now();
        System.out.println("대기 끝! 모든 스레드 실행 시작: " + startTime.format(formatter));
        //모든 스레드가 완료될 때까지 대기
        endLatch.await();

        //종료 처리
        executor.shutdown();
        LocalDateTime endTime = LocalDateTime.now();
        System.out.println("모든 스레드 실행 종료: " + endTime.format(formatter));

        for (long id : ids) {
            //최종 포인트 검증
            UserPoint afterAllChargeUserPoint = userPointService.selectUserPoint(id);
            long expectedPoint = ownPointAmount + (chargePointAmount * threadCount);

            //예상 값과 실제 값이 일치하는지 검증
            assertEquals(expectedPoint, afterAllChargeUserPoint.point());
        }

    }

    @Test
    @DisplayName("[ReentrantLock]5명의 유저가 1,000 포인트씩 100번의 포인트 충전을 동시 요청했을때 모두 정상적으로 반영되어야 한다.")
    void concurrencyTest2FiveUser() throws InterruptedException, BrokenBarrierException {
        long[] ids = {11L,12L,13L,14L,15L};
        long ownPointAmount = 0L;
        long chargePointAmount = 1_000L;
        int threadCount = 100;
        int totalThreadCount = threadCount * ids.length;

        //모든 스레드가 동시에 시작할 수 있도록 돕는 Barrier
        //threadCount + 1 (메인 스레드)
        CyclicBarrier startBarrier = new CyclicBarrier(totalThreadCount + 1);

        //모든 스레드가 작업을 완료했음을 알리는 Latch
        CountDownLatch endLatch = new CountDownLatch(totalThreadCount);

        //Thread Pool 생성
        ExecutorService executor = Executors.newFixedThreadPool(totalThreadCount);

        for (long id : ids) {
            for (int i = 0; i < threadCount; i++) {
                final long currentId = id;
                executor.submit(() -> {
                    try {
                        //모든 스레드가 Barrier에 도착할 때까지 대기
                        startBarrier.await();

                        //포인트 충전
                        userPointService.chargePointOnlyReentrantLock(currentId, chargePointAmount);
                    } catch (Exception e) {
                        System.err.println("스레드 실행 중 오류: " + e.getMessage());
                    } finally {
                        //이 스레드가 작업을 완료했음을 Latch에 알림
                        endLatch.countDown();
                    }
                });
            }
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

        //모든 스레드가 준비될 때까지 대기
        startBarrier.await();
        LocalDateTime startTime = LocalDateTime.now();
        System.out.println("대기 끝! 모든 스레드 실행 시작: " + startTime.format(formatter));
        //모든 스레드가 완료될 때까지 대기
        endLatch.await();

        //종료 처리
        executor.shutdown();
        LocalDateTime endTime = LocalDateTime.now();
        System.out.println("모든 스레드 실행 종료: " + endTime.format(formatter));

        for (long id : ids) {
            //최종 포인트 검증
            UserPoint afterAllChargeUserPoint = userPointService.selectUserPoint(id);
            long expectedPoint = ownPointAmount + (chargePointAmount * threadCount);

            //예상 값과 실제 값이 일치하는지 검증
            assertEquals(expectedPoint, afterAllChargeUserPoint.point());
        }

    }
    
}
