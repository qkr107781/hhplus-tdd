package io.hhplus.tdd.unit;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.dto.PointHistory;
import io.hhplus.tdd.point.dto.TransactionType;
import io.hhplus.tdd.point.dto.UserPoint;
import io.hhplus.tdd.point.service.UserPointService;
import io.hhplus.tdd.point.util.exception.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * PointService 메소드의 종료점(반환 결과, 상태값 변경, 외부 의존성)에 대해 테스트 코드 작성
 */
@ExtendWith(MockitoExtension.class)
class UserPointServiceTests {

	@Mock
	private UserPointTable userPointTable;//유저 포인트 Mock 객체

	@Mock
	private PointHistoryTable pointHistoryTable;//충전/사용 내역 Mock 객체

	//유저 ID
	private final long id = 11L;

	@Test
	@DisplayName("[포인트 충전][최대 잔고 초과]충전 요청 포인트 + 소유 포인트가 1,000,000P 초과 일때 충전 실패")
	void overChargeAttempt(){
	//Given
		//충전 포인트: 최대 잔고인 1_000_000P를 넘어가는 경계값
		long chargePointAmount = 2L;

		//최대 잔고에 근접한 유저포인트 Mock 객체
		when(userPointTable.selectById(id)).thenReturn(new UserPoint(id,999_999L,System.currentTimeMillis()));
	//When
		UserPointService userPointService = new UserPointService(userPointTable,pointHistoryTable);
		CustomException ce = assertThrows(CustomException.class,() -> userPointService.chargePoint(id, chargePointAmount),"충천 포인트 0P");
	//Then
		assertEquals(600,ce.getErrorCode().getStatus());
		assertEquals("OVER_CHARGE",ce.getErrorCode().getCode());
		assertEquals("최대 잔고 초과",ce.getErrorCode().getMessage());
	}

	@Test
	@DisplayName("[포인트 충전]입력받은 포인트 충전")
	void chargePoint(){
	//Given
		//충전 포인트
		long chargePointAmount = 1_000L;

		//소유 포인트가 없는 유저포인트 Mock 객체
		when(userPointTable.selectById(id)).thenReturn(new UserPoint(id,0L,System.currentTimeMillis()));
		//chargePointAmount 만큼 포인트 충전된 Mock 객체
		when(userPointTable.insertOrUpdate(id,chargePointAmount)).thenReturn(new UserPoint(id,chargePointAmount,System.currentTimeMillis()));
	//When
		UserPointService userPointService = new UserPointService(userPointTable,pointHistoryTable);
		UserPoint afterChargeUserPoint =  userPointService.chargePoint(id, chargePointAmount);
	//Then
		//포인트 정상 충전 확인
		assertEquals(id,afterChargeUserPoint.id());
		assertEquals(chargePointAmount,afterChargeUserPoint.point());
	}

	@Test
	@DisplayName("[포인트 충전]충전 내역 기록")
	void recordChargePointHistory(){
	//Given
		//충전 포인트
		long chargePointAmount = 1_000L;

		//소유 포인트가 없는 유저포인트 Mock 객체
		when(userPointTable.selectById(id)).thenReturn(new UserPoint(id,0L,System.currentTimeMillis()));
		//chargePointAmount 만큼 포인트 충전된 Mock 객체
		when(userPointTable.insertOrUpdate(id,chargePointAmount)).thenReturn(new UserPoint(id,chargePointAmount,System.currentTimeMillis()));

		PointHistory chargePointHistory = new PointHistory(1,id,chargePointAmount,TransactionType.CHARGE,System.currentTimeMillis());
		List<PointHistory> pointHistories = new ArrayList<>();
		pointHistories.add(chargePointHistory);
		//충전 내역 입력 Mock 객체
		when(pointHistoryTable.insert(id,chargePointAmount,TransactionType.CHARGE,System.currentTimeMillis())).thenReturn(chargePointHistory);
		//충전 내역 조회 Mock 객체
		when(pointHistoryTable.selectAllByUserId(id)).thenReturn(pointHistories);
	//When
		UserPointService userPointService = new UserPointService(userPointTable,pointHistoryTable);
		userPointService.chargePoint(id, chargePointAmount);

		List<PointHistory> pointHistory = pointHistoryTable.selectAllByUserId(id);
	//Then
		//충전 이력 정상 입력 확인
		assertEquals(1,pointHistory.size());//1회 충전으로 리턴 리스트 사이즈는 1이어야 함
		assertEquals(id,pointHistory.get(0).userId());
		assertEquals(chargePointAmount,pointHistory.get(0).amount());
		assertEquals(TransactionType.CHARGE,pointHistory.get(0).type());
	}

	@ParameterizedTest
	@DisplayName("[포인트 사용][잔고 부족]소유 포인트가 0P 이거나 사용할 포인트보다 작은 경우 사용 실패")
	@ValueSource(longs = {0L,10_000L})
	void notEnoughValance(long ownPointAmount){
	//Given
		long usePointAmount = 100_000L;

		//소유 포인트를 ValueSouce에서 입력받는 유저포인트 Mock 객체
		when(userPointTable.selectById(id)).thenReturn(new UserPoint(id,ownPointAmount,System.currentTimeMillis()));
	//When
		UserPointService userPointService = new UserPointService(userPointTable,pointHistoryTable);
		CustomException ce = assertThrows(CustomException.class,() -> userPointService.usePoint(id, usePointAmount - ownPointAmount),"잔여 포인트 부족");
	//Then
		assertEquals(602,ce.getErrorCode().getStatus());
		assertEquals("NOT_ENOUGH_VALANCE",ce.getErrorCode().getCode());
		assertEquals("잔여 포인트 부족",ce.getErrorCode().getMessage());
	}

