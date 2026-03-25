package com.YuanQi.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.YuanQi.pojo.GeneratedContent;
import com.YuanQi.service.GeneratedContentService;
import com.YuanQi.utils.Result;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员-生成内容管理控制器
 */
@RestController
@RequestMapping("/content")
@RequiredArgsConstructor
public class GeneratedContentController {

    private final GeneratedContentService generatedContentService;

    /**
     * 分页查询自己的生成内容记录
     */
    @GetMapping("/my")
    public Result<IPage<GeneratedContent>> my(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer status) {
        IPage<GeneratedContent> result = generatedContentService.pageList(page, size, StpUtil.getLoginIdAsLong(), type, status);
        return Result.success(result);
    }

    /**
     * 分页查询生成内容记录（管理员）
     */
    @GetMapping("/list")
    @SaCheckRole("admin")
    public Result<IPage<GeneratedContent>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer status) {
        IPage<GeneratedContent> result = generatedContentService.pageList(page, size, userId, type, status);
        return Result.success(result);
    }

    /**
     * 删除自己的生成内容记录
     */
    @DeleteMapping("/my/{id}")
    public Result<Void> deleteMyContent(@PathVariable Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        generatedContentService.deleteByIdAndUserId(id, userId);
        return Result.success();
    }

    /**
     * 删除生成内容记录（管理员）
     */
    @DeleteMapping("/{id}")
    @SaCheckRole("admin")
    public Result<Void> delete(@PathVariable Long id) {
        generatedContentService.removeById(id);
        return Result.success();
    }
}
