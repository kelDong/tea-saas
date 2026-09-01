package com.teasaas.common.result;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 统一分页结果（接口协议 1.2：list/total/page/size）
 *
 * @param <T> 记录类型
 */
@Data
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<T> list;
    private Long total;
    private Integer page;
    private Integer size;

    public static <T> PageResult<T> of(List<T> list, Long total, Integer page, Integer size) {
        PageResult<T> result = new PageResult<>();
        result.setList(list);
        result.setTotal(total);
        result.setPage(page);
        result.setSize(size);
        return result;
    }

    public static <T> PageResult<T> empty(Integer page, Integer size) {
        return of(List.of(), 0L, page, size);
    }
}
