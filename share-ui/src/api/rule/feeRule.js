import request from '@/utils/request'

// 查询全部费用规则列表
export function getALLFeeRuleList() {
  return request({
    url: '/device/feeRule/getALLFeeRuleList',
    method: 'get'
  })
}
