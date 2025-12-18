package seucxxy.csd.backend.cet4.service;

@FunctionalInterface
public interface CET4PaperGenerationProgressListener {
    void onProgress(int completed, int total);
}
