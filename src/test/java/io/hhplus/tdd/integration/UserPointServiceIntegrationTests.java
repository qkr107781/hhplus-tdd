package io.hhplus.tdd.integration;

import io.hhplus.tdd.point.dto.PointHistory;
import io.hhplus.tdd.point.dto.TransactionType;
import io.hhplus.tdd.point.dto.UserPoint;
import io.hhplus.tdd.point.service.UserPointService;
import io.hhplus.tdd.point.util.exception.CustomException;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class) // @Order 어노테이션을 기준으로 순서 지정
public class UserPointServiceIntegrationTests {

    @Autowired
    UserPointService userPointService;

    /**
     * 실체 서비스 되는 환경과 유사하게 실행시켜 테스트 진행
     * 내부 구현은 모른채 반환된 입력 -> 결과만 체크하여 테스트 통과 여부 확인
     * Autowired로 UserPointService 주입받아서 사용하고
     * Spring Bean으로 로딩된 database 하위 패키지 클래스들의 Map에 값을 입력/수정/조회 하면서 테스트 진행됨
     *
     * 시나리오
     * 1. 현재 소유 포인트 조회(0P)
     * 2. 충전 및 내역 저장(10_000P)
     * 3. 충전 내역 조회(CHARGE, 10_000P)
     * 4. 소유 포인트 조회(10_000P)
     * 5. 사용 및 내역 저장(3_000P)
     * 6. 사용 내역 조회(USE, 3_000P)
     * 7. 잔여 포인트 조회(7_000P)
     * 8. 로직 수행 중 예외 처리
     */

    private final long id = 11L;

    @Test
    @Order(1)
    void selectOwnPoint1(){
    //Given
        long ownUserPointAmount = 0L;
    //When
        //1. 현재 소유 포인트 조회(0P)
        UserPoint selectOwnUserPoint = userPointService.selectUserPoint(id);
    //Then
        assertEquals(id,selectOwnUserPoint.id());
        //현재 소유 포인트 검증(0P)
        assertEquals(ownUserPointAmount,selectOwnUserPoint.point());
    }

    @Test
    @Order(2)
    void chargePointAndRecordHistory(){
    //Given
        long chargePointAmount = 10_000L;
    //When
        //2. 충전 및 내역 저장(10_000P)
        UserPoint chargeUserPoint = userPointService.chargePoint(id,chargePointAmount);
    //Then
        assertEquals(id,chargeUserPoint.id());
        //충전 후 소유 포인트 검증(10_000P)
        assertEquals(chargePointAmount,chargeUserPoint.point());
    }

    @Test
    @Order(3)
    void selectChargeHistory(){
    //Given
        long chargePointAmount = 10_000L;
    //When
        //3. 충전 내역 조회(CHARGE, 10_000P)
        List<PointHistory> chargeHistorys = userPointService.selectUserPointHistory(id);
    //Then
        assertEquals(1,chargeHistorys.size());//1회 충전으로 리턴 리스트 사이즈는 1이어야 함
        assertEquals(id,chargeHistorys.get(0).userId());
        //충전 내역이 충전 포인트와 동일한지 검증(10_000P)
        assertEquals(chargePointAmount,chargeHistorys.get(0).amount());
        //내역 타입이 CHARGE인지 검증
        assertEquals(TransactionType.CHARGE,chargeHistorys.get(0).type());
    }

    @Test
    @Order(4)
    void selectOwnPoint2(){
    //Given
        long ownUserPointAmount = 10_000L;
    //When
        //4. 소유 포인트 조회(10_000P)
        UserPoint selectAfterChargeUserPoint = userPointService.selectUserPoint(id);
    //Then
        assertEquals(id,selectAfterChargeUserPoint.id());
        //충전 후 소유 포인트 검증(10_000P)
        assertEquals(ownUserPointAmount,selectAfterChargeUserPoint.point());
    }

    @Test
    @Order(5)
    void usePointAndRecordHistory(){
    //Given
        long usePointAmount = 3_000L;
        long remainingUserPointAmount = 7_000L;
    //When
        //5. 사용 및 내역 저장(3_000P)
        UserPoint usePoint = userPointService.usePoint(id,usePointAmount);
    //Then
        assertEquals(id,usePoint.id());
        //사용 후 잔여 포인트 검증(7_000P)
        assertEquals(remainingUserPointAmount,usePoint.point());
    }

    @Test
    @Order(6)
    void selectUseHistory(){
    //Given
        long usePointAmount = 3_000L;
    //When
        //6. 사용 내역 조회(USE, 3_000P)
        List<PointHistory> useHistorys = userPointService.selectUserPointHistory(id);
    //Then
        assertEquals(2,useHistorys.size());//앞에 충전도 했어서 리턴 리스트 사이즈는 2이어야 함
        assertEquals(id,useHistorys.get(1).userId());
        //사용 내역이 사용 포인트와 동일한지 검증(3_000P)
        assertEquals(usePointAmount,useHistorys.get(1).amount());
        //내역 타입이 USE인지 검증
        assertEquals(TransactionType.USE,useHistorys.get(1).type());
    }

    @Test
    @Order(7)
    void selectOwnPoint3(){
    //Given
        long ownUserPointAmount = 7_000L;
    //When
        //7. 잔여 포인트 조회(7_000P)
        UserPoint selectAfterUseUserPoint = userPointService.selectUserPoint(id);
    //Then
        assertEquals(id,selectAfterUseUserPoint.id());
        //사용 후 잔여 포인트 검증(7_000P)
        assertEquals(ownUserPointAmount,selectAfterUseUserPoint.point());
    }

    @Test
    @Order(8)
    void testException(){
    //Given
        long chargePointAmount = 993_001L;
        long usePointAmount = 7_001L;
    //When
        //8. 로직 수행 중 예외 처리
        //[포인트 충전][최대 잔고 초과]충전 요청 포인트 + 소유 포인트가 1,000,000P 초과 일때 충전 실패
        CustomException overChargeEx = assertThrows(CustomException.class, () -> userPointService.chargePoint(id, chargePointAmount),"최대 잔고 초과");
        //[포인트 사용][잔고 부족]소유 포인트가 0P 이거나 사용할 포인트보다 작은 경우 사용 실패
        CustomException notEnoughValanceEx = assertThrows(CustomException.class, () -> userPointService.usePoint(id, usePointAmount),"잔고 부족");
    //Then
        assertEquals(600,overChargeEx.getErrorCode().getStatus());
        assertEquals("OVER_CHARGE",overChargeEx.getErrorCode().getCode());
        assertEquals("최대 잔고 초과",overChargeEx.getErrorCode().getMessage());

        assertEquals(602,notEnoughValanceEx.getErrorCode().getStatus());
        assertEquals("NOT_ENOUGH_VALANCE",notEnoughValanceEx.getErrorCode().getCode());
        assertEquals("잔여 포인트 부족",notEnoughValanceEx.getErrorCode().getMessage());
    }

}
