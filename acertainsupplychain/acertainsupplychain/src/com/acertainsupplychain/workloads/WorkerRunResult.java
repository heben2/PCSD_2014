package com.acertainsupplychain.workloads;

/**
 * 
 * WorkerRunResult class represents the result returned by a worker class after
 * running the workload interactions
 * 
 */
public class WorkerRunResult {
	private int successfulInteractions; // total number of successful interactions
	private int totalInteractions; // total number of interactions 
	private long elapsedTimeInNanoSecs; // total time taken to run all
										// interactions
	private int totalGlobalClientRuns;  // total number of
    									// global client interaction
    									// runs

	public WorkerRunResult(int successfulInteractions, long elapsedTimeInNanoSecs,
			int totalInteractions, int totalGlobalClientRuns) {
		this.setSuccessfulInteractions(successfulInteractions);
		this.setElapsedTimeInNanoSecs(elapsedTimeInNanoSecs);
		this.setTotalInteractions(totalInteractions);
		this.setTotalGlobalClientRuns(totalGlobalClientRuns);
	}

	public int getTotalInteractions() {
		return totalInteractions;
	}

	public void setTotalInteractions(int totalInteractions) {
		this.totalInteractions = totalInteractions;
	}

	public int getSuccessfulInteractions() {
		return successfulInteractions;
	}

	public void setSuccessfulInteractions(int successfulInteractions) {
		this.successfulInteractions = successfulInteractions;
	}

	public long getElapsedTimeInNanoSecs() {
		return elapsedTimeInNanoSecs;
	}

	public void setElapsedTimeInNanoSecs(long elapsedTimeInNanoSecs) {
		this.elapsedTimeInNanoSecs = elapsedTimeInNanoSecs;
	}

	public int getTotalGlobalClientRuns() {
		return totalGlobalClientRuns;
	}

	public void setTotalGlobalClientRuns(
			int totalGlobalClientRuns) {
		this.totalGlobalClientRuns = totalGlobalClientRuns;
	}

}
