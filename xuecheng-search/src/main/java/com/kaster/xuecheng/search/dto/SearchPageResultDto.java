package com.kaster.xuecheng.search.dto;

import com.kaster.xuecheng.base.model.PageResult;
import lombok.Data;
import lombok.ToString;

import java.util.List;

@Data
@ToString
public class SearchPageResultDto<T> extends PageResult<T> {

    //大分类列表
    List<String> mtList;
    //小分类列表
    List<String> stList;

    public SearchPageResultDto(List<T> items, long counts, long page, long pageSize) {
        super(items, counts, page, pageSize);
    }

}
