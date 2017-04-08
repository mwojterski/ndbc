package io.trane.ndbc.datasource;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import io.trane.future.Future;
import io.trane.future.Local;
import io.trane.ndbc.Connection;
import io.trane.ndbc.DataSource;
import io.trane.ndbc.PreparedStatement;
import io.trane.ndbc.ResultSet;

public class DefaultDataSource<C extends Connection> implements DataSource<C> {

  private final Pool<C> pool;
  private final Local<C> currentTransation;

  public DefaultDataSource(Pool<C> pool) {
    super();
    this.pool = pool;
    this.currentTransation = Local.apply();
  }

  @Override
  public Future<ResultSet> query(String query) {
    return withConnection(c -> c.query(query));
  }

  @Override
  public Future<Integer> execute(String statement) {
    return withConnection(c -> c.execute(statement));
  }

  @Override
  public Future<ResultSet> query(PreparedStatement query) {
    return withConnection(c -> c.query(query));
  }

  @Override
  public Future<Integer> execute(PreparedStatement statement) {
    return withConnection(c -> c.execute(statement));
  }

  @Override
  public <R> Future<R> transactional(Supplier<Future<R>> supplier) {
    if (currentTransation.get().isPresent())
      return Future.flatApply(supplier);
    else
      return pool.apply(c -> {
        currentTransation.set(Optional.of(c));
        return c.withTransaction(supplier).ensure(() -> currentTransation.set(Optional.empty()));
      });
  }

  @Override
  public final <R> Future<R> withConnection(Function<C, Future<R>> f) {
    return currentTransation.get().map(f).orElseGet(() -> pool.apply(f));
  }
}