package com.teasaas.payment.service;

import com.teasaas.common.result.PageResult;
import com.teasaas.payment.dto.RefundApplyDTO;
import com.teasaas.payment.entity.PayRefund;
import com.teasaas.payment.vo.RefundVO;

/**
 * 退款服务（F-PAY-02，接口需求 9.2，P0-5 决策）
 *
 * <p>一期仅整单退（金额=实付）；微信部分原路退（mock 即时成功）；
 * 储值部分退回储值账户（批次 4 钱包落地后开通）；已核销券不退还；
 * 退款成功回补库存（inv_stock_log bizType=3，orderNo 幂等）。</p>
 */
public interface PayRefundService {

    /** 申请退款（R2 全部 / R6/R7 名下门店；订单状态 2/3/4 可退） */
    RefundVO apply(Long orderId, RefundApplyDTO dto);

    /** 退款列表（R2 全部 / R6/R7 名下门店订单） */
    PageResult<PayRefund> page(int page, int size);
}
