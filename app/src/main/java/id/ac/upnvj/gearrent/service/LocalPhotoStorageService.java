package id.ac.upnvj.gearrent.service;

import id.ac.upnvj.gearrent.security.AccessContextResolver;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/** Local disk-backed implementation of PhotoStorageService for M2 development & automated tests. */
@ApplicationScoped
public class LocalPhotoStorageService implements PhotoStorageService {

    @ConfigProperty(name = "gearrent.storage.bucket", defaultValue = "gearrent-inspections")
    String bucket;

    @ConfigProperty(name = "gearrent.storage.local-dir")
    String localDir;

    @Override
    public StoredPhoto store(String tenantId, Long inspectionId, String filename, String contentType, byte[] data) {
        if (data == null || data.length == 0) {
            throw AccessContextResolver.error(Response.Status.BAD_REQUEST, "Photo payload must not be empty.");
        }
        String safeName = sanitizeFilename(filename);
        String objectKey = tenantId + "/inspections/" + inspectionId + "/" + UUID.randomUUID() + "-" + safeName;
        Path target = resolveSafePath(objectKey);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, data);
        } catch (IOException e) {
            throw AccessContextResolver.error(Response.Status.INTERNAL_SERVER_ERROR, "Failed to store photo.");
        }
        String resolvedType = (contentType != null && !contentType.isBlank()) ? contentType : "application/octet-stream";
        return new StoredPhoto(bucket, objectKey, resolvedType, data.length);
    }

    @Override
    public byte[] load(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            throw AccessContextResolver.error(Response.Status.NOT_FOUND, "No photo attached to this inspection.");
        }
        Path source = resolveSafePath(objectKey);
        if (!Files.exists(source)) {
            throw AccessContextResolver.error(Response.Status.NOT_FOUND, "Stored photo object not found.");
        }
        try {
            return Files.readAllBytes(source);
        } catch (IOException e) {
            throw AccessContextResolver.error(Response.Status.INTERNAL_SERVER_ERROR, "Failed to read photo.");
        }
    }

    private Path resolveSafePath(String objectKey) {
        Path base = Path.of(localDir).toAbsolutePath().normalize();
        Path resolved = base.resolve(objectKey).normalize();
        if (!resolved.startsWith(base)) {
            throw AccessContextResolver.error(Response.Status.BAD_REQUEST, "Invalid photo object key.");
        }
        return resolved;
    }

    private static String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "inspection.bin";
        }
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
