package com.manerfan.booster.core.database.druid;

import java.util.Date;
import java.util.Objects;

import com.alibaba.druid.pool.DruidDataSourceStatLogger;
import com.alibaba.druid.pool.DruidDataSourceStatLoggerAdapter;
import com.alibaba.druid.pool.DruidDataSourceStatValue;
import com.alibaba.druid.stat.JdbcSqlStatValue;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.annotation.JSONField;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Delegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DruidStatLogger
 *
 * <pre>
 *     Druid统计监控日志
 * </pre>
 *
 * @author Maner.Fan
 * @date 2022/8/6
 */
public class DruidStatLogger extends DruidDataSourceStatLoggerAdapter implements DruidDataSourceStatLogger {
    private final String appName;
    private final long slowSqlMillis;
    private final Logger log;

    public DruidStatLogger(
        String appName,
        String logName,
        long slowSqlMillis) {
        this.appName = appName;
        this.log = LoggerFactory.getLogger(logName);
        this.slowSqlMillis = slowSqlMillis;
    }

    @Override
    public void log(DruidDataSourceStatValue statValue) {
        if (Objects.isNull(statValue.getSqlList())) {
            return;
        }

        statValue.getSqlList().stream()
            .map(this::sqlInfo)
            .filter(this::shouldLog)
            .forEach(this::log);
    }

    /**
     * 判断是否输出日志
     */
    protected boolean shouldLog(SqlInfo sqlInfo) {
        return sqlInfo.isSlowSql() || sqlInfo.isErrorSql();
    }

    /**
     * 日志的具体实现
     */
    protected void log(SqlInfo sqlInfo) {
        log.warn("{}", JSON.toJSONString(sqlInfo));
    }

    private SqlInfo sqlInfo(JdbcSqlStatValue statValue) {
        return SqlInfo.builder()
            .time(new Date())
            .appName(appName)
            .slowSqlMillis(slowSqlMillis)
            .jdbcSqlStatValue(statValue)
            .build();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SqlInfo {
        /**
         * 记录时间
         */
        @JSONField(format="yyyy-MM-dd HH:mm:ss")
        private Date time;

        /**
         * 系统
         */
        private String appName;

        @JSONField(serialize = false)
        private long slowSqlMillis;

        @Delegate
        @JSONField(serialize = false)
        private JdbcSqlStatValue jdbcSqlStatValue;

        public boolean isErrorSql() {
            return jdbcSqlStatValue.getExecuteErrorCount() > 0;
        }

        public boolean isSlowSql() {
            return jdbcSqlStatValue.getExecuteMillisMax() > slowSqlMillis;
        }
    }
}