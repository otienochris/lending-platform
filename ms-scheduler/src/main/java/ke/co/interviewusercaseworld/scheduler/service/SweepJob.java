package ke.co.interviewusercaseworld.scheduler.service;

public interface SweepJob {
    String getCron();

    void schedule(String currentSweepJob);

    void delete();
}
