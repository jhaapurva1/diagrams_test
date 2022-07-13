package com.meesho.cps.db.redis.dao;

public interface SuppliersWeeklyBudgetExhaustEventStateDao {

    boolean isEventAlreadyFired(Long supplierId);

    void setEventAsFired(Long supplierId);

}
