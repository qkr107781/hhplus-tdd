package io.hhplus.tdd.point.controller;

import io.hhplus.tdd.point.dto.PointHistory;
import io.hhplus.tdd.point.dto.UserPoint;
import io.hhplus.tdd.point.service.UserPointService;
import io.hhplus.tdd.point.util.exception.CustomException;
import io.hhplus.tdd.point.util.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/point")
public class PointController {

    private static final Logger log = LoggerFactory.getLogger(PointController.class);

    private final UserPointService userPointService;

    public PointController(UserPointService userPointService){
        this.userPointService = userPointService;
    }

    /**
     * TODO - 특정 유저의 포인트를 조회하는 기능을 작성해주세요.
     */
    @GetMapping("{id}")
    public UserPoint point(
            @PathVariable long id
    ) {
        return new UserPoint(0, 0, 0);
    }

    /**
     * TODO - 특정 유저의 포인트 충전/이용 내역을 조회하는 기능을 작성해주세요.
     */
    @GetMapping("{id}/histories")
    public List<PointHistory> history(
            @PathVariable long id
    ) {
        return List.of();
    }

    /**
     * TODO - 특정 유저의 포인트를 충전하는 기능을 작성해주세요.
     */
    @PatchMapping("{id}/charge")
    public ResponseEntity<UserPoint> charge(
            @PathVariable long id,
            @RequestBody long chargeAmount
    ) {

        //최소 충전 포인트 체크
        if (chargeAmount <= UserPoint.MIN_POINT) {
            return ResponseEntity.badRequest().build();
        }
        //최대 충전 포인트 체크
        if (chargeAmount > UserPoint.MAX_POINT_PER_ONCE) {
            return ResponseEntity.badRequest().build();
        }

        UserPoint afterChargePoint = userPointService.chargePoint(id, chargeAmount);

        return ResponseEntity.ok(afterChargePoint);

    }
        /**
         * TODO - 특정 유저의 포인트를 사용하는 기능을 작성해주세요.
         */
    @PatchMapping("{id}/use")
    public ResponseEntity<UserPoint> use(
            @PathVariable long id,
            @RequestBody long usePointAmount
    ) {

        //사용 포인트 최소/최대 값 체크
        if(usePointAmount < UserPoint.MIN_POINT || usePointAmount > UserPoint.MAX_POINT){
            return ResponseEntity.badRequest().build();
        }

        UserPoint afterUsePoint = userPointService.usePoint(id, usePointAmount);

        return ResponseEntity.ok(afterUsePoint);
    }
}
