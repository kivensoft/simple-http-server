package cn.kivensoft.http;

import cn.kivensoft.sql.BaseDao;

abstract public class DbCommand<T, R> extends BaseCommand<T, R> {
	abstract protected void initDao();
	abstract protected BaseDao getDao();
	
	public DbCommand() {
		super();
		initDao();
	}
	
	@Override
	protected void onException(Exception ex) {
		getDao().rollback();
	}

	@Override
	protected void onFinally() {
		getDao().close();
	}

	@Override
	protected void onFail(R value) throws Exception {
		getDao().rollback();
	}

	@Override
	protected void onSuccess(R value) throws Exception{
		getDao().commit();
	}

}
