package com.share.order.api.factory;

import com.share.common.core.domain.R;
import com.share.order.api.api.RemoteOrderInfoService;
import com.share.order.api.domain.OrderInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class RemoteOrderInfoFallbackFactory implements FallbackFactory<RemoteOrderInfoService>
{
    private static final Logger log = LoggerFactory.getLogger(RemoteOrderInfoFallbackFactory.class);

    @Override
    public RemoteOrderInfoService create(Throwable throwable)
    {
        log.error("订单服务调用失败:{}", throwable.getMessage());
        return new RemoteOrderInfoService()
        {

            @Override
            public R<OrderInfo> getNoFinishOrder(Long userId, String source) {
                return R.fail("获取用户未完成订单失败:" + throwable.getMessage());
            }

            @Override
            public R<OrderInfo> getByOrderNo(String orderNo, String source) {
                return R.fail("获取用户订单编号失败:" + throwable.getMessage());
            }
        };
    }
}
