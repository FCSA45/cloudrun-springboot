// API for Cabinet Type management
import request from '@/utils/request'

// 查询柜机类型列表
// Exporting listCabinetType function
export function listCabinetType(query) {
  return request({
    url: '/device/cabinetType/list',
    method: 'get',
    params: query
  })
}
export function addCabinetType(data) {
  return request({
    url: '/device/cabinetType',
    method: 'post',
    data: data
  })
}
export function getCabinetType(id) {
  return request({
    url: '/device/cabinetType/' + id,
    method: 'get'
  })
}

// 修改柜机类型
export function updateCabinetType(data) {
  return request({
    url: '/device/cabinetType',
    method: 'put',
    data: data
  })
}
export function delCabinetType(id) {
  return request({
    url: '/device/cabinetType/' + id,
    method: 'delete'
  })
}

// 查询全部柜机类型列表
export function getCabinetTypeList() {
  return request({
    url: '/device/cabinetType/getCabinetTypeList',
    method: 'get'
  })
}
