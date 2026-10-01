package com.ardencm.posttrade.allocation;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/allocations")
public class AllocationController {
    private final AllocationService service;

    public AllocationController(AllocationService service) {
        this.service = service;
    }

    @PostMapping
    public List<Allocation> allocate(@RequestBody BlockTrade block) {
        return service.allocate(block);
    }
}
