package id.ac.upnvj.gearrent.service;

/**
 * Abstraction for private inspection photo object storage.
 * M2 uses a local temporary-directory backend; subsequent MinIO integration swaps in an S3 implementation.
 */
public interface PhotoStorageService {

    record StoredPhoto(String bucket, String objectKey, String contentType, long sizeBytes) {}

    StoredPhoto store(String tenantId, Long inspectionId, String filename, String contentType, byte[] data);

    byte[] load(String objectKey);
}
