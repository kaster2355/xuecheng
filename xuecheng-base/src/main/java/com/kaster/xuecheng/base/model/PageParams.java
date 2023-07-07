package com.kaster.xuecheng.base.model;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * 分页查询分页参数
 */
@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class PageParams {

    @ApiModelProperty("页码")
    private Long pageNo = 1L;
    @ApiModelProperty("每页记录数")
    private Long pageSize = 30L;
}
