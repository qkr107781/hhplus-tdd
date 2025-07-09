package io.hhplus.tdd.integration;

import io.hhplus.tdd.point.dto.TransactionType;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class) // @Order 어노테이션을 기준으로 순서 지정
public class PointControllerIntegrationTests {

    @Autowired
    MockMvc mockMvc;

    /**
     * 실체 서비스 되는 환경과 유사하게 실행시켜 테스트 진행
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
    @DisplayName("1. 현재 소유 포인트 조회(0P)")
    void selectOwnPoint1() throws Exception{
        //Given
        long ownUserPointAmout = 0L;

        //When
        mockMvc.perform(get("/point/11"))
                //Then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.point").value(ownUserPointAmout));
    }

    @Test
    @Order(2)
    @DisplayName("2. 충전 및 내역 저장(10_000P)")
    void chargePointAndRecordHistory() throws Exception{
        //Given
        long chargePointAmount = 10_000L;

        //When
        mockMvc.perform(patch("/point/11/charge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(chargePointAmount)))
                //Then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.point").value(chargePointAmount));
    }

    @Test
    @Order(3)
    @DisplayName("3. 충전 내역 조회(CHARGE, 10_000P)")
    void selectChargeHistory() throws Exception{
        //Given
        long chargePointAmount = 10_000L;

        //When
        mockMvc.perform(get("/point/11/histories"))
                //Then
                .andExpect(status().isOk())
                //1회 충전으로 1번째 값 검증
                .andExpect(jsonPath("$[0].userId").value(String.valueOf(id)))
                //충전 내역이 충전 포인트와 동일한지 검증(10_000P)
                .andExpect(jsonPath("$[0].amount").value(String.valueOf(chargePointAmount)))
                //내역 타입이 CHARGE인지 검증
                .andExpect(jsonPath("$[0].type").value(TransactionType.CHARGE.toString()));
    }

    @Test
    @Order(4)
    @DisplayName("4. 소유 포인트 조회(10_000P)")
    void selectOwnPoint2() throws Exception{
        //Given
        long ownUserPointAmount = 10_000L;

        //When
        mockMvc.perform(get("/point/11"))
                //Then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                //충전 후 소유 포인트 검증(10_000P)
                .andExpect(jsonPath("$.point").value(ownUserPointAmount));
    }

    @Test
    @Order(5)
    @DisplayName("5. 사용 및 내역 저장(3_000P)")
    void usePointAndRecordHistory() throws Exception{
        //Given
        long usePointAmount = 3_000L;
        long ownUserPointAmount = 10_000L;
        long remainingUserPoint = ownUserPointAmount - usePointAmount;

        //When
        mockMvc.perform(patch("/point/11/use")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(usePointAmount)))
                    //Then
                    .andExpect(status().isOk())
                    //사용 후 잔여 포인트 검증(7_000P)
                    .andExpect(jsonPath("$.point").value(remainingUserPoint));
    }

    @Test
    @Order(6)
    @DisplayName("6. 사용 내역 조회(USE, 3_000P)")
    void selectUseHistory() throws Exception{
        //Given
        long usePointAmount = 3_000L;

        //When
        mockMvc.perform(get("/point/11/histories"))
                //Then
                .andExpect(status().isOk())
                //앞에 충전도 했어서 리스트의 2번째 값 검증
                .andExpect(jsonPath("$[1].userId").value(String.valueOf(id)))
                //사용 내역이 사용 포인트와 동일한지 검증(3_000P)
                .andExpect(jsonPath("$[1].amount").value(String.valueOf(usePointAmount)))
                //내역 타입이 USE인지 검증
                .andExpect(jsonPath("$[1].type").value(TransactionType.USE.toString()));
    }

    @Test
    @Order(7)
    @DisplayName("7. 잔여 포인트 조회(7_000P)")
    void selectOwnPoint3() throws Exception{
        //Given
        long ownUserPointAmount = 7_000L;

        //When
        mockMvc.perform(get("/point/11"))
                //Then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                //사용 후 잔여 포인트 검증(7_000P)
                .andExpect(jsonPath("$.point").value(ownUserPointAmount));
    }

    @Test
    @Order(8)
    @DisplayName("8. 로직 수행 중 예외 처리")
    void testException() throws Exception{
    //Given
        long chargePointAmount = 993_001L;
        long usePointAmount = 7_001L;

        //[포인트 사용][사용 포인트 제한]사용 포인트가 0P 이하 이거나 최대 잔고인 1,000,000P를 초과하는 경우 사용 실패
        //When
        mockMvc.perform(patch("/point/11/charge")
                        .contentType(MediaType.APPLICATION_JSON)
                        //Given
                        .content(String.valueOf(chargePointAmount)))
                //Then
                .andExpect(status().isInternalServerError());

        //[포인트 사용][잔고 부족]소유 포인트가 0P 이거나 사용할 포인트보다 작은 경우 사용 실패
        //When
        mockMvc.perform(patch("/point/11/use")
                        .contentType(MediaType.APPLICATION_JSON)
                        //Given
                        .content(String.valueOf(usePointAmount)))
                //Then
                .andExpect(status().isInternalServerError());
    }

}
