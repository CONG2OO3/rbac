package com.company.rbac.dashboard.service;

import com.company.rbac.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class VisitService {

    private static final ZoneId REPORT_ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter DAY_LABEL = DateTimeFormatter.ofPattern("MM-dd");
    private static final DateTimeFormatter MONTH_LABEL = DateTimeFormatter.ofPattern("yyyy-MM");

    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public void recordVisit(long userId) {
        Date today = Date.valueOf(LocalDate.now(REPORT_ZONE));
        jdbcTemplate.update("""
                INSERT INTO sys_visit_daily (visit_date, pv, uv)
                VALUES (?, 1, 0)
                ON DUPLICATE KEY UPDATE pv = pv + 1
                """, today);
        int newVisitor = jdbcTemplate.update("""
                INSERT IGNORE INTO sys_visit_visitor (visit_date, user_id)
                VALUES (?, ?)
                """, today, userId);
        if (newVisitor == 1) {
            jdbcTemplate.update("""
                    UPDATE sys_visit_daily
                    SET uv = uv + 1
                    WHERE visit_date = ?
                    """, today);
        }
    }

    public TrafficSummary traffic(String range) {
        LocalDate today = LocalDate.now(REPORT_ZONE);
        LocalDate firstDay = switch (range) {
            case "7d" -> today.minusDays(6);
            case "30d" -> today.minusDays(29);
            case "1y" -> YearMonth.from(today).minusMonths(11).atDay(1);
            default -> throw new BusinessException("不支持的统计时间范围");
        };

        List<DailyVisit> daily = jdbcTemplate.query("""
                SELECT visit_date, pv, uv
                FROM sys_visit_daily
                WHERE visit_date BETWEEN ? AND ?
                ORDER BY visit_date
                """, (result, index) -> new DailyVisit(
                        result.getDate("visit_date").toLocalDate(),
                        result.getLong("pv"),
                        result.getLong("uv")),
                Date.valueOf(firstDay), Date.valueOf(today));

        Map<LocalDate, DailyVisit> byDay = new LinkedHashMap<>();
        daily.forEach(item -> byDay.put(item.date(), item));
        List<TrendPoint> points = "1y".equals(range)
                ? monthlyPoints(firstDay, today, daily)
                : dailyPoints(firstDay, today, byDay);

        long todayPv = byDay.containsKey(today) ? byDay.get(today).pv() : 0;
        long todayUv = byDay.containsKey(today) ? byDay.get(today).uv() : 0;
        long totalPv = daily.stream().mapToLong(DailyVisit::pv).sum();
        Long distinctUsers = jdbcTemplate.queryForObject("""
                SELECT COUNT(DISTINCT user_id)
                FROM sys_visit_visitor
                WHERE visit_date BETWEEN ? AND ?
                """, Long.class, Date.valueOf(firstDay), Date.valueOf(today));
        return new TrafficSummary(
                range,
                todayPv,
                todayUv,
                totalPv,
                distinctUsers == null ? 0 : distinctUsers,
                points);
    }

    private List<TrendPoint> dailyPoints(
            LocalDate firstDay,
            LocalDate today,
            Map<LocalDate, DailyVisit> byDay) {
        List<TrendPoint> points = new ArrayList<>();
        for (LocalDate day = firstDay; !day.isAfter(today); day = day.plusDays(1)) {
            DailyVisit visit = byDay.get(day);
            points.add(new TrendPoint(
                    day.format(DAY_LABEL),
                    visit == null ? 0 : visit.pv(),
                    visit == null ? 0 : visit.uv()));
        }
        return points;
    }

    private List<TrendPoint> monthlyPoints(
            LocalDate firstDay,
            LocalDate today,
            List<DailyVisit> daily) {
        Map<YearMonth, long[]> byMonth = new LinkedHashMap<>();
        for (YearMonth month = YearMonth.from(firstDay);
                !month.isAfter(YearMonth.from(today));
                month = month.plusMonths(1)) {
            byMonth.put(month, new long[2]);
        }
        daily.forEach(item -> {
            long[] values = byMonth.get(YearMonth.from(item.date()));
            values[0] += item.pv();
        });
        jdbcTemplate.query("""
                SELECT DATE_FORMAT(visit_date, '%Y-%m') AS visit_month,
                       COUNT(DISTINCT user_id) AS uv
                FROM sys_visit_visitor
                WHERE visit_date BETWEEN ? AND ?
                GROUP BY visit_month
                """, result -> {
            YearMonth month = YearMonth.parse(result.getString("visit_month"));
            byMonth.get(month)[1] = result.getLong("uv");
        }, Date.valueOf(firstDay), Date.valueOf(today));
        return byMonth.entrySet().stream()
                .map(item -> new TrendPoint(
                        item.getKey().format(MONTH_LABEL),
                        item.getValue()[0],
                        item.getValue()[1]))
                .toList();
    }

    private record DailyVisit(LocalDate date, long pv, long uv) {
    }

    public record TrendPoint(String label, long pv, long uv) {
    }

    public record TrafficSummary(
            String range,
            long todayPv,
            long todayUv,
            long totalPv,
            long totalUv,
            List<TrendPoint> points) {
    }
}
