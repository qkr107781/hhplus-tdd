package io.hhplus.tdd;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.dto.PointHistory;
import io.hhplus.tdd.point.dto.TransactionType;
import io.hhplus.tdd.point.dto.UserPoint;
import io.hhplus.tdd.point.service.UserPointService;
import io.hhplus.tdd.point.util.exception.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TddApplicationTests {

	@ParameterizedTest
	@DisplayName("[충전 금액 부족]입력받은 포인트가 0P 이하 일때 충전 실패, [1회 충전 금액 제한]입력받은 포인트가 100,000P 초과 일때 충전 실패")
	@ValueSource(longs = {0L,100_001L})
	void validationInputChargePoint(long chargePointAmount) throws CustomException {
	//Given
		long id = 11L;

		//소유 포인트가 없는 유저포인트 객체 생성
		UserPointTable userPointTable= new UserPointTable();
		userPointTable.insertOrUpdate(id,0L);

		//사용 내역 객체 생성
		PointHistoryTable pointHistoryTable = new PointHistoryTable();
	//When
		UserPointService userPointService = new UserPointService(userPointTable,pointHistoryTable);
		CustomException ce = assertThrows(CustomException.class,() -> userPointService.chargePoint(id, chargePointAmount),"충천 포인트 0P or 최대 충천 포인트 초과");
	//Then
		assertEquals(600,ce.getErrorCode().getStatus());//input 값에 대한 예외처리는 모두 600코드 return status 통일
	}

	@Test
	@DisplayName("[최대 잔고 초과]충전 요청 포인트 + 소유 포인트가 1,000,000P 초과 일때 충전 실패")
	void overChargeAttempt(){
	//Given
		long id = 11L;
		long chargePointAmount = 100L;

		//최대 잔고에 근접한 유저포인트 객체 생성
		UserPointTable userPointTable= new UserPointTable();
		userPointTable.insertOrUpdate(id,999_999L);

		//사용 내역 객체 생성
		PointHistoryTable pointHistoryTable = new PointHistoryTable();
	//When
		UserPointService userPointService = new UserPointService(userPointTable,pointHistoryTable);
		CustomException ce = assertThrows(CustomException.class,() -> userPointService.chargePoint(id, chargePointAmount),"충천 포인트 0P");
	//Then
		assertEquals("최대 잔고 초과",ce.getErrorCode().getMessage());
	}

	@Test
	@DisplayName("[포인트 충전]입력받은 포인트 충전 및 충전 내역 기록")
	void chargePointSuccess(){
	//Given
		long id = 11L;
		long chargePointAmount = 1_000L;

	//When
		//소유 포인트가 없는 유저포인트 객체 생성
		UserPointTable userPointTable= new UserPointTable();
		userPointTable.insertOrUpdate(id,0L);

		//사용 내역 객체 생성
		PointHistoryTable pointHistoryTable = new PointHistoryTable();

		UserPointService userPointService = new UserPointService(userPointTable,pointHistoryTable);
		UserPoint afterChargeUserPoint =  userPointService.chargePoint(id, chargePointAmount);
	//Then
		List<PointHistory> pointHistory = pointHistoryTable.selectAllByUserId(id);

		//충전 이력 정상 입력 확인
		assertEquals(1,pointHistory.size());//1회 충전으로 리턴 리스트 사이즈는 1이어야 함
		assertEquals(id,pointHistory.get(0).userId());
		assertEquals(chargePointAmount,pointHistory.get(0).amount());
		assertEquals(TransactionType.CHARGE,pointHistory.get(0).type());

		//포인트 정상 충전 확인
		assertEquals(id,afterChargeUserPoint.id());
		assertEquals(chargePointAmount,afterChargeUserPoint.point());
	}

	@ParameterizedTest
	@DisplayName("[잔고 부족]소유 포인트가 0P 이거나 사용할 포인트보다 작은 경우 사용 실패")
	@ValueSource(longs = {0L,10_000L})
	void notEnoughValance(long ownPointAmount){
	//Given
		long id = 11L;
		long usePointAmount = 100_000L;

		//유저포인트 객체 생성
		UserPointTable userPointTable= new UserPointTable();
		userPointTable.insertOrUpdate(id,ownPointAmount);

		//사용 내역 객체 생성
		PointHistoryTable pointHistoryTable = new PointHistoryTable();
	//When
		UserPointService userPointService = new UserPointService(userPointTable,pointHistoryTable);
		CustomException ce = assertThrows(CustomException.class,() -> userPointService.usePoint(id, usePointAmount),"잔여 포인트 부족");
	//Then
		assertEquals(602,ce.getErrorCode().getStatus());
	}

	@ParameterizedTest
	@DisplayName("[사용 포인트 제한]사용 포인트가 0P 이하 이거나 최대 잔고인 1,000,000P를 초과하는 경우 사용 실패")
	@ValueSource(longs = {-1L,1_000_001L})
	void invalidUsePoint(long usePointAmount){
	//Given
		long id = 11L;

		//유저포인트 객체 생성
		UserPointTable userPointTable= new UserPointTable();
		userPointTable.insertOrUpdate(id,0L);

		//사용 내역 객체 생성
		PointHistoryTable pointHistoryTable = new PointHistoryTable();
	//When
		UserPointService userPointService = new UserPointService(userPointTable,pointHistoryTable);
		CustomException ce = assertThrows(CustomException.class,() -> userPointService.usePoint(id, usePointAmount),"사용 포인트 제한");
	//Then
		assertEquals(600,ce.getErrorCode().getStatus());
	}
}
