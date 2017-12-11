//
// Scaled - a scalable editor extensible via JVM languages
// http://github.com/scaled/scaled/blob/master/LICENSE

package scaled

import java.util.concurrent.{Executor => JExecutor, ExecutorService}

/** Static bits for [[Executor]]. */
object Executor {

  /** Used to report errors for actions queued for execution on the UI thread. */
  trait ErrorHandler {
    /** Reports an unexpected error to the user. */
    def emitError (err :Throwable) :Unit
  }
}

/** Handles invoking execution units on the UI thread and background threads. */
abstract class Executor {
  import Executor._

  /** Runs operations on the UI thread. */
  val ui :Scheduler

  /** Runs operations on a background thread pool. */
  val bg :Scheduler

  /** The handler to which uncaught exceptions thrown by operations are reported. */
  val errHandler :ErrorHandler

  /** A Java `ExecutorService` which runs operations on a background thread pool. This is exposed
    * as a full executor service to provide its more robust API, but do not shut this service down.
    * That is handled by the editor. */
  def bgService :ExecutorService = throw new UnsupportedOperationException()

  /** Invokes `op` on the next UI tick. This can be invoked from the UI thread to defer an
    * operation until the next tick, or it can be invoked from a background thread to process
    * results back on the UI thread.
    */
  def runOnUI (op :Runnable) :Unit = ui.execute(new Runnable() {
    override def run () = try op.run() catch {
      case t :Throwable => errHandler.emitError(t)
    }
  })

  /** Invokes `op` on a background thread. There are a pool of background threads, so multiple
    * invocations of this method may result in the operations being run in parallel. An operation
    * sent to a background thread should not manipulate any data visible to other threads. Use
    * [[runOnUI]] to send results back to the UI thread to display to the user.
    */
  def runInBG (op :Runnable) :Unit = bg.execute(op)

  /** A Scala-friendly [[runOnUI(Runnable)]]. */
  def runOnUI[U] (op : => U) :Unit = ui.execute(new Runnable() {
    override def run () = try op catch {
      case t :Throwable => errHandler.emitError(t)
    }
  })

  /** A Scala-friendly [[runInBG(Runnable)]]. */
  def runInBG (op : => Unit) :Unit = bg.execute(new Runnable() {
    override def run () = op
  })

  /** Runs `op` on a background thread, reports the results on the UI thread.
    * @return a future that will deliver the result or cause of failure when available.
    */
  def runAsync[R] (op : => R) :Future[R] = {
    val result = uiPromise[R]
    bg.execute(new Runnable() {
      override def run () = try result.succeed(op) catch { case t :Throwable => result.fail(t) }
    })
    result
  }

  /** Returns a promise which will notify listeners of success or failure on the UI thread,
    * regardless of the thread on which it is completed.
    */
  def uiPromise[R] :Promise[R] = new Promise[R]() {
    private def superSucceed (value :R) = super.succeed(value)
    override def succeed (value :R) = runOnUI(new Runnable() {
      def run () = superSucceed(value)
    })
    private def superFail (cause :Throwable) = super.fail(cause)
    override def fail (cause :Throwable) = runOnUI(new Runnable() {
      def run () = superFail(cause)
    })
  }
}
