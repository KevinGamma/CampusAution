package com.campus.auction.controller;

import com.campus.auction.annotation.RoleAccess;
import com.campus.auction.common.Result;
import com.campus.auction.enums.UserRole;
import com.campus.auction.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/images")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    /**
     * POST /api/images/upload — upload a single product or avatar image.
     *
     * <p>Accepts multipart/form-data with a {@code file} part.
     * Returns the relative URL path that can be stored and later fetched via
     * the static resource handler at {@code GET /api/images/{filename}}.
     *
     * <p>Allowed types: JPEG, PNG, GIF, WebP. Max size: 10 MB.
     * Requires an authenticated STUDENT or ADMIN token.
     */
    @PostMapping("/upload")
    @RoleAccess({UserRole.STUDENT, UserRole.ADMIN})
    public Result<String> upload(@RequestParam("file") MultipartFile file) {
        return Result.ok(fileService.uploadImage(file));
    }
}
