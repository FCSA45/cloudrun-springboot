<template>
   <div class="app-container">
      <el-row :gutter="20">
         <el-col :span="6" :xs="24">
            <el-card class="box-card">
               <template v-slot:header>
                 <div class="clearfix">
                   <span>个人信息</span>
                 </div>
               </template>
               <div>
                  <div class="text-center">
                     <userAvatar />
                  </div>
                  <ul class="list-group list-group-striped">
                     <li class="list-group-item">
                        <svg-icon icon-class="user" />用户名称
                        <div class="pull-right">{{ state.user.userName }}</div>
                     </li>
                     <li class="list-group-item">
                        <svg-icon icon-class="phone" />手机号码
                        <div class="pull-right">{{ state.user.phonenumber }}</div>
                     </li>
                     <li class="list-group-item">
                        <svg-icon icon-class="email" />用户邮箱
                        <div class="pull-right">{{ state.user.email }}</div>
                     </li>
                     <li class="list-group-item">
                        <svg-icon icon-class="tree" />所属部门
                        <div class="pull-right" v-if="state.user.dept">{{ state.user.dept.deptName }} / {{ state.postGroup }}</div>
                     </li>
                     <li class="list-group-item">
                        <svg-icon icon-class="peoples" />所属角色
                        <div class="pull-right">{{ state.roleGroup }}</div>
                     </li>
                     <li class="list-group-item">
                        <svg-icon icon-class="date" />创建日期
                        <div class="pull-right">{{ state.user.createTime }}</div>
                     </li>
                  </ul>
               </div>
            </el-card>
         </el-col>
         <el-col :span="18" :xs="24">
            <el-card>
               <template v-slot:header>
                 <div class="clearfix">
                   <span>基本资料</span>
                 </div>
               </template>
               <el-tabs v-model="activeTab">
                  <el-tab-pane label="基本资料" name="userinfo">
                     <userInfo :user="state.user" />
                  </el-tab-pane>
                           <el-tab-pane label="账户信息" name="wallet">
                              <div class="wallet-panel" v-loading="walletState.loading">
                                 <el-empty v-if="!walletState.loading && !walletState.info" description="暂无账户信息" />
                                 <el-descriptions v-else :column="1" border class="wallet-descriptions">
                                    <el-descriptions-item label="用户昵称">
                                       {{ (walletState.info && walletState.info.nickname) || '-' }}
                                    </el-descriptions-item>
                                    <el-descriptions-item label="账户余额">
                                       ￥{{ formatAmount(walletState.info && walletState.info.balance) }}
                                    </el-descriptions-item>
                                    <el-descriptions-item label="冻结金额">
                                       ￥{{ formatAmount(walletState.info && walletState.info.frozenAmount) }}
                                    </el-descriptions-item>
                                 </el-descriptions>
                              </div>
                           </el-tab-pane>
                  <el-tab-pane label="修改密码" name="resetPwd">
                     <resetPwd />
                  </el-tab-pane>
               </el-tabs>
            </el-card>
         </el-col>
      </el-row>
   </div>
</template>

<script setup name="Profile">
import userAvatar from "./userAvatar";
import userInfo from "./userInfo";
import resetPwd from "./resetPwd";
import { getUserProfile, getUserWalletProfile } from "@/api/system/user";

const activeTab = ref("userinfo");
const state = reactive({
  user: {},
   roleGroup: {},
   postGroup: {}
});

const walletState = reactive({
   loading: false,
   info: null
});

function getUser() {
  getUserProfile().then(response => {
    state.user = response.data;
    state.roleGroup = response.roleGroup;
    state.postGroup = response.postGroup;
  });
};

function loadWalletProfile() {
   walletState.loading = true;
   getUserWalletProfile()
      .then(response => {
         walletState.info = response.data || null;
      })
      .finally(() => {
         walletState.loading = false;
      });
}

const formatAmount = (value) => {
   const amount = Number(value);
   return Number.isFinite(amount) ? amount.toFixed(2) : "0.00";
};

getUser();
loadWalletProfile();
</script>

<style scoped>
.wallet-panel {
   min-height: 180px;
   padding: 12px 16px;
}

.wallet-descriptions {
   margin-top: 8px;
}
</style>
