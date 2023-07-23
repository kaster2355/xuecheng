package com.kaster.xuecheng.content.feignclient;

import com.kaster.xuecheng.content.model.dto.CourseIndex;
import feign.hystrix.FallbackFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SearchServiceClientFallbackFactory implements FallbackFactory<SearchServiceClient> {
    @Override
    public SearchServiceClient create(Throwable throwable) {
        return courseIndex -> {
            log.debug("调用搜索发生熔断走降级方法,熔断异常:{}", throwable.getMessage());
            return false;
        };
    }
}
