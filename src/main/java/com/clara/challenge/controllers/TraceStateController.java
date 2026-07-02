package com.clara.challenge.controllers;

import com.clara.challenge.dtos.TraceStatusResponse;
import com.clara.challenge.services.TraceStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/traces")
@RequiredArgsConstructor
public class TraceStateController {

  private final TraceStateService traceStateService;

  @GetMapping("/{traceId}/status")
  public TraceStatusResponse getStatus(@PathVariable String traceId) {
    return traceStateService.getStatus(traceId);
  }
}
