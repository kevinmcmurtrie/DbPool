package us.pixelmemory.dbPool;

/**
 * Basic profiles to reduce initial configuration effort for the DbPoolSettings object
 */
public enum DbPoolProfile {
	/**
	 * Very conservative database use
	 */
	TINY {
		@Override
		DbPoolSettings apply(DbPoolSettings ps) {
			ps.setMaxOpen(64);
			ps.setGiveUpMillis(45 * 1000);
			ps.setGiveUpBrokenMillis(1000);
			ps.setMaxIdleMillis(2000);
			ps.setOpenBrokenRateMillis(1000);
			ps.setOpenConcurrent(2);
			ps.setValidateInterval(30 * 1000);
			ps.setWarnLongUseMillis(15 * 60 * 1000);
			ps.setFifo(true);
			return ps;
		}
	},
	/**
	 * Surge smoothing and normal failure timeouts for interactive tasks
	 */
	GENTLE {
		@Override
		DbPoolSettings apply(DbPoolSettings ps) {
			ps.setMaxOpen(200);
			ps.setGiveUpMillis(30 * 1000);
			ps.setGiveUpBrokenMillis(1000);
			ps.setMaxIdleMillis(30 * 1000);
			ps.setOpenBrokenRateMillis(500);
			ps.setOpenConcurrent(6);
			ps.setValidateInterval(30 * 1000);
			ps.setWarnLongUseMillis(15 * 60 * 1000);
			ps.setFifo(true);
			return ps;
		}
	},
	/**
	 * Reduced surge smoothing and long failure timeouts for back-end tasks
	 */
	RELIABLE {
		@Override
		DbPoolSettings apply(DbPoolSettings ps) {
			ps.setMaxOpen(1000);
			ps.setGiveUpMillis(60 * 1000);
			ps.setGiveUpBrokenMillis(30 * 1000);
			ps.setMaxIdleMillis(60 * 1000);
			ps.setOpenBrokenRateMillis(500);
			ps.setOpenConcurrent(8);
			ps.setValidateInterval(30 * 1000);
			ps.setWarnLongUseMillis(30 * 60 * 1000);
			ps.setFifo(true);
			return ps;
		}
	},
	/**
	 * Very little surge smoothing and fast failure timeouts
	 */
	FAST {
		@Override
		DbPoolSettings apply(DbPoolSettings ps) {
			ps.setMaxOpen(1000);
			ps.setGiveUpMillis(5 * 1000);
			ps.setGiveUpBrokenMillis(500);
			ps.setMaxIdleMillis(5 * 60 * 1000);
			ps.setOpenBrokenRateMillis(250);
			ps.setOpenConcurrent(24);
			ps.setValidateInterval(60 * 1000);
			ps.setWarnLongUseMillis(60 * 1000);
			ps.setFifo(false);
			return ps;
		}
	};
	
	abstract DbPoolSettings apply(DbPoolSettings ps);
}
