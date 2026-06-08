package com.campus.auction.resource;

import com.campus.auction.annotation.RoleAccess;
import com.campus.auction.common.Result;
import com.campus.auction.enums.UserRole;
import com.campus.auction.exception.ServiceException;
import com.campus.auction.service.FileService;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import lombok.RequiredArgsConstructor;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

@Component
@Path("/api/v1/images")
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
public class FileResource {

    private final FileService fileService;

    /**
     * POST /api/v1/images/upload — upload a single product or avatar image.
     *
     * <p>Accepts multipart/form-data with a "file" part. The incoming stream is
     * adapted to a Spring {@link MultipartFile} via {@link FormDataMultipartFile},
     * which keeps small files in memory and spills large files to a temporary file
     * to avoid heap pressure / OOM under concurrency. The adapter is {@link Closeable}
     * and wrapped in try-with-resources so any temp file is deleted as soon as the
     * request finishes (success or failure).
     *
     * <p>{@link FileService} is consumed unchanged.
     */
    @POST
    @Path("/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)   // overrides the absent class-level @Consumes
    @RoleAccess({UserRole.STUDENT, UserRole.ADMIN})
    public Result<String> upload(
            @FormDataParam("file") InputStream                fileStream,
            @FormDataParam("file") FormDataContentDisposition fileDetail,
            @FormDataParam("file") FormDataBodyPart           bodyPart) throws IOException {

        if (fileStream == null) {
            throw new ServiceException(HttpStatus.BAD_REQUEST, "No file provided");
        }

        String originalFilename = fileDetail != null ? fileDetail.getFileName() : "upload";
        String contentType = bodyPart != null
                ? bodyPart.getMediaType().toString()
                : MediaType.APPLICATION_OCTET_STREAM;

        // try-with-resources guarantees temp-file cleanup once the service has consumed the file.
        try (FormDataMultipartFile multipartFile =
                     FormDataMultipartFile.consume("file", originalFilename, contentType, fileStream)) {
            return Result.ok(fileService.uploadImage(multipartFile));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Adapter: wraps Jersey multipart data as a Spring MultipartFile so that
    // FileService.uploadImage(MultipartFile) needs no changes whatsoever.
    //
    // Memory strategy (hybrid threshold spill):
    //   • ≤ MEMORY_THRESHOLD bytes → buffered in a byte[] (fast, no disk I/O).
    //   • >  MEMORY_THRESHOLD bytes → streamed to a temp file; the heap never
    //     holds more than one read chunk beyond the threshold.
    //   • Closeable → temp file deleted via try-with-resources in upload().
    // ─────────────────────────────────────────────────────────────────────────
    static final class FormDataMultipartFile implements MultipartFile, Closeable {

        /** Files at or below this size stay on the heap; larger ones spill to disk. */
        private static final int MEMORY_THRESHOLD = 2 * 1024 * 1024; // 2 MB
        private static final int CHUNK = 8 * 1024;                   // 8 KB read buffer

        private final String name;
        private final String originalFilename;
        private final String contentType;
        private final long   size;

        /** Exactly one of these is non-null: in-memory buffer XOR spilled temp file. */
        private final byte[] content;              // non-null  ⇔ small file
        private final java.nio.file.Path tempFile; // non-null  ⇔ large file

        private FormDataMultipartFile(String name, String originalFilename, String contentType,
                                      long size, byte[] content, java.nio.file.Path tempFile) {
            this.name             = name;
            this.originalFilename = originalFilename;
            this.contentType      = contentType;
            this.size             = size;
            this.content          = content;
            this.tempFile         = tempFile;
        }

        /**
         * Drains {@code in}, keeping the bytes in memory while small and spilling to a
         * temp file once {@link #MEMORY_THRESHOLD} is exceeded. The input stream is fully
         * consumed; the caller still owns closing {@code in} (Jersey does that).
         */
        static FormDataMultipartFile consume(String name, String originalFilename,
                                             String contentType, InputStream in) throws IOException {
            ByteArrayOutputStream heapBuffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[CHUNK];
            long total = 0;
            int n;

            while ((n = in.read(chunk)) != -1) {
                total += n;

                if (total <= MEMORY_THRESHOLD) {
                    heapBuffer.write(chunk, 0, n);
                    continue;
                }

                // Threshold exceeded → spill everything (already-buffered + current + rest) to disk.
                java.nio.file.Path temp = Files.createTempFile("campus-upload-", ".tmp");
                try (OutputStream fout = Files.newOutputStream(temp)) {
                    heapBuffer.writeTo(fout);      // flush what was buffered so far
                    fout.write(chunk, 0, n);       // the chunk that tipped us over
                    while ((n = in.read(chunk)) != -1) {   // drain the remainder straight to disk
                        total += n;
                        fout.write(chunk, 0, n);
                    }
                } catch (IOException ex) {
                    Files.deleteIfExists(temp);    // don't leak a half-written temp file
                    throw ex;
                }
                return new FormDataMultipartFile(name, originalFilename, contentType, total, null, temp);
            }

            // Stayed within the threshold → keep it on the heap.
            return new FormDataMultipartFile(name, originalFilename, contentType,
                    total, heapBuffer.toByteArray(), null);
        }

        private boolean spilled() { return tempFile != null; }

        @Override public String  getName()             { return name; }
        @Override public String  getOriginalFilename() { return originalFilename; }
        @Override public String  getContentType()      { return contentType; }
        @Override public boolean isEmpty()             { return size == 0; }
        @Override public long    getSize()             { return size; }

        @Override
        public InputStream getInputStream() throws IOException {
            return spilled() ? Files.newInputStream(tempFile) : new ByteArrayInputStream(content);
        }

        /**
         * Note: for a spilled file this re-reads the whole file into a byte[].
         * {@link FileService} never calls this (it uses {@link #getInputStream()}),
         * so the heap-friendly path is preserved in practice.
         */
        @Override
        public byte[] getBytes() throws IOException {
            return spilled() ? Files.readAllBytes(tempFile) : content;
        }

        @Override
        public Resource getResource() {
            return spilled() ? new FileSystemResource(tempFile)
                             : new ByteArrayResource(content, originalFilename);
        }

        /** Zero-copy for the spilled case — never re-inflates the file onto the heap. */
        @Override
        public void transferTo(File dest) throws IOException {
            if (spilled()) {
                Files.copy(tempFile, dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } else {
                Files.write(dest.toPath(), content);
            }
        }

        /** Deletes the backing temp file, if any. Invoked by try-with-resources. */
        @Override
        public void close() throws IOException {
            if (spilled()) {
                Files.deleteIfExists(tempFile);
            }
        }
    }
}
