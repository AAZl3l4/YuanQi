package com.YuanQi.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.YuanQi.pojo.KnowledgeBase;
import com.YuanQi.service.KnowledgeBaseService;
import com.YuanQi.utils.Result;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 知识库控制器
 */
@RestController
@RequestMapping("/knowledge")
@RequiredArgsConstructor
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;

    /**
     * 创建知识库
     */
    @PostMapping("/create")
    public Result<KnowledgeBase> create(@RequestBody @Validated KnowledgeBase knowledgeBase) {
        Long userId = StpUtil.getLoginIdAsLong();
        knowledgeBase.setUserId(userId);
        KnowledgeBase created = knowledgeBaseService.create(knowledgeBase);
        return Result.success(created);
    }

    /**
     * 更新知识库
     */
    @PutMapping("/update")
    public Result<KnowledgeBase> update(@RequestBody KnowledgeBase knowledgeBase) {
        KnowledgeBase updated = knowledgeBaseService.update(knowledgeBase);
        return Result.success(updated);
    }

    /**
     * 删除知识库
     */
    @DeleteMapping("/my/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        knowledgeBaseService.delete(id);
        return Result.success();
    }

    /**
     * 获取知识库详情
     */
    @GetMapping("/{id}")
    public Result<KnowledgeBase> getById(@PathVariable Long id) {
        KnowledgeBase knowledgeBase = knowledgeBaseService.checkOwner(id);
        return Result.success(knowledgeBase);
    }

    /**
     * 获取当前用户的知识库列表（分页）
     */
    @GetMapping("/my")
    public Result<IPage<KnowledgeBase>> my(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        IPage<KnowledgeBase> result = knowledgeBaseService.pageList(page, size, userId, id);
        return Result.success(result);
    }

    /**
     * 管理员查询知识库列表（可按用户筛选）
     */
    @SaCheckRole("admin")
    @GetMapping("/list")
    public Result<IPage<KnowledgeBase>> adminList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long id) {
        IPage<KnowledgeBase> result = knowledgeBaseService.pageList(page, size, userId, id);
        return Result.success(result);
    }

    /**
     * 管理员删除任意知识库
     */
    @SaCheckRole("admin")
    @DeleteMapping("/{id}")
    public Result<Void> adminDelete(@PathVariable Long id) {
        knowledgeBaseService.adminDelete(id);
        return Result.success();
    }
}
