package com.YuanQi.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
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
     * 删除生成内容记录（管理员）
     */
    @DeleteMapping("/{id}")
    @SaCheckRole("admin")
    public Result<Void> delete(@PathVariable Long id) {
        generatedContentService.removeById(id);
        return Result.success();
    }
}
