package io.hhplus.tdd.unit;

import io.hhplus.tdd.point.controller.PointController;
import io.hhplus.tdd.point.dto.PointHistory;
import io.hhplus.tdd.point.dto.TransactionType;
import io.hhplus.tdd.point.dto.UserPoint;
import io.hhplus.tdd.point.service.UserPointService;
import io.hhplus.tdd.point.util.exception.CustomException;
import io.hhplus.tdd.point.util.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 입력 값 검증, Service 로직 실행 결과에 대한 테스트 코드 작성
 */
@WebMvcTest(PointController.class)
public class PointControllerTests {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    UserPointService userPointService;

    @ParameterizedTest
    @DisplayName("[충전 금액 부족]입력받은 포인트가 0P 이하 일때 충전 실패, [1회 충전 금액 제한]입력받은 포인트가 100,000P 초과 일때 충전 실패")
    @ValueSource(longs = {0L,100_001L})
    void validationInputChargePoint(long chargePointAmount) throws Exception {
        //When
        mockMvc.perform(patch("/point/11/charge")
                        .contentType(MediaType.APPLICATION_JSON)
                        //Given
                        .content(String.valueOf(chargePointAmount)))
                    //Then
                    .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("[포인트 충전]입력받은 포인트 충전 - 성공")
    void chargePointSuccess() throws Exception {
    //Given
        long id = 11L;
        long chargePointAmount = 1_000L;

        //chargePointAmount 만큼 포인트 충전 되는 Mock 객체
        when(userPointService.chargePoint(id,chargePointAmount)).thenReturn(new UserPoint(id,chargePointAmount,System.currentTimeMillis()));
    //When
        mockMvc.perform(patch("/point/11/charge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(chargePointAmount)))
                    //Then
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.point").value(chargePointAmount));
    }

    @Test
    @DisplayName("[최대 잔고 초과]충전 요청 포인트 + 소유 포인트가 1,000,000P 초과 일때 충전 실패")
    void chargePointFail() throws Exception {
        //Given
        long id = 11L;
        long chargePointAmount = 2L;

        //최대 잔고 초과 충전 요청 시 OVER_CHARGE 에러코드 리턴
        when(userPointService.chargePoint(id, chargePointAmount)).thenThrow(new CustomException(ErrorCode.OVER_CHARGE));
        //When
        mockMvc.perform(patch("/point/11/charge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(chargePointAmount)))
                    //Then
                    .andExpect(status().isInternalServerError());
    }

    @ParameterizedTest
    @DisplayName("[사용 포인트 제한]사용 포인트가 0P 이하 이거나 최대 잔고인 1,000,000P를 초과하는 경우 사용 실패")
    @ValueSource(longs = {-1L,1_000_001L})
    void invalidUsePoint(long usePointAmount) throws Exception {
        //When
        mockMvc.perform(patch("/point/11/use")
                        .contentType(MediaType.APPLICATION_JSON)
                        //Given
                        .content(String.valueOf(usePointAmount)))
                    //Then
                    .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("[포인트 사용]입력받은 포인트 만큼 소유 포인트 에서 차감")
    void usePointAndRecordHistory() throws Exception {
        //Given
        long id = 11L;
        long usePointAmount = 1_000L;
        long ownUserPoint = 10_000L;
        long remainingUserPoint = ownUserPoint - usePointAmount;

        UserPoint userPoint = new UserPoint(id,remainingUserPoint,System.currentTimeMillis());
        when(userPointService.usePoint(id,usePointAmount)).thenReturn(userPoint);
        //When
        mockMvc.perform(patch("/point/11/use")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(usePointAmount)))
                    //Then
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.point").value(remainingUserPoint));
    }

    @Test
    @DisplayName("[잔고 부족]소유 포인트가 0P 이거나 사용할 포인트보다 작은 경우 사용 실패")
    void notEnoughValance() throws Exception {
        //Given
        long id = 11L;
        long usePointAmount = 100_000L;

        //최대 잔고 초과 충전 요청 시 NOT_ENOUGH_VALANCE 에러코드 리턴
        when(userPointService.usePoint(id, usePointAmount)).thenThrow(new CustomException(ErrorCode.NOT_ENOUGH_VALANCE));
        //When
        mockMvc.perform(patch("/point/11/use")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(usePointAmount)))
                    //Then
                    .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("[포인트 조회]소유 포인트 조회")
    void selectOwnPoint() throws Exception{
    //Given
        long id = 11L;
        long ownUserPoint = 10_000L;

        UserPoint userPoint = new UserPoint(id,ownUserPoint,System.currentTimeMillis());
        when(userPointService.selectUserPoint(id)).thenReturn(userPoint);
        //When
        mockMvc.perform(get("/point/11"))
                    //Then
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(id))
                    .andExpect(jsonPath("$.point").value(ownUserPoint));
    }

    @Test
    @DisplayName("[포인트 충전/사용 내역 조회]포인트 충전/사용 내역 조회")
    void selectUserPointHistory() throws Exception{
    //Given
        long id = 11L;
        long chargePoint = 10_000L;
        long usePoint = 1_000L;

        PointHistory chargePointHistory = new PointHistory(1,id,chargePoint,TransactionType.CHARGE,System.currentTimeMillis());
        PointHistory usePointHistory = new PointHistory(2,id,usePoint,TransactionType.USE,System.currentTimeMillis());

        List<PointHistory> pointHistories = new ArrayList<>();
        pointHistories.add(chargePointHistory);
        pointHistories.add(usePointHistory);

        when(userPointService.selectUserPointHistory(id)).thenReturn(pointHistories);
        //When
        mockMvc.perform(get("/point/11/histories"))
                //Then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(pointHistories.get(0).userId()))
                .andExpect(jsonPath("$[0].amount").value(pointHistories.get(0).amount()))
                .andExpect(jsonPath("$[0].type").value(pointHistories.get(0).type().toString()))
                .andExpect(jsonPath("$[1].userId").value(pointHistories.get(1).userId()))
                .andExpect(jsonPath("$[1].amount").value(pointHistories.get(1).amount()))
                .andExpect(jsonPath("$[1].type").value(pointHistories.get(1).type().toString()));
    }
}
