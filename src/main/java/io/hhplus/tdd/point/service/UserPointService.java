package io.hhplus.tdd.point.service;

import io.hhplus.tdd.point.dto.UserPoint;
import io.hhplus.tdd.point.util.exception.CustomException;
import io.hhplus.tdd.point.util.exception.ErrorCode;
import org.springframework.stereotype.Service;

@Service
public class UserPointService {

    public UserPoint chargePoint(long id, long chargePointAmount) throws CustomException {
        if(chargePointAmount <= 0L){
            throw new CustomException(ErrorCode.ZERO_POINT);
        }
        return new UserPoint(11L,0L,System.currentTimeMillis());
    }

}
