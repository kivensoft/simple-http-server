package cn.kivensoft.http;

import cn.kivensoft.sql.BaseDbContext;

abstract public class DbCommand<T, R> extends BaseCommand<T, R> {

	abstract protected BaseDbContext getDbContext();
	
	@Override
	protected void onException(Exception ex) {
		BaseDbContext dbContext = getDbContext();
		if (dbContext != null) dbContext.rollback();
	}

	@Override
	protected void onFinally() {
		BaseDbContext dbContext = getDbContext();
		if (dbContext != null) dbContext.close();
	}

	@Override
	protected void onFail(R value) throws Exception {
		BaseDbContext dbContext = getDbContext();
		if (dbContext != null) dbContext.rollback();
	}

	@Override
	protected void onSuccess(R value) throws Exception{
		BaseDbContext dbContext = getDbContext();
		if (dbContext != null) dbContext.commit();
	}

}
