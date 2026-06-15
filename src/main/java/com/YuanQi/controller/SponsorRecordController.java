package com.YuanQi.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.YuanQi.pojo.SponsorRecord;
import com.YuanQi.service.SponsorRecordService;
import com.YuanQi.utils.Result;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 赞助记录控制�? */
@RestController
@RequestMapping("/sponsor")
@RequiredArgsConstructor
public class SponsorRecordController {

    private final SponsorRecordService sponsorRecordService;

    /**
     * 获取赞助榜单（公开接口，按金额降序）    */
    @GetMapping("/list")
    public Result<List<SponsorRecord>> list() {
        LambdaQueryWrapper<SponsorRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(SponsorRecord::getAmount);
        List<SponsorRecord> list = sponsorRecordService.list(wrapper);
        return Result.success(list);
    }

    /**
     * 分页查询赞助记录（管理员）     */
    @SaCheckRole("admin")
    @GetMapping("/page")
    public Result<IPage<SponsorRecord>> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        Page<SponsorRecord> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<SponsorRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(SponsorRecord::getAmount);
        IPage<SponsorRecord> result = sponsorRecordService.page(pageParam, wrapper);
        return Result.success(result);
    }

    /**
     * 新增赞助记录（管理员）     */
    @SaCheckRole("admin")
    @PostMapping
    public Result<Void> add(@RequestBody SponsorRecord sponsorRecord) {
        sponsorRecordService.save(sponsorRecord);
        return Result.success();
    }

    /**
     * 修改赞助记录（管理员）     */
    @SaCheckRole("admin")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody SponsorRecord sponsorRecord) {
        sponsorRecord.setId(id);
        sponsorRecordService.updateById(sponsorRecord);
        return Result.success();
    }

    /**
     * 删除赞助记录（管理员)    */
    @SaCheckRole("admin")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        sponsorRecordService.removeById(id);
        return Result.success();
    }
}
