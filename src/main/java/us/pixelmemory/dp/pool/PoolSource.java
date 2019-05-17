package us.pixelmemory.dp.pool;

public interface PoolSource<T, ERR extends Exception> {
	T get() throws ERR;
	void takeBack(T element) throws ERR;
	boolean validate(T element) throws ERR;
	void shutdown();
}
