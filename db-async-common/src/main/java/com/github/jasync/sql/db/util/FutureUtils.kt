package com.github.jasync.sql.db.util

import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import java.util.function.BiConsumer
import java.util.function.Function


inline fun <A> Try<A>.asCompletedFuture(): CompletableFuture<A> = when (this) {
  is Success -> CompletableFuture.completedFuture(this.value)
  is Failure -> CompletableFuture<A>().failed(this.exception)
}

inline fun <A> CompletableFuture<A>.getAsTry(millis: Long, unit: TimeUnit): Try<A> = Try { get(millis, unit) }

inline fun <A, B> CompletableFuture<A>.mapTry(crossinline f: (A, Throwable?) -> B): CompletableFuture<B> =
    handle{a,t: Throwable? -> f(a,t)}

inline fun <A, B> CompletableFuture<A>.map(crossinline f: (A) -> B): CompletableFuture<B> =
    thenApply { f(it) }

inline fun <A, B> CompletableFuture<A>.map(executor: Executor, crossinline f: (A) -> B): CompletableFuture<B> =
    thenApplyAsync(Function { f(it) }, executor)

inline fun <A, B> CompletableFuture<A>.flatMap(executor: Executor, crossinline f: (A) -> CompletableFuture<B>): CompletableFuture<B> =
    thenComposeAsync(Function { f(it) }, executor)

fun <A> CompletableFuture<CompletableFuture<A>>.flatten(executor: Executor): CompletableFuture<A> = flatMap(executor) { it }

inline fun <A> CompletableFuture<A>.filter(executor: Executor, crossinline predicate: (A) -> Boolean): CompletableFuture<A> =
    map(executor) {
      if (predicate(it)) it else throw NoSuchElementException("CompletableFuture.filter predicate is not satisfied")
    }


inline fun <A> CompletableFuture<A>.onFailure(executor: Executor, crossinline onFailureFun: (Throwable) -> Unit): CompletableFuture<A> =
    whenCompleteAsync(BiConsumer { _, t -> if (t != null) onFailureFun(t) }, executor)

inline fun <A> CompletableFuture<A>.onComplete(executor: Executor, crossinline onCompleteFun: (A, Throwable?) -> Unit): CompletableFuture<A> =
    whenCompleteAsync(BiConsumer { a, t -> onCompleteFun(a, t) }, executor)

inline fun <A> CompletableFuture<A>.onComplete(crossinline onCompleteFun: (A, Throwable?) -> Unit): CompletableFuture<A> =
    whenComplete { a, t -> onCompleteFun(a, t) }

inline fun <A> CompletableFuture<A>.onComplete(crossinline onCompleteFun: (Try<A>) -> Unit): CompletableFuture<A> =
    whenComplete { a, t -> onCompleteFun(if (t != null) Try.raise(t) else Try.just(a)) }

inline fun <A> CompletableFuture<A>.onComplete(executor: Executor, crossinline onCompleteFun: (Try<A>) -> Unit): CompletableFuture<A> =
    whenCompleteAsync(BiConsumer { a, t -> onCompleteFun(if (t != null) Try.raise(t) else Try.just(a)) }, executor)

fun <A> CompletableFuture<A>.success(a: A): CompletableFuture<A> = this.also { it.complete(a) }

fun <A> CompletableFuture<A>.tryFailure(e: Throwable): Boolean = this.completeExceptionally(e)
fun <A> CompletableFuture<A>.failure(e: Throwable): Boolean = this.completeExceptionally(e)
fun <A> CompletableFuture<A>.failed(e: Throwable): CompletableFuture<A> = this.also { it.completeExceptionally(e) }
val <A> CompletableFuture<A>.isSuccess: Boolean get()= this.isDone && this.isCompleted
val <A> CompletableFuture<A>.isFailure: Boolean get()= this.isDone && this.isCompletedExceptionally


fun <A> CompletableFuture<A>.complete(t: Try<A>) = when (t) {
  is Success -> this.complete(t.value)
  is Failure -> this.completeExceptionally(t.exception)
}

val <A> CompletableFuture<A>.isCompleted get() = this.isDone

object FuturePromise {
  fun <A> successful(a: A): CompletableFuture<A> = CompletableFuture<A>().also { it.complete(a) }
  fun <A> failed(t: Throwable): CompletableFuture<A> = CompletableFuture<A>().failed(t)
}

