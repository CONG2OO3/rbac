package com.company.rbac.system.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.company.rbac.common.exception.BusinessException;
import com.company.rbac.system.entity.*;
import com.company.rbac.system.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SystemCrudService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysMenuMapper menuMapper;
    private final SysDeptMapper deptMapper;
    private final SysPostMapper postMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final ObjectMapper objectMapper;

    private <T> IPage<T> page(BaseMapper<T> mapper, long pageNum, long pageSize,
                              String keyword, String... columns) {
        QueryWrapper<T> query = new QueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            query.and(wrapper -> {
                for (int i = 0; i < columns.length; i++) {
                    if (i == 0) {
                        wrapper.like(columns[i], keyword);
                    } else {
                        wrapper.or().like(columns[i], keyword);
                    }
                }
            });
        }
        query.orderByDesc("id");
        return mapper.selectPage(new Page<>(pageNum, pageSize), query);
    }

    public IPage<SysUser> users(long pageNum, long pageSize, String keyword) {
        return page(userMapper, pageNum, pageSize, keyword, "user_name", "nick_name", "phone");
    }

    public SysUser user(Long id) { return userMapper.selectById(id); }

    @Transactional
    public void save(SysUser user) {
        if (user.getId() == null) {
            SysUser existing = userMapper.selectByUserNameIncludingDeleted(user.getUserName());
            if (existing != null && !Integer.valueOf(1).equals(existing.getDeleted())) {
                throw new BusinessException("账号已存在");
            }
            if (existing != null) {
                userRoleMapper.delete(new QueryWrapper<SysUserRole>()
                        .eq("user_id", existing.getId()));
                userMapper.physicalDeleteById(existing.getId());
            }
        }
        userMapper.insert(user);
    }

    public void update(SysUser user) { userMapper.updateById(user); }
    @Transactional
    public void removeUsers(List<Long> ids) {
        ids.forEach(id -> {
            userRoleMapper.delete(new QueryWrapper<SysUserRole>()
                    .eq("user_id", id));
            userMapper.physicalDeleteById(id);
        });
    }

    public IPage<SysRole> roles(long pageNum, long pageSize, String keyword) {
        return page(roleMapper, pageNum, pageSize, keyword, "role_name", "role_key");
    }

    public void save(SysRole role) { roleMapper.insert(role); }
    public void update(SysRole role) { roleMapper.updateById(role); }
    public void removeRoles(List<Long> ids) { ids.forEach(roleMapper::deleteById); }

    public IPage<SysMenu> menus(long pageNum, long pageSize, String keyword) {
        return page(menuMapper, pageNum, pageSize, keyword, "menu_name", "perms");
    }

    public List<SysMenu> menuTree() {
        return menuMapper.selectList(new QueryWrapper<SysMenu>().orderByAsc("parent_id", "order_num"));
    }

    public void save(SysMenu menu) { menuMapper.insert(menu); }
    public void update(SysMenu menu) { menuMapper.updateById(menu); }
    public void removeMenus(List<Long> ids) { ids.forEach(menuMapper::deleteById); }

    public IPage<SysDept> depts(long pageNum, long pageSize, String keyword) {
        return page(deptMapper, pageNum, pageSize, keyword, "dept_name", "leader", "phone");
    }

    public void save(SysDept dept) { deptMapper.insert(dept); }
    public void update(SysDept dept) { deptMapper.updateById(dept); }
    public void removeDepts(List<Long> ids) { ids.forEach(deptMapper::deleteById); }

    public IPage<SysPost> posts(long pageNum, long pageSize, String keyword) {
        return page(postMapper, pageNum, pageSize, keyword, "post_name", "post_code");
    }

    public void save(SysPost post) { postMapper.insert(post); }
    public void update(SysPost post) { postMapper.updateById(post); }
    public void removePosts(List<Long> ids) { ids.forEach(postMapper::deleteById); }

    public List<?> all(String resource) {
        return switch (resource) {
            case "users" -> userMapper.selectList(new QueryWrapper<SysUser>().orderByAsc("id"));
            case "roles" -> roleMapper.selectList(new QueryWrapper<SysRole>().orderByAsc("id"));
            case "menus" -> menuMapper.selectList(new QueryWrapper<SysMenu>().orderByAsc("id"));
            case "departments" -> deptMapper.selectList(new QueryWrapper<SysDept>().orderByAsc("id"));
            case "positions" -> postMapper.selectList(new QueryWrapper<SysPost>().orderByAsc("id"));
            default -> throw new BusinessException("不支持的数据类型");
        };
    }

    public void importRows(String resource, List<Map<String, String>> rows) {
        for (Map<String, String> row : rows) {
            switch (resource) {
                case "users" -> save(objectMapper.convertValue(row, SysUser.class));
                case "roles" -> save(objectMapper.convertValue(row, SysRole.class));
                case "menus" -> save(objectMapper.convertValue(row, SysMenu.class));
                case "departments" -> save(objectMapper.convertValue(row, SysDept.class));
                case "positions" -> save(objectMapper.convertValue(row, SysPost.class));
                default -> throw new BusinessException("不支持的数据类型");
            }
        }
    }

    public List<Long> userRoleIds(Long userId) {
        return userRoleMapper.selectList(new QueryWrapper<SysUserRole>().eq("user_id", userId))
                .stream().map(SysUserRole::getRoleId).toList();
    }

    public void setUserRoles(Long userId, List<Long> roleIds) {
        userRoleMapper.delete(new QueryWrapper<SysUserRole>().eq("user_id", userId));
        roleIds.forEach(roleId -> {
            SysUserRole relation = new SysUserRole();
            relation.setUserId(userId);
            relation.setRoleId(roleId);
            userRoleMapper.insert(relation);
        });
    }

    public List<Long> roleMenuIds(Long roleId) {
        return roleMenuMapper.selectList(new QueryWrapper<SysRoleMenu>().eq("role_id", roleId))
                .stream().map(SysRoleMenu::getMenuId).toList();
    }

    public void setRoleMenus(Long roleId, List<Long> menuIds) {
        roleMenuMapper.delete(new QueryWrapper<SysRoleMenu>().eq("role_id", roleId));
        menuIds.forEach(menuId -> {
            SysRoleMenu relation = new SysRoleMenu();
            relation.setRoleId(roleId);
            relation.setMenuId(menuId);
            roleMenuMapper.insert(relation);
        });
    }
}
