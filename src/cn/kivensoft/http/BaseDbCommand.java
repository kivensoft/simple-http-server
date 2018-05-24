package cn.kivensoft.http;

import cn.kivensoft.sql.BaseDbContext;

abstract public class BaseDbCommand<T, R> extends BaseCommand<T, R> {

	abstract protected BaseDbContext getDbContext();
	
	@Override
	protected R onException(Exception ex) {
		BaseDbContext dbContext = getDbContext();
		if (dbContext != null) dbContext.rollback();
		return error("系统内部错误.");
	}

	@Override
	protected void onFinally() {
		BaseDbContext dbContext = getDbContext();
		if (dbContext != null) dbContext.close();
	}

	@Override
	protected R onFail(R value) throws Exception {
		BaseDbContext dbContext = getDbContext();
		if (dbContext != null) dbContext.rollback();
		return value;
	}

	@Override
	protected R onSuccess(R value) throws Exception{
		BaseDbContext dbContext = getDbContext();
		if (dbContext != null) dbContext.commit();
		return value;
	}

}
