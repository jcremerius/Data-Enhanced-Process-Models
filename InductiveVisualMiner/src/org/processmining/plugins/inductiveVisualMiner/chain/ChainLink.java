package org.processmining.plugins.inductiveVisualMiner.chain;

import java.util.UUID;
import java.util.concurrent.Executor;

import javax.swing.SwingUtilities;

import org.processmining.framework.plugin.ProMCanceller;

public abstract class ChainLink<State, I, O> {
	private Runnable onStart;
	private Runnable onStartStatus;
	private Runnable onComplete;
	private Runnable onCompleteStatus;
	private OnException onException;
	private Runnable onInvalidate;

	private boolean isComplete = false;
	private IvMCanceller currentExecutionCanceller = null;
	private UUID currentExecutionId = null;

	public abstract String getName();

	/**
	 * 
	 * @return The text that is to be shown when this chainlink is executing,
	 *         with two postfix dots.
	 */
	public abstract String getStatusBusyMessage();

	/**
	 * 
	 * @return
	 * 
	 * 		Gathers all inputs required for the computation
	 */
	protected abstract I generateInput(State state);

	/**
	 * Performs the computation, given the input. Side-effects not allowed;
	 * should be thread-safe.
	 * 
	 * @param input
	 * @param canceller
	 * @return
	 * @throws Exception
	 */
	protected abstract O executeLink(I input, IvMCanceller canceller) throws Exception;

	/**
	 * 
	 * @param result
	 * @param state
	 * 
	 *            Processes the result of the computation. Guarantee: if
	 *            executed, then all inputs are still relevant and have not been
	 *            replaced.
	 */
	protected abstract void processResult(O result, State state);

	/**
	 * 
	 * @param state
	 * 
	 *            Invalidate the results of this computation.
	 */
	protected abstract void invalidateResult(State state);

	public boolean isComplete() {
		return isComplete;
	}

	public boolean equals(Object other) {
		return this.getClass() == other.getClass();
	}

	public void cancelAndInvalidateResult(State state) {
		//System.out.println("invalidate " + this);
		isComplete = false;
		if (currentExecutionCanceller != null) {
			currentExecutionCanceller.cancel();
		}
		currentExecutionId = null;
		currentExecutionCanceller = null;
		if (onInvalidate != null) {
			onInvalidate.run();
		}
		invalidateResult(state);
	}

	public void execute(ProMCanceller globalCanceller, Executor executor, final State state,
			final Chain<State> chain) {
		if (currentExecutionCanceller != null) {
			currentExecutionCanceller.cancel();
		}
		currentExecutionCanceller = new IvMCanceller(globalCanceller);
		currentExecutionId = UUID.randomUUID();
		isComplete = false;

		final IvMCanceller canceller = currentExecutionCanceller;
		final UUID id = currentExecutionId;

		final I input = generateInput(state);

		executor.execute(new Runnable() {
			public void run() {
				if (onStart != null) {
					SwingUtilities.invokeLater(onStart);
				}
				if (onStartStatus != null) {
					SwingUtilities.invokeLater(onStartStatus);
				}
				final O result;
				try {
					result = executeLink(input, canceller);
				} catch (final Exception e) {
					try {
						SwingUtilities.invokeLater(new Runnable() {
							public void run() {
								onException.onException(e);
							}
						});
						e.printStackTrace();
					} catch (Exception e1) {
						e1.printStackTrace();
					}
					return;
				}
				//process the result
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						if (id.equals(currentExecutionId) && !canceller.isCancelled()) {
							processResult(result, state);
							if (onComplete != null) {
								onComplete.run();
							}
							if (onCompleteStatus != null) {
								onCompleteStatus.run();
							}
							isComplete = true;

							chain.executeNext(ChainLink.this);
						} else {

						}
					}
				});
			}
		});
	}

	/**
	 * Sets a callback that is executed on start of execution. Will be executed
	 * in the main (gui) thread.
	 * 
	 * @param onStart
	 */
	public void setOnStart(Runnable onStart) {
		this.onStart = onStart;
	}

	/**
	 * Sets a callback that is executed on start of execution. Will be executed
	 * in the main (gui) thread.
	 * 
	 * @param onStartStatus
	 */
	public void setOnStartStatus(Runnable onStartStatus) {
		this.onStartStatus = onStartStatus;
	}

	/**
	 * Sets a callback that is executed on completion of execution. Will be
	 * executed in the main (gui) thread.
	 * 
	 * @param onStart
	 */
	public void setOnComplete(Runnable onComplete) {
		this.onComplete = onComplete;
	}

	/**
	 * Sets a callback that is executed on completion of execution. Will be
	 * executed in the main (gui) thread.
	 * 
	 * @param onStart
	 */
	public void setOnCompleteStatus(Runnable onCompleteStatus) {
		this.onCompleteStatus = onCompleteStatus;
	}

	public void setOnException(OnException onException) {
		this.onException = onException;
	}

	public void setOnInvalidate(Runnable onInvalidate) {
		this.onInvalidate = onInvalidate;
	}
}
