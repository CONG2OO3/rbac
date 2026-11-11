package com.company.rbac.monitor.service;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.rbac.common.api.PageResult;
import com.company.rbac.common.exception.BusinessException;
import com.company.rbac.system.entity.SysUserRole;
import com.company.rbac.system.entity.SysUser;
import com.company.rbac.system.mapper.SysRoleMapper;
import com.company.rbac.system.mapper.SysUserRoleMapper;
import com.company.rbac.system.mapper.SysUserMapper;
import com.sun.management.OperatingSystemMXBean;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MonitorService {

    private final JdbcTemplate jdbcTemplate;
    private final StringRedisTemplate redisTemplate;
    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;

    public ServerSnapshot serverSnapshot() {
        var runtime = ManagementFactory.getRuntimeMXBean();
        MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
        ThreadMXBean threads = ManagementFactory.getThreadMXBean();
        java.lang.management.OperatingSystemMXBean operatingSystem =
                ManagementFactory.getOperatingSystemMXBean();

        long heapUsed = memory.getHeapMemoryUsage().getUsed();
        long heapMax = memory.getHeapMemoryUsage().getMax();
        double cpuPercent = 0;
        long physicalTotal = 0;
        long physicalUsed = 0;
        if (operatingSystem instanceof OperatingSystemMXBean system) {
            cpuPercent = percent(system.getCpuLoad());
            physicalTotal = system.getTotalMemorySize();
            physicalUsed = Math.max(0, physicalTotal - system.getFreeMemorySize());
        }

        File disk = new File(".").getAbsoluteFile();
        long diskTotal = disk.getTotalSpace();
        long diskUsed = Math.max(0, diskTotal - disk.getUsableSpace());
        boolean mysqlHealthy = available(() -> jdbcTemplate.queryForObject("SELECT 1", Integer.class));
        boolean redisHealthy = available(() -> redisTemplate.execute(
                (RedisCallback<String>) connection -> connection.ping()));

        return new ServerSnapshot(
                LocalDateTime.now(),
                Math.round(cpuPercent * 10) / 10.0,
                heapUsed,
                heapMax,
                physicalUsed,
                physicalTotal,
                diskUsed,
                diskTotal,
                threads.getThreadCount(),
                runtime.getUptime(),
                operatingSystem.getAvailableProcessors(),
                operatingSystem.getName(),
                System.getProperty("java.version"),
                mysqlHealthy,
                redisHealthy);
    }

    public PageResult<OnlineUser> onlineUsers(long pageNum, long pageSize, String keyword) {
        if (pageNum < 1 || pageSize < 1 || pageSize > 100) {
            throw new BusinessException("分页参数不正确");
        }
        List<TokenEntry> entries = activeTokens();
        List<Long> userIds = entries.stream().map(TokenEntry::userId).distinct().toList();
        Map<Long, SysUser> users = userIds.isEmpty()
                ? Map.of()
                : userMapper.selectBatchIds(userIds).stream()
                        .collect(Collectors.toMap(SysUser::getId, Function.identity()));
        String filter = keyword == null ? "" : keyword.trim().toLowerCase();
        List<OnlineUser> matches = entries.stream()
                .map(entry -> toOnlineUser(entry, users.get(entry.userId())))
                .filter(user -> filter.isEmpty()
                        || user.userName().toLowerCase().contains(filter)
                        || user.nickName().toLowerCase().contains(filter)
                        || user.ipAddress().toLowerCase().contains(filter))
                .sorted(Comparator.comparing(OnlineUser::loginTime,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        long start = (pageNum - 1) * pageSize;
        List<OnlineUser> records = start >= matches.size()
                ? List.of()
                : matches.subList((int) start, (int) Math.min(start + pageSize, matches.size()));
        return new PageResult<>(records, matches.size(), pageNum, pageSize);
    }

    public void kickout(String sessionId) {
        requireAdmin();
        String currentToken = StpUtil.getTokenValue();
        for (TokenEntry entry : activeTokens()) {
            if (!entry.sessionId().equals(sessionId)) {
                continue;
            }
            if (entry.token().equals(currentToken)) {
                throw new BusinessException("不能强制下线当前会话");
            }
            StpUtil.kickoutByTokenValue(entry.token());
            return;
        }
        throw new BusinessException("该用户已下线，请刷新列表");
    }

    private void requireAdmin() {
        List<Long> roleIds = userRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getUserId, StpUtil.getLoginIdAsLong()))
                .stream()
                .map(SysUserRole::getRoleId)
                .toList();
        if (roleIds.isEmpty() || roleMapper.selectBatchIds(roleIds).stream()
                .noneMatch(role -> "admin".equals(role.getRoleKey())
                        && "0".equals(role.getStatus()))) {
            throw new BusinessException(403, "仅管理员可以强制下线用户");
        }
    }

    private List<TokenEntry> activeTokens() {
        List<TokenEntry> entries = new ArrayList<>();
        String tokenKeyPrefix = StpUtil.getStpLogic().splicingKeyTokenValue("");
        ScanOptions scan = ScanOptions.scanOptions()
                .match(tokenKeyPrefix + "*")
                .count(200)
                .build();
        try (Cursor<String> tokenKeys = redisTemplate.scan(scan)) {
            while (tokenKeys.hasNext()) {
                String tokenKey = tokenKeys.next();
                if (!tokenKey.startsWith(tokenKeyPrefix)) {
                    continue;
                }
                String token = tokenKey.substring(tokenKeyPrefix.length());
                Object loginId = StpUtil.getLoginIdByToken(token);
                if (loginId == null || StpUtil.getTokenTimeout(token) == -2
                        || StpUtil.getStpLogic().getTokenActiveTimeoutByToken(token) == -2) {
                    continue;
                }
                try {
                    entries.add(new TokenEntry(token, Long.parseLong(loginId.toString()), digest(token)));
                } catch (NumberFormatException ignored) {
                    // Other token types do not belong to sys_user.
                }
            }
        }
        return entries;
    }

    private OnlineUser toOnlineUser(TokenEntry entry, SysUser user) {
        SaSession session = StpUtil.getStpLogic().getTokenSessionByToken(entry.token(), false);
        String userName = user == null ? String.valueOf(entry.userId()) : user.getUserName();
        String nickName = user == null ? userName : user.getNickName();
        String ip = attribute(session, "loginIp", user == null ? "" : user.getLoginIp());
        long loginMillis = session == null ? 0 : session.getCreateTime();
        LocalDateTime loginTime = loginMillis > 0
                ? localTime(loginMillis)
                : user == null ? null : user.getLoginTime();
        long lastActiveMillis = StpUtil.getStpLogic().getTokenLastActiveTime(entry.token());
        LocalDateTime lastActiveTime = lastActiveMillis > 0
                ? localTime(lastActiveMillis)
                : loginTime;
        return new OnlineUser(
                entry.sessionId(),
                entry.userId(),
                userName,
                nickName,
                ip,
                attribute(session, "browser", "未知"),
                attribute(session, "operatingSystem", "未知"),
                loginTime,
                lastActiveTime,
                Objects.equals(entry.token(), StpUtil.getTokenValue()));
    }

    private String attribute(SaSession session, String key, String fallback) {
        Object value = session == null ? null : session.get(key);
        return value == null ? Objects.toString(fallback, "") : value.toString();
    }

    private LocalDateTime localTime(long millis) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault());
    }

    private String digest(String token) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    private boolean available(java.util.function.Supplier<?> check) {
        try {
            return check.get() != null;
        } catch (Exception exception) {
            return false;
        }
    }

    private double percent(double ratio) {
        return ratio < 0 ? 0 : Math.min(100, ratio * 100);
    }

    private record TokenEntry(String token, long userId, String sessionId) {
    }

    public record ServerSnapshot(
            LocalDateTime sampledAt,
            double cpuPercent,
            long heapUsed,
            long heapMax,
            long physicalMemoryUsed,
            long physicalMemoryTotal,
            long diskUsed,
            long diskTotal,
            int threadCount,
            long uptimeMillis,
            int processorCount,
            String osName,
            String javaVersion,
            boolean mysqlHealthy,
            boolean redisHealthy) {
    }

    public record OnlineUser(
            String sessionId,
            long userId,
            String userName,
            String nickName,
            String ipAddress,
            String browser,
            String operatingSystem,
            LocalDateTime loginTime,
            LocalDateTime lastActiveTime,
            boolean currentSession) {
    }
}
