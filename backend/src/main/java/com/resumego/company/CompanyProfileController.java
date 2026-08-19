package com.resumego.company;

import com.resumego.common.ApiResponse;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/company-profiles")
public class CompanyProfileController {

    private final CompanyProfileService companyProfileService;

    @GetMapping("/resolve")
    public ResponseEntity<ApiResponse<Map<String, Object>>> resolveByCompanyName(
            @RequestParam(required = false) String companyName) {
        return ResponseEntity.ok(ApiResponse.ok(
                companyProfileService.findEnabledProfileByCompanyName(companyName)
        ));
    }
}
