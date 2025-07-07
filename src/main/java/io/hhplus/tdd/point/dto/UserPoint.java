package io.hhplus.tdd.point.dto;

public record UserPoint(
        long id,
        long point,
        long updateMillis
) {

    public static final long MIN_POINT = 0L;
    public static final long MAX_POINT = 1_000_000L;
    public static final long MAX_POINT_PER_ONCE = 100_000L;

    public static UserPoint empty(long id) {
        return new UserPoint(id, 0, System.currentTimeMillis());
    }
}
