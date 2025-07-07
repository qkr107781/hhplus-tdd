package io.hhplus.tdd.point.service;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.dto.TransactionType;
import io.hhplus.tdd.point.dto.UserPoint;
import io.hhplus.tdd.point.util.exception.CustomException;
import io.hhplus.tdd.point.util.exception.ErrorCode;
import org.springframework.stereotype.Service;

@Service
public class UserPointService {

    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;

    public UserPointService(UserPointTable userPointTable, PointHistoryTable pointHistoryTable) {
        this.userPointTable = userPointTable;
        this.pointHistoryTable = pointHistoryTable;
    }

    /**
     * 입력받은 유저 포인트 충전
     * @param id 유저 ID
     * @param chargePointAmount 충전 요청 포인트
     * @return UserPoint 충전 완료된 유저 데이터
     * @throws CustomException 예외처리 공통 클래스
     */
    public UserPoint chargePoint(long id, long chargePointAmount) throws CustomException {
        //최소 충전 포인트 체크
        if(chargePointAmount <= 0L){
            throw new CustomException(ErrorCode.ZERO_POINT);
        }
        //최대 충전 포인트 체크
        if(chargePointAmount > 100_000L){
            throw new CustomException(ErrorCode.LIMIT_ONETIME_CHARGE_AMOUNT);
        }

        //현재 소유 포인트 조회
        UserPoint currentUserPoint = userPointTable.selectById(id);

        //소유 포인트와 충전 포인트 합
        chargePointAmount += currentUserPoint.point();
        //최대 잔고 초과 충전 체크
        if(chargePointAmount > 1_000_000L){
            throw new CustomException(ErrorCode.OVER_CHARGE);
        }

        //충전
        UserPoint afterChargeUserPoint = userPointTable.insertOrUpdate(id,chargePointAmount);

        //충전 내역 기록
        pointHistoryTable.insert(id,chargePointAmount, TransactionType.CHARGE,afterChargeUserPoint.updateMillis());

        return afterChargeUserPoint;
    }

    public UserPoint usePoint(long id, long usePointAmount){

        //현재 소유 포인트 조회
        UserPoint currentUserPoint = userPointTable.selectById(id);
        long ownUserPoint = currentUserPoint.point();
        
        //잔여 포인트가 0P 이거나 잔여 포인트 - 사용 포인트가 0P 보다 작은지 체크
        if(ownUserPoint == 0 || (ownUserPoint - usePointAmount) < 0){
            throw new CustomException(ErrorCode.NOT_ENOUGH_VALANCE);
        }

        return null;
    }

}
