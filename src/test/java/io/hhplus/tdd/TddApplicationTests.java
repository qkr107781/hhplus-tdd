package io.hhplus.tdd;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.dto.PointHistory;
import io.hhplus.tdd.point.dto.TransactionType;
import io.hhplus.tdd.point.dto.UserPoint;
import io.hhplus.tdd.point.service.UserPointService;
import io.hhplus.tdd.point.util.exception.CustomException;
import org.apache.catalina.core.ApplicationContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TddApplicationTests {

	@MockBean
	private UserPointTable userPointTable;

	@MockBean
	private PointHistoryTable pointHistoryTable;

	@Test
	@DisplayName("[충전 금액 부족]입력받은 포인트가 0P 이하 일때 충전 실패")
	void chargePointEmpty() throws CustomException {
	//Given
		long id = 11L;
		long chargePointAmount = 0L;
	//When
		UserPointService userPointService = new UserPointService(userPointTable,pointHistoryTable);
		CustomException ce = assertThrows(CustomException.class,() -> userPointService.chargePoint(id, chargePointAmount),"충천 포인트 0P");
	//Then
		assertEquals("충천 포인트 0P",ce.getErrorCode().getMessage());
	}

	@Test
	@DisplayName("[1회 충전 금액 제한]입력받은 포인트가 100,000P 초과 일때 충전 실패")
	void limitOnOnetimeChargingAmount(){
	//Given
		long id = 11L;
		long chargePointAmount = 100001L;
	//When
		UserPointService userPointService = new UserPointService(userPointTable,pointHistoryTable);
		CustomException ce = assertThrows(CustomException.class,() -> userPointService.chargePoint(id, chargePointAmount),"충천 포인트 0P");
	//Then
		assertEquals("최대 충천 포인트 초과",ce.getErrorCode().getMessage());
	}

	@Test
	@DisplayName("[최대 잔고 초과]충전 요청 포인트 + 소유 포인트가 1,000,000P 초과 일때 충전 실패")
	void overChargeAttempt(){
	//Given
		long id = 11L;
		long chargePointAmount = 100L;

		UserPointTable userPointTableNew = new UserPointTable();
		userPointTableNew.insertOrUpdate(id,999_999L);
	//When
		UserPointService userPointService = new UserPointService(userPointTableNew,pointHistoryTable);
		CustomException ce = assertThrows(CustomException.class,() -> userPointService.chargePoint(id, chargePointAmount),"충천 포인트 0P");
	//Then
		assertEquals("최대 잔고 초과",ce.getErrorCode().getMessage());
	}

	@Test
	@DisplayName("[포인트 충전]입력받은 포인트 충전 및 충전 내역 기록")
	void chargePointSuccess(){
	//Given
		long id = 11L;
		long chargePointAmount = 1000L;

	//When
		//유저포인트 객체 생성
		UserPointTable userPointTableNew = new UserPointTable();
		//유저 생성
		userPointTableNew.insertOrUpdate(11L,0);
		//이력 객체 생성
		PointHistoryTable pointHistoryTableNew = new PointHistoryTable();

		UserPointService userPointService = new UserPointService(userPointTableNew,pointHistoryTableNew);
		UserPoint afterChargeUserPoint =  userPointService.chargePoint(id, chargePointAmount);
	//Then
		List<PointHistory> pointHistory = pointHistoryTableNew.selectAllByUserId(id);

		//충전 이력 정상 입력 확인
		assertEquals(1,pointHistory.size());//1회 충전으로 리턴 리스트 사이즈는 1이어야 함
		assertEquals(id,pointHistory.get(0).userId());
		assertEquals(chargePointAmount,pointHistory.get(0).amount());
		assertEquals(TransactionType.CHARGE,pointHistory.get(0).type());

		//포인트 정상 충전 확인
		assertEquals(id,afterChargeUserPoint.id());
		assertEquals(chargePointAmount,afterChargeUserPoint.point());
	}
}
