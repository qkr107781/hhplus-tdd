package io.hhplus.tdd;

import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.service.UserPointService;
import io.hhplus.tdd.point.util.exception.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TddApplicationTests {

	@Mock
	private UserPointTable userPointTable;

	@Test
	@DisplayName("[충전 금액 부족]입력받은 포인트가 0P 이하 일때 충전 실패")
	void chargePointEmpty() throws CustomException {
	//Given
		long id = 11L;
		long chargePointAmount = 0L;
	//When
		UserPointService userPointService = new UserPointService();
		CustomException ce = assertThrows(CustomException.class,() -> userPointService.chargePoint(id, chargePointAmount),"충천 포인트 0P");
	//Then
		assertEquals("충천 포인트 0P",ce.getErrorCode().getMessage());
	}
}
