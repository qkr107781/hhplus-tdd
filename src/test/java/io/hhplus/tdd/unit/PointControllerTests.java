package io.hhplus.tdd.unit;

import io.hhplus.tdd.point.controller.PointController;
import io.hhplus.tdd.point.dto.PointHistory;
import io.hhplus.tdd.point.dto.TransactionType;
import io.hhplus.tdd.point.dto.UserPoint;
import io.hhplus.tdd.point.service.UserPointService;
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
    @DisplayName("[포인트 충전]입력받은 포인트 충전 및 충전 내역 기록")
    void chargePointSuccess() throws Exception {
    //Given
        long id = 11L;
        long chargePointAmount = 1_000L;

        UserPoint userPoint = new UserPoint(id,chargePointAmount,System.currentTimeMillis());
        when(userPointService.chargePoint(id,chargePointAmount)).thenReturn(userPoint);
    //When
        mockMvc.perform(patch("/point/11/charge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(chargePointAmount)))
                    //Then
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.point").value(chargePointAmount));
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

        List<PointHistory> pointHistories = new ArrayList<PointHistory>();
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
