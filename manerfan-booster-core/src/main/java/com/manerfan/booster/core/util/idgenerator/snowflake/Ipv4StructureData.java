package com.manerfan.booster.core.util.idgenerator.snowflake;

import lombok.Getter;
import lombok.ToString;

/**
 * Ipv4StructureData
 *
 * <pre>
 *      生成的带ipv4信息的结构化ID数据
 * </pre>
 *
 * @author Maner.Fan
 * @date 2022/8/6
 */
@ToString(callSuper = true)
public class Ipv4StructureData extends StructureData {
    @Getter
    private final String host;

    public static Ipv4StructureData from(StructureData workerData) {
        return new Ipv4StructureData(workerData);
    }

    private Ipv4StructureData(StructureData workerData) {
        super(workerData.getTimestamp(), workerData.getWorkId(), workerData.getSequence());
        this.host = Ipv4IdGenerator.longToIPv4(workerData.getWorkId())
            .orElseThrow(() -> new IllegalStateException("Cannot Parse a valid IPv4 HostAddress!"));
    }
}