	@Test
	@DisplayName("[포인트 사용]입력받은 포인트 만큼 소유 포인트 에서 차감")
	void usePoint(){
	//Given
		long usePointAmount = 1_000L;
		long ownUserPointAmount = 10_000L;
		long remainingUserPointAmount = ownUserPointAmount - usePointAmount;

		//소유 포인트 10_000P 유저포인트 Mock 객체
		when(userPointTable.selectById(id)).thenReturn(new UserPoint(id,ownUserPointAmount,System.currentTimeMillis()));
		//usePointAmount 만큼 포인트 사용 후 remainingUserPointAmount 유저포인트 Mock 객체
		when(userPointTable.insertOrUpdate(id,remainingUserPointAmount)).thenReturn(new UserPoint(id,remainingUserPointAmount,System.currentTimeMillis()));
	//When
		UserPointService userPointService = new UserPointService(userPointTable,pointHistoryTable);
		UserPoint afterUseUserPoint =  userPointService.usePoint(id, usePointAmount);
	//Then
		//포인트 정상 사용 확인
		assertEquals(id,afterUseUserPoint.id());
		assertEquals(remainingUserPointAmount,afterUseUserPoint.point());
	}

	@Test
	@DisplayName("[포인트 사용]사용 내역 기록")
	void recordUsePointHistory(){
	//Given
		long usePointAmount = 1_000L;
		long ownUserPointAmount = 10_000L;
		long remainingUserPointAmount = ownUserPointAmount - usePointAmount;

		//소유 포인트 10_000P 유저포인트 Mock 객체
		when(userPointTable.selectById(id)).thenReturn(new UserPoint(id,ownUserPointAmount,System.currentTimeMillis()));
		//usePointAmount 만큼 포인트 사용 후 remainingUserPointAmount 유저포인트 Mock 객체
		when(userPointTable.insertOrUpdate(id,remainingUserPointAmount)).thenReturn(new UserPoint(id,remainingUserPointAmount,System.currentTimeMillis()));

		PointHistory usePointHistory = new PointHistory(1,id,usePointAmount,TransactionType.USE,System.currentTimeMillis());
		List<PointHistory> pointHistories = new ArrayList<>();
		pointHistories.add(usePointHistory);
		//사용 내역 입력 Mock 객체
		when(pointHistoryTable.insert(id,usePointAmount,TransactionType.USE,System.currentTimeMillis())).thenReturn(usePointHistory);
		//사용 내역 조회 Mock 객체
		when(pointHistoryTable.selectAllByUserId(id)).thenReturn(pointHistories);
	//When
		UserPointService userPointService = new UserPointService(userPointTable,pointHistoryTable);
		userPointService.usePoint(id, usePointAmount);

		List<PointHistory> pointHistory = pointHistoryTable.selectAllByUserId(id);
	//Then
		//사용 내역 정상 입력 확인
		assertEquals(1,pointHistory.size());//1회 사용으로 리턴 리스트 사이즈는 1이어야 함
		assertEquals(id,pointHistory.get(0).userId());
		assertEquals(usePointAmount,pointHistory.get(0).amount());
		assertEquals(TransactionType.USE,pointHistory.get(0).type());
	}

	@Test
	@DisplayName("[포인트 조회]소유 포인트 조회")
	void selectOwnPoint(){
	//Given
		long ownUserPointAmount = 10_000L;

		//소유 포인트 10_000P 유저포인트 Mock 객체
		when(userPointTable.selectById(id)).thenReturn(new UserPoint(id,ownUserPointAmount,System.currentTimeMillis()));
	//When
		UserPointService userPointService = new UserPointService(userPointTable,pointHistoryTable);
		UserPoint currentUserPoint = userPointService.selectUserPoint(id);
	//Then
		assertEquals(ownUserPointAmount,currentUserPoint.point());
	}

	@Test
	@DisplayName("[포인트 충전/사용 내역 조회]포인트 충전/사용 내역 조회")
	void selectUserPointHistory(){
	//Given
		long chargePointAmount = 10_000L;
		long usePointAmount = 1_000L;

		PointHistory chargePointHistory = new PointHistory(1,id,chargePointAmount,TransactionType.CHARGE,System.currentTimeMillis());
		PointHistory usePointHistory = new PointHistory(2,id,usePointAmount,TransactionType.USE,System.currentTimeMillis());
		List<PointHistory> pointHistories = new ArrayList<>();
		pointHistories.add(chargePointHistory);
		pointHistories.add(usePointHistory);
		//사용 내역 조회 Mock 객체
		when(pointHistoryTable.selectAllByUserId(id)).thenReturn(pointHistories);
	//When
		UserPointService userPointService = new UserPointService(userPointTable,pointHistoryTable);
		List<PointHistory> pointHistorieList = userPointService.selectUserPointHistory(id);
	//Then
		//충전 내역 정상 입력 확인
		assertEquals(2,pointHistorieList.size());//충전 후 사용으로 리턴 리스트 사이즈는 2이어야 함
		assertEquals(id,pointHistorieList.get(0).userId());
		assertEquals(chargePointAmount,pointHistorieList.get(0).amount());
		assertEquals(TransactionType.CHARGE,pointHistorieList.get(0).type());

		//사용 내역 정상 입력 확인
		assertEquals(2,pointHistorieList.size());//충전 후 사용으로 리턴 리스트 사이즈는 2이어야 함
		assertEquals(id,pointHistorieList.get(1).userId());
		assertEquals(usePointAmount,pointHistorieList.get(1).amount());
		assertEquals(TransactionType.USE,pointHistorieList.get(1).type());
	}
}
