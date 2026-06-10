package com.bookly.domain.tenant;

public enum Plan {
    FREE(1, 50),
    PRO(5, Integer.MAX_VALUE),
    ENTERPRISE(Integer.MAX_VALUE, Integer.MAX_VALUE);

    private final int maxStaff;
    private final int maxMonthlyBookings;

    Plan(int maxStaff, int maxMonthlyBookings) {
        this.maxStaff = maxStaff;
        this.maxMonthlyBookings = maxMonthlyBookings;
    }

    public int getMaxStaff() { return maxStaff; }
    public int getMaxMonthlyBookings() { return maxMonthlyBookings; }
    public boolean hasUnlimitedStaff() { return maxStaff == Integer.MAX_VALUE; }
    public boolean hasUnlimitedBookings() { return maxMonthlyBookings == Integer.MAX_VALUE; }
}
