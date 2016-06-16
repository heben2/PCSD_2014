package com.acertainsupplychain.workloads;

public class AggregateResult {
	private int workers;
	private double throughput;
	private double succRatio;
	private double globalClientXactRatio;
	
	public AggregateResult(int workers, double throughput, double succRatio, double globalClientXactRatio) {
		this.workers = workers;
		this.throughput = throughput;
		this.succRatio = succRatio;
		this.globalClientXactRatio = globalClientXactRatio;
	}

	public int getWorkers() {
		return workers;
	}

	public void setWorkers(int workers) {
		this.workers = workers;
	}

	public double getThroughput() {
		return throughput;
	}

	public void setThroughput(double throughput) {
		this.throughput = throughput;
	}
	
    public double getsuccRatio() {
        return succRatio;
    }

    public void setsuccRatio(double succRatio) {
        this.succRatio = succRatio;
    }
    
    public double getGlobalClientXactRatio() {
        return globalClientXactRatio;
    }

    public void setGlobalClientXactRatio(double globalClientXactRatio) {
        this.globalClientXactRatio = globalClientXactRatio;
    }
}
