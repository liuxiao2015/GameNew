package com.game.api.activity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 奖励 DTO
 *
 * @author GameServer
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RewardDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 奖励类型 (1:货币 2:道具 3:经验)
     */
    private int type;

    /**
     * 道具ID
     */
    private int itemId;

    /**
     * 道具名称
     */
    private String itemName;

    /**
     * 道具图标
     */
    private String itemIcon;

    /**
     * 数量
     */
    private long count;

    /**
     * 品质
     */
    private int quality;
}
