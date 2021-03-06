package se.sics.kompics.wan.job;

import se.sics.kompics.Response;


public class JobStopResponse extends Response {

	public enum Status {
		ALREADY_STOPPED, STOPPED, COULD_NOT_FIND_PROCESS_HANDLE_TO_STOP_JOB
	};

	private final Status status;

	private String msg;

	private final int jobId;

	public JobStopResponse(JobStopRequest request, int jobId, JobStopResponse.Status status,
			String msg) {
		super(request);
		this.jobId = jobId;
		this.status = status;
		this.msg = msg;
	}

	public int getJobId() {
		return jobId;
	}

	public Status getStatus() {
		return status;
	}

	public String getMsg() {
		return msg;
	}

}
