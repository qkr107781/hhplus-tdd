package io.hhplus.tdd.unit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.controller.PointController;
import io.hhplus.tdd.point.service.UserPointService;
import io.hhplus.tdd.point.util.exception.CustomException;
import io.hhplus.tdd.point.util.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PointController.class)
public class PointControllerTests {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    UserPointService userPointService;

    @Autowired
    ObjectMapper objectMapper;

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

}
